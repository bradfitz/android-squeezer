package uk.org.ngo.squeezer.service;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.client.transport.HttpClientTransport;
import org.cometd.client.transport.MessageClientTransport;
import org.cometd.client.transport.TransportListener;
import org.cometd.common.TransportException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpDestination;
import org.eclipse.jetty.client.Origin;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.http.HttpDestinationOverHTTP;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpStatus;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class HttpStreamingTransport extends HttpClientTransport implements MessageClientTransport {
    public static final String NAME = "streaming";
    public static final String PREFIX = "http-streaming.json";
    public static final String MAX_BUFFER_SIZE_OPTION = "maxBufferSize";
    private static final String TAG = HttpStreamingTransport.class.getSimpleName();

    private ScheduledExecutorService _scheduler;
    private boolean _shutdownScheduler;

    private final Delegate _delegate;
    private TransportListener _listener;

    private final HttpClient _httpClient;
    private final List<Request> _requests = new ArrayList<>();
    private volatile boolean _aborted;
    private volatile int _maxBufferSize;
    private volatile boolean _appendMessageType;
    private volatile CookieManager _cookieManager;

    public HttpStreamingTransport(Map<String, Object> options, HttpClient httpClient) {
        this(null, options, httpClient);
    }

    public HttpStreamingTransport(String url, Map<String, Object> options, HttpClient httpClient) {
        super(NAME, url, options);
        _httpClient = httpClient;
        _delegate = new Delegate();
        setOptionPrefix(PREFIX);
    }

    @Override
    public void setMessageTransportListener(TransportListener listener) {
        _listener = listener;
    }

    @Override
    public boolean accept(String bayeuxVersion) {
        return true;
    }

    @Override
    public void init() {
        super.init();

        _aborted = false;

        long defaultMaxNetworkDelay = _httpClient.getIdleTimeout();
        if (defaultMaxNetworkDelay <= 0)
            defaultMaxNetworkDelay = 10000;
        setMaxNetworkDelay(defaultMaxNetworkDelay);

        _maxBufferSize = getOption(MAX_BUFFER_SIZE_OPTION, 1024 * 1024);

        Pattern uriRegexp = Pattern.compile("(^https?://(((\\[[^\\]]+\\])|([^:/\\?#]+))(:(\\d+))?))?([^\\?#]*)(.*)?");
        Matcher uriMatcher = uriRegexp.matcher(getURL());
        if (uriMatcher.matches()) {
            String afterPath = uriMatcher.group(9);
            _appendMessageType = afterPath == null || afterPath.trim().length() == 0;
        }
        _cookieManager = new CookieManager(getCookieStore(), CookiePolicy.ACCEPT_ALL);

        if (_scheduler == null) {
            _shutdownScheduler = true;
            int threads = Math.max(1, Runtime.getRuntime().availableProcessors() / 4);
            ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(threads);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scheduler.setRemoveOnCancelPolicy(true);
            }
            _scheduler = scheduler;
        }

    }

    @Override
    public void abort() {
        List<Request> requests = new ArrayList<>();
        synchronized (this) {
            _aborted = true;
            requests.addAll(_requests);
            _requests.clear();
        }
        for (Request request : requests) {
            request.abort(new Exception("Transport " + this + " aborted"));
        }
        shutdownScheduler();
    }

    @Override
    public void terminate()
    {
        shutdownScheduler();
        super.terminate();
    }

    private void shutdownScheduler()
    {
        if (_shutdownScheduler)
        {
            _shutdownScheduler = false;
            _scheduler.shutdownNow();
            _scheduler = null;
        }
    }

    @Override
    public void send(final TransportListener listener, final List<Message.Mutable> messages) {
        if (!_delegate.connected) connect(getURL(), listener, messages);
        if (!_delegate.connected) return;

        if (Channel.META_CONNECT.equals(messages.get(0).getChannel()) ||
                Channel.META_SUBSCRIBE.equals(messages.get(0).getChannel()) ||
                Channel.META_HANDSHAKE.equals(messages.get(0).getChannel())) {
            delegateSend(listener, messages);
        } else {
            transportSend(listener, messages);
        }
    }

    protected void connect(String urlString, TransportListener listener, List<Message.Mutable> messages) {
        try {
            URL url = new URL(urlString);
            String host = url.getHost();
            int port = url.getPort();
            _delegate.connect(host, port);
        } catch (IOException e) {
            listener.onFailure(e, messages);
        }
    }

    private void delegateSend(final TransportListener listener, final List<Message.Mutable> messages) {
        _delegate.registerMessages(listener, messages);
        try {
            String content = generateJSON(messages);

            // The onSending() callback must be invoked before the actual send
            // otherwise we may have a race condition where the response is so
            // fast that it arrives before the onSending() is called.
            //Log.v(TAG,"Sending messages " + content);
            listener.onSending(messages);

            _delegate.send(content);
        } catch (Throwable x) {
            _delegate.fail(x, "Exception");
        }
    }

    private void transportSend(final TransportListener listener, final List<Message.Mutable> messages) {
        String url = getURL();
        final URI uri = URI.create(url);
        if (_appendMessageType && messages.size() == 1) {
            Message.Mutable message = messages.get(0);
            if (message.isMeta()) {
                String type = message.getChannel().substring(Channel.META.length());
                if (url.endsWith("/"))
                    url = url.substring(0, url.length() - 1);
                url += type;
            }
        }

        final Request request = _httpClient.newRequest(url).method(HttpMethod.POST);
        request.header(HttpHeader.CONTENT_TYPE.asString(), "text/json;charset=UTF-8");

        StringBuilder builder = new StringBuilder();
        for (HttpCookie cookie : getCookieStore().get(uri)) {
            builder.setLength(0);
            builder.append(cookie.getName()).append("=").append(cookie.getValue());
            request.header(HttpHeader.COOKIE.asString(), builder.toString());
        }

        String content = generateJSON(messages);
        //Log.v(TAG,"Sending messages " + content);
        request.content(new StringContentProvider(content));

        customize(request);

        synchronized (this) {
            if (_aborted)
                throw new IllegalStateException("Aborted");
            _requests.add(request);
        }

        request.listener(new Request.Listener.Adapter() {
            @Override
            public void onHeaders(Request request) {
                listener.onSending(messages);
            }
        });

        long maxNetworkDelay = getMaxNetworkDelay();

        // Set the idle timeout for this request larger than the total timeout
        // so there are no races between the two timeouts
        request.idleTimeout(maxNetworkDelay * 2, TimeUnit.MILLISECONDS);
        request.timeout(maxNetworkDelay, TimeUnit.MILLISECONDS);
        request.send(new BufferingResponseListener(_maxBufferSize) {
            @Override
            public boolean onHeader(Response response, HttpField field) {
                HttpHeader header = field.getHeader();
                if (header != null && (header == HttpHeader.SET_COOKIE || header == HttpHeader.SET_COOKIE2)) {
                    // We do not allow cookies to be handled by HttpClient, since one
                    // HttpClient instance is shared by multiple BayeuxClient instances.
                    // Instead, we store the cookies in the BayeuxClient instance.
                    Map<String, List<String>> cookies = new HashMap<>(1);
                    cookies.put(field.getName(), Collections.singletonList(field.getValue()));
                    storeCookies(uri, cookies);
                    return false;
                }
                return true;
            }

            private void storeCookies(URI uri, Map<String, List<String>> cookies) {
                try {
                    _cookieManager.put(uri, cookies);
                } catch (IOException x) {
                    Log.w(TAG, "", x);
                }
            }

            @Override
            public void onComplete(Result result) {
                synchronized (HttpStreamingTransport.this) {
                    _requests.remove(result.getRequest());
                }

                if (result.isFailed()) {
                    listener.onFailure(result.getFailure(), messages);
                    return;
                }

                Response response = result.getResponse();
                int status = response.getStatus();
                if (status == HttpStatus.OK_200) {
                    String content = getContentAsString();
                    if (content != null && content.length() > 0) {
                        try {
                            List<Message.Mutable> messages = parseMessages(content);
                            //Log.v(TAG, "Received messages " + messages);
                            for (Message.Mutable message : messages) {
                                // LMS echoes the data field in the publish response for messages to the
                                // slim/unsubscribe channel.
                                // This causes the comet libraries to decide the message is not a publish response.
                                // We remove the data field for susch messages, to have them correctly recognized
                                // as publish responses.
                                if (message.getChannel() != null && message.getChannel().startsWith("/slim/")) {
                                    message.remove(Message.DATA_FIELD);
                                }

                                if (message.isSuccessful() && Channel.META_DISCONNECT.equals(message.getChannel())) {
                                    _delegate.disconnect("Disconnect");
                                }
                            }
                            listener.onMessages(messages);
                        } catch (ParseException x) {
                            listener.onFailure(x, messages);
                        }
                    } else {
                        Map<String, Object> failure = new HashMap<>(2);
                        // Convert the 200 into 204 (no content)
                        failure.put("httpCode", 204);
                        TransportException x = new TransportException(failure);
                        listener.onFailure(x, messages);
                    }
                } else {
                    Map<String, Object> failure = new HashMap<>(2);
                    failure.put("httpCode", status);
                    TransportException x = new TransportException(failure);
                    listener.onFailure(x, messages);
                }
            }
        });
    }

    private static void sendText(PrintWriter writer, String json, HttpFields customHeaders) {
        StringBuilder msg = new StringBuilder("POST /cometd HTTP/1.1\r\n" +
                HttpHeader.CONTENT_TYPE.asString() + ": text/json;charset=UTF-8\r\n" +
                HttpHeader.CONTENT_LENGTH.asString() + ": " + json.length() + "\r\n");

        for (HttpField httpField : customHeaders) {
            if (httpField.getHeader() != HttpHeader.ACCEPT_ENCODING) {
                msg.append(httpField.getName()).append(": ").append(httpField.getValue()).append("\r\n");
            }
        }
        msg.append("\r\n").append(json);
        //Log.v(TAG,"sendtext: " + msg);
        writer.print(msg.toString());
        writer.flush();
    }

    private class Delegate {
        private final Socket socket;
        private final HttpFields headers;
        private PrintWriter writer;
        private boolean connected;

        private final Map<String, Exchange> _exchanges = new ConcurrentHashMap<>();
        private Map<String, Object> _advice;
        private long interval;

        public Delegate() {
            socket = new Socket();
            Request request = _httpClient.newRequest(getURL());
            customize(request);
            headers = request.getHeaders();
            headers.add(getHostField(request));
        }

        private HttpField getHostField(Request request) {
            String scheme = request.getScheme().toLowerCase(Locale.ENGLISH);
            if (!HttpScheme.HTTP.is(scheme) && !HttpScheme.HTTPS.is(scheme))
                throw new IllegalArgumentException("Invalid protocol " + scheme);

            String host = request.getHost().toLowerCase(Locale.ENGLISH);
            Origin origin = new Origin(scheme, host, request.getPort());
            HttpDestination destination = new HttpDestinationOverHTTP(_httpClient, origin);
            return destination.getHostField();
        }

        public void connect(String host, int port) throws IOException {
            socket.connect(new InetSocketAddress(host, port), 4000); // TODO use proper timeout
            connected = true;
            writer = new PrintWriter(socket.getOutputStream());
            new ListeningThread(this, socket.getInputStream()).start();
        }

        private void registerMessages(TransportListener listener, List<Message.Mutable> messages) {
            synchronized (this) {
                for (Message.Mutable message : messages)
                    registerMessage(message, listener);
            }
        }

        private void registerMessage(final Message.Mutable message, final TransportListener listener) {
            // Calculate max network delay
            long maxNetworkDelay = getMaxNetworkDelay();
            if (Channel.META_CONNECT.equals(message.getChannel())) {
                Map<String, Object> advice = message.getAdvice();
                if (advice == null)
                    advice = _advice;
                if (advice != null) {
                    Object timeout = advice.get(Message.TIMEOUT_FIELD);
                    if (timeout instanceof Number)
                        maxNetworkDelay += ((Number) timeout).intValue();
                    else if (timeout != null)
                        maxNetworkDelay += Integer.parseInt(timeout.toString());
                }
            }

            // Schedule a task to expire if the maxNetworkDelay elapses
            final long expiration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + maxNetworkDelay;
            ScheduledFuture<?> task = _scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
                    long delay = now - expiration;
                    if (delay > 5000) // TODO: make the max delay a parameter ?
                        Log.d(TAG, "Message " + message + " expired " + delay + " ms too late");
                    //Log.d(TAG,"Expiring message " + message);
                    fail(new TimeoutException(), "Expired");
                }
            }, maxNetworkDelay, TimeUnit.MILLISECONDS);

            // Register the exchange
            // Message responses must have the same messageId as the requests

            Exchange exchange = new Exchange(message, listener, task);
            //Log.d(TAG, "Registering " + exchange);
            Object existing = _exchanges.put(message.getId(), exchange);
            // Paranoid check
            if (existing != null)
                throw new IllegalStateException();
        }

        private Exchange deregisterMessage(Message.Mutable message) {
            Exchange exchange = (message.getId() != null) ? _exchanges.remove(message.getId()) : null;
            //Log.d(TAG, "Deregistering " + exchange + " for message " + message);
            if (exchange != null)
                exchange.task.cancel(false);

            return exchange;
        }

        /**
         * Workaround missing fields in replies from LMS
         * LMS does not put ID on all replies. For such a message, this method tries to find the
         * message in _exchanges which this message is a reply to.
         * <ol>
         *     <li>For messages with channel META_CONNECT, META_HANDSHAKE or META_SUBSCRIBE we
         *     look for a message with that channel (assuming that there is only one such message.
         *     </li>
         *     <li>For messages without channel we check if it has advice action, in which case we look for
         *     META_CONNECT and META_HANDSHAKE</li>
         * </ol>
         */
        private void fixMessage(Message.Mutable message) {
            String channel = message.getChannel();

            if (Channel.META_CONNECT.equals(channel) || Channel.META_HANDSHAKE.equals(channel) || Channel.META_SUBSCRIBE.equals(channel)) {
                for (Exchange exchange : _exchanges.values()) {
                    if (channel.equals(exchange.message.getChannel())) {
                        message.setId(exchange.message.getId());
                        break;
                    }
                }
            } else
            if (channel == null && (getAdviceAction(message.getAdvice()) != null)) {
                for (Exchange exchange : _exchanges.values()) {
                    channel = exchange.message.getChannel();
                    if (Channel.META_CONNECT.equals(channel) || Channel.META_HANDSHAKE.equals(channel)) {
                        message.setId(exchange.message.getId());
                        message.setChannel(channel);
                        break;
                    }
                }
            }
        }

        public void send(String content) {
            PrintWriter session;
            synchronized (this) {
                session = writer;
            }
            try {
                if (session == null)
                    throw new IOException("Unconnected");

                sendText(session, content, headers);
            } catch (Throwable x) {
                fail(x, "Exception");
            }
        }

        private void onData(String data) {
            try {
                List<Message.Mutable> messages = parseMessages (data);
                //Log.v(TAG,"Received messages " + data);
                onMessages(messages);
            } catch (ParseException x) {
                fail(x, "Exception");
            }
        }

        private void onMessages(List<Message.Mutable> messages) {
            for (Message.Mutable message : messages) {
                if (isReply(message)) {
                    if (message.getId() == null) {
                        fixMessage(message);
                    }

                    // If the server sends an interval with the handshake response, set it to zero
                    // so we can send the connect message immediately.
                    // But store the interval so we can use it for subsequent connect messages, if
                    // the server does not supply an interval in the connect response.
                    if (Channel.META_HANDSHAKE.equals(message.getChannel()) && message.isSuccessful()) {
                        Map<String, Object> advice = message.getAdvice();
                        if (advice != null && advice.containsKey(Message.INTERVAL_FIELD)) {
                            interval = ((Number)advice.get(Message.INTERVAL_FIELD)).longValue();
                            if (interval > 0) {
                                advice.put(Message.INTERVAL_FIELD, 0);
                            }
                        }
                    }

                    // Remembering the advice must be done before we notify listeners
                    // otherwise we risk that listeners send a connect message that does
                    // not take into account the timeout to calculate the maxNetworkDelay
                    if (Channel.META_CONNECT.equals(message.getChannel()) && message.isSuccessful()) {
                        Map<String, Object> advice = message.getAdvice();
                        if (advice != null) {
                            // Remember the advice so that we can properly calculate the max network delay
                            if (advice.get(Message.TIMEOUT_FIELD) != null)
                                _advice = advice;
                        } else {
                            // If the server doesn't send an interval with the connect response, but
                            // we have one from the handshake response, then use that to avoid sending
                            // connect messages continuously.
                            if (interval > 0) {
                                message.put(Message.ADVICE_FIELD, Collections.singletonMap(Message.INTERVAL_FIELD, interval));
                            }
                        }
                    }

                    Exchange exchange = deregisterMessage(message);
                    if (exchange != null) {
                        exchange.listener.onMessages(Collections.singletonList(message));
                    } else if (message.containsKey("error")) {
                        failMessages(null);

                        // We send messages with no channel to the handshake listener
                        if (message.getChannel() == null) {
                            message.setChannel(Channel.META_HANDSHAKE);
                        }

                        _listener.onFailure(null, Collections.singletonList(message));
                    } else {
                        // If the exchange is missing, then the message has expired, and we do not notify
                        Log.d(TAG, "Could not find request for reply " +  message);
                    }
                } else {
                    _listener.onMessages(Collections.singletonList(message));
                }
            }
        }

        private void fail(Throwable failure, String reason) {
            disconnect(reason);
            failMessages(failure);
        }

        private void failMessages(Throwable cause) {
            List<Message.Mutable> messages = new ArrayList<>(1);
            for (Exchange exchange : new ArrayList<>(_exchanges.values())) {
                Message.Mutable message = exchange.message;
                if (deregisterMessage(message) == exchange) {
                    messages.add(message);
                    exchange.listener.onFailure(cause, messages);
                    messages.clear();
                }
            }
        }

        private void disconnect(String reason) {
            if (connected)
                shutdown(reason);
        }

        private void shutdown(String reason) {
            connected = false;
            writer = null;
            Log.v(TAG, "Closing socket, reason: " + reason);
            try {
                socket.close();
            } catch (IOException x) {
                Log.w(TAG, "Could not close socket", x);
            }
        }
    }

    private static class Exchange {
        private final Message.Mutable message;
        private final TransportListener listener;
        private final ScheduledFuture<?> task;

        public Exchange(Message.Mutable message, TransportListener listener, ScheduledFuture<?> task) {
            this.message = message;
            this.listener = listener;
            this.task = task;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " " + message;
        }
    }

    private static class ListeningThread extends Thread {
        private Delegate delegate;
        private final BufferedReader reader;

        public ListeningThread(Delegate delegate, InputStream inputStream) throws UnsupportedEncodingException {
            this.delegate = delegate;
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        }

        @Override
        public void run() {
            while (delegate.connected) {
                try {
                    int status = parseHttpStatus(readLine());

                    boolean chunked = false;
                    int contentSize = 0;
                    String headerLine;
                    while (!"".equals(headerLine = readLine())) {
                        if ("Transfer-Encoding: chunked".equals(headerLine))
                            chunked = true;
                        int pos = headerLine.indexOf("Content-Length: ");
                        if (pos == 0) {
                            contentSize = Integer.parseInt(headerLine.substring("Content-Length: ".length()));
                        }
                    }

                    if (!chunked) {
                        String content = read(contentSize);
                        if (content.length() > 0) {
                            if (status == HttpStatus.OK_200) {
                                delegate.onData(content);
                            }
                        } else {
                            Map<String, Object> failure = new HashMap<>(2);
                            // Convert the 200 into 204 (no content)
                            failure.put("httpCode", HttpStatus.NO_CONTENT_204);
                            TransportException x = new TransportException(failure);
                            delegate.fail(x, "No content");
                        }
                    } else {
                        while (!"0".equals(readLine())) {
                            String data = readLine();
                            if (status == HttpStatus.OK_200) {
                                delegate.onData(data);
                            }
                        }
                        readLine();//Read final/empty chunk
                        delegate.disconnect("End of chunks");
                    }

                    if (status != HttpStatus.OK_200) {
                        Map<String, Object> failure = new HashMap<>(2);
                        failure.put("httpCode", status);
                        TransportException x = new TransportException(failure);
                        delegate.fail(x, "Unexpected HTTP status code");
                    }
                } catch (IOException e) {
                    if (delegate.connected) {
                        delegate.fail(e, "Server disconnected");
                    }
                    return;
                }
            }
        }

        Pattern httpStatusLinePattern = Pattern.compile("HTTP/1.1 (\\d{3}) \\p{all}+");
        private int parseHttpStatus(String statusLine) {
            Matcher m = httpStatusLinePattern.matcher(statusLine);
            try {
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            } catch (NumberFormatException e) {
            }
            return -1;
        }

        private String readLine() throws IOException {
            String inputLine = reader.readLine();
            if (inputLine == null) {
                throw new EOFException();
            }
            return inputLine;
        }

        private String read(int size) throws IOException {
            char[] buffer = new char[size];
            int length = reader.read(buffer);
            if (length != size) {
                throw new EOFException("Expected " + size + " characters, but got " + length);
            }
            return new String(buffer);
        }
    }


    private static String getAdviceAction(Map<String, Object> advice)
    {
        String action = null;
        if (advice != null && advice.containsKey(Message.RECONNECT_FIELD))
            action = (String)advice.get(Message.RECONNECT_FIELD);
        return action;
    }

    private static boolean isReply(Message message) {
        return message.isMeta() || message.isPublishReply();
    }


    protected void customize(Request request)
    {
    }

}
