package com.danga.squeezer;
import com.danga.squeezer.model.SqueezeSong;

oneway interface IServiceSongListCallback {
  void onSongsReceived(int count, int pos, in List<SqueezeSong> songs);
}

