package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerArtist;

oneway interface IServiceArtistListCallback {
  void onArtistsReceived(int count, int max, int start, in List<SqueezerArtist> artists);
}

