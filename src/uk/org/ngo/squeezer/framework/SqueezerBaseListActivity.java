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


import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.util.ImageCache.ImageCacheParams;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.util.RetainFragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;

/**
 * A generic base class for an activity to list items of a particular
 * SqueezeServer data type. The data type is defined by the generic type
 * argument, and must be an extension of {@link SqueezerItem}. You must provide
 * an {@link SqueezerItemView} to provide the view logic used by this activity.
 * This is done by implementing
 * {@link #createItemView()}.
 * <p>
 * When the activity is first created ({@link #onCreate(Bundle)}), an empty
 * {@link SqueezerItemListAdapter} is created using the provided
 * {@link SqueezerItemView}. See {@link SqueezerItemListActivity} for see
 * details of ordering and receiving of list items from SqueezeServer, and
 * handling of item selection.
 *
 * @param <T> Denotes the class of the items this class should list
 * @author Kurt Aaholst
 */
public abstract class SqueezerBaseListActivity<T extends SqueezerItem> extends SqueezerItemListActivity {
    private static final String TAG = SqueezerBaseListActivity.class.getName();

    /** Tag for mItemView in mRetainFragment. */
    public static final String TAG_ITEM_VIEW = "itemView";

    /** Tag for _mImageFetcher in mRetainFragment. */
    public static final String TAG_IMAGE_FETCHER = "imageFetcher";

    /** Tag for itemAdapter in mRetainFragment. */
    public static final String TAG_ADAPTER = "adapter";

    private ListView mListView;
	private SqueezerItemAdapter<T> itemAdapter;

    /** Progress bar (spinning) while items are loading. */
    private ProgressBar loadingProgress;
	private SqueezerItemView<T> itemView;

    /** An ImageFetcher for loading thumbnails. */
    private ImageFetcher _mImageFetcher;

    /** ImageCache parameters for the album art. */
    private ImageCacheParams mImageCacheParams;

    /** Fragment to retain information across the activity lifecycle. */
    private RetainFragment mRetainFragment;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        mRetainFragment = RetainFragment.getInstance(TAG, getSupportFragmentManager());

        setContentView(R.layout.item_list);
		mListView = (ListView) findViewById(R.id.item_list);

        loadingProgress = (ProgressBar) findViewById(R.id.loading_progress);
        mListView.setAdapter(getItemAdapter());

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getItemAdapter().onItemSelected(position);
            }
        });

        mListView.setOnScrollListener(new ScrollListener());

        mListView.setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                // Release strong reference when a view is recycled
                final ImageView imageView = (ImageView) view.findViewById(R.id.icon);
                if (imageView != null) {
                    imageView.setImageBitmap(null);
                }
            }
        });

        // Delegate context menu creation to the adapter.
        mListView.setOnCreateContextMenuListener(getItemAdapter());
    }

    private void createImageFetcher() {
        // Get an ImageFetcher to scale artwork to the size of the icon view.
        Resources resources = getResources();
        int iconSize = (Math.max(
                resources.getDimensionPixelSize(R.dimen.album_art_icon_height),
                resources.getDimensionPixelSize(R.dimen.album_art_icon_width)));
        _mImageFetcher = new ImageFetcher(this, iconSize);
        _mImageFetcher.setLoadingImage(R.drawable.icon_pending_artwork);
        mImageCacheParams = new ImageCacheParams(this, "artwork");
        mImageCacheParams.setMemCacheSizePercent(this, 0.12f);
    }

    public ImageFetcher getImageFetcher() {
        if (_mImageFetcher == null) {
            _mImageFetcher = (ImageFetcher) mRetainFragment.get(TAG_IMAGE_FETCHER);
            if (_mImageFetcher == null) {
                createImageFetcher();
                mRetainFragment.put(TAG_IMAGE_FETCHER, _mImageFetcher);
            }
        }

        return _mImageFetcher;
    }

	/**
	 * @return A new view logic to be used by this activity
	 */
	abstract protected SqueezerItemView<T> createItemView();

    @Override
    public final boolean onContextItemSelected(MenuItem menuItem) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) menuItem.getMenuInfo();

        return itemAdapter.doItemContext(menuItem, menuInfo.position);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        maybeOrderVisiblePages(mListView);
    }

    @Override
    public void onResume() {
        super.onResume();
        getImageFetcher().addImageCache(getSupportFragmentManager(), mImageCacheParams);
        if (getService() != null) {
            maybeOrderVisiblePages(mListView);
        }
    }

    @Override
    public void onPause() {
        if (_mImageFetcher != null) {
            _mImageFetcher.closeCache();
        }
        super.onPause();
    }

    /**
     * @return The current {@link SqueezerItemView}, creating it if necessary
     */
    public SqueezerItemView<T> getItemView() {
        if (itemView == null) {
            //noinspection unchecked
            itemView = (SqueezerItemView<T>) mRetainFragment.get(TAG_ITEM_VIEW);
            if (itemView == null) {
                itemView = createItemView();
                mRetainFragment.put(TAG_ITEM_VIEW, itemView);
            }
        }
        return itemView;
    }

    /**
     * @return The current {@link SqueezerItemAdapter}, creating it if
     *         necessary.
     */
    public SqueezerItemAdapter<T> getItemAdapter() {
        if (itemAdapter == null) {
            //noinspection unchecked
            itemAdapter = (SqueezerItemAdapter<T>)mRetainFragment.get(TAG_ADAPTER);
            if (itemAdapter == null) {
                itemAdapter = createItemListAdapter(getItemView());
                mRetainFragment.put(TAG_ADAPTER, itemAdapter);
            }
        }

        return itemAdapter;
    }

	/**
	 * @return The {@link ListView} used by this activity
	 */
    public ListView getListView() {
        return mListView;
    }

    protected SqueezerItemAdapter<T> createItemListAdapter(SqueezerItemView<T> itemView) {
        return new SqueezerItemListAdapter<T>(itemView, getImageFetcher());
    }

    public void onItemsReceived(final int count, final int start, final List<T> items) {
        super.onItemsReceived(count, start);

		getUIThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                mListView.setVisibility(View.VISIBLE);
                loadingProgress.setVisibility(View.GONE);
                getItemAdapter().update(count, start, items);
            }
        });
	}

    protected class ScrollListener extends SqueezerItemListActivity.ScrollListener {
        ScrollListener() {
            super();
        }

        /**
         * Pauses cache disk fetches if the user is flinging the list, or if
         * their finger is still on the screen.
         */
        @Override
        public void onScrollStateChanged(AbsListView listView, int scrollState) {
            super.onScrollStateChanged(listView, scrollState);

            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
                scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                getImageFetcher().setPauseWork(true);
            } else {
                getImageFetcher().setPauseWork(false);
            }
        }
    }
}
