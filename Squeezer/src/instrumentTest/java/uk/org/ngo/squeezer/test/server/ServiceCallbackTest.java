package uk.org.ngo.squeezer.test.server;

import android.os.RemoteException;

import uk.org.ngo.squeezer.IServiceCallback;
import uk.org.ngo.squeezer.model.Player;

public class ServiceCallbackTest extends IServiceCallback.Stub {

    int onPlayerChanged;

    Player currentPlayer;

    int onConnectionChanged;

    boolean isConnected;

    boolean isPostConnect;

    boolean isLoginFailed;


    @Override
    public void onPlayerChanged(Player player) throws RemoteException {
        onPlayerChanged++;
        currentPlayer = player;
    }

    @Override
    public void onConnectionChanged(boolean isConnected, boolean postConnect, boolean loginFailed)
            throws RemoteException {
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
    public void onShuffleStatusChanged(boolean initial, int shuffleStatus) throws RemoteException {
        // TODO Auto-generated method stub
    }

    @Override
    public void onRepeatStatusChanged(boolean initial, int repeatStatus) throws RemoteException {
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
