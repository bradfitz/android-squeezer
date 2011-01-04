package com.danga.squeezer;

import com.danga.squeezer.IServiceCallback;
import com.danga.squeezer.IServicePlayerListCallback;
import com.danga.squeezer.IServiceAlbumListCallback;
import com.danga.squeezer.IServiceArtistListCallback;
import com.danga.squeezer.IServiceSongListCallback;
import com.danga.squeezer.model.SqueezePlayer;
import com.danga.squeezer.model.SqueezeAlbum;
import com.danga.squeezer.model.SqueezeArtist;

interface ISqueezeService {
	    // For the activity to get callbacks on interesting events:
	    void registerCallback(IServiceCallback callback);
        void unregisterCallback(IServiceCallback callback);

	    // Instructing the service to connect to the SqueezeCenter server:
	    // hostPort is the port of the CLI interface.
		void startConnect(String hostPort);
		void disconnect();
        boolean isConnected();
        
        // For the SettingsActivity to notify the Service that a setting changed.
        void preferenceChanged(String key);

		// Call this to change the player we are controlling
	    void setActivePlayer(in SqueezePlayer player);

		// Returns the empty string (not null) if no player is set. 
        String getActivePlayerId();
        String getActivePlayerName();

	    ////////////////////
  	    // Depends on active player:
  	    
  	    boolean canPowerOn();
  	    boolean canPowerOff();
        boolean powerOn();
        boolean powerOff();
        boolean isPlaying();
        boolean togglePausePlay();
        boolean play();
        boolean stop();
        boolean nextTrack();
        boolean previousTrack();
        boolean playAlbum(in SqueezeAlbum album);
        boolean playlistIndex(int index);
        
        // Return 0 if unknown:
        int getSecondsTotal();
        int getSecondsElapsed();
        
        // Never return null:  (always empty string when unknown)
        String currentArtist();
        String currentAlbum();
        String currentSong();
        String currentAlbumArtUrl();

        // Returns new (predicted) volume.  Typical deltas are +10 or -10.
        // Note the volume changed callback will also still be run with
        // the correct value as returned by the server later.
        int adjustVolumeBy(int delta);
        
        // Player list activity
        boolean players();
	    void registerPlayerListCallback(IServicePlayerListCallback callback);
        void unregisterPlayerListCallback(IServicePlayerListCallback callback);
        
        // Album list activity
        boolean albums(in SqueezeArtist artist);
	    void registerAlbumListCallback(IServiceAlbumListCallback callback);
        void unregisterAlbumListCallback(IServiceAlbumListCallback callback);
        
        // Artist list activity
        boolean artists();
	    void registerArtistListCallback(IServiceArtistListCallback callback);
        void unregisterArtistListCallback(IServiceArtistListCallback callback);
        
        // Song list activity
        boolean songs();
	    void registerSongListCallback(IServiceSongListCallback callback);
        void unregisterSongListCallback(IServiceSongListCallback callback);
}
