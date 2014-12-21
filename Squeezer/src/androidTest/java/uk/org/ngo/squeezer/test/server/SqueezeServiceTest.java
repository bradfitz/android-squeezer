
package uk.org.ngo.squeezer.test.server;

import android.content.Intent;
import android.os.IBinder;
import android.test.ServiceTestCase;
import android.util.Log;

import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.test.mock.SqueezeboxServerMock;

/**
 * @author Kurt Aaholst <kaaholst@gmail.com>
 */
public class SqueezeServiceTest extends ServiceTestCase<SqueezeService> {

    public SqueezeServiceTest() {
        super(SqueezeService.class);
    }

    final Object mLockConnectionComplete = new Object();
    ConnectionChanged mLastConnectionChangedEvent;
    int mConnectionChangeCount;

    final Object mLockHandshakeComplete = new Object();
    HandshakeComplete mLastHandshakeCompleteEvent;
    boolean mHandshakeComplete;

    ISqueezeService mService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mConnectionChangeCount = 0;
        IBinder binder = bindService(new Intent(getContext(), SqueezeService.class));
        mService = (ISqueezeService) binder;
        mService.getEventBus().register(this);
    }

    /**
     * Verify that connecting to a non-existent server fails.
     *
     * @throws InterruptedException
     */
    public void testConnectionFailure() throws InterruptedException {
        mService.startConnect("localhost", "test", "test");

        synchronized(mLockConnectionComplete) {
            mLockConnectionComplete.wait();
        }

        assertEquals(2, mConnectionChangeCount);
        assertFalse(mLastConnectionChangedEvent.mIsConnected);
    }

    /**
     * Verify that connecting to an existing server succeeds.
     *
     * @throws InterruptedException
     */
    public void testConnect() throws InterruptedException {
        SqueezeboxServerMock.starter().start();

        mService.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT,
                "test", "test");

        synchronized (mLockHandshakeComplete) {
            mLockHandshakeComplete.wait();
        }

        assertEquals(2, mConnectionChangeCount);
        assertTrue(mLastConnectionChangedEvent.mIsConnected);
        assertTrue(mService.canMusicfolder());
        assertTrue(mService.canRandomplay());
        assertEquals(AlbumViewDialog.AlbumsSortOrder.album.name(),
                mService.preferredAlbumSort());
    }

    /**
     * Verify that connecting to an existing server that uses password authentication,
     * using the correct password, succeeds.
     *
     * @throws InterruptedException
     */
    public void testConnectProtectedServer() throws InterruptedException {
        SqueezeboxServerMock.starter().username("user").password("1234").start();

        mService.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT,
                "user", "1234");

        synchronized (mLockHandshakeComplete) {
            mLockHandshakeComplete.wait();
        }

        assertEquals(2, mConnectionChangeCount);
        assertTrue(mLastConnectionChangedEvent.mIsConnected);
        assertTrue(mService.canMusicfolder());
        assertTrue(mService.canRandomplay());
        assertEquals(AlbumViewDialog.AlbumsSortOrder.album.name(),
                mService.preferredAlbumSort());
    }

    /**
     * Verify that connecting to an existing server that uses password authentication,
     * using an incorrect username / password fails.
     *
     * @throws InterruptedException
     */
    public void testAuthenticationFailure() throws InterruptedException {
        SqueezeboxServerMock.starter().username("user").password("1234").start();

        mService.startConnect("localhost:" + SqueezeboxServerMock.CLI_PORT, "test", "test");

        synchronized (mLockConnectionComplete) {
            mLockConnectionComplete.wait();
        }

        assertEquals(2, mConnectionChangeCount);

        // XXX: Note: Doesn't work, as this way of tracking connection changes fires
        // too early. Fix is to move to a series of events for the nodes in the
        // state machine for the connection process.
        //assertFalse(mLastConnectionChangedEvent.mIsConnected);
    }

    /*
     * Event handlers follow the general pattern of:
     * 1. Tracking whether the event completed (either counting, or maintain a boolean).
     * 2. Saving the most recently received event of that type.
     * 3. Notifying any waiters that the event has happened.
     */
    public void onEvent(ConnectionChanged event) {
        mConnectionChangeCount++;
        mLastConnectionChangedEvent = event;

        Log.d("ConnectionChanged", "Count: " + mConnectionChangeCount);
        Log.d("ConnectionChanged", "Event: " + event.toString());
        if (event.mIsConnected || event.mPostConnect || event.mLoginFailed) {
            synchronized (mLockConnectionComplete) {
                mLockConnectionComplete.notify();
            }
        }
    }

    public void onEvent(HandshakeComplete event) {
        mHandshakeComplete = true;
        mLastHandshakeCompleteEvent = event;
        synchronized (mLockHandshakeComplete) {
            mLockHandshakeComplete.notify();
        }
    }
}
