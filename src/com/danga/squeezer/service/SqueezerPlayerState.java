package com.danga.squeezer.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.danga.squeezer.Util;
import com.danga.squeezer.model.SqueezerSong;

public class SqueezerPlayerState {
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPoweredOn = new AtomicBoolean(false);
    
    private final AtomicReference<SqueezerSong> currentSong = new AtomicReference<SqueezerSong>();
    private final AtomicReference<String> currentArtworkUrl = new AtomicReference<String>();
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
    	SqueezerSong currentValue = currentSong.get();
		if (currentValue == null && newValue == null)
			return false;
		if (currentValue == null || !currentValue.equals(newValue)) {
			currentSong.set(newValue);
			return true;
		}
		return false;
    }

	
	public String getCurrentArtworkUrl() {
		return currentArtworkUrl.get();
	}
    public SqueezerPlayerState setCurrentArtworkUrl(String value) {
    	currentArtworkUrl.set(value);
    	return this;
    }
    public boolean currentArtworkUrlUpdated(String value) {
    	return Util.atomicStringUpdated(currentArtworkUrl, value);
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
