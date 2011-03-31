package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerSong;

oneway interface IServicePlaylistMaintenanceCallback {
  void onRenameFailed(String msg);
  void onCreateFailed(String msg);
}

