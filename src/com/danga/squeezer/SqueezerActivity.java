package com.danga.squeezer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SqueezerActivity extends Activity {
    private static final String TAG = "SqueezerActivity";
    private static final String DISCONNECTED_TEXT = "Disconnected.";
    private static final int DIALOG_CHOOSE_PLAYER = 0;
    private static final int DIALOG_ABOUT = 1;
    private static final int DIALOG_CONNECTING = 2;

    private ISqueezeService serviceStub = null;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isPlaying = new AtomicBoolean(false);

    private TextView albumText;
    private TextView artistText;
    private TextView trackText;   
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private Toast activeToast;

    private Handler uiThreadHandler = new Handler();
	
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            serviceStub = ISqueezeService.Stub.asInterface(service);
        	Log.v(TAG, "Service bound");
        	uiThreadHandler.post(new Runnable() {
        	    public void run() {
        	        updateUIFromServiceState();
        	    }
        	});
        	try {
        	    serviceStub.registerCallback(serviceCallback);
        	} catch (RemoteException e) {
        	    e.printStackTrace();
        	}
        }

        public void onServiceDisconnected(ComponentName name) {
            serviceStub = null;
        };
    };
    
    // Where we're connecting to.
    private boolean connectInProgress = false;
    private String connectingTo = null;
    private ProgressDialog connectingDialog = null;
	
    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Intercept volume keys to control SqueezeCenter volume.
        // TODO: make this actually work.  It's something like this, but not quite.
        // Other apps do it, so should be possible somehow.
        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.root_layout);
        mainLayout.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    // TODO: notify user somehow. Toast?
                    changeVolumeBy(+5);
                    return true;
                }
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    // TODO: notify user somehow. Toast?
                    changeVolumeBy(-5);
                    return true;
                }
                return false;
            }
        });
        
        albumText = (TextView) findViewById(R.id.albumname);
        artistText = (TextView) findViewById(R.id.artistname);
        trackText = (TextView) findViewById(R.id.trackname);
        playPauseButton = (ImageButton) findViewById(R.id.pause);
        nextButton = (ImageButton) findViewById(R.id.next);
        prevButton = (ImageButton) findViewById(R.id.prev);
		
        playPauseButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (serviceStub == null) return;
                    try {
                        if (isConnected.get()) {
                            Log.v(TAG, "Pause...");
                            serviceStub.togglePausePlay();
                        } else {
                            // When we're not connected, the play/pause
                            // button turns into a green connect button.
                            onUserInitiatesConnect();
                        }
                    } catch (RemoteException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
	    });
        
        nextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (serviceStub == null) return;
                try {
                    serviceStub.nextTrack();
                } catch (RemoteException e) { }
            }
        });

        prevButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (serviceStub == null) return;
                try {
                    serviceStub.previousTrack();
                } catch (RemoteException e) { }
            }
        });
        
        // Hacks for now, making shuffle & repeat do volume up & down, until
        // these do something & volume is fixed to use the side keys.
        ((ImageButton) this.findViewById(R.id.shuffle)).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        changeVolumeBy(+5);
                    }
                });
        ((ImageButton) this.findViewById(R.id.repeat)).setOnClickListener(
                new OnClickListener() {
                    public void onClick(View v) {
                        changeVolumeBy(-5);
                    }
                });
        
        
    }
    
    private boolean changeVolumeBy(int delta) {
        Log.v(TAG, "Adjust volume by: " + delta);
        if (serviceStub == null) {
            return false;
        }
        try {
            serviceStub.adjustVolumeBy(delta);
            return true;
        } catch (RemoteException e) {
        }
        return false;
    }

    // Should only be called the UI thread.
    private void setConnected(boolean connected, boolean postConnect) {
        Log.v(TAG, "setConnected(" + connected + ", " + postConnect + ")");
        if (postConnect) {
            connectInProgress = false;
            Log.v(TAG, "Post-connect; connectingDialog == " + connectingDialog);
            if (connectingDialog != null) {
                Log.v(TAG, "Dismissing...");
                connectingDialog.dismiss();
                connectInProgress = false;
                if (!connected) {
                  Toast.makeText(this, "Connection failed.  Check settings.", Toast.LENGTH_LONG).show();
                  return;
                }
            }
        }

    	isConnected.set(connected);
    	nextButton.setEnabled(connected);
    	prevButton.setEnabled(connected);
    	if (!connected) {
            nextButton.setImageResource(0);
            prevButton.setImageResource(0);
            artistText.setText(DISCONNECTED_TEXT);
            albumText.setText("");
            trackText.setText("");
            setTitleForPlayer(null);
    	} else {
            nextButton.setImageResource(android.R.drawable.ic_media_next);
            prevButton.setImageResource(android.R.drawable.ic_media_previous);
            updateSongInfoFromService();
    	}
    	updatePlayPauseIcon();
    }

    private void updatePlayPauseIcon() {
        uiThreadHandler.post(new Runnable() {
            public void run() {
                if (!isConnected.get()) {
                    playPauseButton.setImageResource(android.R.drawable.presence_online);  // green circle
                } else if (isPlaying.get()) {
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                }
            }
        });
    }

    // May be called from any thread.
    private void setTitleForPlayer(final String playerName) {
        uiThreadHandler.post(new Runnable() {
            public void run() {
                if (playerName != null && !"".equals(playerName)) {
                    setTitle("Squeezer: " + playerName);
                } else {
                    setTitle("Squeezer");
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume...");

        // Start it and have it run forever (until it shuts itself down).
        // This is required so swapping out the activity (and unbinding the
        // service connection in onPause) doesn't cause the service to be
        // killed due to zero refcount.  This is our signal that we want
        // it running in the background.
        startService(new Intent(this, SqueezeService.class));
        
        bindService(new Intent(this, SqueezeService.class),
                    serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService; serviceStub = " + serviceStub);
        
        if (serviceStub != null) {
            updateUIFromServiceState();
            try {
                serviceStub.registerCallback(serviceCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "error registering callback: " + e);
            }
        }
    }

    // Should only be called from the UI thread.
    private void updateUIFromServiceState() {
        // Update the UI to reflect connection state.  Basically just for
        // the initial display, as changing the prev/next buttons to empty
        // doesn't seem to work in onCreate.  (LayoutInflator still running?)
        setConnected(isConnected(), false);

        // TODO(bradfitz): remove this check once everything is converted into
        // safe accessors like isConnected() already is.
        if (serviceStub == null) {
            Log.e(TAG, "Can't update UI with null serviceStub");
            return;
        }

        try {
            setTitleForPlayer(serviceStub.getActivePlayerName());
            isPlaying.set(serviceStub.isPlaying());
            updatePlayPauseIcon();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception: " + e);
        }
       
    }
    
    // Should only be called from the UI thread.
    private void updateSongInfoFromService() {
        artistText.setText("TODO");
        albumText.setText("TODO");
        trackText.setText(getServiceCurrentSong());
        
        // TODO: BLOCKING: this is blocking in the GUI thread.
        ImageView albumArt = (ImageView) findViewById(R.id.album);
        boolean imageSet = false;
        try {
            String albumArtUrl = serviceStub.currentAlbumArtUrl();
            Log.v(TAG, "Album art URL: " + albumArtUrl);
            if (albumArtUrl != null && albumArtUrl.length() > 0) {
                URL url = new URL(albumArtUrl); 
                InputStream inputStream;    
                inputStream = (InputStream) url.getContent();
                Drawable d = Drawable.createFromStream(inputStream, "src");
                albumArt.setImageDrawable(d);
                imageSet = true;
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        } catch (RemoteException e) {
        }
        if (!imageSet) {
            albumArt.setImageDrawable(null);
        }
    }
    
    private String getActivePlayerId() {
        if (serviceStub == null) {
            return "";
        }
        try {
            return serviceStub.getActivePlayerId();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in isConnected(): " + e);
        }
        return "";
    }
    
    private String getServiceCurrentSong() {
        if (serviceStub == null) {
            return "";
        }
        try {
            return serviceStub.currentSong();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in isConnected(): " + e);
        }
        return "";
    }
    
    private String getServiceCurrentAlbum() {
        if (serviceStub == null) {
            return "";
        }
        try {
            return serviceStub.currentAlbum();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in isConnected(): " + e);
        }
        return "";
    }
    
    private String getServiceCurrentArtist() {
        if (serviceStub == null) {
            return "";
        }
        try {
            return serviceStub.currentArtist();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in isConnected(): " + e);
        }
        return "";
    }


    private boolean isConnected() {
        if (serviceStub == null) {
            return false;
        }
        try {
            return serviceStub.isConnected(); 
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in isConnected(): " + e);
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (serviceStub != null) {
            try {
                serviceStub.unregisterCallback(serviceCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.squeezer, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	boolean connected = isConnected.get();

    	// Only show one of connect and disconnect:
    	MenuItem connect = menu.findItem(R.id.menu_item_connect);
    	connect.setVisible(!connected);
    	MenuItem disconnect = menu.findItem(R.id.menu_item_disconnect);
    	disconnect.setVisible(connected);

    	// Disable things that don't work when not connected.
        MenuItem players = menu.findItem(R.id.menu_item_players);
        players.setEnabled(connected);
        MenuItem search = menu.findItem(R.id.menu_item_search);
        search.setEnabled(connected);

    	return true;	
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_CHOOSE_PLAYER:
            final List<String> playerIds = new ArrayList<String>();
            final List<String> playerNames = new ArrayList<String>();
            try {
                if (!serviceStub.getPlayers(playerIds, playerNames)) {
                    Log.e(TAG, "No players in onPlayersDiscovered?");
                    return null;
                }
            } catch (RemoteException e) {
                return null;
            }
            final CharSequence[] items = new CharSequence[playerNames.size()];
            int n = 0;
            int checkedItem = -1;
            String currentPlayerId = currentPlayerId();
            for (String playerName : playerNames) {
                items[n] = playerName;
                if (playerIds.get(n).equals(currentPlayerId)) {
                    checkedItem = n;
                }
                n++;
            }
            builder.setTitle("Choose Player");
            builder.setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int indexSelected) {
                        String playerId = playerIds.get(indexSelected);
                        try {
                            serviceStub.setActivePlayer(playerId);
                        } catch (RemoteException e) {
                            Log.e(TAG, "Error setting active player: " + e);
                        }
                        dialog.dismiss();
                    }
                });
            return builder.create();
        case DIALOG_ABOUT:
            builder.setTitle("About");
            builder.setMessage(R.string.about_text);
            return builder.create();
        case DIALOG_CONNECTING:
            // Note: this only happens the first time.  onPrepareDialog is called on each connect.
            connectingDialog = new ProgressDialog(this);
            connectingDialog.setTitle("Connecting");
            connectingDialog.setIndeterminate(true);
            return connectingDialog;
        }
        return null;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case DIALOG_CHOOSE_PLAYER:
            // TODO(bradfitz): move most the code above for CHOOSE_PLAYER into here,
            // where it'll actually re-query the server every time.  This only really
            // matters if players change while the Service is still running.
            return;
        case DIALOG_CONNECTING:
            ProgressDialog connectingDialog = (ProgressDialog) dialog;
            connectingDialog.setMessage(
                    "Connecting to SqueezeBox CLI at " + connectingTo);
            if (!connectInProgress) {
                // Lose the race?  If Service/network is very fast.
                connectingDialog.dismiss();
            }
            return;
        }
    }
    
    /**
     * Returns current player id (the mac address) or null/empty.
     */
    private String currentPlayerId() {
        if (serviceStub == null) {
            return null;
        }
        try {
            return serviceStub.getActivePlayerId(); 
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in isConnected(): " + e);
        }
        return null;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
      	case R.id.menu_item_settings:
            SettingsActivity.show(this);
            return true;
      	case R.id.menu_item_connect:
      	    onUserInitiatesConnect();
            return true;
      	case R.id.menu_item_disconnect:
            try {
                serviceStub.disconnect();
            } catch (RemoteException e) {
            	AlertDialog alert = new AlertDialog.Builder(this)
                    .setMessage("Error: " + e)
                    .create();
            	alert.show();
            }
            return true;
      	case R.id.menu_item_players:
      	    showDialog(DIALOG_CHOOSE_PLAYER);
      	    return true;
        case R.id.menu_item_about:
            showDialog(DIALOG_ABOUT);
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void onUserInitiatesConnect() {
        if (serviceStub == null) {
            Log.e(TAG, "serviceStub is null.");
            return;
        }
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);
        final String ipPort = preferences.getString(Preferences.KEY_SERVERADDR, null);
        if (ipPort == null || ipPort.length() == 0) {
            SettingsActivity.show(this);
            return;
        }
        connectInProgress = true;
        connectingTo = ipPort;
        Log.v(TAG, "User-initiated connect to: " + ipPort);

        showDialog(DIALOG_CONNECTING);
        try {
            serviceStub.startConnect(ipPort);
        } catch (RemoteException e) {
            Log.e(TAG, "exception starting to connect: " + e);
        }
    }
	
    private IServiceCallback serviceCallback = new IServiceCallback.Stub() {
        public void onConnectionChanged(final boolean isConnected,
                                        final boolean postConnect)
                       throws RemoteException {
                // TODO Auto-generated method stub
                Log.v(TAG, "Connected == " + isConnected + " (postConnect==" + postConnect + ")");
                uiThreadHandler.post(new Runnable() {
                        public void run() {
                            setConnected(isConnected, postConnect);
                        }
                    });
            }

            public void onPlayersDiscovered() throws RemoteException {
                List<String> playerIds = new ArrayList<String>();
                List<String> playerNames = new ArrayList<String>();
                if (!serviceStub.getPlayers(playerIds, playerNames)) {
                    Log.e(TAG, "No players in onPlayersDiscovered?");
                    return;
                }
                int n = 0;
                for (String playerId : playerIds) {
                    String playerName = playerNames.get(n++);
                    Log.v(TAG, "player: " + playerId + ", " + playerName);
                }
            }

            public void onPlayerChanged(final String playerId,
                                        final String playerName) throws RemoteException {
                Log.v(TAG, "player now " + playerId + ": " + playerName);
                setTitleForPlayer(playerName);
            }

            public void onMusicChanged() throws RemoteException {
                uiThreadHandler.post(new Runnable() {
                        public void run() {
                            updateSongInfoFromService();
                        }
                    });
            }

            public void onVolumeChange(final int newVolume) throws RemoteException {
                Log.v(TAG, "Volume = " + newVolume);
                uiThreadHandler.post(new Runnable() {
                    public void run() {
                        if (activeToast != null) {
                            activeToast.setText("Volume: " + newVolume);
                        } else {
                            activeToast = Toast.makeText(SqueezerActivity.this, "Volume: " + newVolume, Toast.LENGTH_SHORT);
                        }
                        activeToast.show();
                    }
                });
            }

            public void onPlayStatusChanged(boolean newStatus)
                throws RemoteException {
                isPlaying.set(newStatus);
                updatePlayPauseIcon();
            }
        };
}
