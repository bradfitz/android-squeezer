package uk.org.ngo.squeezer.download;

import android.content.Context;

import java.io.File;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithText;
import uk.org.ngo.squeezer.model.Song;

public enum DownloadPathStructure implements EnumWithText{
    ARTIST_ARTISTALBUM(R.string.download_path_structure_artist_artistalbum) {
        @Override
        public String get(Song song) {
            return new File(song.artist, song.artist + " - " + song.album).getPath();
        }
    },
    ARTIST_ALBUM(R.string.download_path_structure_artist_album) {
        @Override
        public String get(Song song) {
            return new File(song.artist, song.album).getPath();
        }
    },
    ARTISTALBUM(R.string.download_path_structure_artistalbum) {
        @Override
        public String get(Song song) {
            return song.artist + " - " + song.album;
        }
    },
    ALBUM(R.string.download_path_structure_album) {
        @Override
        public String get(Song song) {
            return song.album;
        }
    },
    ARTIST(R.string.download_path_structure_artist) {
        @Override
        public String get(Song song) {
            return song.artist;
        }
    };

    private final int labelId;

    public abstract String get(Song song);

    DownloadPathStructure(int labelId) {
        this.labelId = labelId;
    }

    @Override
    public String getText(Context context) {
        return context.getString(labelId);
    }
}