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
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class HomeMenuActivity extends BaseListActivity<Plugin> {

    /** Home menu node this activity instance shows */
    private String node;

    /** Home menu tree as received from slimserver */
    private List<Plugin> homeMenu = new ArrayList<>();


    @Override
    protected ItemView<Plugin> createItemView() {
        return new PluginView(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleIntent(getIntent());
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
    protected boolean needPlayer() {
        return true;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        if (service.getActivePlayer() != null) {
            homeMenu.clear();
            service.homeItems(start, this);
        }
    }

    @Override
    public void onItemsReceived(int count, int start, Map<String, Object> parameters, List<Plugin> items, Class<Plugin> dataType) {
        homeMenu.addAll(items);
        if (homeMenu.size() == count) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    clearItemAdapter();
                }
            });
            List<Plugin> menu = getMenuNode();
            HomeMenuActivity.super.onItemsReceived(menu.size(), 0, menu, dataType);
        }
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

    public static void show(Context context, String node) {
        final Intent intent = new Intent(context, HomeMenuActivity.class);
        intent.putExtra("node", node);
        context.startActivity(intent);
    }

}
