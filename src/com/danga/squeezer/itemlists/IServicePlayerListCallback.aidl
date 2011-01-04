package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerPlayer;

oneway interface IServicePlayerListCallback {
  void onPlayersReceived(int count, int max, int pos, in List<SqueezerPlayer> players);
}

