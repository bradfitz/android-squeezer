package com.danga.squeezer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerState {

    private final AtomicReference<String> currentSong = new AtomicReference<String>();
    private final AtomicReference<String> currentArtist = new AtomicReference<String>();
    private final AtomicReference<String> currentAlbum = new AtomicReference<String>();
    private final AtomicReference<String> currentArtworkTrackId = new AtomicReference<String>();
    private final AtomicReference<Integer> currentTimeSecond = new AtomicReference<Integer>();
    private final AtomicReference<Integer> currentSongDuration = new AtomicReference<Integer>();
    private final AtomicBoolean isPlaying = new AtomicBoolean(false);

}
