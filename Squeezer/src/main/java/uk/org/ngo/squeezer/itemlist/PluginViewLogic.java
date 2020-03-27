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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.itemlist.dialog.ArtworkDialog;
import uk.org.ngo.squeezer.itemlist.dialog.ChoicesDialog;
import uk.org.ngo.squeezer.itemlist.dialog.InputTextDialog;
import uk.org.ngo.squeezer.itemlist.dialog.InputTimeDialog;
import uk.org.ngo.squeezer.itemlist.dialog.SlideShow;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;

/**
 * Delegate with view logic for {@link Plugin} which can be used from any {@link BaseActivity}
 */
public class PluginViewLogic implements IServiceItemListCallback<Plugin>, PopupMenu.OnDismissListener {
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
     * popup. See {@link ArtworkDialog#show(BaseActivity, Action)}
     */
    void execGoAction(BaseItemView.ViewHolder viewHolder, Item item, int alreadyPopped) {
        if (item.showBigArtwork) {
            ArtworkDialog.show(activity, item.goAction);
        } else if (item.goAction.isSlideShow()) {
            SlideShow.show(activity, item.goAction);
        } else if (item.goAction.isContextMenu()) {
            showContextMenu(viewHolder, item.goAction);
        } else if (item.doAction) {
            if (item.hasInput()) {
                if (item.hasChoices()) {
                    ChoicesDialog.show(activity, item, alreadyPopped);
                } else if ("time".equals(item.input.inputStyle)) {
                    InputTimeDialog.show(activity, item, alreadyPopped);
                } else {
                    InputTextDialog.show(activity, item, alreadyPopped);
                }
            } else {
                activity.action(item, item.goAction, alreadyPopped);
            }
        } else {
            PluginListActivity.show(activity, item, item.goAction);
        }
    }

    // Only touch these from the main thread
    private int contextStack = 0;
    private PopupMenu contextPopup;
    private BaseItemView.ViewHolder contextMenuViewHolder;

    public void showContextMenu(BaseItemView.ViewHolder viewHolder, Item item) {
        if (item.moreAction != null) {
            showContextMenu(viewHolder, item.moreAction);
        } else {
            showStandardContextMenu(viewHolder.contextMenuButtonHolder, item);
        }
    }

    private void showContextMenu(BaseItemView.ViewHolder viewHolder, Action action) {
        contextMenuViewHolder = viewHolder;
        contextStack = 1;
        orderContextMenu(action);
    }

    private void showStandardContextMenu(View v, final Item item) {
        contextPopup = new PopupMenu(activity, v);
        Menu menu = contextPopup.getMenu();

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
            menu.add(Menu.NONE, R.id.more, Menu.NONE, R.string.MORE);
        }

        contextPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return doStandardItemContext(menuItem, item);
            }
        });
        contextPopup.setOnDismissListener(this);
        contextPopup.show();
    }

    private boolean doStandardItemContext(MenuItem menuItem, Item item) {
        switch (menuItem.getItemId()) {
            case R.id.play_now:
                activity.action(item, item.playAction);
                return true;
            case R.id.add_to_playlist:
                activity.action(item, item.addAction);
                return true;
            case R.id.play_next:
                activity.action(item, item.insertAction);
                return true;
            case R.id.more:
                PluginListActivity.show(activity, item, item.moreAction);
                return true;
        }
        return false;
    }

    private void showContextMenu(final BaseItemView.ViewHolder viewHolder, final List<Plugin> items) {
        contextPopup = new PopupMenu(activity, viewHolder.contextMenuButtonHolder);
        Menu menu = contextPopup.getMenu();

        int index = 0;
        for (Plugin plugin : items) {
            menu.add(Menu.NONE, index++, Menu.NONE, plugin.getName()).setEnabled(plugin.goAction != null);
        }

        contextPopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                return doItemContext(viewHolder, items.get(menuItem.getItemId()));
            }
        });
        contextPopup.setOnDismissListener(this);
        contextPopup.show();
    }

    private boolean doItemContext(BaseItemView.ViewHolder viewHolder, Plugin item) {
        Action.NextWindow nextWindow = (item.goAction != null ? item.goAction.action.nextWindow : item.nextWindow);
        if (nextWindow != null) {
            activity.action(item, item.goAction, contextStack);
        } else {
            execGoAction(viewHolder, item, contextStack);
        }
        return true;
    }

    private void orderContextMenu(Action action) {
        ISqueezeService service = activity.getService();
        if (service != null) {
            contextMenuViewHolder.contextMenuButton.setVisibility(View.GONE);
            contextMenuViewHolder.contextMenuLoading.setVisibility(View.VISIBLE);
            service.pluginItems(action, this);
        }
    }

    @Override
    public Object getClient() {
        return activity;
    }

    @Override
    public void onItemsReceived(int count, int start, final Map<String, Object> parameters, final List<Plugin> items, Class<Plugin> dataType) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                contextMenuViewHolder.contextMenuButton.setVisibility(View.VISIBLE);
                contextMenuViewHolder.contextMenuLoading.setVisibility(View.GONE);
                showContextMenu(contextMenuViewHolder, items);
            }
        });
    }

    public void resetContextMenu() {
        if (contextMenuViewHolder != null) {
            contextMenuViewHolder.contextMenuButton.setVisibility(View.VISIBLE);
            contextMenuViewHolder.contextMenuLoading.setVisibility(View.GONE);
        }

        if (contextPopup != null) {
            contextPopup.dismiss();
            contextPopup = null;
        }

        contextStack = 0;
        contextMenuViewHolder = null;
    }

    @Override
    public void onDismiss(PopupMenu menu) {
        contextPopup = null;
    }
}
