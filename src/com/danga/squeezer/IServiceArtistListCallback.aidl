package com.danga.squeezer;
import com.danga.squeezer.model.SqueezeArtist;

oneway interface IServiceArtistListCallback {
  void onArtistsReceived(int count, int pos, in List<SqueezeArtist> albums);
}

