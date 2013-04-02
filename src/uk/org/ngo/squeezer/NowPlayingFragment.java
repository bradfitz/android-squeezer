/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import uk.org.ngo.squeezer.dialogs.AboutDialog;
import uk.org.ngo.squeezer.dialogs.ConnectingDialog;
import uk.org.ngo.squeezer.dialogs.EnableWifiDialog;
import uk.org.ngo.squeezer.dialogs.SqueezerAuthenticationDialog;
import uk.org.ngo.squeezer.framework.HasUiThread;
import uk.org.ngo.squeezer.itemlists.SqueezerAlbumListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerCurrentPlaylistActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerPlayerListActivity;
import uk.org.ngo.squeezer.itemlists.SqueezerSongListActivity;
import uk.org.ngo.squeezer.model.SqueezerAlbum;
import uk.org.ngo.squeezer.model.SqueezerArtist;
import uk.org.ngo.squeezer.model.SqueezerSong;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.util.ImageFetcher.ImageFetcherParams;
import uk.org.ngo.squeezer.util.UIUtils;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class NowPlayingFragment extends Fragment implements
        HasUiThread {
    private final String TAG = "NowPlayingFragment";

    private FragmentActivity mActivity;
    private ISqueezeService mService = null;

    private final AtomicReference<SqueezerSong> currentSong = new AtomicReference<SqueezerSong>();
    private final AtomicBoolean connectInProgress = new AtomicBoolean(false);

    private TextView albumText;
    private TextView artistText;
    private TextView trackText;
    private TextView currentTime;
    private TextView totalTime;
    private MenuItem connectButton;
    private MenuItem disconnectButton;
    private MenuItem poweronButton;
    private MenuItem poweroffButton;
    private MenuItem playersButton;
    private MenuItem playlistButton;
    private MenuItem searchButton;
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private ImageView albumArt;
    private SeekBar seekBar;

    // Updating the seekbar
    private boolean updateSeekBar = true;
    private int secondsIn;
    private int secondsTotal;
    private final static int UPDATE_TIME = 1;

    /** ImageFetcher for (large) album cover art */
    private ImageFetcher mLargeImageFetcher;

    private final Handler uiThreadHandler = new Handler() {
        // Normally I'm lazy and just post Runnables to the uiThreadHandler
        // but time updating is special enough (it happens every second) to
        // take care not to allocate so much memory which forces Dalvik to GC
        // all the time.
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE_TIME) {
                updateTimeDisplayTo(secondsIn, secondsTotal);
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            NetworkInfo networkInfo = intent
                    .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()) {
                Log.v(TAG, "Received WIFI connected broadcast");
                if (!isConnected()) {
                    // Requires a serviceStub. Else we'll do this on the service
                    // connection callback.
                    if (mService != null) {
                        Log.v(TAG, "Initiated connect on WIFI connected");
                        startVisibleConnection();
                    }
                }
            }
        }
    };

    private ConnectingDialog connectingDialog = null;
    public void clearConnectingDialog() {
        connectingDialog = null;
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.v(TAG, "ServiceConnection.onServiceConnected()");
            mService = ISqueezeService.Stub.asInterface(binder);
            try {
                NowPlayingFragment.this.onServiceConnected();
            } catch (RemoteException e) {
                Log.e(TAG, "Error in onServiceConnected: " + e);
            }
        }
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        };
    };

    private boolean mFullHeightLayout;

    /**
     * Called before onAttach. Pull out the layout spec to figure out which
     * layout to use later.
     */
    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);

        int layout_height = attrs.getAttributeUnsignedIntValue(
                "http://schemas.android.com/apk/res/android",
                "layout_height", 0);

        mFullHeightLayout = (layout_height == ViewGroup.LayoutParams.FILL_PARENT);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = (FragmentActivity) activity;
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Set up a server connection, if it is not present
        if (getConfiguredCliIpPort(getSharedPreferences()) == null)
            SettingsActivity.show(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v;

        if (mFullHeightLayout) {
            v = inflater.inflate(R.layout.now_playing_fragment_full, container, false);

            artistText = (TextView) v.findViewById(R.id.artistname);
            playPauseButton = (ImageButton) v.findViewById(R.id.pause);
            nextButton = (ImageButton) v.findViewById(R.id.next);
            prevButton = (ImageButton) v.findViewById(R.id.prev);
            currentTime = (TextView) v.findViewById(R.id.currenttime);
            totalTime = (TextView) v.findViewById(R.id.totaltime);
            seekBar = (SeekBar) v.findViewById(R.id.seekbar);
        } else {
            v = inflater.inflate(R.layout.now_playing_fragment_mini, container, false);
        }

        albumArt = (ImageView) v.findViewById(R.id.album);
        trackText = (TextView) v.findViewById(R.id.trackname);
        albumText = (TextView) v.findViewById(R.id.albumname);

        // Set up the image fetcher, max cover art size is 512K.
        ImageFetcherParams params = new ImageFetcherParams();
        params.mMaxThumbnailBytes = 512 * 1024 * 1024; // 512K
        mLargeImageFetcher = UIUtils.getImageFetcher(mActivity, params);

        if (mFullHeightLayout) {
            /*
             * TODO: Simplify these following the notes at
             * http://developer.android.com/resources/articles/ui-1.6.html.
             * Maybe. because the TextView resources don't support the
             * android:onClick attribute.
             */
            playPauseButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mService == null)
                        return;
                    try {
                        if (isConnected()) {
                            Log.v(TAG, "Pause...");
                            mService.togglePausePlay();
                        } else {
                            // When we're not connected, the play/pause
                            // button turns into a green connect button.
                            onUserInitiatesConnect();
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "Service exception from togglePausePlay(): " + e);
                    }
                }
            });

            nextButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mService == null)
                        return;
                    try {
                        mService.nextTrack();
                    } catch (RemoteException e) {
                    }
                }
            });

            prevButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    if (mService == null)
                        return;
                    try {
                        mService.previousTrack();
                    } catch (RemoteException e) {
                    }
                }
            });

            artistText.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    SqueezerSong song = getCurrentSong();
                    if (song != null) {
                        if (!song.isRemote())
                            SqueezerAlbumListActivity.show(mActivity,
                                    new SqueezerArtist(song.getArtist_id(), song.getArtist()));
                    }
                }
            });

            albumText.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    SqueezerSong song = getCurrentSong();
                    if (song != null) {
                        if (!song.isRemote())
                            SqueezerSongListActivity.show(mActivity,
                                    new SqueezerAlbum(song.getAlbum_id(), song.getAlbum()));
                    }
                }
            });

            trackText.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    SqueezerSong song = getCurrentSong();
                    if (song != null) {
                        if (!song.isRemote())
                            SqueezerSongListActivity.show(mActivity,
                                    new SqueezerArtist(song.getArtist_id(), song.getArtist()));
                    }
                }
            });

            seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
                SqueezerSong seekingSong;

                // Update the time indicator to reflect the dragged thumb
                // position.
                public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                    if (fromUser) {
                        currentTime.setText(Util.makeTimeString(progress));
                    }
                }

                // Disable updates when user drags the thumb.
                public void onStartTrackingTouch(SeekBar s) {
                    seekingSong = getCurrentSong();
                    updateSeekBar = false;
                }

                // Re-enable updates. If the current song is the same as when
                // we started seeking then jump to the new point in the track,
                // otherwise ignore the seek.
                public void onStopTrackingTouch(SeekBar s) {
                    SqueezerSong thisSong = getCurrentSong();

                    updateSeekBar = true;

                    if (seekingSong == thisSong) {
                        setSecondsElapsed(s.getProgress());
                    }
                }
            });
        } else {
            // Clicking on the layout goes to NowPlayingActivity.
            v.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    NowPlayingActivity.show(mActivity);
                }
            });
        }

        return v;
    }

    /**
     * Use this to post Runnables to work off thread
     */
    public Handler getUIThreadHandler() {
        return uiThreadHandler;
    }

    // Should only be called the UI thread.
    private void setConnected(boolean connected, boolean postConnect, boolean loginFailure) {
        Log.v(TAG, "setConnected(" + connected + ", " + postConnect + ", " + loginFailure + ")");
        if (postConnect) {
            connectInProgress.set(false);
            if (connectingDialog != null) {
                Log.d(TAG, "Dismissing ConnectingDialog");
                connectingDialog.dismiss();
            } else {
                Log.d(TAG, "Got connection failure, but ConnectingDialog wasn't showing");
            }
            connectingDialog = null;
            if (!connected) {
                // TODO: Make this a dialog? Allow the user to correct the
                // server settings here?
                Toast.makeText(mActivity, getText(R.string.connection_failed_text),
                        Toast.LENGTH_LONG)
                        .show();
            }
        }
        if (loginFailure) {
            Toast.makeText(mActivity, getText(R.string.login_failed_text), Toast.LENGTH_LONG).show();
            new SqueezerAuthenticationDialog().show(mActivity.getSupportFragmentManager(), "AuthenticationDialog");
        }

        // These are all set at the same time, so one check is sufficient
        if (connectButton != null) {
            connectButton.setVisible(!connected);
            disconnectButton.setVisible(connected);
            playersButton.setEnabled(connected);
            playlistButton.setEnabled(connected);
            searchButton.setEnabled(connected);
        }

        if (mFullHeightLayout) {
            nextButton.setEnabled(connected);
            prevButton.setEnabled(connected);
        }

        if (!connected) {
            albumArt.setImageResource(R.drawable.icon_album_noart_143);
            updateSongInfo(null);

            if (mFullHeightLayout) {
                nextButton.setImageResource(0);
                prevButton.setImageResource(0);
                artistText.setText(getText(R.string.disconnected_text));
                currentTime.setText("--:--");
                totalTime.setText("--:--");
                seekBar.setEnabled(false);
                seekBar.setProgress(0);
            }
        } else {
            updateSongInfoFromService();

            if (mFullHeightLayout) {
                nextButton.setImageResource(android.R.drawable.ic_media_next);
                prevButton.setImageResource(android.R.drawable.ic_media_previous);
                seekBar.setEnabled(true);
            }
        }
        updatePlayPauseIcon();
        updateUIForPlayer();
    }

    private void setConnected() {
        setConnected(isConnected(), false, false);
    }

    private void updatePlayPauseIcon() {
        if (mFullHeightLayout) {
            uiThreadHandler.post(new Runnable() {
                public void run() {
                    if (!isConnected()) {
                        playPauseButton.setImageResource(R.drawable.presence_online); // green
                                                                                      // circle
                    } else if (isPlaying()) {
                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                    } else {
                        playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                    }
                }
            });
        }
    }

    private void updateUIForPlayer() {
        uiThreadHandler.post(new Runnable() {
            public void run() {
                if (mFullHeightLayout) {
                    String playerName = getActivePlayerName();
                    if (playerName != null && !"".equals(playerName)) {
                        mActivity.setTitle(playerName);
                    } else {
                        mActivity.setTitle(getText(R.string.app_name));
                    }
                }
                poweronButton.setVisible(canPowerOn());
                poweroffButton.setVisible(canPowerOff());
            }
        });
    }

    protected void onServiceConnected() throws RemoteException {
        Log.v(TAG, "Service bound");
        mService.registerCallback(serviceCallback);
        uiThreadHandler.post(new Runnable() {
            public void run() {
                updateUIFromServiceState();
            }
        });

        // Assume they want to connect...
        if (!isConnected()) {
            startVisibleConnection();
        }
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
        mActivity.startService(new Intent(mActivity, SqueezeService.class));

        if (mService != null) {
            uiThreadHandler.post(new Runnable() {
                public void run() {
                    updateUIFromServiceState();
                }
            });
        }

        if (isAutoConnect(getSharedPreferences()))
            mActivity.registerReceiver(broadcastReceiver, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));

        mActivity.bindService(new Intent(mActivity, SqueezeService.class), serviceConnection,
                Context.BIND_AUTO_CREATE);
        Log.d(TAG, "did bindService; serviceStub = " + mService);
    }

    // Should only be called from the UI thread.
    private void updateUIFromServiceState() {
        // Update the UI to reflect connection state. Basically just for
        // the initial display, as changing the prev/next buttons to empty
        // doesn't seem to work in onCreate. (LayoutInflator still running?)
        Log.d(TAG, "updateUIFromServiceState");
        setConnected();
    }

    private void updateTimeDisplayTo(int secondsIn, int secondsTotal) {
        if (mFullHeightLayout) {
            if (updateSeekBar) {
                if (seekBar.getMax() != secondsTotal) {
                    seekBar.setMax(secondsTotal);
                    totalTime.setText(Util.makeTimeString(secondsTotal));
                }
                seekBar.setProgress(secondsIn);
                currentTime.setText(Util.makeTimeString(secondsIn));
            }
        }
    }

    // Should only be called from the UI thread.
    private void updateSongInfoFromService() {
        SqueezerSong song = getCurrentSong();
        updateSongInfo(song);
        updateTimeDisplayTo(getSecondsElapsed(), getSecondsTotal());
        updateAlbumArtIfNeeded(song);
    }

    private void updateSongInfo(SqueezerSong song) {
        if (song != null) {
            albumText.setText(song.getAlbum());
            trackText.setText(song.getName());
            if (mFullHeightLayout) {
                artistText.setText(song.getArtist());
            }
        } else {
            albumText.setText("");
            trackText.setText("");
            if (mFullHeightLayout) {
                artistText.setText("");
            }
        }
    }

    // Should only be called from the UI thread.
    private void updateAlbumArtIfNeeded(SqueezerSong song) {
        Log.v(TAG, "updateAlbumArtIfNeeded");
        if (Util.atomicReferenceUpdated(currentSong, song)) {
            if (song == null || song.getArtworkUrl(mService) == null) {
                albumArt.setImageResource(R.drawable.icon_album_noart_143);
                return;
            }

            mLargeImageFetcher.loadImage(song.getArtworkUrl(mService), albumArt);
        }
    }

    private int getSecondsElapsed() {
        if (mService == null) {
            return 0;
        }
        try {
            return mService.getSecondsElapsed();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in getSecondsElapsed(): " + e);
        }
        return 0;
    }

    private int getSecondsTotal() {
        if (mService == null) {
            return 0;
        }
        try {
            return mService.getSecondsTotal();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in getSecondsTotal(): " + e);
        }
        return 0;
    }

    private boolean setSecondsElapsed(int seconds) {
        if (mService == null) {
            return false;
        }
        try {
            return mService.setSecondsElapsed(seconds);
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in setSecondsElapsed(" + seconds + "): " + e);
        }
        return true;
    }

    private SqueezerSong getCurrentSong() {
        if (mService == null) {
            return null;
        }
        try {
            return mService.getCurrentSong();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in getCurrentSong(): " + e);
        }
        return null;
    }

    private String getActivePlayerName() {
        if (mService == null) {
            return null;
        }
        try {
            return mService.getActivePlayerName();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in getActivePlayerName(): " + e);
        }
        return null;
    }

    private boolean isConnected() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isConnected();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in isConnected(): " + e);
        }
        return false;
    }

    private boolean isPlaying() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.isPlaying();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in isPlaying(): " + e);
        }
        return false;
    }

    private boolean canPowerOn() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.canPowerOn();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in canPowerOn(): " + e);
        }
        return false;
    }

    private boolean canPowerOff() {
        if (mService == null) {
            return false;
        }
        try {
            return mService.canPowerOff();
        } catch (RemoteException e) {
            Log.e(TAG, "Service exception in canPowerOff(): " + e);
        }
        return false;
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause...");
        if (mService != null) {
            try {
                mService.unregisterCallback(serviceCallback);
                if (serviceConnection != null) {
                    mActivity.unbindService(serviceConnection);
                }
            } catch (RemoteException e) {
                Log.e(TAG, "Service exception in onPause(): " + e);
            }
        }
        if (isAutoConnect(getSharedPreferences()))
            mActivity.unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    /**
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu,
     *      android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // I confess that I don't understand why using the inflater passed as
        // an argument here doesn't work -- but if you do it crashes without
        // a stracktrace on API 7.
        MenuInflater i = mActivity.getMenuInflater();
        i.inflate(R.menu.squeezer, menu);

        connectButton = menu.findItem(R.id.menu_item_connect);
        disconnectButton = menu.findItem(R.id.menu_item_disconnect);
        poweronButton = menu.findItem(R.id.menu_item_poweron);
        poweroffButton = menu.findItem(R.id.menu_item_poweroff);
        playersButton = menu.findItem(R.id.menu_item_players);
        playlistButton = menu.findItem(R.id.menu_item_playlist);
        searchButton = menu.findItem(R.id.menu_item_search);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                SettingsActivity.show(mActivity);
                return true;
            case R.id.menu_item_search:
                mActivity.onSearchRequested();
                return true;
            case R.id.menu_item_connect:
                onUserInitiatesConnect();
                return true;
            case R.id.menu_item_disconnect:
                try {
                    mService.disconnect();
                } catch (RemoteException e) {
                    Toast.makeText(mActivity, e.toString(),
                            Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.menu_item_poweron:
                try {
                    mService.powerOn();
                } catch (RemoteException e) {
                    Toast.makeText(mActivity, e.toString(), Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.menu_item_poweroff:
                try {
                    mService.powerOff();
                } catch (RemoteException e) {
                    Toast.makeText(mActivity, e.toString(),
                            Toast.LENGTH_LONG).show();
                }
                return true;
            case R.id.menu_item_playlist:
                SqueezerCurrentPlaylistActivity.show(mActivity);
                break;
            case R.id.menu_item_players:
                SqueezerPlayerListActivity.show(mActivity);
                return true;
            case R.id.menu_item_about:
                new AboutDialog().show(getFragmentManager(), "AboutDialog");
                return true;
        }
        return false;
    }

    private SharedPreferences getSharedPreferences() {
        return mActivity.getSharedPreferences(Preferences.NAME, Context.MODE_PRIVATE);
    }

    private String getConfiguredCliIpPort(final SharedPreferences preferences) {
        return getStringPreference(preferences, Preferences.KEY_SERVERADDR, null);
    }

    private String getConfiguredUserName(final SharedPreferences preferences) {
        return getStringPreference(preferences, Preferences.KEY_USERNAME, "test");
    }

    private String getConfiguredPassword(final SharedPreferences preferences) {
        return getStringPreference(preferences, Preferences.KEY_PASSWORD, "test1");
    }

    private String getStringPreference(final SharedPreferences preferences, String preference, String defaultValue) {
        final String pref = preferences.getString(preference, null);
        if (pref == null || pref.length() == 0) {
            return defaultValue;
        }
        return pref;
    }

    private boolean isAutoConnect(final SharedPreferences preferences) {
        return preferences.getBoolean(Preferences.KEY_AUTO_CONNECT, true);
    }

    private void onUserInitiatesConnect() {
        // Set up a server connection, if it is not present
        if (getConfiguredCliIpPort(getSharedPreferences()) == null) {
            SettingsActivity.show(mActivity);
            return;
        }

        if (mService == null) {
            Log.e(TAG, "serviceStub is null.");
            return;
        }
        startVisibleConnection();
    }

    public void startVisibleConnection() {
        Log.v(TAG, "startVisibleConnection..., connectInProgress: " + connectInProgress.get());
        uiThreadHandler.post(new Runnable() {
            public void run() {
                SharedPreferences preferences = getSharedPreferences();
                String ipPort = getConfiguredCliIpPort(preferences);
                if (ipPort == null)
                    return;

                // If we are configured to automatically connect on Wi-Fi availability
                // we will also give the user the opportunity to enable Wi-Fi
                if (isAutoConnect(preferences)) {
                    WifiManager wifiManager = (WifiManager) mActivity
                            .getSystemService(Context.WIFI_SERVICE);
                    if (!wifiManager.isWifiEnabled()) {
                        new EnableWifiDialog().show(getFragmentManager(), "EnableWifiDialog");
                        return;
                        // When a Wi-Fi connection is made this method will be called again by the
                        // broadcastReceiver
                    }
                }

                if (connectInProgress.get()) {
                    Log.v(TAG, "Connection is allready in progress, connecting aborted");
                    return;
                }
                connectingDialog = ConnectingDialog.addTo(mActivity, ipPort);
                if (connectingDialog != null) {
                    Log.v(TAG, "startConnect, ipPort: " + ipPort);
                    connectInProgress.set(true);
                    try {
                        getConfiguredCliIpPort(preferences);
                        mService.startConnect(ipPort, getConfiguredUserName(preferences), getConfiguredPassword(preferences));
                    } catch (RemoteException e) {
                        Toast.makeText(mActivity, "startConnection error: " + e,
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // We couldn't create the connect progress bar. If this
                    // happens because of the android life cycle, then we are
                    // fine, and will get back here shortly, otherwise the user
                    // will have to press the connect button again.
                    Log.v(TAG, "Could not show the connect dialog, connecting aborted");
                }
            }
        });
    }

    private final IServiceCallback serviceCallback = new IServiceCallback.Stub() {
        public void onConnectionChanged(final boolean isConnected,
                                        final boolean postConnect,
                                        final boolean loginFailed)
                       throws RemoteException {
            Log.v(TAG, "Connected == " + isConnected + " (postConnect==" + postConnect + ")");
            uiThreadHandler.post(new Runnable() {
                public void run() {
                    setConnected(isConnected, postConnect, loginFailed);
                }
            });
        }

        public void onPlayerChanged(final String playerId,
                                    final String playerName) throws RemoteException {
            Log.v(TAG, "player now " + playerId + ": " + playerName);
            updateUIForPlayer();
        }

        public void onMusicChanged() throws RemoteException {
            uiThreadHandler.post(new Runnable() {
                public void run() {
                    updateSongInfoFromService();
                }
            });
        }

        public void onPlayStatusChanged(boolean newStatus)
                throws RemoteException {
            updatePlayPauseIcon();
        }

        public void onTimeInSongChange(final int secondsIn, final int secondsTotal)
                throws RemoteException {
            NowPlayingFragment.this.secondsIn = secondsIn;
            NowPlayingFragment.this.secondsTotal = secondsTotal;
            uiThreadHandler.sendEmptyMessage(UPDATE_TIME);
        }

        public void onPowerStatusChanged()
                throws RemoteException {
            updateUIForPlayer();
        }

        public void onHandshakeCompleted() throws RemoteException {
            // Do nothing.
        }
    };

}
