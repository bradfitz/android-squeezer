package com.danga.squeezer;

import java.util.Arrays;


import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class SqueezerRandomMixActivity extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String[] values = getResources().getStringArray(R.array.random_mix_items);
		int[] icons = new int[values.length];
		Arrays.fill(icons, R.drawable.icon_ml_random);
		icons[icons.length -1] = R.drawable.icon_ml_genres;
		setListAdapter(new IconRowAdapter(this, values, icons));

		getListView().setOnItemClickListener(onItemClick);
	}

	private OnItemClickListener onItemClick = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			Toast.makeText(SqueezerRandomMixActivity.this, "option " + position, Toast.LENGTH_LONG).show();
			switch (position) {
			}
		}
	};

	static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerRandomMixActivity.class);
        context.startActivity(intent);
    }

}
