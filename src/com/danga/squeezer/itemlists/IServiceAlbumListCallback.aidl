package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerAlbum;

oneway interface IServiceAlbumListCallback {
  void onAlbumsReceived(int count, int start, in List<SqueezerAlbum> albums);
}

