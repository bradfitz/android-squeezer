/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.framework;


import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.org.ngo.squeezer.itemlists.SqueezerAlbumArtView;
import uk.org.ngo.squeezer.menu.MenuFragment;
import uk.org.ngo.squeezer.menu.SqueezerMenuFragment;
import uk.org.ngo.squeezer.service.SqueezeService;
import uk.org.ngo.squeezer.util.ImageCache;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * This class defines the common minimum, which any activity browsing the
 * SqueezeServer's database must implement.
 * 
 * @author Kurt Aaholst
 */
public abstract class SqueezerItemListActivity extends SqueezerBaseActivity {

    /** Indicates that album artwork can be updated. */
    public static final int MESSAGE_UPDATE_ALBUM_ARTWORK = 0;

    /** How long to wait before showing album artwork, in ms. */
    public static final int DELAY_SHOW_ALBUM_ARTWORK = 250;

    protected final Handler mScrollHandler = new ScrollHandler();

    /** The last known scroll state. */
    protected int mScrollState = ScrollManager.SCROLL_STATE_IDLE;

    protected ListView mListView;

    /** Is the user no longer touching the screen? */
    private boolean mFingerUp = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MenuFragment.add(this, SqueezerMenuFragment.class);
    };

    /**
     * This is called when the service is connected.
     * <p>
     * You must register a callback for {@link SqueezeService} to call when the ordered items
     * from {@link #orderPage(int)} are received from SqueezeServer. This callback must pass
     * these items on to {@link SqueezerItemListAdapter#update(int, int, int, List)}.
     *
     * @throws RemoteException
     */
	protected abstract void registerCallback() throws RemoteException;

	/**
	 * This is called when the service is disconnected.
	 * @throws RemoteException
	 */
	protected abstract void unregisterCallback() throws RemoteException;

    /**
     * Implementations must start an asynchronous fetch of items, when this is called.
     * @throws RemoteException
     * @param start Position in list to start the fetch. Pass this on to {@link SqueezeService}
     */
	protected abstract void orderPage(int start) throws RemoteException;

	private final Set<Integer> orderedPages = new HashSet<Integer>();

	/**
	 * Order page at specified position, if it has not already been ordered.
	 * @param pagePosition
	 */
	public void maybeOrderPage(int pagePosition) {
        if (!orderedPages.contains(pagePosition)) {
			orderedPages.add(pagePosition);
			try {
				orderPage(pagePosition);
			} catch (RemoteException e) {
				Log.e(getTag(), "Error ordering items (" + pagePosition + "): " + e);
			}
		}
	}

	/**
	 * Clear all information about which pages has been ordered, and reorder the first page
	 */
	public void reorderItems() {
		orderedPages.clear();
		maybeOrderPage(0);
	}

    /** An update of the artwork to be displayed is pending */
    public boolean mArtworkUpdateIsPending;

    public int getScrollState() {
	    return mScrollState;
	}

    public boolean isArtworkUpdatePending() {
        return mArtworkUpdateIsPending;
    }

    protected class ScrollManager implements AbsListView.OnScrollListener {
        public ScrollManager() {
        }

        /**
         * If the user has stopped flinging, start showing album artwork,
         * optionally with a delay if their finger is still on the screen. Allow
         * disk access in the cache.
         * <p>
         * If they've started flinging then stop any pending artwork update
         * messages, and pause cache disk access.
         */
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mScrollState == SCROLL_STATE_FLING && scrollState != SCROLL_STATE_FLING) {
                final Handler handler = mScrollHandler;
                final Message message = handler.obtainMessage(MESSAGE_UPDATE_ALBUM_ARTWORK,
                        SqueezerItemListActivity.this);
                handler.removeMessages(MESSAGE_UPDATE_ALBUM_ARTWORK);
                handler.sendMessageDelayed(message, mFingerUp ? 0 : DELAY_SHOW_ALBUM_ARTWORK);
                mArtworkUpdateIsPending = true;
                ImageCache.getInstance().setPauseDiskAccess(false);
            } else if (scrollState == SCROLL_STATE_FLING) {
                mArtworkUpdateIsPending = false;
                mScrollHandler.removeMessages(MESSAGE_UPDATE_ALBUM_ARTWORK);
                ImageCache.getInstance().setPauseDiskAccess(true);
            }

            mScrollState = scrollState;
        }

        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
        }
    }

    protected static class ScrollHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_ALBUM_ARTWORK:
                    ((SqueezerItemListActivity) msg.obj).updateAlbumArtwork();
                    break;
            }
        }
    }

    protected class FingerTracker implements View.OnTouchListener {
        public boolean onTouch(View view, MotionEvent event) {
            final int action = event.getAction();
            mFingerUp = action == MotionEvent.ACTION_UP || action ==
                    MotionEvent.ACTION_CANCEL;
            if (mFingerUp && mScrollState != ScrollManager.SCROLL_STATE_FLING) {
                sendUpdateArtwork();
            }
            return false;
        }
    }

    /**
     * Sends an immediate message that displayed artwork must be refreshed,
     * overriding any delayed messages that may be in the queue.
     */
    private void sendUpdateArtwork() {
        Handler handler = mScrollHandler;
        Message message = handler.obtainMessage(MESSAGE_UPDATE_ALBUM_ARTWORK,
                SqueezerItemListActivity.this);
        handler.removeMessages(MESSAGE_UPDATE_ALBUM_ARTWORK);
        mArtworkUpdateIsPending = true;
        handler.sendMessage(message);
    }

    private final ImageDownloader imageDownloader = new ImageDownloader();

    public void updateAlbumArtwork() {
        mArtworkUpdateIsPending = false;
        final ListView listview = mListView;
        if (listview == null) {
            // XXX: This can be null in the search results expandable list.
            // Maybe we need a ArtworkItemListActivity (or move to fragments
            // here?)
            return;
        }

        final int count = listview.getChildCount();

        for (int i = 0; i < count; i++) {
            final View view = listview.getChildAt(i);
            final SqueezerAlbumArtView.ViewHolder holder = (SqueezerAlbumArtView.ViewHolder) view
                    .getTag();

            if (holder != null && holder.updateArtwork) {
                imageDownloader.downloadUrlToImageView(holder.artworkUrl, holder.icon);
                holder.updateArtwork = false;
            }
        }

        mListView.invalidate();
    }
}
