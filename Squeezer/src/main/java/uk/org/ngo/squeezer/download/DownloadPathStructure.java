package uk.org.ngo.squeezer.download;

import android.content.Context;

import java.io.File;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithText;
import uk.org.ngo.squeezer.model.Album;
import uk.org.ngo.squeezer.model.Song;

public enum DownloadPathStructure implements EnumWithText{
    ARTIST_ARTISTALBUM(R.string.download_path_structure_artist_artistalbum) {
        @Override
        public String get(Album album) {
            return new File(album.getArtist(), album.getArtist() + " - " + album.getName()).getPath();
        }
    },
    ARTIST_ALBUM(R.string.download_path_structure_artist_album) {
        @Override
        public String get(Album album) {
            return new File(album.getArtist(), album.getName()).getPath();
        }
    },
    ARTISTALBUM(R.string.download_path_structure_artistalbum) {
        @Override
        public String get(Album album) {
            return album.getArtist() + " - " + album.getName();
        }
    },
    ALBUM(R.string.download_path_structure_album) {
        @Override
        public String get(Album album) {
            return album.getName();
        }
    },
    ARTIST(R.string.download_path_structure_artist) {
        @Override
        public String get(Album album) {
            return album.getArtist();
        }
    };

    private final int labelId;

    protected abstract String get(Album album);

    DownloadPathStructure(int labelId) {
        this.labelId = labelId;
    }

    @Override
    public String getText(Context context) {
        return context.getString(labelId);
    }

    public String get(Song song) {
        return get(song.getAlbum());
    }
}