package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerPlugin;

oneway interface IServicePluginListCallback {
  void onPluginsReceived(int count, int pos, in List<SqueezerPlugin> albums);
}

