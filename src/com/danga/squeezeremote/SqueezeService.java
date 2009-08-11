package com.danga.squeezeremote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class SqueezeService extends Service {
	private static final String TAG = "SqueezeService";
	private static final int PLAYBACKSERVICE_STATUS = 1;
		
	private final AtomicBoolean isConnected = new AtomicBoolean(false);
	private final AtomicBoolean isPlaying = new AtomicBoolean(false);
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();
	private final AtomicReference<IServiceCallback> callback = new AtomicReference<IServiceCallback>();
	private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();
	private Thread listeningThread = null;
	
    @Override
    public void onCreate() {
    	super.onCreate();
    	
        // Clear leftover notification in case this service previously got killed while playing                                                
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(PLAYBACKSERVICE_STATUS);
    }
	
	@Override
	public IBinder onBind(Intent intent) {
		return squeezeService;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		disconnect();
		callback.set(null);
	}

	private void disconnect() {
		Socket socket = socketRef.get();
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {}
		}
		socketRef.set(null);
		socketWriter.set(null);
		isConnected.set(false);
		isPlaying.set(false);
		setConnectionState(false);
	}

	private synchronized void sendCommand(String string) {
		PrintWriter writer = socketWriter.get();
		if (writer == null) return;
		Log.v(TAG, "SENDING: " + string);
		writer.println(string);
	}
	
	private void onLineReceived(String serverLine) {
		Log.v(TAG, "LINE: " + serverLine);
	}
	
	private void startListeningThread() {
		sendCommand("listen 1");
		if (listeningThread != null) {
			listeningThread.stop();
		}
		listeningThread = new ListeningThread(socketRef.get());
		listeningThread.start();
	}

	private void setConnectionState(boolean currentState) {
		isConnected.set(currentState);
		if (callback.get() == null) {
			return;
		}
		try {
			Log.d(TAG, "pre-call setting callback connection state to: " + currentState);
			callback.get().onConnectionChanged(currentState);
			Log.d(TAG, "post-call setting callback connection state.");
		} catch (RemoteException e) {
		}
	}
	
	private void setPlayingState(boolean state) {
		isPlaying.set(state);
        NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (state) {
            Notification status = new Notification();
            //status.contentView = views;
            PendingIntent pIntent = PendingIntent.getActivity(this, 0,
                    new Intent(this, SqueezeRemoteActivity.class), 0);
            status.setLatestEventInfo(this, "Music Playing", "Content Text", pIntent);
            status.flags |= Notification.FLAG_ONGOING_EVENT;
            status.icon = R.drawable.stat_notify_musicplayer;
            //status.contentIntent = PendingIntent.getActivity(this, 0,
            //        new Intent(this, SqueezeRemoteActivity.class), 0);
            nm.notify(PLAYBACKSERVICE_STATUS, status);
		} else {
	        nm.cancel(PLAYBACKSERVICE_STATUS);
		}
		
		if (callback.get() == null) {
			return;
		}
		try {
			callback.get().onPlayStatusChanged(state);
		} catch (RemoteException e) {
		}

	}

	private final ISqueezeService.Stub squeezeService = new ISqueezeService.Stub() {

	    public void registerCallback(IServiceCallback callback) throws RemoteException {
	    	SqueezeService.this.callback.set(callback);
	    	callback.onConnectionChanged(isConnected.get());
	    }
	    
	    public void unregisterCallback(IServiceCallback callback) throws RemoteException {
	    	SqueezeService.this.callback.compareAndSet(callback, null);
	    }

		public int adjustVolumeBy(int delta) throws RemoteException {
			return 0;
		}

		public boolean isConnected() throws RemoteException {
			return isConnected.get();
		}

		public void startConnect(final String hostPort) throws RemoteException {
			int colonPos = hostPort.indexOf(":");
			boolean noPort = colonPos == -1;
			final int port = noPort? 9090 : Integer.parseInt(hostPort.substring(colonPos + 1));
			final String host = noPort ? hostPort : hostPort.substring(0, colonPos);
			executor.execute(new Runnable() {
				public void run() {
					SqueezeService.this.disconnect();
					Socket socket = new Socket();
					try {
						socket.connect(
								new InetSocketAddress(host, port),
								1500 /* ms timeout */);
						socketRef.set(socket);
						Log.d(TAG, "Connected to: " + hostPort);
						socketWriter.set(new PrintWriter(socket.getOutputStream(), true));
						Log.d(TAG, "writer == " + socketWriter.get());
						setConnectionState(true);
						Log.d(TAG, "connection state broadcasted true.");
						startListeningThread();
					} catch (SocketTimeoutException e) {
						Log.e(TAG, "Socket timeout connecting to: " + hostPort);
					} catch (IOException e) {
						Log.e(TAG, "IOException connecting to: " + hostPort);
					}
				}

			});
		}

		public void disconnect() throws RemoteException {
			if (!isConnected()) return;
			SqueezeService.this.disconnect();
		}
		
		public boolean togglePausePlay() throws RemoteException {
			if (!isConnected()) {
				return false;
			}
			Log.v(TAG, "pause...");
			if (isPlaying.get()) {
				setPlayingState(false);
				sendCommand("00%3A04%3A20%3A17%3A04%3A7f pause 1");
			} else {
				setPlayingState(true);
				// TODO: use 'pause 0 <fade_in_secs>' to fade-in if we knew it was
				// actually paused (as opposed to not playing at all) 
				sendCommand("00%3A04%3A20%3A17%3A04%3A7f play");
			}
			Log.v(TAG, "paused.");
			return true;
		}

		public boolean play() throws RemoteException {
			if (!isConnected()) {
				return false;
			}
			Log.v(TAG, "play..");
			isPlaying.set(true);
			sendCommand("00%3A04%3A20%3A17%3A04%3A7f play");
			Log.v(TAG, "played.");
			return true;
		}

		public boolean stop() throws RemoteException {
			if (!isConnected()) {
				return false;
			}
			isPlaying.set(false);
			sendCommand("00%3A04%3A20%3A17%3A04%3A7f stop");
			return true;
		}

		public boolean isPlaying() throws RemoteException {
			return isPlaying.get();
		}
	};

	private class ListeningThread extends Thread {
		private final Socket socket;
		public ListeningThread(Socket socket) {
			this.socket = socket;	
		}
		
		@Override
		public void run() {
			BufferedReader in;
			try {
				in = new BufferedReader(
						new InputStreamReader(socket.getInputStream()),
						128);
			} catch (IOException e) {
				Log.v(TAG, "IOException while reading from server: " + e);
				SqueezeService.this.disconnect();
				return;
			}
			while (true) {
				String line;
				try {
					line = in.readLine();
				} catch (IOException e) {
					Log.v(TAG, "IOException while reading from server: " + e);
					SqueezeService.this.disconnect();
					return;
				}
				if (line == null) {
					// Socket disconnected.
					Log.v(TAG, "Server disconnected.");
					SqueezeService.this.disconnect();
					return;
				}
				SqueezeService.this.onLineReceived(line);
			}
		}
	}

}
