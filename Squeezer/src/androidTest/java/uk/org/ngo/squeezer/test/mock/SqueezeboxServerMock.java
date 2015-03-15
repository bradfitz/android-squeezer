
package uk.org.ngo.squeezer.test.mock;

import android.util.Log;

import junit.framework.AssertionFailedError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog.AlbumsSortOrder;

/**
 * Emulates LMS for testing purposes
 * <p/>
 * Each instance will wait for an incoming connection, accept it, read commands from the
 * inputstream, and reply to the outputstream, until the connection is broken or the exit command is
 * received, at which point the connection is terminated.
 * <p/>
 * To make a new connection a new instance must be started.
 *
 * @author Kurt Aaholst <kaaholst@gmail.com>
 */
public class SqueezeboxServerMock extends Thread {

    private static final String TAG = SqueezeboxServerMock.class.getSimpleName();

    public static final int CLI_PORT = 9091;

    private final Object serverReadyMonitor = new Object();

    private boolean accepting;

    public static class Starter {

        public SqueezeboxServerMock start() {
            SqueezeboxServerMock server = new SqueezeboxServerMock(this);
            server.start();
            synchronized (server.serverReadyMonitor) {
                while (!server.accepting) {
                    try {
                        server.serverReadyMonitor.wait(2000);
                        if (!server.accepting) {
                            throw new AssertionFailedError("Expected the mock server to start");
                        }
                    } catch (InterruptedException e) {
                        Log.w(TAG, "Interrupted while waiting for the mock server to start");
                    }
                }
            }
            return server;
        }

        public Starter username(String username) {
            this.username = username;
            return this;
        }

        public Starter password(String password) {
            this.password = password;
            return this;
        }

        public Starter canRandomplay(boolean canRandomplay) {
            this.canRandomplay = canRandomplay;
            return this;
        }

        public Starter canMusicFolder(boolean canMusicFolder) {
            this.canMusicFolder = canMusicFolder;
            return this;
        }

        public Starter albumsSortOrder(AlbumsSortOrder albumsSortOrder) {
            this.albumsSortOrder = albumsSortOrder;
            return this;
        }

        private String username;

        private String password;

        private boolean canRandomplay = true;

        private boolean canMusicFolder = true;

        private AlbumsSortOrder albumsSortOrder = AlbumsSortOrder.album;
    }

    public static Starter starter() {
        return new Starter();
    }

    private SqueezeboxServerMock(Starter starter) {
        username = starter.username;
        password = starter.password;
        canRandomplay = starter.canRandomplay;
        canMusicFolder = starter.canMusicFolder;
        albumsSortOrder = starter.albumsSortOrder;
    }

    private String username;

    private String password;

    private boolean canMusicFolder;

    private boolean canRandomplay;

    private AlbumsSortOrder albumsSortOrder;

    @Override
    public void run() {
        ServerSocket serverSocket;
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        try {
            // Establish server socket
            serverSocket = new ServerSocket(CLI_PORT);
            serverSocket.setReuseAddress(true);

            // Wait for incoming connection
            Log.d(TAG, "Mock server listening on port: " + serverSocket.getLocalPort());
            synchronized (serverReadyMonitor) {
                accepting = true;
                serverReadyMonitor.notifyAll();
            }
            socket = serverSocket.accept();

            Log.d(TAG, "Mock server connected to: " + socket.getRemoteSocketAddress());
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()), 128);
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new Error(e);
        }

        boolean loggedIn = (username == null || password == null);

        while(! Thread.interrupted()) {
            // read data from Socket
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                line = null;
            }
            Log.d(TAG, "Mock server got: " + line);
            if (line == null) {
                break; // Client disconnected
            }

            String[] tokens = line.split(" ");

            if ("login".equals(tokens[0])) {
                out.println(tokens[0] + ' ' + tokens[1] + " ******");
                if (username != null && password != null) {
                    if (tokens.length < 2 || !username.equals(tokens[1])) {
                        break;
                    }
                    if (tokens.length < 3 || !password.equals(tokens[2])) {
                        break;
                    }
                }
                loggedIn = true;
            } else {
                if (!loggedIn) {
                    break;
                }

                if ("exit".equals(line)) {
                    out.println(line);
                    break;
                } else if ("listen 1".equals(line)) {
                    //Just ignore, mock doesn't support server side events
                    out.println("listen 1");
                } else if ("can musicfolder ?".equals(line)) {
                    out.println("can musicfolder " + (canMusicFolder ? 1 : 0));
                } else if ("can randomplay ?".equals(line)) {
                    out.println("can randomplay " + (canRandomplay ? 1 : 0));
                } else if ("pref httpport ?".equals(line)) {
                    out.println("pref httpport 9092");
                } else if ("pref jivealbumsort ?".equals(line)) {
                    out.println("pref jivealbumsort " + albumsSortOrder);
                } else if ("version ?".equals(line)) {
                    out.println("version 7.7.2");
                } else if ("players".equals(tokens[0])) {
                    //TODO implement
                } else {
                    out.println(line);
                }
            }
        }
        try {
            Log.d(TAG, "Mock server closing socket");
            socket.close();
            serverSocket.close();
        } catch (IOException e) {}
    }

}
