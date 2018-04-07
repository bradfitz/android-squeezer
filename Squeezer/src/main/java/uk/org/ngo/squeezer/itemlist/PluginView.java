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

package uk.org.ngo.squeezer.itemlist;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.ServerString;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class PluginView extends BaseItemView<Plugin> implements IServiceItemListCallback<Plugin> {

    private final BaseListActivity<Plugin> activity;

    public PluginView(BaseListActivity<Plugin> activity) {
        super(activity);
        this.activity = activity;

        setViewParams(VIEW_PARAM_ICON | VIEW_PARAM_CONTEXT_BUTTON);
        setLoadingViewParams(VIEW_PARAM_ICON);
    }

    @Override
    public void bindView(View view, Plugin item) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());
        // If the item has an image, then fetch and display it
        if (item.getIcon() != null) {
            ImageFetcher.getInstance(getActivity()).loadImage(item.getIcon(), viewHolder.icon,
                    mIconWidth, mIconHeight);
        } else {
            // Otherwise we will revert to some other icon. This is not an exact approach, more
            // like a best effort.
            if (item.isPlayable()) {
                viewHolder.icon.setImageResource(R.drawable.ic_songs);
            } else {
                viewHolder.icon.setVisibility(View.GONE);
            }
        }

    }

    @Override
    public String getQuantityString(int quantity) {
        throw new UnsupportedOperationException("quantities are not supported for plugins");
    }

    @Override
    public boolean isSelectable(Plugin item) {
        return item.isSelectable();
    }

    @Override
    public void onItemSelected(int index, Plugin item) {
        PluginListActivity.show(getActivity(), item);
    }

    // Only touch these from the main thread
    private boolean contextMenuReady = false;
    private boolean contextMenuWaiting = false;
    private ViewHolder contextViewHolder;
    private Map<String, Object> contextParameters;
    private List<Plugin> contextItems;

    @Override
    public void onCreateContextMenu(ContextMenu menu, final View v, ItemView.ContextMenuInfo menuInfo) {
        final Plugin item = (Plugin) menuInfo.item;
        if (!contextMenuReady && !contextMenuWaiting) {
            contextParameters = null;
            contextItems = null;
            if (item.hasSlimContextMenu()) {
                ISqueezeService service = activity.getService();
                if (service != null) {
                    contextMenuWaiting = true;
                    contextViewHolder = (ViewHolder) v.getTag();
                    contextViewHolder.contextMenuButton.setVisibility(View.INVISIBLE);
                    contextViewHolder.contextMenuLoading.setVisibility(View.VISIBLE);
                    service.pluginItems(item, item.moreAction, this);
                }
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
                    menu.add(Menu.NONE, R.id.more, Menu.NONE, getActivity().getServerString(ServerString.MORE));
                }
            }
        } else if (contextMenuReady) {
            contextMenuReady = false;
            contextViewHolder.contextMenuButton.setVisibility(View.VISIBLE);
            contextViewHolder.contextMenuLoading.setVisibility(View.INVISIBLE);
            if (contextParameters.containsKey("title")) {
                menu.setHeaderTitle(Util.getString(contextParameters, "title"));
            }
            int index = 0;
            for (Plugin plugin : contextItems) {
                menu.add(Menu.NONE, index++, Menu.NONE, plugin.getName()).setEnabled(plugin.isSelectable());
            }
        }
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, Plugin selectedItem) {
        if (contextItems != null) {
            Plugin plugin = contextItems.get(menuItem.getItemId());
            PluginListActivity.show(getActivity(), plugin, plugin.goAction);
            return true;
        } else {
        switch (menuItem.getItemId()) {
            case R.id.play_now:
                getActivity().action(selectedItem, selectedItem.playAction);
                return true;
            case R.id.add_to_playlist:
                getActivity().action(selectedItem, selectedItem.addAction);
                return true;
            case R.id.play_next:
                getActivity().action(selectedItem, selectedItem.insertAction);
                return true;
            case R.id.more:
                PluginListActivity.show(getActivity(), selectedItem, selectedItem.moreAction);
                return true;
        }
        return false;
    }
    }

    @Override
    public Object getClient() {
        return getActivity();
    }

    @Override
    public void onItemsReceived(int count, int start, final Map<String, Object> parameters, final List<Plugin> items, Class<Plugin> dataType) {
        getActivity().getUIThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                contextMenuReady = true;
                contextMenuWaiting = false;
                contextParameters = parameters;
                contextItems = items;
                activity.getListView().showContextMenu();
            }
        });
    }
}
