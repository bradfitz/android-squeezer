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


import java.lang.reflect.Method;
import java.util.List;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.util.ImageCache.ImageCacheParams;
import uk.org.ngo.squeezer.util.ImageFetcher;
import uk.org.ngo.squeezer.util.RetainFragment;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ViewAnimator;

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

    /** Tag to specify (in extra value of starting intent) whether to use a grid layout in {@link #mListView}*/
    protected static final String TAG_GRID_LAYOUT = "grid_layout";

    /** Tag for _mImageFetcher in mRetainFragment. */
    private static final String TAG_IMAGE_FETCHER = "imageFetcher";

    /** Tag for first visible position in mRetainFragment. */
    private static final String TAG_POSITION = "position";

    /** Tag for itemAdapter in mRetainFragment. */
    private static final String TAG_ADAPTER = "adapter";

    /** Holds the layouts {@link #mListView} can use*/
    private ViewAnimator viewAnimator;

    /** Displays the {@link SqueezerItem}s of {@link #itemAdapter} */
    private AbsListView mListView;

    /**
     * Adapter which holds the {@link SqueezerItem}s of the {@link #mListView},
     * and the {@link SqueezerItemView} which generates the views.
     */
	private SqueezerItemAdapter<T> itemAdapter;

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
        viewAnimator = (ViewAnimator) findViewById(R.id.list_view);
        setupListView();
    }

    /**
     * Set the currently displayed child of the {@link #viewAnimator} by its ID.
     *
     * @param id ID of child.
     * @throws IllegalArgumentException If child with specified ID does not exist.
     * @return The displayed child
     */
    private View setListView(int id) {
        Log.d(getTag(), "setListView(" + (id == R.id.item_grid ? "grid" : (id == R.id.item_list ? "list" : "loading")) + ")");
        for (int i = viewAnimator.getChildCount() - 1; i >= 0; i--) {
            View child = viewAnimator.getChildAt(i);
            if (child.getId() == id) {
                if (i != viewAnimator.getDisplayedChild()) {
                    viewAnimator.setDisplayedChild(i);
                }
                return child;
            }
        }
        throw new IllegalArgumentException("View with ID " + id + " not found");
    }

    /** Get the view ID of the currently displayed child. */
    private int getListViewId() {
        return viewAnimator.getChildAt(viewAnimator.getDisplayedChild()).getId();
    }

    /**
     * Set {@link #mListView} according to the extra value {@link #TAG_GRID_LAYOUT}
     * of the starting intent.
     *
     * @see #getIntent()
     */
    private void setListView() {
        Bundle extras = getIntent().getExtras();
        boolean isGrid = (extras != null && extras.getBoolean(TAG_GRID_LAYOUT, false));
        mListView = (AbsListView) setListView((isGrid ? R.id.item_grid : R.id.item_list));
    }

    /** Initialize {@link #mListView} */
    private void setupListView() {
        setListView();

        // Delegate item selection handling to the adapter
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

    /**
     * Call this to dynamically change {@link #mListView} to the one specified in
     * {@link #TAG_GRID_LAYOUT}.
     */
    protected void setSelectedView() {
        saveVisiblePosition();
        setupListView();
        setAdapter();
        getItemAdapter().setItemView(createItemView());
    }

    /**
     * Set our adapter on the listview.
     * <p>
     * This can't be done in {@link #onCreate(android.os.Bundle)} because
     * getView might be called before the service is connected, so we need to
     * delay it.
     * <p>
     * However when we set the adapter after onCreate the list is scrolled to
     * top, so we retain the visible position.
     * <p>
     * Call this method when the service is connected
     */
    private void setAdapter() {
        // setAdapter is not defined for AbsListView before API level 11, but
        // it is for concrete implementations, so we call it by reflection
        try {
            Method method = mListView.getClass().getMethod("setAdapter", ListAdapter.class);
            method.invoke(mListView, getItemAdapter());
        } catch (Exception e) {
            Log.e(getTag(), "Error calling 'setAdapter'", e);
        }

        Integer position = (Integer)mRetainFragment.get(TAG_POSITION);
        if (position != null) {
            if (mListView instanceof ListView)
                ((ListView)mListView).setSelectionFromTop(position, 0);
            else
                mListView.setSelection(position);
        }
    }

    protected ImageFetcher createImageFetcher() {
        // Get an ImageFetcher to scale artwork to the size of the icon view.
        Resources resources = getResources();
        int iconSize = (Math.max(
                resources.getDimensionPixelSize(R.dimen.album_art_icon_height),
                resources.getDimensionPixelSize(R.dimen.album_art_icon_width)));
        ImageFetcher imageFetcher = new ImageFetcher(this, iconSize);
        imageFetcher.setLoadingImage(R.drawable.icon_pending_artwork);
        return  imageFetcher;
    }

    protected void createImageCacheParams() {
        mImageCacheParams = new ImageCacheParams(this, "artwork");
        mImageCacheParams.setMemCacheSizePercent(this, 0.12f);
    }

    public ImageFetcher getImageFetcher() {
        if (_mImageFetcher == null) {
            _mImageFetcher = (ImageFetcher) mRetainFragment.get(TAG_IMAGE_FETCHER);
            if (_mImageFetcher == null) {
                _mImageFetcher = createImageFetcher();
                createImageCacheParams();
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
        setAdapter();
    }

    @Override
    public void onResume() {
        super.onResume();
        getImageFetcher().addImageCache(getSupportFragmentManager(), mImageCacheParams);
        if (getService() != null) {
            maybeOrderVisiblePages(mListView);
            setAdapter();
        }
    }

    @Override
    public void onPause() {
        if (_mImageFetcher != null) {
            _mImageFetcher.closeCache();
        }
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveVisiblePosition();
    }

    /**
     * Store the first visible position of {@link #mListView}, in the {@link #mRetainFragment},
     * so we can later retrieve it.
     *
     * @see android.widget.AbsListView#getFirstVisiblePosition()
     */
    private void saveVisiblePosition() {
        mRetainFragment.put(TAG_POSITION, mListView.getFirstVisiblePosition());
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
                itemAdapter = createItemListAdapter(createItemView());
                mRetainFragment.put(TAG_ADAPTER, itemAdapter);
            }
        }

        return itemAdapter;
    }

	/**
	 * @return The {@link AbsListView} used by this activity
	 */
    public AbsListView getListView() {
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
                if (getListViewId() == R.id.loading_progress) {
                    setListView();
                }
                getItemAdapter().update(count, start, items);
            }
        });
	}

    @Override
    public boolean maybeOrderPage(int pagePosition) {
        // If page 0 was requested then this is the first page of data.  Hide the listview,
        // and make loadingProgress visible to provide feedback to the user.
        // TODO: This should be removed in favour of showing a progress spinner in the actionbar.
        if (super.maybeOrderPage(pagePosition)) {
            if (pagePosition == 0) {
                setListView(R.id.loading_progress);
            }
            return true;
        } else {
            return false;
        }
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
