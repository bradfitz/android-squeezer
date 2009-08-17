package com.danga.squeezer;

oneway interface IServiceCallback {
  void onMusicChanged();
  void onPlayersDiscovered();

  // Empty strings to denote no default player.
  void onPlayerChanged(in String playerId, in String playerName);

  // postConnect is only true for the very first callback after a new initial connect.
  void onConnectionChanged(boolean isConnected, boolean postConnect);
  
  void onPlayStatusChanged(boolean isPlaying);
  void onVolumeChange(int newVolume);
  void onTimeInSongChange(int secondsIn, int secondsTotal);
}

