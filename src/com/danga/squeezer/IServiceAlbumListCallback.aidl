package com.danga.squeezer;
import com.danga.squeezer.model.SqueezeAlbum;

oneway interface IServiceAlbumListCallback {
  void onAlbumsReceived(int count, int pos, in List<SqueezeAlbum> albums);
}

