package uk.org.ngo.squeezer.test.server;

import android.os.RemoteException;
import uk.org.ngo.squeezer.IServiceCallback;

public class ServiceCallbackTest extends IServiceCallback.Stub {
    int onPlayerChanged;
    String currentPlayerId;
    String currentPlayerName;
    int onConnectionChanged;
    boolean isConnected;
    boolean isPostConnect;
    boolean isLoginFailed;

    @Override
    public void onPlayerChanged(String playerId, String playerName) throws RemoteException {
        onPlayerChanged++;
        currentPlayerId = playerId;
        currentPlayerName = playerName;
    }

    @Override
    public void onConnectionChanged(boolean isConnected, boolean postConnect, boolean loginFailed) throws RemoteException {
        onConnectionChanged++;
        this.isConnected = isConnected;
        isPostConnect = postConnect;
        isLoginFailed = loginFailed;
    }

    @Override
    public void onPlayStatusChanged(String playStatus) throws RemoteException {
        // TODO Auto-generated method stub
    }

    @Override
    public void onShuffleStatusChanged(int shuffleStatus) throws RemoteException {
        // TODO Auto-generated method stub
    }

    @Override
    public void onRepeatStatusChanged(int repeatStatus) throws RemoteException {
        // TODO Auto-generated method stub
    }

    @Override
    public void onTimeInSongChange(int secondsIn, int secondsTotal) throws RemoteException {
        // TODO Auto-generated method stub
    }

    @Override
    public void onPowerStatusChanged(boolean canPowerOn, boolean canPowerOff)
            throws RemoteException {
        // TODO Auto-generated method stub
    }

}
