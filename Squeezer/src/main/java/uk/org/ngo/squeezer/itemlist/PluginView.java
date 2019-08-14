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

import android.app.Activity;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.framework.Window;
import uk.org.ngo.squeezer.itemlist.dialog.AlbumViewDialog;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class PluginView extends BaseItemView<Plugin> {
    private final PluginViewLogic logicDelegate;
    private Window.WindowStyle windowStyle;
    private AlbumViewDialog.AlbumListLayout listLayout;

    PluginView(BaseListActivity<Plugin> activity, Window.WindowStyle windowStyle, AlbumViewDialog.AlbumListLayout listLayout) {
        super(activity);
        setWindowStyle(windowStyle, listLayout);
        this.logicDelegate = new PluginViewLogic(activity);
        setLoadingViewParams(viewParamIcon());
    }

    void setWindowStyle(Window.WindowStyle windowStyle, AlbumViewDialog.AlbumListLayout listLayout) {
        this.windowStyle = windowStyle;
        this.listLayout = listLayout;
        if (listLayout == AlbumViewDialog.AlbumListLayout.grid) {
            mIconWidth = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_grid_width);
            mIconHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_grid_height);
        } else {
            mIconWidth = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_width);
            mIconHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_height);
        }
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, int position, Plugin item) {
        @ViewParam int viewParams = (viewParamIcon() | viewParamContext(item));
        View view = getAdapterView(convertView, parent, viewParams);
        bindView(view, item);
        return view;
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, @ViewParam int viewParams) {
        return listLayout == AlbumViewDialog.AlbumListLayout.grid
                ? getAdapterView(convertView, parent, viewParams, R.layout.grid_item)
                : super.getAdapterView(convertView, parent, viewParams);
    }

    private int viewParamIcon() {
        return windowStyle == Window.WindowStyle.TEXT_ONLY ? 0 : VIEW_PARAM_ICON;
    }

    private int viewParamContext(Plugin item) {
        return item.hasContextMenu() ? VIEW_PARAM_CONTEXT_BUTTON : 0;
    }

    @Override
    public void bindView(View view, Plugin item) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());
        // If the item has an image, then fetch and display it
        if (item.hasArtwork()) {
            ImageFetcher.getInstance(getActivity()).loadImage(item.getIcon(), viewHolder.icon,
                    mIconWidth, mIconHeight);
        } else {
            viewHolder.icon.setImageResource(item.getIconResource());
        }

    }

    @Override
    public String getQuantityString(int quantity) {
        return "plugins";
        //throw new UnsupportedOperationException("quantities are not supported for plugins");
    }

    @Override
    public boolean isSelectable(Plugin item) {
        return item.isSelectable();
    }

    @Override
    public void onItemSelected(int index, Plugin item) {
        Action.NextWindow nextWindow = (item.goAction != null ? item.goAction.action.nextWindow : item.nextWindow);
        if (nextWindow != null) {
            getActivity().action(item, item.goAction);
            switch (nextWindow.nextWindow) {
                case nowPlaying:
                    // Do nothing as now playing is always available in Squeezer (maybe toast the action)
                    break;
                case playlist:
                    CurrentPlaylistActivity.show(getActivity());
                    break;
                case home:
                    HomeActivity.show(getActivity());
                    break;
                case parent:
                case parentNoRefresh:
                    getActivity().finish();
                    break;
                case grandparent:
                    getActivity().setResult(Activity.RESULT_OK, new Intent(PluginListActivity.FINISH));
                    getActivity().finish();
                    break;
                case refresh:
                    getActivity().clearAndReOrderItems();
                    break;
                case refreshOrigin:
                    getActivity().setResult(Activity.RESULT_OK, new Intent(PluginListActivity.RELOAD));
                    getActivity().finish();
                    break;
                case windowId:
                    //TODO implement
                    break;
            }
        } else {
            if (item.goAction != null)
                logicDelegate.execGoAction(item);
            else if (item.hasSubItems())
                PluginListActivity.show(getActivity(), item);
            else if (item.getNode() != null)
                HomeMenuActivity.show(getActivity(), item.getId());
        }
   }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, ItemView.ContextMenuInfo menuInfo) {
        logicDelegate.onCreateContextMenu(menu, v, menuInfo.item);
    }

    @Override
    public boolean doItemContext(MenuItem menuItem, int index, Plugin selectedItem) {
        return logicDelegate.doItemContext(menuItem, selectedItem);
    }
}
