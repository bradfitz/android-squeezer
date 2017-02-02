/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ExpandableListView;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;

public class SearchActivity extends ItemListActivity {

    private View loadingLabel;

    private ExpandableListView resultsExpandableListView;

    private SearchAdapter searchResultsAdapter;

    private String searchString;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_layout);

        loadingLabel = findViewById(R.id.loading_label);

        searchResultsAdapter = new SearchAdapter(this);
        resultsExpandableListView = (ExpandableListView) findViewById(R.id.search_expandable_list);
        resultsExpandableListView.setOnScrollListener(new ScrollListener());

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            doSearch(query);
        }
    }

    @Override
    public final boolean onContextItemSelected(MenuItem menuItem) {
        if (getService() != null) {
            return searchResultsAdapter.doItemContext(menuItem);
        }
        return false;
    }

    /**
     * Performs the search now that the service connection is active.
     */
    @Override
    protected void onServiceConnected(@NonNull ISqueezeService service) {
        super.onServiceConnected(service);
        doSearch();
    }

    /**
     * Setting the list adapter will trigger a layout pass, which requires information from
     * the server.  Only do this after the handshake has completed.  When done, perform the
     * search.
     */
    public void onEventMainThread(HandshakeComplete event) {
        resultsExpandableListView.setAdapter(searchResultsAdapter);
        doSearch();
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        service.search(start, searchString, itemListCallback);
    }

    @Override
    public void maybeOrderVisiblePages(AbsListView listView) {
        final int firstVisiblePosition = listView.getFirstVisiblePosition();
        int currentPagePosition = -1;
        for (int pos = 0; pos < listView.getChildCount(); pos++) {
            long packedPosition = resultsExpandableListView.getExpandableListPosition(firstVisiblePosition + pos);
            if (ExpandableListView.PACKED_POSITION_TYPE_CHILD == ExpandableListView.getPackedPositionType(packedPosition)) {
                final int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
                final Integer colCount = searchResultsAdapter.getGroup(groupPosition).getColCount();
                final int childPosition = ExpandableListView.getPackedPositionChild(packedPosition) * colCount;
                int pagePosition = (childPosition / mPageSize) * mPageSize;
                if (pagePosition != currentPagePosition) {
                    maybeOrderPage(currentPagePosition = pagePosition);
                }
            }
        }
    }

    /**
     * Saves the search query, and attempts to query the service for <code>searchString</code>. If
     * the service binding has not completed yet then {@link #onEventMainThread(HandshakeComplete)}
     * will re-query for the saved search query.
     *
     * @param searchString The string to search fo.
     */
    private void doSearch(String searchString) {
        this.searchString = searchString;
        if (searchString != null && searchString.length() > 0 && getService() != null) {
            clearAndReOrderItems();
        }
    }

    @Override
    protected void clearItemAdapter() {
        resultsExpandableListView.setVisibility(View.GONE);
        loadingLabel.setVisibility(View.VISIBLE);
        searchResultsAdapter.clear();
    }

    /**
     * Searches for the saved search query.
     */
    private void doSearch() {
        doSearch(searchString);
    }

    private final IServiceItemListCallback itemListCallback = new IServiceItemListCallback() {
        @Override
        public void onItemsReceived(final int count, final int start, Map parameters, final List items, final Class dataType) {
            SearchActivity.super.onItemsReceived(count, start, items.size());

            getUIThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    searchResultsAdapter.updateItems(count, start, items, dataType);
                    loadingLabel.setVisibility(View.GONE);
                    resultsExpandableListView.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public Object getClient() {
            return SearchActivity.this;
        }
    };

}
