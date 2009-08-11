package com.danga.squeezeremote;

import com.danga.squeezeremote.IServiceCallback;

interface ISqueezeService {
		void startConnect(String hostPort);
		void disconnect();

        boolean isConnected();
        boolean isPlaying();
	    void registerCallback(IServiceCallback callback);
        void unregisterCallback(IServiceCallback callback);
        boolean togglePausePlay();
        boolean play();
        boolean stop();
        int adjustVolumeBy(int delta);
}
