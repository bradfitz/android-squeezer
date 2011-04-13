package com.danga.squeezer.itemlists;
import com.danga.squeezer.model.SqueezerYear;

oneway interface IServiceYearListCallback {
  void onYearsReceived(int count, int pos, in List<SqueezerYear> albums);
}

