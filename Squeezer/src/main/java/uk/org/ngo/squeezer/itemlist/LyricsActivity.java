/*
 * Copyright (c) 2017 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.itemlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.google.common.base.Joiner;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.model.Song;

/**
 * Activity which shows the lyrics of a song
 */

public class LyricsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lyrics_activity);
        TextView titleView = (TextView) findViewById(R.id.song_name);
        TextView albumArtistView = (TextView) findViewById(R.id.artist_album);
        TextView lyricsView = (TextView) findViewById(R.id.lyrics);

        Bundle extras = getIntent().getExtras();
        Song song = extras.getParcelable("song");

        assert song != null;
        titleView.setText(song.getName());
        albumArtistView.setText(Joiner.on(" - ").skipNulls().join(song.getArtist(), song.getAlbumName()));
        lyricsView.setText(song.getLyrics());
    }

    public static void show(Context context, Song song) {
        final Intent intent = new Intent(context, LyricsActivity.class);
        intent.putExtra("song", song);
        context.startActivity(intent);
    }

}
