package uk.org.ngo.squeezer.test.server;

import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.IServiceCallback;

public class ServiceCallbackTest implements IServiceCallback {

    int onPlayerChanged;

    Player currentPlayer;

    int onConnectionChanged;

    boolean isConnected;

    boolean isPostConnect;

    boolean isLoginFailed;


    @Override
    public void onPlayerChanged(Player player) {
        onPlayerChanged++;
        currentPlayer = player;
    }

    @Override
    public void onConnectionChanged(boolean isConnected, boolean postConnect, boolean loginFailed) {
        onConnectionChanged++;
        this.isConnected = isConnected;
        isPostConnect = postConnect;
        isLoginFailed = loginFailed;
    }

    @Override
    public void onPlayStatusChanged(String playStatus) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onShuffleStatusChanged(boolean initial, int shuffleStatus) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onRepeatStatusChanged(boolean initial, int repeatStatus) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onTimeInSongChange(int secondsIn, int secondsTotal) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPowerStatusChanged(boolean canPowerOn, boolean canPowerOff) {
        // TODO Auto-generated method stub
    }

    @Override
    public Object getClient() {
        return this;
    }
}
