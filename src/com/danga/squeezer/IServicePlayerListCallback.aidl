package com.danga.squeezer;
import com.danga.squeezer.model.SqueezePlayer;

oneway interface IServicePlayerListCallback {
  void onPlayersReceived(int count, int pos, in List<SqueezePlayer> players);
}

