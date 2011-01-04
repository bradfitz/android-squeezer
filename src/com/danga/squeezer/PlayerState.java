package com.danga.squeezer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerState {
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
    public PlayerState setPlaying(boolean state) {
    	isPlaying.set(state);
    	return this;
    }

    public boolean isPoweredOn() {
		return isPoweredOn.get();
	}
    public PlayerState setPoweredOn(boolean state) {
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
    public PlayerState setCurrentSong(String value) {
    	currentSong.set(value);
    	return this;
    }
	
	public String getCurrentArtist() {
		return currentArtist.get();
	}
	public String getCurrentArtistNonNull() {
		return Util.nonNullString(currentArtist);
	}
    public PlayerState setCurrentArtist(String value) {
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
    public PlayerState setCurrentAlbum(String value) {
    	currentAlbum.set(value);
    	return this;
    }
	
	public String getCurrentArtworkTrackId() {
		return currentArtworkTrackId.get();
	}
    public PlayerState setCurrentArtworkTrackId(String value) {
    	currentArtworkTrackId.set(value);
    	return this;
    }
    public boolean currentArtworkTrackIdUpdated(String value) {
    	return Util.atomicStringUpdated(currentArtworkTrackId, value);
    }
	
	public String getCurrentArtworkUrl() {
		return currentArtworkUrl.get();
	}
    public PlayerState setCurrentArtworkUrl(String value) {
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
    public PlayerState setCurrentTimeSecond(Integer value) {
    	currentTimeSecond.set(value);
    	return this;
    }
	
	public Integer getCurrentSongDuration() {
		return currentSongDuration.get();
	}
	public Integer getCurrentSongDuration(int defaultValue) {
		return Util.getAtomicInteger(currentSongDuration, defaultValue);
	}
    public PlayerState setCurrentSongDuration(Integer value) {
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
