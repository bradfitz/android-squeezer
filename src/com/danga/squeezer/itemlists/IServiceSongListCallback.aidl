package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerSong;

oneway interface IServiceSongListCallback {
  void onSongsReceived(int count, int max, int pos, in List<SqueezerSong> songs);
}

