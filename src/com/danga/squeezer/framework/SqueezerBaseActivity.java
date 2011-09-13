package com.danga.squeezer.framework;

import com.danga.squeezer.service.ISqueezeService;
import com.danga.squeezer.service.SqueezeService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Common base class for all activities in the squeezer
 * @author Kurt Aaholst
 *
 */
public abstract class SqueezerBaseActivity extends Activity {
	private ISqueezeService service = null;
	private Handler uiThreadHandler = new Handler() {};

	protected abstract void onServiceConnected() throws RemoteException;
    
    protected String getTag() {
    	return getClass().getSimpleName();
	}

	public void setService(ISqueezeService service) {
		this.service = service;
	}

	/**
	 * @return The squeezeservice, or null if not bound
	 */
	public ISqueezeService getService() {
		return service;
	}

	private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ISqueezeService.Stub.asInterface(binder);
   			try {
   				SqueezerBaseActivity.this.onServiceConnected();
            } catch (RemoteException e) {
                Log.e(getTag(), "Error in onServiceConnected: " + e);
            }
        }
		public void onServiceDisconnected(ComponentName name) {
            service = null;
        };
    };

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, SqueezeService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(getTag(), "did bindService; serviceStub = " + getService());
    }

	@Override
    public void onPause() {
        super.onPause();
        if (serviceConnection != null) {
        	unbindService(serviceConnection);
        }
    }

	/**
	 * Use this to post Runnables to work off thread
	 */
	public Handler getUIThreadHandler() {
		return uiThreadHandler;
	}

	
	// This section is just an easier way to call squeeze service
    
	public boolean play(SqueezerItem item) throws RemoteException {
		return playlistControl(PlaylistControlCmd.load, item);
	}
	
	public boolean add(SqueezerItem item) throws RemoteException {
		return playlistControl(PlaylistControlCmd.add, item);
	}
	
	public boolean insert(SqueezerItem item) throws RemoteException {
		return playlistControl(PlaylistControlCmd.insert, item);
	}
	
    private boolean playlistControl(PlaylistControlCmd cmd, SqueezerItem item) throws RemoteException {
        if (service == null) {
            return false;
        }
        service.playlistControl(cmd.name(), item.getClass().getName(), item.getId());
        return true;
    }
    
    private enum PlaylistControlCmd {
    	load,
    	add,
    	insert;
    }
	
}