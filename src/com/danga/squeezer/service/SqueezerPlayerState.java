package com.danga.squeezer.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.danga.squeezer.Util;

public class SqueezerPlayerState {
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);
    private final AtomicBoolean isPoweredOn = new AtomicBoolean(false);
    
    private final AtomicReference<String> currentSong = new AtomicReference<String>();
    private final AtomicReference<String> currentArtist = new AtomicReference<String>();
    private final AtomicReference<String> currentAlbum = new AtomicReference<String>();
    private final AtomicReference<String> currentArtworkTrackId = new AtomicReference<String>();
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
    

	public String getCurrentSong() {
		return currentSong.get();
	}
	public String getCurrentSongNonNull() {
		return Util.nonNullString(currentSong);
	}
    public boolean currentSongUpdated(String value) {
    	return Util.atomicStringUpdated(currentSong, value);
    }
    public SqueezerPlayerState setCurrentSong(String value) {
    	currentSong.set(value);
    	return this;
    }
	
	public String getCurrentArtist() {
		return currentArtist.get();
	}
	public String getCurrentArtistNonNull() {
		return Util.nonNullString(currentArtist);
	}
    public SqueezerPlayerState setCurrentArtist(String value) {
    	currentArtist.set(value);
    	return this;
    }
    public boolean currentArtistUpdated(String value) {
    	return Util.atomicStringUpdated(currentArtist, value);
    }
	
	public String getCurrentAlbum() {
		return currentAlbum.get();
	}
    public boolean currentAlbumUpdated(String value) {
    	return Util.atomicStringUpdated(currentAlbum, value);
    }
	public String getCurrentAlbumNonNull() {
		return Util.nonNullString(currentAlbum);
	}
    public SqueezerPlayerState setCurrentAlbum(String value) {
    	currentAlbum.set(value);
    	return this;
    }
	
	public String getCurrentArtworkTrackId() {
		return currentArtworkTrackId.get();
	}
    public SqueezerPlayerState setCurrentArtworkTrackId(String value) {
    	currentArtworkTrackId.set(value);
    	return this;
    }
    public boolean currentArtworkTrackIdUpdated(String value) {
    	return Util.atomicStringUpdated(currentArtworkTrackId, value);
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
        setCurrentArtist(null);
        setCurrentAlbum(null);
        setCurrentArtworkTrackId(null);
        setCurrentArtworkUrl(null);
        setCurrentTimeSecond(null);
        setCurrentSongDuration(null);
	}
    
}
