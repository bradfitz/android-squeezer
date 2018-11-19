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

package uk.org.ngo.squeezer.itemlist;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.org.ngo.squeezer.BuildConfig;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.dialog.ChangeLogDialog;
import uk.org.ngo.squeezer.dialog.TipsDialog;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.ActivePlayerChanged;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;

public class HomeMenuActivity extends BaseListActivity<Plugin> {
    private final String TAG = HomeMenuActivity.class.getSimpleName();

    private GoogleAnalyticsTracker tracker;

    /** Home menu node this activity instance shows */
    private String node;

    /** Home menu tree as received from slimserver */
    private List<Plugin> homeMenu = new ArrayList<>();


    /** View to display when no players are connected. */
    private View emptyView;

    @Override
    protected ItemView<Plugin> createItemView() {
        return new PluginView(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        emptyView = findViewById(android.R.id.empty);

        handleIntent(getIntent());

        // Turn off the home icon.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final SharedPreferences preferences = getSharedPreferences(Preferences.NAME, 0);

        // Enable Analytics if the option is on, and we're not running in debug
        // mode so that debug tests don't pollute the stats.
        if ((!BuildConfig.DEBUG) && preferences.getBoolean(Preferences.KEY_ANALYTICS_ENABLED, true)) {
            Log.v("TAG", "Tracking page view '" + TAG + "'");
            // Start the tracker in manual dispatch mode...
            tracker = GoogleAnalyticsTracker.getInstance();
            tracker.startNewSession("UA-26457780-1", this);
            tracker.trackPageView(TAG);
        }

        // Show the change log if necessary.
        ChangeLogDialog changeLog = new ChangeLogDialog(this);
        if (changeLog.isFirstRun()) {
            if (changeLog.isFirstRunEver()) {
                changeLog.skipLogDialog();
            } else {
                changeLog.getThemedLogDialog().show();
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String node = null;
        if (extras != null) {
            node = extras.getString("node");
        }
        if (node == null) {
            node = "home";
        }
        this.node = node;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        if (service.getActivePlayer() != null) {
            homeMenu.clear();
            service.homeItems(start, this);
        }
    }

    @Override
    public void onItemsReceived(int count, int start, List<Plugin> items) {
        homeMenu.addAll(items);
        if (homeMenu.size() == count) {
            List<Plugin> menu = getMenuNode();
            super.onItemsReceived(menu.size(), 0, menu);
        }
    }

    @Override
    protected void showLoading() {
        // This may be called before emptyView is defined, which is fine cause in this case it is allready invisible
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }

        super.showLoading();
    }

    private List<Plugin> getMenuNode() {
        ArrayList<Plugin> menu = new ArrayList<>();
        for (Plugin item : homeMenu) {
            if (node.equals(item.getNode())) {
                menu.add(item);
            }
        }
        Collections.sort(menu, new Comparator<Plugin>() {
            @Override
            public int compare(Plugin o1, Plugin o2) {
                if (o1.getWeight() == o2.getWeight()) {
                    return o1.getName().compareTo(o2.getName());
                }
                return o1.getWeight() - o2.getWeight();
            }
        });
        return menu;
    }


    @MainThread
    public void onEventMainThread(ActivePlayerChanged event) {
        Log.i(getTag(), "ActivePlayerChanged: " + event.player);
        if (event.player == null) {
            hideLoading();
            emptyView.setVisibility(View.VISIBLE);
            getListView().setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            getListView().setVisibility(View.VISIBLE);
            clearAndReOrderItems();
        }
    }


    @MainThread
    public void onEventMainThread(HandshakeComplete event) {
        super.onEventMainThread(event);

        // Show a tip about volume controls, if this is the first time this app
        // has run. TODO: Add more robust and general 'tips' functionality.
        PackageInfo pInfo;
        try {
            final SharedPreferences preferences = getSharedPreferences(Preferences.NAME,
                    0);

            pInfo = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            if (preferences.getLong("lastRunVersionCode", 0) == 0) {
                new TipsDialog().show(getSupportFragmentManager(), "TipsDialog");
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong("lastRunVersionCode", pInfo.versionCode);
                editor.apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Nothing to do, don't crash.
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Send analytics stats (if enabled).
        if (tracker != null) {
            tracker.dispatch();
            tracker.stopSession();
        }
    }

    public static void show(Context context, String node) {
        final Intent intent = new Intent(context, HomeMenuActivity.class);
        intent.putExtra("node", node);
        context.startActivity(intent);
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, HomeMenuActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("node", "home");
        context.startActivity(intent);
    }

}
