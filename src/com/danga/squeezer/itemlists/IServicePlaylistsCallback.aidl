package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerPlaylist;

oneway interface IServicePlaylistsCallback {
  void onPlaylistsReceived(int count, int max, int pos, in List<SqueezerPlaylist> albums);
}

