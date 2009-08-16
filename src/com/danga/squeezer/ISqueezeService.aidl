package com.danga.squeezer;

import com.danga.squeezer.IServiceCallback;

interface ISqueezeService {
	    // For the activity to get callbacks on interesting events:
	    void registerCallback(IServiceCallback callback);
        void unregisterCallback(IServiceCallback callback);

	    // Instructing the service to connect to the SqueezeCenter server:
	    // hostPort is the port of the CLI interface.
		void startConnect(String hostPort);
		void disconnect();
        boolean isConnected();

	    // Returns true if players are known.  You should wait for the
	    // onPlayersDiscovered() callback before calling this.
		boolean getPlayers(out List<String> playerId,
   					       out List<String> playerName);

	    // Returns true if the player is known.					    
	    boolean setActivePlayer(in String playerId);

		// Returns the empty string (not null) if no player is set. 
        String getActivePlayerId();
        String getActivePlayerName();

	    ////////////////////
  	    // Depends on active player:
  	    
        boolean isPlaying();
        boolean togglePausePlay();
        boolean play();
        boolean stop();
        boolean nextTrack();
        boolean previousTrack();
        
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

}
