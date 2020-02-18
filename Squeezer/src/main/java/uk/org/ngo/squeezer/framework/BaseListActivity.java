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


import android.os.Bundle;
import androidx.annotation.MainThread;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.RecyclerListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.util.ImageFetcher;


/**
 * A generic base class for an activity to list items of a particular SqueezeServer data type. The
 * data type is defined by the generic type argument, and must be an extension of {@link Item}. You
 * must provide an {@link ItemView} to provide the view logic used by this activity. This is done by
 * implementing {@link #createItemView()}.
 * <p>
 * When the activity is first created ({@link #onCreate(Bundle)}), an empty {@link ItemAdapter}
 * is created using the provided {@link ItemView}. See {@link ItemListActivity} for see details of
 * ordering and receiving of list items from SqueezeServer, and handling of item selection.
 *
 * @param <T> Denotes the class of the items this class should list
 *
 * @author Kurt Aaholst
 */
public abstract class BaseListActivity<T extends Item> extends ItemListActivity implements IServiceItemListCallback<T> {

    /**
     * Tag for first visible position in mRetainFragment.
     */
    private static final String TAG_POSITION = "position";

    /**
     * Tag for itemAdapter in mRetainFragment.
     */
    public static final String TAG_ADAPTER = "adapter";

    private ItemAdapter<T> itemAdapter;

    /**
     * Can't do much here, as content is based on settings, and which data to display, which is controlled by data
     * returned from server.
     * <p>
     * See {@link #setupListView(AbsListView)} and {@link #onItemsReceived(int, int, Map, List, Class)} for the actual setup of
     * views and adapter
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
    }

    @Override
    protected AbsListView setupListView(AbsListView listView) {
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                getItemAdapter().onItemSelected(view, position);
            }
        });

        listView.setOnScrollListener(new ScrollListener());

        listView.setRecyclerListener(new RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                // Release strong reference when a view is recycled
                final ImageView imageView = view.findViewById(R.id.icon);
                if (imageView != null) {
                    imageView.setImageBitmap(null);
                }
            }
        });

        setupAdapter(listView);

        return listView;
    }

    @MainThread
    public void onEventMainThread(HandshakeComplete event) {
        super.onEventMainThread(event);
        if (!needPlayer() || getService().getActivePlayer() != null) {
            maybeOrderVisiblePages(getListView());
        } else {
            showEmptyView();
        }
    }

    /**
     * Returns the ID of a content view to be used by this list activity.
     * <p>
     * The content view must contain a {@link AbsListView} with the id {@literal item_list} in order
     * to be valid.
     *
     * @return The ID
     */
    protected int getContentView() {
        return R.layout.slim_browser_layout;
    }

    /**
     * @return A new view logic to be used by this activity
     */
    abstract protected ItemView<T> createItemView();

    /**
     * Set our adapter on the list view.
     * <p>
     * This can't be done in {@link #onCreate(android.os.Bundle)} because getView might be called
     * before the handshake is complete, so we need to delay it.
     * <p>
     * However when we set the adapter after onCreate the list is scrolled to top, so we retain the
     * visible position.
     * <p>
     * Call this method after the handshake is complete.
     */
    private void setupAdapter(AbsListView listView) {
        listView.setAdapter(getItemAdapter());

        Integer position = (Integer) getRetainedValue(TAG_POSITION);
        if (position != null) {
            if (listView instanceof ListView) {
                ((ListView) listView).setSelectionFromTop(position, 0);
            } else {
                listView.setSelection(position);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveVisiblePosition();
    }

    /**
     * Store the first visible position of {@link #getListView()}, in the retain fragment, so
     * we can later retrieve it.
     *
     * @see android.widget.AbsListView#getFirstVisiblePosition()
     */
    private void saveVisiblePosition() {
        putRetainedValue(TAG_POSITION, getListView().getFirstVisiblePosition());
    }

    /**
     * @return The current {@link ItemAdapter}'s {@link ItemView}
     */
    public ItemView<T> getItemView() {
        return getItemAdapter().getItemView();
    }

    /**
     * @return The current {@link ItemAdapter}, creating it if necessary.
     */
    public ItemAdapter<T> getItemAdapter() {
        if (itemAdapter == null) {
            //noinspection unchecked
            itemAdapter = (ItemAdapter<T>) getRetainedValue(TAG_ADAPTER);
            if (itemAdapter == null) {
                itemAdapter = createItemListAdapter(createItemView());
                putRetainedValue(TAG_ADAPTER, itemAdapter);
            } else {
                // We have just retained the item adapter, we need to create a new
                // item view logic, cause it holds a reference to the old activity
                itemAdapter.setItemView(createItemView());
                // Update views with the count from the retained item adapter
                itemAdapter.onCountUpdated();
            }
        }

        return itemAdapter;
    }

    @Override
    protected void clearItemAdapter() {
        getItemAdapter().clear();
    }

    protected ItemAdapter<T> createItemListAdapter(ItemView<T> itemView) {
        return new ItemAdapter<>(itemView);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <IT extends Item> void updateAdapter(int count, int start, List<IT> items, Class<IT> dataType) {
        getItemAdapter().update(count, start, (List<T>) items);
    }

    @Override
    public void onItemsReceived(int count, int start, Map<String, Object> parameters, List<T> items, Class<T> dataType) {
        super.onItemsReceived(count, start, items, dataType);
    }

    @Override
    public Object getClient() {
        return this;
    }

    protected class ScrollListener extends ItemListActivity.ScrollListener {

        ScrollListener() {
            super();
        }

        /**
         * Pauses cache disk fetches if the user is flinging the list, or if their finger is still
         * on the screen.
         */
        @Override
        public void onScrollStateChanged(AbsListView listView, int scrollState) {
            super.onScrollStateChanged(listView, scrollState);

            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
                    scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                ImageFetcher.getInstance(BaseListActivity.this).setPauseWork(true);
            } else {
                ImageFetcher.getInstance(BaseListActivity.this).setPauseWork(false);
            }
        }
    }
}
