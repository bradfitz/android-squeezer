/*
 * Copyright (c) 2009 Brad Fitzpatrick <brad@danga.com>
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

package com.danga.squeezer.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.danga.squeezer.Util;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerPlayerState {
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPoweredOn = new AtomicBoolean(false);

    private final AtomicReference<SqueezerSong> currentSong = new AtomicReference<SqueezerSong>();
    private final AtomicReference<Integer> currentTimeSecond = new AtomicReference<Integer>();
    private final AtomicReference<Integer> currentSongDuration = new AtomicReference<Integer>();


	public boolean isPlaying() {
		return isPlaying.get();
	}
    public SqueezerPlayerState setPlaying(boolean state) {
    	isPlaying.set(state);
    	return this;
    }

    public boolean isPoweredOn() {
		return isPoweredOn.get();
	}
    public SqueezerPlayerState setPoweredOn(boolean state) {
    	isPoweredOn.set(state);
    	return this;
    }

    public SqueezerSong getCurrentSong() {
    	return currentSong.get();
    }
    public String getCurrentSongName() {
    	SqueezerSong song = currentSong.get();
    	return (song != null) ? song.getName() :  "";
    }
    public SqueezerPlayerState setCurrentSong(SqueezerSong song) {
    	currentSong.set(song);
    	return this;
    }
    public boolean setCurrentSongUpdated(SqueezerSong newValue) {
    	return Util.atomicSongUpdated(currentSong, newValue);
    }

	public Integer getCurrentTimeSecond() {
		return currentTimeSecond.get();
	}
	public Integer getCurrentTimeSecond(int defaultValue) {
		return Util.getAtomicInteger(currentTimeSecond, defaultValue);
	}
    public SqueezerPlayerState setCurrentTimeSecond(Integer value) {
    	currentTimeSecond.set(value);
    	return this;
    }

	public Integer getCurrentSongDuration() {
		return currentSongDuration.get();
	}
	public Integer getCurrentSongDuration(int defaultValue) {
		return Util.getAtomicInteger(currentSongDuration, defaultValue);
	}
    public SqueezerPlayerState setCurrentSongDuration(Integer value) {
    	currentSongDuration.set(value);
    	return this;
    }

	public void clear() {
		setPlaying(false);
        setCurrentSong(null);
        setCurrentTimeSecond(null);
        setCurrentSongDuration(null);
	}

}
