/*
 * Copyright (c) 2019 Kurt Aaholst <kaaholst@gmail.com>
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
import androidx.fragment.app.FragmentManager;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;
import java.util.Stack;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.dialog.ArtworkDialog;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;

/**
 * Delegate with view logic for {@link Plugin} which can be used from any {@link BaseActivity}
 */
public class PluginViewLogic implements IServiceItemListCallback<Plugin> {
    private final BaseActivity activity;

    public PluginViewLogic(BaseActivity activity) {
        this.activity = activity;
    }

    /**
     * Perform the <code>go</code> action of the supplied item.
     * <p>
     * If this is a <code>do</code> action and it doesn't require input, it is performed immediately
     * by calling {@link BaseActivity#action(Item, Action) }.
     * <p>
     * Otherwise we pass the action to a sub <code>activity</code> (window in slim terminology) which
     * collects the input if required and performs the action. See {@link PluginListActivity#show(Activity, Item, Action)}
     * <p>
     * Finally if the (unsupported) "showBigArtwork" flag is present in an item the <code>do</code>
     * action will return an artwork id or URL, which can be used the fetch an image to display in a
     * popup. See {@link ArtworkDialog#show(FragmentManager, Action)}
     */
    void execGoAction(Item item) {
        if (item.showBigArtwork) {
            ArtworkDialog.show(activity.getSupportFragmentManager(), item.goAction);
        } else if (item.doAction && !item.hasInput()) {
            activity.action(item, item.goAction);
        } else {
            PluginListActivity.show(activity, item, item.goAction);
        }
    }

    // Only touch these from the main thread
    private boolean contextMenuReady = false;
    private boolean contextMenuWaiting = false;
    private Stack<Action> contextStack;
    private View contextMenuView;
    private String contextMenuTitle;
    private List<Plugin> contextItems;

    public void onCreateContextMenu(final ContextMenu menu, final View v, Item item) {
        if (!contextMenuReady && !contextMenuWaiting) {
            contextMenuTitle = null;
            contextItems = null;
            if (item.moreAction != null) {
                contextMenuView = v;
                contextStack = new Stack<>();
                contextStack.push(item.moreAction);
                orderContextMenu(item.moreAction);
            } else {
                if (item.playAction != null) {
                    menu.add(Menu.NONE, R.id.play_now, Menu.NONE, R.string.PLAY_NOW);
                }
                if (item.addAction != null) {
                    menu.add(Menu.NONE, R.id.add_to_playlist, Menu.NONE, R.string.ADD_TO_END);
                }
                if (item.insertAction != null) {
                    menu.add(Menu.NONE, R.id.play_next, Menu.NONE, R.string.PLAY_NEXT);
                }
                if (item.moreAction != null) {
                    menu.add(Menu.NONE, R.id.more, Menu.NONE, activity.getServerString(ServerString.MORE));
                }
            }
        } else if (contextMenuReady) {
            contextMenuReady = false;
            // TODO some callback or other way to handle button visibility
            BaseItemView.ViewHolder viewHolder = (BaseItemView.ViewHolder) contextMenuView.getTag();
            viewHolder.contextMenuButton.setVisibility(View.VISIBLE);
            viewHolder.contextMenuLoading.setVisibility(View.INVISIBLE);
            if (contextStack.size() > 1) {
                View headerVew = activity.getLayoutInflater().inflate(R.layout.context_menu_header, (ViewGroup) v, false);
                menu.setHeaderView(headerVew);
                ImageView backButton = (ImageView) headerVew.findViewById(R.id.back);
                if (contextStack.size() > 1) {
                    backButton.setVisibility(View.VISIBLE);
                    backButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            menu.close();
                            contextStack.pop();
                            orderContextMenu(contextStack.peek());
                        }
                    });
                } else {
                    backButton.setVisibility(View.GONE);
                }
                if (contextMenuTitle != null) {
                    ((TextView) headerVew.findViewById(R.id.title)).setText(contextMenuTitle);
                }
            } else if (contextMenuTitle != null) {
                menu.setHeaderTitle(contextMenuTitle);
            }
            int index = 0;
            for (Plugin plugin : contextItems) {
                menu.add(Menu.NONE, index++, Menu.NONE, plugin.getName()).setEnabled(plugin.goAction != null);
            }
        }
    }

    private void orderContextMenu(Action action) {
        ISqueezeService service = activity.getService();
        if (service != null) {
            contextMenuWaiting = true;
            // TODO some callback or other way to handle button visibility
            BaseItemView.ViewHolder viewHolder = (BaseItemView.ViewHolder) contextMenuView.getTag();
            viewHolder.contextMenuButton.setVisibility(View.GONE);
            viewHolder.contextMenuLoading.setVisibility(View.VISIBLE);
            service.pluginItems(action, this);
        }
    }

    public boolean doItemContext(MenuItem menuItem, Item selectedItem) {
        if (contextItems != null) {
            selectedItem = contextItems.get(menuItem.getItemId());
            Action.NextWindow nextWindow = (selectedItem.goAction != null ? selectedItem.goAction.action.nextWindow : selectedItem.nextWindow);
            if (nextWindow != null) {
                activity.action(selectedItem, selectedItem.goAction);
                switch (nextWindow.nextWindow) {
                    case playlist:
                        CurrentPlaylistActivity.show(activity);
                        break;
                    case home:
                        HomeActivity.show(activity);
                        break;
                    case parent: // For centext menus parent and grandparent hide the context menu(s) and reload items
                    case grandparent:
                    case parentNoRefresh:
                    case refresh:
                    case refreshOrigin:
                        break;
                    case windowId:
                        //TODO implement
                        break;
                }
            } else {
                if (selectedItem.goAction.isContextMenu()) {
                    contextStack.push(selectedItem.goAction);
                    orderContextMenu(selectedItem.goAction);
                } else {
                    execGoAction(selectedItem);
                }
            }
            return true;
        } else {
        switch (menuItem.getItemId()) {
            case R.id.play_now:
                activity.action(selectedItem, selectedItem.playAction);
                return true;
            case R.id.add_to_playlist:
                activity.action(selectedItem, selectedItem.addAction);
                return true;
            case R.id.play_next:
                activity.action(selectedItem, selectedItem.insertAction);
                return true;
            case R.id.more:
                PluginListActivity.show(activity, selectedItem, selectedItem.moreAction);
                return true;
        }
        return false;
    }
    }

    @Override
    public Object getClient() {
        return activity;
    }

    @Override
    public void onItemsReceived(int count, int start, final Map<String, Object> parameters, final List<Plugin> items, Class<Plugin> dataType) {
        activity.getUIThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                contextMenuReady = true;
                contextMenuWaiting = false;
                if (parameters.containsKey("title")) {
                    contextMenuTitle = Util.getString(parameters, "title");
                }
                contextItems = items;
                // TODO callback?
                contextMenuView.showContextMenu();
            }
        });
    }
}
