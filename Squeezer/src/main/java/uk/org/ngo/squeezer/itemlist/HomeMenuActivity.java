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


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.Window;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.MenuStatusEvent;

public class HomeMenuActivity extends PluginListActivity {

    /**
     * Tag in retain fragment for the player for the home menu.
     * <p>
     * This is retained, so we can reorder the home menu, if the player has changed when the
     * activity is resumed
     * */
    private static final String TAG_HOME_MENU_PLAYER = "HomeMenuPlayer";

    /**
     * Home menu tree as received from slimserver
     * <p>
     * This is retained to be able to update the home menu from a menu status
     * */
    private static final String TAG_HOME_MENU = "HomeMenu";
    private List<Plugin> homeMenu = new Vector<>();


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        putRetainedValue(TAG_HOME_MENU_PLAYER, getActivePlayerId());
        putRetainedValue(TAG_HOME_MENU, homeMenu);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        homeMenu = (List<Plugin>) getRetainedValue(TAG_HOME_MENU);
    }

    private String getActivePlayerId() {
        ISqueezeService service = getService();
        if (service != null) {
            Player activePlayer = service.getActivePlayer();
            return (activePlayer != null ? activePlayer.getId() : null);
        }
        return null;
    }

    @Override
    protected boolean needPlayer() {
        return true;
    }

    @Override
    @MainThread
    public void onEventMainThread(HandshakeComplete event) {
        super.onEventMainThread(event);
        String homeMenuPlayerId = (String) getRetainedValue(TAG_HOME_MENU_PLAYER);
        if (homeMenuPlayerId != null && (!homeMenuPlayerId.equals(getActivePlayerId()))) {
            clearAndReOrderItems();
        }
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        if (service.getActivePlayer() != null) {
            homeMenu.clear();
            service.homeItems(start, this);
        }
    }

    public void onEvent(MenuStatusEvent event) {
        if (event.playerId.equals(getActivePlayerId())) {
            for (Plugin menuItem : event.menuItems) {
                Plugin item = null;
                for (Plugin menu : homeMenu) {
                    if (menuItem.getId().equals(menu.getId())) {
                        item = menu;
                        break;
                    }
                }
                if (item != null) {
                    homeMenu.remove(item);
                }
                if (MenuStatusEvent.ADD.equals(event.menuDirective)) {
                    homeMenu.add(menuItem);
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    clearItemAdapter();
                }
            });
            updateMenu();
        }
    }

    @Override
    public void onItemsReceived(int count, int start, Map<String, Object> parameters, List<Plugin> items, Class<Plugin> dataType) {
        homeMenu.addAll(items);
        if (homeMenu.size() == count) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (plugin.window == null) {
                        applyWindowStyle(Window.WindowStyle.ICON_TEXT);
                    }
                    clearItemAdapter();
                }
            });
            jiveMainNodes();
            updateMenu();
        }
    }

    private void updateMenu() {
        List<Plugin> menu = getMenuNode(plugin.getId());
        super.onItemsReceived(menu.size(), 0, menu, Plugin.class);
    }

    private void jiveMainNodes() {
        addNode(Plugin.SETTINGS);
        addNode(Plugin.ADVANCED_SETTINGS);
    }

    private void addNode(Plugin plugin) {
        if (!homeMenu.contains(plugin))
            homeMenu.add(plugin);
    }

    private List<Plugin> getMenuNode(String node) {
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

    public static void show(Activity activity, Item plugin) {
        final Intent intent = new Intent(activity, HomeMenuActivity.class);
        intent.putExtra(Plugin.class.getName(), plugin);
        activity.startActivity(intent);
    }

}
