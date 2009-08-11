package com.danga.squeezeremote;

oneway interface IServiceCallback {
        void onMusicChanged(String artist, String album, String song, String coverArtUrl);
		void onConnectionChanged(boolean isConnected);
		void onPlayStatusChanged(boolean isPlaying);
		void onVolumeChange(int newVolume);
}
