
package uk.org.ngo.squeezer.test.server;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.test.ServiceTestCase;

import uk.org.ngo.squeezer.itemlists.dialogs.SqueezerAlbumViewDialog.AlbumsSortOrder;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.test.mock.SqueezeboxServerMock;

/**
 * @author Kurt Aaholst <kaaholst@gmail.com>
 */
public class SqueezeServiceTest extends ServiceTestCase<SqueezeService> {

    public SqueezeServiceTest() {
        super(SqueezeService.class);
    }

    public void testConnectionFailure() throws RemoteException, InterruptedException {
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        ISqueezeService service = ISqueezeService.Stub.asInterface(binder);
        ServiceCallbackTest serviceCallback = new ServiceCallbackTest();

        service.registerCallback(serviceCallback);
        service.startConnect("localhost", "test", "test");
        Thread.sleep(1000); //TODO proper synchronization
        
        assertEquals(2, serviceCallback.onConnectionChanged);
        assertFalse(serviceCallback.isConnected);
    }

    public void testConnect() throws RemoteException, InterruptedException {
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        ISqueezeService service = ISqueezeService.Stub.asInterface(binder);
        ServiceCallbackTest serviceCallback = new ServiceCallbackTest();
        WaitForHandshake waitForHandshake = new WaitForHandshake(service);

        SqueezeboxServerMock.starter().start();
        service.registerCallback(serviceCallback);
        service.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT, "test", "test");
        waitForHandshake.waitForHandshakeCompleted();
        
        assertEquals(2, serviceCallback.onConnectionChanged);
        assertTrue(serviceCallback.isConnected);
        assertTrue(service.canMusicfolder());
        assertTrue(service.canRandomplay());
        assertEquals(AlbumsSortOrder.album.name(), service.preferredAlbumSort());
    }

    public void testConnectProtectedServer() throws RemoteException, InterruptedException {
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        ISqueezeService service = ISqueezeService.Stub.asInterface(binder);
        ServiceCallbackTest serviceCallback = new ServiceCallbackTest();
        
        SqueezeboxServerMock.starter().username("user").password("1234").start();

        service.registerCallback(serviceCallback);
        WaitForHandshake waitForHandshake = new WaitForHandshake(service);
        service.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT, "user", "1234");
        waitForHandshake.waitForHandshakeCompleted();
        
        assertEquals(2, serviceCallback.onConnectionChanged);
        assertTrue(serviceCallback.isConnected);
        assertTrue(service.canMusicfolder());
        assertTrue(service.canRandomplay());
        assertEquals(AlbumsSortOrder.album.name(), service.preferredAlbumSort());
    }

    public void testAuthenticationFailure() throws RemoteException, InterruptedException {
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        ISqueezeService service = ISqueezeService.Stub.asInterface(binder);
        ServiceCallbackTest serviceCallback = new ServiceCallbackTest();
        
        SqueezeboxServerMock.starter().username("user").password("1234").start();

        service.registerCallback(serviceCallback);
        service.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT, "test", "test");
        Thread.sleep(1000); //TODO proper synchronization
        
        assertEquals(3, serviceCallback.onConnectionChanged);
        assertFalse(serviceCallback.isConnected);
    }

}
