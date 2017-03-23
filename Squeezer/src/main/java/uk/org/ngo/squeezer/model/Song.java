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

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.google.common.base.Objects;
import com.google.common.base.Strings;

import java.io.File;
import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.download.DownloadFilenameStructure;
import uk.org.ngo.squeezer.download.DownloadPathStructure;
import uk.org.ngo.squeezer.framework.ArtworkItem;

public class Song extends ArtworkItem {
    private static final String TAG = "Song";

    @Override
    public String getPlaylistTag() {
        return "track_id";
    }

    @Override
    public String getFilterTag() {
        return "track_id";
    }

    /** The "track" or "title" value from the server. */
    @NonNull private final String mName;

    @Override
    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull private final String mArtist;

    @NonNull
    public String getArtist() {
        return mArtist;
    }

    @NonNull private Album mAlbum;

    @NonNull
    public Album getAlbum() {
        return mAlbum;
    }

    @NonNull private final String mAlbumName;

    @NonNull
    public String getAlbumName() {
        return mAlbumName;
    }

    private final boolean mCompilation;

    public boolean getCompilation() {
        return mCompilation;
    }

    private final int mDuration;

    public int getDuration() {
        return mDuration;
    }

    private final int mYear;

    public int getYear() {
        return mYear;
    }

    @NonNull private final String mArtistId;

    @NonNull
    public String getArtistId() {
        return mArtistId;
    }

    @NonNull private final String mAlbumId;

    @NonNull
    public String getAlbumId() {
        return mAlbumId;
    }

    private boolean mRemote;

    public boolean isRemote() {
        return mRemote;
    }

    private final int mTrackNum;

    public int getTrackNum() {
        return mTrackNum;
    }

    /** The URL of the track on the server. This is the file:/// URL, not the URL to download it. */
    @NonNull private final Uri mUrl;

    @NonNull
    public Uri getUrl() {
        return mUrl;
    }

    /** The URL to use to download the song. */
    @NonNull private Uri mDownloadUrl;

    @NonNull
    public Uri getDownloadUrl() {
        return mDownloadUrl;
    }

    @NonNull private final String mButtons;

    @NonNull
    public String getButtons() {
        return mButtons;
    }

    @NonNull private Uri mArtworkUrl = Uri.EMPTY;

    /**
     * @return Whether the song has artwork associated with it.
     */
    public boolean hasArtwork() {
        return ! (mArtworkUrl.equals(Uri.EMPTY));
    }

    @NonNull
    public Uri getArtworkUrl() {
        return mArtworkUrl;
    }

    public Song(Map<String, Object> record) {
        if (getId() == null) {
            setId(getString(record, "track_id"));
        }
        if (getId() == null) {
            setId(getString(record, "id"));
        }

        mName = getStringOrEmpty(record, record.containsKey("track") ? "track" : "title");

        mArtist = getStringOrEmpty(record, "artist");
        mAlbumName = getStringOrEmpty(record, "album");
        mCompilation = getInt(record, "compilation") == 1;
        mDuration = getInt(record, "duration");
        mYear = getInt(record, "year");
        mArtistId = getStringOrEmpty(record, "artist_id");
        mAlbumId = getStringOrEmpty(record, "album_id");
        mRemote = getInt(record, "remote") != 0;
        mTrackNum = getInt(record, "tracknum", 1);

        mArtworkUrl = Uri.parse(getStringOrEmpty(record, "artwork_url"));
        mUrl = Uri.parse(getStringOrEmpty(record, "url"));
        mDownloadUrl = Uri.parse(getStringOrEmpty(record, "download_url"));
        mButtons = getStringOrEmpty(record, "buttons");

        String artworkTrackId = getString(record,"artwork_track_id");

        Album album = new Album(mAlbumId, mAlbumName);
        album.setArtist(mCompilation ? "Various" : mArtist);
        album.setArtwork_track_id(artworkTrackId);
        album.setArtworkUrl(mArtworkUrl);
        album.setYear(mYear);
        mAlbum = album;
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }

        @Override
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }
    };

    private Song(Parcel source) {
        setId(source.readString());
        mName = source.readString();
        mArtist = source.readString();
        mAlbumName = source.readString();
        mCompilation = source.readInt() == 1;
        mDuration = source.readInt();
        mYear = source.readInt();
        mArtistId = source.readString();
        mAlbumId = source.readString();
        setArtwork_track_id(source.readString());
        mTrackNum = source.readInt();
        mUrl = Uri.parse(source.readString());
        mButtons = source.readString();
        mDownloadUrl = Uri.parse(source.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getId());
        dest.writeString(mName);
        dest.writeString(mArtist);
        dest.writeString(mAlbumName);
        dest.writeInt(mCompilation ? 1 : 0);
        dest.writeInt(mDuration);
        dest.writeInt(mYear);
        dest.writeString(mArtistId);
        dest.writeString(mAlbumId);
        dest.writeString(getArtwork_track_id());
        dest.writeInt(mTrackNum);
        dest.writeString(mUrl.toString());
        dest.writeString(mButtons);
        dest.writeString(mDownloadUrl.toString());
    }

    @Override
    public String toStringOpen() {
        return super.toStringOpen() + ", mArtist: " + mArtist + ", year: " + mYear;
    }

    /**
     * Extend the equality test by looking at additional track information.
     * <p>
     * This is to deal with songs from remote streams where the stream might provide a single
     * song ID for multiple consecutive songs in the stream.
     *
     * @param o The object to test.
     * @return
     */
    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }

        // super.equals() has already checked that o is not null and is of the same class.
        Song s = (Song)o;

        if (! s.getName().equals(mName)) {
            return false;
        }

        if (! s.getAlbumName().equals(mAlbumName)) {
            return false;
        }

        if (! s.getArtist().equals(mArtist)) {
            return false;
        }

        if (! s.getArtworkUrl().equals(mArtworkUrl)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId(), mName, mAlbumName, mArtist, mArtworkUrl);
    }

    public String getLocalPath(DownloadPathStructure downloadPathStructure, DownloadFilenameStructure downloadFilenameStructure) {
        return new File(downloadPathStructure.get(this), downloadFilenameStructure.get(this)).getPath();
    }
}
