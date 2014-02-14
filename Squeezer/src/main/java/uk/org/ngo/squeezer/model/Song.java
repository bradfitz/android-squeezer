/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.model;

import android.os.Parcel;

import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.ArtworkItem;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class Song extends ArtworkItem {

    @Override
    public String getPlaylistTag() {
        return "track_id";
    }

    private String name;

    @Override
    public String getName() {
        return name;
    }

    public Song setName(String name) {
        this.name = name;
        return this;
    }

    private String artist;

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    private Album album;

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    private String albumName;

    public String getAlbumName() {
        return albumName;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    private boolean compilation;

    public boolean getCompilation() {
        return compilation;
    }

    public void setCompilation(boolean compilation) {
        this.compilation = compilation;
    }

    private int duration;

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    private int year;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    private String artist_id;

    public String getArtist_id() {
        return artist_id;
    }

    public void setArtist_id(String artist_id) {
        this.artist_id = artist_id;
    }

    private String album_id;

    public String getAlbum_id() {
        return album_id;
    }

    public void setAlbum_id(String album_id) {
        this.album_id = album_id;
    }

    private boolean remote;

    public boolean isRemote() {
        return remote;
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    public int tracknum;

    public int getTracknum() {
        return tracknum;
    }

    public void setTracknum(int tracknum) {
        this.tracknum = tracknum;
    }

    private String artwork_url;

    public String getArtwork_url() {
        return artwork_url;
    }

    public void setArtwork_url(String artworkUrl) {
        artwork_url = artworkUrl;
    }

    public String getArtworkUrl(ISqueezeService service) {
        if (getArtwork_track_id() != null) {
            if (service == null) {
                return null;
            }
            return service.getAlbumArtUrl(getArtwork_track_id());
        }
        return getArtwork_url();
    }

    public Song(Map<String, String> record) {
        if (getId() == null) {
            setId(record.get("track_id"));
        }
        if (getId() == null) {
            setId(record.get("id"));
        }
        setName(record.containsKey("track") ? record.get("track") : record.get("title"));
        setArtist(record.get("artist"));
        setAlbumName(record.get("album"));
        setCompilation(Util.parseDecimalIntOrZero(record.get("compilation")) == 1);
        setDuration(Util.parseDecimalIntOrZero(record.get("duration")));
        setYear(Util.parseDecimalIntOrZero(record.get("year")));
        setArtist_id(record.get("artist_id"));
        setAlbum_id(record.get("album_id"));
        setRemote(Util.parseDecimalIntOrZero(record.get("remote")) != 0);
        setTracknum(Util.parseDecimalInt(record.get("tracknum"), 1));
        setArtwork_url(record.get("artwork_url"));

        // Work around a (possible) bug in the Squeezeserver.
        //
        // I've seen tracks where the "coverart" tag comes back positive (1)
        // but there's no "artwork_track_id" tag. If that happens, use this
        // song's ID as the artwork_track_id.
        String artworkTrackId = record.get("artwork_track_id");
        if (artworkTrackId != null) {
            setArtwork_track_id(artworkTrackId);
        } else {
            // If there's no cover art then the server doesn't respond
            // "coverart:0" or something useful like that, it just doesn't
            // include a response.  Hence these shenanigans.
            String coverArt = record.get("coverart");
            if (coverArt != null && coverArt.equals("1")) {
                setArtwork_track_id(getId());
            }
        }

        Album album = new Album(album_id, albumName);
        album.setArtist(compilation ? "Various" : artist);
        album.setArtwork_track_id(artworkTrackId);
        album.setYear(year);
        setAlbum(album);
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        public Song[] newArray(int size) {
            return new Song[size];
        }

        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }
    };

    private Song(Parcel source) {
        setId(source.readString());
        name = source.readString();
        artist = source.readString();
        albumName = source.readString();
        compilation = source.readInt() == 1;
        duration = source.readInt();
        year = source.readInt();
        artist_id = source.readString();
        album_id = source.readString();
        setArtwork_track_id(source.readString());
        tracknum = source.readInt();
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(name);
        dest.writeString(artist);
        dest.writeString(albumName);
        dest.writeInt(compilation ? 1 : 0);
        dest.writeInt(duration);
        dest.writeInt(year);
        dest.writeString(artist_id);
        dest.writeString(album_id);
        dest.writeString(getArtwork_track_id());
        dest.writeInt(tracknum);
    }

    @Override
    public String toString() {
        return "id=" + getId() + ", name=" + name + ", artist=" + artist + ", year=" + year;
    }

}
