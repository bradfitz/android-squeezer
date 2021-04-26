package uk.org.ngo.squeezer.model;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.download.DownloadFilenameStructure;
import uk.org.ngo.squeezer.download.DownloadPathStructure;

public class Song {
    public String id;
    public String title;
    public int trackNum;
    public String artist;
    public String album;
    public String albumArtist;

    @NonNull
    public Uri url;

    public Song(Map<String, Object> record) {
        id = Util.getString(record, "id");
        title = Util.getString(record, "title");
        trackNum = Util.getInt(record, "tracknum", 1);
        artist = Util.getString(record, "artist");
        album = Util.getString(record, "album");
        boolean compilation = (Util.getInt(record, "compilation", 0) == 1);
        albumArtist = (compilation ? "Various" : artist); // TODO maybe use the artist role tag ("A")
        url = Uri.parse(Util.getStringOrEmpty(record, "url"));
    }

    public String getLocalPath(DownloadPathStructure downloadPathStructure, DownloadFilenameStructure downloadFilenameStructure) {
        return new File(downloadPathStructure.get(this), downloadFilenameStructure.get(this)).getPath();
    }

    @NonNull
    @Override
    public String toString() {
        return "Song{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", trackNum=" + trackNum +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", albumArtist='" + albumArtist + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}