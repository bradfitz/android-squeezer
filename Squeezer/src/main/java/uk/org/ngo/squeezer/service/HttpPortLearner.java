/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.service;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.regex.Pattern;


class HttpPortLearner {
    private static final String TAG = HttpPortLearner.class.getSimpleName();

    /** {@link java.util.regex.Pattern} that splits strings on spaces. */
    private static final Pattern mSpaceSplitPattern = Pattern.compile(" ");

    private Socket socket;
    private PrintWriter socketWriter;
    private BufferedReader socketReader;

    /**
     * Connect to the LMS, log in and request http port.
     * @return LMS http port
     * @throws IOException if connection fails, or we can't get the http port
     */
    public int learnHttpPort(String host, int cliPort, String userName, String password) throws IOException {
        Log.d(TAG, "learnHttpPort(" + userName + "@" + host + ":" + cliPort + ")");
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, cliPort), 4000 /* ms timeout */);
        socketWriter = new PrintWriter(socket.getOutputStream(), true);
        socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        socket.setSoTimeout(5 * 1000);

        executeCommand("login " + userName + " " + password);
        String response = executeCommand("pref httpport ?");
        disconnect();

        String[] tokens = (response != null? mSpaceSplitPattern.split(response) : null);
        if (tokens == null || tokens.length != 3 || !"pref".equals(tokens[0]) || !"httpport".equals(tokens[1])) {
            throw new IOException("Cannot learn http port, unexpected response: " + response);
        }

        return Integer.parseInt(tokens[2]);
    }

    /** Disconnect from the server. */
    private void disconnect() {
        if (socket != null && socket.isConnected()) {
            try {
                socket.close();
                socketReader = null;
                socketWriter = null;
            } catch (Exception e) {
                Log.e(TAG, "Error closing socket", e);
            }
            socket = null;
        }

    }

    private String executeCommand(String command) {
        String response = null;
        Log.d(TAG, "SEND: " + command);
        if (writeCommand(command)) {
            response = readResponse();
            Log.d(TAG, "RECV: " + response);
        }
        return response;
    }


    private String readResponse() {
        String response = null;
        try {
            if (socketReader != null) {
                response = socketReader.readLine();
            }
        } catch (SocketTimeoutException sto) {
            Log.e(TAG, "Timeout reading socket, disconnecting from server");
        } catch (IOException e) {
            Log.e(TAG, "error reading response", e);
        }
        return response;
    }

    private boolean writeCommand(String command) {
        boolean written = false;
        if (socketWriter != null) {
            socketWriter.println(command);
            if (!socketWriter.checkError()) {
                written = true;
            } else {
                Log.e(TAG, "Error writing response, disconnecting from server");
                disconnect();
            }
        }
        return written;
    }

}
