package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerPluginItem;

oneway interface IServicePluginItemListCallback {
  void onPluginItemsReceived(int count, int pos, in Map parameters, in List<SqueezerPluginItem> albums);
}

