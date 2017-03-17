package uk.org.ngo.squeezer.service;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;

import org.apache.http.impl.client.DefaultHttpClient;
import org.cometd.bayeux.Channel;
import org.cometd.bayeux.Message;
import org.cometd.client.transport.HttpClientTransport;
import org.cometd.client.transport.MessageClientTransport;
import org.cometd.client.transport.TransportListener;
import org.cometd.common.TransportException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
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

    private Delegate _delegate = new Delegate();
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

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
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
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void send(final TransportListener listener, final List<Message.Mutable> messages) {
        if (!_delegate.connected) connect(getURL(), listener, messages);
        if (!_delegate.connected) return;

        if (Channel.META_CONNECT.equals(messages.get(0).getChannel()) ||
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

    private void delegateSend(final TransportListener listener, final List<Message.Mutable> messages)
    {
        _delegate.registerMessages(listener, messages);
        try {
            String content = generateJSON(messages);

            // The onSending() callback must be invoked before the actual send
            // otherwise we may have a race condition where the response is so
            // fast that it arrives before the onSending() is called.
            if (logger.isDebugEnabled())
                logger.debug("Sending messages {}", content);
            listener.onSending(messages);

            _delegate.send(content);
        } catch (Throwable x) {
            _delegate.fail(x, "Exception");
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void transportSend(final TransportListener listener, final List<Message.Mutable> messages)
    {
        String url = getURL();
        final URI uri = URI.create(url);
        if (_appendMessageType && messages.size() == 1)
        {
            Message.Mutable message = messages.get(0);
            if (message.isMeta())
            {
                String type = message.getChannel().substring(Channel.META.length());
                if (url.endsWith("/"))
                    url = url.substring(0, url.length() - 1);
                url += type;
            }
        }

        final Request request = _httpClient.newRequest(url).method(HttpMethod.POST);
        request.header(HttpHeader.CONTENT_TYPE.asString(), "application/json;charset=UTF-8");

        StringBuilder builder = new StringBuilder();
        for (HttpCookie cookie : getCookieStore().get(uri))
        {
            builder.setLength(0);
            builder.append(cookie.getName()).append("=").append(cookie.getValue());
            request.header(HttpHeader.COOKIE.asString(), builder.toString());
        }

        request.content(new StringContentProvider(generateJSON(messages)));

        customize(request);

        synchronized (this)
        {
            if (_aborted)
                throw new IllegalStateException("Aborted");
            _requests.add(request);
        }

        request.listener(new Request.Listener.Adapter()
        {
            @Override
            public void onHeaders(Request request)
            {
                listener.onSending(messages);
            }
        });

        long maxNetworkDelay = getMaxNetworkDelay();

        // Set the idle timeout for this request larger than the total timeout
        // so there are no races between the two timeouts
        request.idleTimeout(maxNetworkDelay * 2, TimeUnit.MILLISECONDS);
        request.timeout(maxNetworkDelay, TimeUnit.MILLISECONDS);
        request.send(new BufferingResponseListener(_maxBufferSize)
        {
            @Override
            public boolean onHeader(Response response, HttpField field)
            {
                HttpHeader header = field.getHeader();
                if (header != null && (header == HttpHeader.SET_COOKIE || header == HttpHeader.SET_COOKIE2))
                {
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

            private void storeCookies(URI uri, Map<String, List<String>> cookies)
            {
                try
                {
                    _cookieManager.put(uri, cookies);
                }
                catch (IOException x)
                {
                    if (logger.isDebugEnabled())
                        logger.debug("", x);
                }
            }

            @Override
            public void onComplete(Result result)
            {
                synchronized (HttpStreamingTransport.this)
                {
                    _requests.remove(result.getRequest());
                }

                if (result.isFailed())
                {
                    listener.onFailure(result.getFailure(), messages);
                    return;
                }

                Response response = result.getResponse();
                int status = response.getStatus();
                if (status == HttpStatus.OK_200)
                {
                    String content = getContentAsString();
                    if (content != null && content.length() > 0)
                    {
                        try
                        {
                            List<Message.Mutable> messages = parseMessages(content);
                            if (logger.isDebugEnabled())
                                logger.debug("Received messages {}", messages);
                            listener.onMessages(messages);
                        }
                        catch (ParseException x)
                        {
                            listener.onFailure(x, messages);
                        }
                    }
                    else
                    {
                        Map<String, Object> failure = new HashMap<>(2);
                        // Convert the 200 into 204 (no content)
                        failure.put("httpCode", 204);
                        TransportException x = new TransportException(failure);
                        listener.onFailure(x, messages);
                    }
                }
                else
                {
                    Map<String, Object> failure = new HashMap<>(2);
                    failure.put("httpCode", status);
                    TransportException x = new TransportException(failure);
                    listener.onFailure(x, messages);
                }
            }
        });
    }


    private class Delegate {
        private Socket socket;
        private PrintWriter writer;
        private boolean connected;

        private final Map<String, Exchange> _exchanges = new ConcurrentHashMap<>();
        private boolean _connected;
        private boolean _disconnected;
        private Map<String, Object> _advice;

        public Delegate() {
            new DefaultHttpClient();
            socket = new Socket();

        }

        public void connect(String host, int port) throws IOException {
            Log.i(TAG, "connect(" + host + ", " + port + ")");
            socket.connect(new InetSocketAddress(host, port), 4000); // TODO use proper timeout
            connected = true;
            Log.i(TAG, "connected to: " + host + ":" + port);
            writer = new PrintWriter(socket.getOutputStream());
            new ListeningThread(this, socket.getInputStream()).start();
        }

        private void registerMessages(TransportListener listener, List<Message.Mutable> messages) {
            synchronized (this) {
                for (Message.Mutable message : messages)
                    registerMessage(message, listener);
            }
        }

        private void registerMessage(final Message.Mutable message, final TransportListener listener)
        {
            // Calculate max network delay
            long maxNetworkDelay = getMaxNetworkDelay();
            if (Channel.META_CONNECT.equals(message.getChannel()))
            {
                Map<String, Object> advice = message.getAdvice();
                if (advice == null)
                    advice = _advice;
                if (advice != null)
                {
                    Object timeout = advice.get("timeout");
                    if (timeout instanceof Number)
                        maxNetworkDelay += ((Number)timeout).intValue();
                    else if (timeout != null)
                        maxNetworkDelay += Integer.parseInt(timeout.toString());
                }
                _connected = true;
            }

            // Schedule a task to expire if the maxNetworkDelay elapses
            final long expiration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime()) + maxNetworkDelay;
            ScheduledFuture<?> task = _scheduler.schedule(new Runnable()
            {
                @Override
                public void run()
                {
                    long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
                    long delay = now - expiration;
                    if (logger.isDebugEnabled())
                    {
                        if (delay > 5000) // TODO: make the max delay a parameter ?
                            logger.debug("Message {} expired {} ms too late", message, delay);
                        logger.debug("Expiring message {}", message);
                    }
                    fail(new TimeoutException(), "Expired");
                }
            }, maxNetworkDelay, TimeUnit.MILLISECONDS);

            // Register the exchange
            // Message responses must have the same messageId as the requests

            Exchange exchange = new Exchange(message, listener, task);
            if (logger.isDebugEnabled())
                logger.debug("Registering {}", exchange);
            Object existing = _exchanges.put(message.getId(), exchange);
            // Paranoid check
            if (existing != null)
                throw new IllegalStateException();
        }

        private Exchange deregisterMessage(Message message)
        {
            Exchange exchange = null;
            // TODO document this
            if (message.getId() != null) {
                exchange = _exchanges.remove(message.getId());
            } else {
                String channel = message.getChannel();
                if (Channel.META_CONNECT.equals(channel) || Channel.META_HANDSHAKE.equals(channel)) {
                    for (Exchange e : _exchanges.values()) {
                        if (channel.equals(e.message.getChannel())) {
                            exchange = _exchanges.remove(e.message.getId());
                            break;
                        }
                    }
                }
            }
            if (Channel.META_CONNECT.equals(message.getChannel()))
                _connected = false;
            else if (Channel.META_DISCONNECT.equals(message.getChannel()))
                _disconnected = true;

            if (logger.isDebugEnabled())
                logger.debug("Deregistering {} for message {}", exchange, message);

            if (exchange != null)
                exchange.task.cancel(false);

            return exchange;
        }

        public void send(String content)
        {
            Socket session;
            synchronized (this)
            {
                session = socket;
            }
            try
            {
                if (session == null)
                    throw new IOException("Unconnected");

                sendText(content);
            }
            catch (Throwable x)
            {
                fail(x, "Exception");
            }
        }

        public void sendText(String json) {
            String msg = "POST /cometd HTTP/1.1\r\n" +
                    "Content-Type: application/json;charset=UTF-8\r\n" +
                    "Content-Length: " + json.length() + "\r\n" +
                    "\r\n" +
                    json;
            Log.i(TAG, "send:\n" + msg);
            writer.print(msg);
            writer.flush();
        }

        protected void onData(String data) {
            try {
                List<Message.Mutable> messages = parseMessages(data);
                if (isAttached()) {
                    if (logger.isDebugEnabled())
                        logger.debug("Received messages {}", data);
                    onMessages(messages);
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug("Discarded messages {}", data);
                }
            } catch (ParseException x) {
                fail(x, "Exception");
            }
        }

        protected void onMessages(List<Message.Mutable> messages) {
            for (Message.Mutable message : messages) {
                if (isReply(message)) {
                    // Remembering the advice must be done before we notify listeners
                    // otherwise we risk that listeners send a connect message that does
                    // not take into account the timeout to calculate the maxNetworkDelay
                    if (Channel.META_CONNECT.equals(message.getChannel()) && message.isSuccessful()) {
                        Map<String, Object> advice = message.getAdvice();
                        if (advice != null) {
                            // Remember the advice so that we can properly calculate the max network delay
                            if (advice.get(Message.TIMEOUT_FIELD) != null)
                                _advice = advice;
                        }
                    }

                    Exchange exchange = deregisterMessage(message);
                    if (exchange != null) {
                        exchange.listener.onMessages(Collections.singletonList(message));
                    } else {
                        // If the exchange is missing, then the message has expired, and we do not notify
                        if (logger.isDebugEnabled())
                            logger.debug("Could not find request for reply {}", message);
                    }

                    if (_disconnected && !_connected)
                        disconnect("Disconnect");
                } else {
                    _listener.onMessages(Collections.singletonList(message));
                }
            }
        }

        protected void fail(Throwable failure, String reason) {
            disconnect(reason);
            failMessages(failure);
        }

        protected void failMessages(Throwable cause) {
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
            if (detach())
                shutdown(reason);
        }

        private boolean isAttached() {
            synchronized (HttpStreamingTransport.this) {
                return this == _delegate;
            }
        }

        private boolean detach() {
            synchronized (HttpStreamingTransport.this) {
                boolean attached = this == _delegate;
                if (attached)
                    _delegate = null;
                return attached;
            }
        }

        protected void shutdown(String reason) {
            Socket session;
            synchronized (this) {
                session = socket;
                close();
            }
            if (session != null) {
                Log.i(TAG, "Closing socket, reason: " + reason);
                try {
                    session.close();
                } catch (IOException x) {
                    Log.w("Could not close socket", x);
                }
            }
        }

        protected void close() {
            synchronized (this) {
                socket = null;
                writer = null;
            }
        }
    }

    private static class Exchange
    {
        private final Message.Mutable message;
        private final TransportListener listener;
        private final ScheduledFuture<?> task;

        public Exchange(Message.Mutable message, TransportListener listener, ScheduledFuture<?> task)
        {
            this.message = message;
            this.listener = listener;
            this.task = task;
        }

        @Override
        public String toString()
        {
            return getClass().getSimpleName() + " " + message;
        }
    }

    private class ListeningThread extends Thread {
        private Delegate delegate;
        private final BufferedReader reader;

        public ListeningThread(Delegate delegate, InputStream inputStream) throws UnsupportedEncodingException {
            this.delegate = delegate;
            reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        }

        @Override
        public void run() {
            Log.i(TAG, "Listening thread started");
            boolean statusOk = false;
            boolean chunked = false;
            int contentSize = 0;
            while (true) {
                try {
                    if (!chunked) {
                        String statusLine = readLine();
                        Log.i(TAG, "status: " + statusLine);
                        statusOk = "HTTP/1.1 200 OK".equals(statusLine);
                    }
                    if (statusOk) {
                        if (!chunked) {
                            String headerLine;
                            while (!"".equals(headerLine = readLine())) {
                                Log.i(TAG, "header: " + headerLine);
                                if ("Transfer-Encoding: chunked".equals(headerLine))
                                    chunked = true;
                                int pos = headerLine.indexOf("Content-Length: ");
                                if (pos == 0) {
                                    contentSize = Integer.parseInt(headerLine.substring("Content-Length: ".length()));
                                }
                            }
                        }
                        if (!chunked) {
                            String content = read(contentSize);
                            if (content.length() > 0) {
                                delegate.onData(content);
                            } else {
                                Map<String, Object> failure = new HashMap<>(2);
                                // Convert the 200 into 204 (no content)
                                failure.put("httpCode", 204);
                                TransportException x = new TransportException(failure);
                                delegate.fail(x, "No content");
                            }
                        }
                        if (chunked) {
                            while (!"0".equals(readLine())) {
                                delegate.onData(readLine());
                            }
                            Log.i(TAG, "reading final/empty chunk");
                            readLine();//Read final/empty chunk
                        }
                    }
                } catch (IOException e) {
                    Log.i(TAG, "Server disconnected; exception=" + e);
                    if (!delegate._disconnected) {
                        delegate.failMessages(e);
                    }
                    return;
                }
            }
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
                throw new EOFException("Excepted " + size + " characters, but got " + length);
            }
            return new String(buffer);
        }
    }

    private boolean isReply(Message message) {
        return message.isMeta() || message.isPublishReply();
    }


    protected void customize(Request request) {
    }

}
