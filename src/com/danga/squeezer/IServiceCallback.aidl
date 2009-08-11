package com.danga.squeezer;

oneway interface IServiceCallback {
  void onMusicChanged(String artist, String album, String song, String coverArtUrl);
  void onPlayersDiscovered();
  void onPlayerChanged(in String playerId, in String playerName);
  void onConnectionChanged(boolean isConnected);
  void onPlayStatusChanged(boolean isPlaying);
  void onVolumeChange(int newVolume);
}
