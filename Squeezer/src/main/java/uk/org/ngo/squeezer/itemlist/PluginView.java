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

import androidx.annotation.LayoutRes;

import java.util.EnumSet;

import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.framework.Window;
import uk.org.ngo.squeezer.itemlist.dialog.ViewDialog;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class PluginView extends BaseItemView<Plugin> {
    private final PluginViewLogic logicDelegate;
    private Window.WindowStyle windowStyle;

    /** Width of the icon, if VIEW_PARAM_ICON is used. */
    private int mIconWidth;

    /** Height of the icon, if VIEW_PARAM_ICON is used. */
    private int mIconHeight;

    PluginView(BaseListActivity<Plugin> activity, Window.WindowStyle windowStyle) {
        super(activity);
        setWindowStyle(windowStyle);
        this.logicDelegate = new PluginViewLogic(activity);
        setLoadingViewParams(viewParamIcon() | VIEW_PARAM_TWO_LINE );
    }

    void setWindowStyle(Window.WindowStyle windowStyle) {
        this.windowStyle = windowStyle;
        if (listLayout() == ViewDialog.ArtworkListLayout.grid) {
            mIconWidth = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_grid_width);
            mIconHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_grid_height);
        } else {
            mIconWidth = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_width);
            mIconHeight = getActivity().getResources().getDimensionPixelSize(R.dimen.album_art_icon_height);
        }
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, int position, Plugin item) {
        @ViewParam int viewParams = (viewParamIcon() | VIEW_PARAM_TWO_LINE | viewParamContext(item));
        View view = getAdapterView(convertView, parent, viewParams);
        bindView(view, item);
        return view;
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, @ViewParam int viewParams) {
        return getAdapterView(convertView, parent, viewParams, layoutResource());
    }

    @LayoutRes private int layoutResource() {
        return (listLayout() == ViewDialog.ArtworkListLayout.grid) ? R.layout.grid_item : R.layout.list_item;
    }

    ViewDialog.ArtworkListLayout listLayout() {
        return listLayout(getActivity(), windowStyle);
    }

    static ViewDialog.ArtworkListLayout listLayout(Activity activity, Window.WindowStyle windowStyle) {
        if (EnumSet.of(Window.WindowStyle.HOME_MENU, Window.WindowStyle.ICON_LIST).contains(windowStyle)) {
            return new Preferences(activity).getAlbumListLayout();
        }
        return ViewDialog.ArtworkListLayout.list;
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
        viewHolder.text2.setText(item.text2);

        // If the item has an image, then fetch and display it
        if (item.hasArtwork()) {
            ImageFetcher.getInstance(getActivity()).loadImage(item.getIcon(), viewHolder.icon,
                    mIconWidth, mIconHeight);
        } else {
            viewHolder.icon.setImageDrawable(item.getIconDrawable(getActivity()));
        }

        if (item.hasContextMenu()) {
            viewHolder.contextMenuButton.setVisibility(item.checkbox == null ? View.VISIBLE : View.GONE);
            viewHolder.contextMenuCheckbox.setVisibility(item.checkbox != null ? View.VISIBLE : View.GONE);
            if (item.checkbox != null) {
                viewHolder.contextMenuCheckbox.setChecked(item.checkbox);
            }
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
    public void onItemSelected(View view, int index, Plugin item) {
        Action.JsonAction action = (item.goAction != null && item.goAction.action != null) ? item.goAction.action : null;
        Action.NextWindow nextWindow = (action != null ? action.nextWindow : item.nextWindow);
        if (item.checkbox != null) {
            item.checkbox = !item.checkbox;
            Action checkboxAction = item.checkboxActions.get(item.checkbox);
            if (checkboxAction != null) {
                getActivity().action(item, checkboxAction);
            }
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.contextMenuCheckbox.setChecked(item.checkbox);
        }
        if (nextWindow != null) {
            if (item.goAction != null) {
                getActivity().action(item, item.goAction);
            }
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
            else if (item.getNode() != null) {
                if ("settingsScreen".equals(item.getId()))
                    ((PluginListActivity)getActivity()).showViewDialog();
                else
                    HomeMenuActivity.show(getActivity(), item);
            }
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
