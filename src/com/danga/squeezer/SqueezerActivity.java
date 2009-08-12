package com.danga.squeezer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class SqueezerActivity extends Activity {
    private static final int DIALOG_CHOOSE_PLAYER = 0;
    private static final String TAG = "SqueezerActivity";
    private static final String DISCONNECTED_TEXT = "Disconnected.";

    private ISqueezeService serviceStub = null;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isPlaying = new AtomicBoolean(false);

    private TextView albumText;
    private TextView artistText;
    private TextView trackText;   
    private ImageButton playPauseButton;

    private Handler uiThreadHandler = new Handler();
	
    private ServiceConnection serviceConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder service) {
        	serviceStub = ISqueezeService.Stub.asInterface(service);
        	Log.v(TAG, "Service bound");
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
	
    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        albumText = (TextView) findViewById(R.id.albumname);
        artistText = (TextView) findViewById(R.id.artistname);
        trackText = (TextView) findViewById(R.id.trackname);
        playPauseButton = (ImageButton) findViewById(R.id.pause);
		
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
    }
    
    // Called only from the UI thread.
    private void setConnected(boolean connected) {
    	isConnected.set(connected);
        ImageButton nextButton = (ImageButton) findViewById(R.id.next);
        ImageButton prevButton = (ImageButton) findViewById(R.id.prev);
    	nextButton.setEnabled(connected);
    	prevButton.setEnabled(connected);
    	if (!connected) {
    	    playPauseButton.setImageResource(android.R.drawable.presence_online);  // green circle
            nextButton.setImageResource(0);
            prevButton.setImageResource(0);
            artistText.setText(DISCONNECTED_TEXT);
            albumText.setText("");
            trackText.setText("");
            setTitleForPlayer(null);
    	} else {
    	    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            nextButton.setImageResource(android.R.drawable.ic_media_next);
            prevButton.setImageResource(android.R.drawable.ic_media_previous);
            if (DISCONNECTED_TEXT.equals(artistText.getText())) {
                artistText.setText("");
            }
    	}
    }

    private void updatePlayPauseIcon() {
        uiThreadHandler.post(new Runnable() {
                public void run() {
                    if (isPlaying.get()) {
                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                    } else {
                        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                }
            });
    }

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
        bindService(new Intent(this, SqueezeService.class),
                    serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService");

        // Update the UI to reflect connection state.  Basically just for
        // the initial display, as changing the prev/next buttons to empty
        // doesn't seem to work in onCreate.  (LayoutInflator still running?)
        setConnected(isConnected.get());
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
            for (String playerName : playerNames) {
                items[n++] = playerName;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose Player");
            builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
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
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void onUserInitiatesConnect() {
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);
        final String ipPort = preferences.getString(Preferences.KEY_SERVERADDR, null);
        if (ipPort == null || ipPort.length() == 0) {
            SettingsActivity.show(this);
            return;
        }
        startConnecting(ipPort);
    }

    private void startConnecting(String ipPort) {
        if (serviceStub == null) {
            Log.e(TAG, "serviceStub is null.");
            return;
        }
        try {
            serviceStub.startConnect(ipPort);
        } catch (RemoteException e) {
            AlertDialog alert = new AlertDialog.Builder(this)
        	.setMessage("Error: " + e)
        	.create();
            alert.show();
        }
    }
	
    private IServiceCallback serviceCallback = new IServiceCallback.Stub() {
            public void onConnectionChanged(final boolean isConnected)
                throws RemoteException {
                // TODO Auto-generated method stub
                Log.v(TAG, "Connected == " + isConnected);
                uiThreadHandler.post(new Runnable() {
                        public void run() {
                            setConnected(isConnected);
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

            public void onMusicChanged(final String artist,
                                       final String album,
                                       final String track,
                                       String coverArtUrl) throws RemoteException {
                // TODO Auto-generated method stub
                uiThreadHandler.post(new Runnable() {
                        public void run() {
                            artistText.setText(artist);
                            albumText.setText(album);
                            trackText.setText(track);
                        }
                    });
            }

            public void onVolumeChange(int newVolume) throws RemoteException {
                // TODO Auto-generated method stub
                Log.v(TAG, "Volume = " + newVolume);
            }

            public void onPlayStatusChanged(boolean newStatus)
                throws RemoteException {
                isPlaying.set(newStatus);
                updatePlayPauseIcon();
            }
        };
}
