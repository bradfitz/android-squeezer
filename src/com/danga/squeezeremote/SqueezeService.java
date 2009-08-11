package com.danga.squeezeremote;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class SqueezeService extends Service {
	private final String TAG = "SqueezeService";
	private final AtomicBoolean isConnected = new AtomicBoolean(false);
	private final AtomicBoolean isPlaying = new AtomicBoolean(false);
	private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private final AtomicReference<Socket> socketRef = new AtomicReference<Socket>();
	private final AtomicReference<IServiceCallback> callback = new AtomicReference<IServiceCallback>();
	private final AtomicReference<PrintWriter> socketWriter = new AtomicReference<PrintWriter>();
	
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
			// Assume for now (this might be wrong) that the state toggles.
			// We'll hear from the socket soon enough what the actual state is.
			setPlayingState(!isPlaying.get());
			socketWriter.get().println("00%3A04%3A20%3A17%3A04%3A7f pause");
			Log.v(TAG, "paused.");
			return true;
		}

		public boolean play() throws RemoteException {
			if (!isConnected()) {
				return false;
			}
			Log.v(TAG, "play..");
			isPlaying.set(true);
			socketWriter.get().println("00%3A04%3A20%3A17%3A04%3A7f play");
			Log.v(TAG, "played.");
			return true;
		}

		public boolean stop() throws RemoteException {
			if (!isConnected()) {
				return false;
			}
			isPlaying.set(false);
			socketWriter.get().println("00%3A04%3A20%3A17%3A04%3A7f stop");
			return true;
		}

		public boolean isPlaying() throws RemoteException {
			return isPlaying.get();
		}
	};

}
