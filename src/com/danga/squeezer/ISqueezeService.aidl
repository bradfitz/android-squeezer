package com.danga.squeezer;

import com.danga.squeezer.IServiceCallback;
import com.danga.squeezer.itemlists.IServicePlayerListCallback;
import com.danga.squeezer.itemlists.IServiceAlbumListCallback;
import com.danga.squeezer.itemlists.IServiceArtistListCallback;
import com.danga.squeezer.itemlists.IServiceYearListCallback;
import com.danga.squeezer.itemlists.IServiceGenreListCallback;
import com.danga.squeezer.itemlists.IServiceSongListCallback;
import com.danga.squeezer.model.SqueezerPlayer;
import com.danga.squeezer.model.SqueezerSong;
import com.danga.squeezer.model.SqueezerAlbum;
import com.danga.squeezer.model.SqueezerArtist;
import com.danga.squeezer.model.SqueezerYear;
import com.danga.squeezer.model.SqueezerGenre;

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
	    void setActivePlayer(in SqueezerPlayer player);

		// Returns the empty string (not null) if no player is set. 
        String getActivePlayerId();
        String getActivePlayerName();

	    ////////////////////
  	    // Depends on active player:
  	    
  	    boolean canPowerOn();
  	    boolean canPowerOff();
        boolean powerOn();
        boolean powerOff();
        boolean canRandomplay();
        boolean isPlaying();
        boolean togglePausePlay();
        boolean play();
        boolean stop();
        boolean nextTrack();
        boolean previousTrack();
        boolean playSong(in SqueezerSong song);
        boolean playAlbum(in SqueezerAlbum album);
        boolean randomPlay(String type);
        boolean playlistIndex(int index);
        
        // Return 0 if unknown:
        int getSecondsTotal();
        int getSecondsElapsed();
        
        // Never return null:  (always empty string when unknown)
        String currentArtist();
        String currentAlbum();
        String currentSong();
        String currentAlbumArtUrl();
        String getAlbumArtUrl(String artworkTrackId);

        // Returns new (predicted) volume.  Typical deltas are +10 or -10.
        // Note the volume changed callback will also still be run with
        // the correct value as returned by the server later.
        int adjustVolumeBy(int delta);
        
        // Player list
        boolean players(int start);
	    void registerPlayerListCallback(IServicePlayerListCallback callback);
        void unregisterPlayerListCallback(IServicePlayerListCallback callback);
        
        // Album list
        boolean albums(int start, String sortOrder, String searchString, in SqueezerArtist artist, in SqueezerYear year, in SqueezerGenre genre);
	    void registerAlbumListCallback(IServiceAlbumListCallback callback);
        void unregisterAlbumListCallback(IServiceAlbumListCallback callback);
        
        // Artist list
        boolean artists(int start, String searchString, in SqueezerGenre genre);
	    void registerArtistListCallback(IServiceArtistListCallback callback);
        void unregisterArtistListCallback(IServiceArtistListCallback callback);
        
        // Year list
        boolean years(int start);
	    void registerYearListCallback(IServiceYearListCallback callback);
        void unregisterYearListCallback(IServiceYearListCallback callback);
        
        // Year list
        boolean genres(int start);
	    void registerGenreListCallback(IServiceGenreListCallback callback);
        void unregisterGenreListCallback(IServiceGenreListCallback callback);
        
        // Song list
        boolean songs(int start, String sortOrder, String searchString, in SqueezerAlbum album, in SqueezerArtist artist, in SqueezerYear year, in SqueezerGenre genre);
        boolean currentPlaylist(int start);
	    void registerSongListCallback(IServiceSongListCallback callback);
        void unregisterSongListCallback(IServiceSongListCallback callback);
        
        // Search
        boolean search(int start, String searchString);

}
