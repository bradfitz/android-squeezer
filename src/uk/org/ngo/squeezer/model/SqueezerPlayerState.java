/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.service.SqueezerServerString;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;


public class SqueezerPlayerState implements Parcelable {

    public SqueezerPlayerState() {
    }

    public static final Creator<SqueezerPlayerState> CREATOR = new Creator<SqueezerPlayerState>() {
        @Override
        public SqueezerPlayerState[] newArray(int size) {
            return new SqueezerPlayerState[size];
        }

        @Override
        public SqueezerPlayerState createFromParcel(Parcel source) {
            return new SqueezerPlayerState(source);
        }
    };

    private SqueezerPlayerState(Parcel source) {
        playStatus = PlayStatus.valueOf(source.readString());
        poweredOn = (source.readByte() == 1);
        shuffleStatus = ShuffleStatus.valueOf(source.readInt());
        repeatStatus = RepeatStatus.valueOf(source.readInt());
        currentSong = source.readParcelable(null);
        currentPlaylistIndex = source.readInt();
        currentTimeSecond = source.readInt();
        currentSongDuration = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(playStatus.name());
        dest.writeByte(poweredOn ? (byte)1 : (byte)0);
        dest.writeInt(shuffleStatus.getId());
        dest.writeInt(repeatStatus.getId());
        dest.writeParcelable(currentSong, 0);
        dest.writeInt(currentPlaylistIndex);
        dest.writeInt(currentTimeSecond);
        dest.writeInt(currentSongDuration);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private boolean poweredOn;

    private PlayStatus playStatus;
    private ShuffleStatus shuffleStatus;
    private RepeatStatus repeatStatus;

    private SqueezerSong currentSong;
    private String currentPlaylist;
    private int currentPlaylistIndex;
    private int currentTimeSecond;
    private int currentSongDuration;

    public boolean isPlaying() {
        return playStatus == PlayStatus.play;
    }
    public PlayStatus getPlayStatus() {
        return playStatus;
    }
    public boolean setPlayStatus(PlayStatus state) {
        boolean changed = playStatus != state;
        playStatus = state;
        return changed;
    }

    public boolean isPoweredOn() {
        return poweredOn;
    }
    public boolean setPoweredOn(boolean state) {
        boolean changed = poweredOn != state;
        poweredOn = state;
        return changed;
    }

    public ShuffleStatus getShuffleStatus() {
        return shuffleStatus;
    }
    public boolean setShuffleStatus(ShuffleStatus status) {
        boolean changed = shuffleStatus != status;
        shuffleStatus = status;
        return changed;
    }

    public RepeatStatus getRepeatStatus() {
        return repeatStatus;
    }
    public boolean setRepeatStatus(RepeatStatus status) {
        boolean changed = repeatStatus != status;
        repeatStatus = status;
        return changed;
    }

    public SqueezerSong getCurrentSong() {
        return currentSong;
    }

    public String getCurrentSongName() {
        return (currentSong != null) ? currentSong.getName() : "";
    }

    public boolean setCurrentSong(SqueezerSong song) {
        boolean changed = (song == null ? (currentSong != null) : !song.equals(currentSong));
        if (changed) currentSong = song;
        return changed;
    }

    public String getCurrentPlaylist() {
        return currentPlaylist;
    }

    public int getCurrentPlaylistIndex() {
        return currentPlaylistIndex;
    }

    public void setCurrentPlaylist(String playlist) {
        currentPlaylist = playlist;
    }

    public SqueezerPlayerState setCurrentPlaylistIndex(int value) {
        currentPlaylistIndex = value;
        return this;
    }

    public int getCurrentTimeSecond() {
        return currentTimeSecond;
    }

    public SqueezerPlayerState setCurrentTimeSecond(int value) {
        currentTimeSecond = value;
        return this;
    }

    public int getCurrentSongDuration() {
        return currentSongDuration;
    }

    public SqueezerPlayerState setCurrentSongDuration(int value) {
        currentSongDuration = value;
        return this;
    }

    public PlayerStateChanged update(Map<String, String> tokenMap) {
        PlayerStateChanged playerStateChanged = new PlayerStateChanged();

        playerStateChanged.musicHasChanged = setCurrentSong(new SqueezerSong(tokenMap));
        playerStateChanged.powerHasChanged = setPoweredOn(Util.parseDecimalIntOrZero(tokenMap.get("power")) == 1);

        playerStateChanged.playStatusHasChanged |= setPlayStatus(PlayStatus.valueOf(tokenMap.get("mode")));
        playerStateChanged.shuffleStatusHasChanged |= setShuffleStatus(ShuffleStatus.valueOf(Util.parseDecimalIntOrZero(tokenMap.get("playlist shuffle"))));
        playerStateChanged.repeatStatusHasChanged |= setRepeatStatus(RepeatStatus.valueOf(Util.parseDecimalIntOrZero(tokenMap.get("playlist repeat"))));

        setCurrentPlaylist(tokenMap.get("playlist_name"));
        setCurrentPlaylistIndex(Util.parseDecimalIntOrZero(tokenMap.get("playlist_cur_index")));

        int lastTime = getCurrentTimeSecond();
        currentTimeSecond = Util.parseDecimalIntOrZero(tokenMap.get("time"));
        currentSongDuration = Util.parseDecimalIntOrZero(tokenMap.get("duration"));
        playerStateChanged.timeHasChanged = (currentTimeSecond != lastTime);

        return playerStateChanged;
    }

    public static enum PlayStatus {
        play,
        pause,
        stop;
    }

    public static enum ShuffleStatus implements EnumWithId {
        SHUFFLE_OFF(0, R.drawable.btn_shuffle_off, SqueezerServerString.SHUFFLE_OFF),
        SHUFFLE_SONG(1, R.drawable.btn_shuffle_song, SqueezerServerString.SHUFFLE_ON_SONGS),
        SHUFFLE_ALBUM(2, R.drawable.btn_shuffle_album, SqueezerServerString.SHUFFLE_ON_ALBUMS);

        private int id;
        private int icon;
        private SqueezerServerString text;
        private static EnumIdLookup<ShuffleStatus> lookup = new EnumIdLookup<ShuffleStatus>(ShuffleStatus.class);

        private ShuffleStatus(int id, int icon, SqueezerServerString text) {
            this.id = id;
            this.icon = icon;
            this.text = text;
        }

        @Override
        public int getId() {
            return id;
        }

        public int getIcon() {
            return icon;
        }

        public SqueezerServerString getText() {
            return text;
        }

        public static ShuffleStatus valueOf(int id) {
            return lookup.get(id);
        }
    }

    public static enum RepeatStatus implements EnumWithId {
        REPEAT_OFF(0, R.drawable.btn_repeat_off, SqueezerServerString.REPEAT_OFF),
        REPEAT_ONE(1, R.drawable.btn_repeat_one, SqueezerServerString.REPEAT_ONE),
        REPEAT_ALL(2, R.drawable.btn_repeat_all, SqueezerServerString.REPEAT_ALL);

        private int id;
        private int icon;
        private SqueezerServerString text;
        private static EnumIdLookup<RepeatStatus> lookup = new EnumIdLookup<RepeatStatus>(RepeatStatus.class);

        private RepeatStatus(int id, int icon, SqueezerServerString text) {
            this.id = id;
            this.icon = icon;
            this.text = text;
        }

        @Override
        public int getId() {
            return id;
        }

        public int getIcon() {
            return icon;
        }

        public SqueezerServerString getText() {
            return text;
        }

        public static RepeatStatus valueOf(int id) {
            return lookup.get(id);
        }
    }

    public interface EnumWithId {
        int getId();
    }

    public static class EnumIdLookup<E extends Enum<E> & EnumWithId> {
        private SparseArray<E> map = new SparseArray<E>();

        public EnumIdLookup(Class<E> enumType) {
            for (E v : enumType.getEnumConstants()) {
                map.put(v.getId(), v);
            }
        }

        public E get(int num) {
            return map.get(num);
        }
    }

    public class PlayerStateChanged {
        public boolean playStatusHasChanged;
        public boolean shuffleStatusHasChanged;
        public boolean repeatStatusHasChanged;
        public boolean musicHasChanged;
        public boolean timeHasChanged;
        public boolean powerHasChanged;
    }

}
