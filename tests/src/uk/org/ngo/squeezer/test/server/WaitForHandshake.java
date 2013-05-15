
package uk.org.ngo.squeezer.test.server;

import junit.framework.AssertionFailedError;
import uk.org.ngo.squeezer.IServiceHandshakeCallback;
import uk.org.ngo.squeezer.service.ISqueezeService;
import android.os.RemoteException;

public class WaitForHandshake extends IServiceHandshakeCallback.Stub {
    private Object handshakeMonitor = new Object();
    private boolean handshakeCompleted;

    public WaitForHandshake(ISqueezeService service) throws RemoteException {
        service.registerHandshakeCallback(this);
    }

    @Override
    synchronized public void onHandshakeCompleted() throws RemoteException {
        synchronized (handshakeMonitor) {
            handshakeCompleted = true;
            handshakeMonitor.notifyAll();
        }
    }

    public void waitForHandshakeCompleted() {
        synchronized (handshakeMonitor) {
            while (!handshakeCompleted)
                try {
                    handshakeMonitor.wait(2000);
                    if (!handshakeCompleted)
                        throw new AssertionFailedError("Expected handshake to complete");
                } catch (InterruptedException e) {
                    System.out.println("InterruptedException caught in waitForHandshakeCompleted");
                }
        }
    }

}
