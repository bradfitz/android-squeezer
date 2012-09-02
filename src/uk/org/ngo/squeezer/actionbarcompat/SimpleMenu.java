/*
 * Copyright 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.actionbarcompat;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * A <em>really</em> dumb implementation of the {@link android.view.Menu} interface, that's only
 * useful for our actionbar-compat purposes. See
 * <code>com.android.internal.view.menu.MenuBuilder</code> in AOSP for a more complete
 * implementation.
 */
class SimpleMenu implements Menu {

    private ActionBarHelperBase mActionBarHelper;
    private Activity mActivity;
    private Resources mResources;

    private List<SimpleMenuItem> mItems;
    private int showAsActionAlwways;
    private List<SimpleMenuItem> showAsActionIfRoom;

    public SimpleMenu(ActionBarHelperBase actionBarHelper) {
        mActionBarHelper = actionBarHelper;
        mActivity = actionBarHelper.mActivity;
        mResources = mActivity.getResources();
        mItems = new ArrayList<SimpleMenuItem>();
        showAsActionIfRoom = new ArrayList<SimpleMenuItem>();
    }

    public Context getContext() {
        return mActivity;
    }

    public Resources getResources() {
        return mResources;
    }

    public ActionBarHelperBase getActionBarHelper() {
        return mActionBarHelper;
    }

    public MenuItem add(CharSequence title) {
        return addInternal(0, 0, title);
    }

    public MenuItem add(int titleRes) {
        return addInternal(0, 0, mResources.getString(titleRes));
    }

    public MenuItem add(int groupId, int itemId, int order, CharSequence title) {
        return addInternal(itemId, order, title);
    }

    public MenuItem add(int groupId, int itemId, int order, int titleRes) {
        return addInternal(itemId, order, mResources.getString(titleRes));
    }

    /**
     * Adds an item to the menu.  The other add methods funnel to this.
     */
    private MenuItem addInternal(int itemId, int order, CharSequence title) {
        final SimpleMenuItem item = new SimpleMenuItem(this, itemId, order, title);
        mItems.add(findInsertIndex(mItems, order), item);
        return item;
    }

    private static int findInsertIndex(List<? extends MenuItem> items, int order) {
        for (int i = items.size() - 1; i >= 0; i--) {
            MenuItem item = items.get(i);
            if (item.getOrder() <= order) {
                return i + 1;
            }
        }

        return 0;
    }

    public int findItemIndex(int id) {
        final int size = size();

        for (int i = 0; i < size; i++) {
            SimpleMenuItem item = mItems.get(i);
            if (item.getItemId() == id) {
                return i;
            }
        }

        return -1;
    }

    public void removeItem(int itemId) {
        removeItemAtInt(findItemIndex(itemId));
    }

    private void removeItemAtInt(int index) {
        if ((index < 0) || (index >= mItems.size())) {
            return;
        }
        mItems.remove(index);
    }

    public void clear() {
        mItems.clear();
    }

    public MenuItem findItem(int id) {
        final int size = size();
        for (int i = 0; i < size; i++) {
            SimpleMenuItem item = mItems.get(i);
            if (item.getItemId() == id) {
                return item;
            }
        }

        return null;
    }

    public int size() {
        return mItems.size();
    }

    public MenuItem getItem(int index) {
        return mItems.get(index);
    }

    // Unsupported operations.

    public SubMenu addSubMenu(CharSequence charSequence) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public SubMenu addSubMenu(int titleRes) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public SubMenu addSubMenu(int groupId, int itemId, int order, CharSequence title) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public SubMenu addSubMenu(int groupId, int itemId, int order, int titleRes) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public int addIntentOptions(int i, int i1, int i2, ComponentName componentName,
            Intent[] intents, Intent intent, int i3, MenuItem[] menuItems) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public void removeGroup(int i) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public void setGroupCheckable(int i, boolean b, boolean b1) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public void setGroupVisible(int i, boolean b) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public void setGroupEnabled(int i, boolean b) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public boolean hasVisibleItems() {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public void close() {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public boolean performShortcut(int i, KeyEvent keyEvent, int i1) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public boolean isShortcutKey(int i, KeyEvent keyEvent) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public boolean performIdentifierAction(int i, int i1) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    public void setQwertyMode(boolean b) {
        throw new UnsupportedOperationException("This operation is not supported for SimpleMenu");
    }

    /**
     * Register how the specified item should display in the Action Bar.
     * 
     * @param menuItem The menu item to register
     * @param showAsAction The action bar state to register
     */
    void setShowAsAction(SimpleMenuItem menuItem, Integer showAsAction) {
        if ((showAsAction & (MenuItem.SHOW_AS_ACTION_ALWAYS)) != 0) showAsActionAlwways++;
        if ((showAsAction & (MenuItem.SHOW_AS_ACTION_IF_ROOM)) != 0) showAsActionIfRoom .add(findInsertIndex(showAsActionIfRoom, menuItem.getOrder()), menuItem);
    }

    /**
     * Figure out whether the specified menu item will fit in the action bar.
     * 
     * @param menuItem The menu item to check for
     * @return True if the menu item fits
     */
    boolean hasRoom(SimpleMenuItem menuItem) {
        int maxIcons = getActionBarMaxIcons();
        int n = 1 + showAsActionAlwways; // Reserve space for the app icon.
        for (SimpleMenuItem ifRoomItem : showAsActionIfRoom) {
            if (++n > maxIcons) return false;
            if (ifRoomItem == menuItem) return true;
        }
        return false;
    }

    int _maxIcons = 0;
    /**
     * Implement the rules in the android action bar design guide.
     * http://developer.android.com/design/patterns/actionbar.html
     * 
     * @return The number of action which will fit in the action bar.
     */
    private int getActionBarMaxIcons() {
        if (_maxIcons != 0) return _maxIcons;
        
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        if (widthPixels < 360)
            _maxIcons = 2;
        else if (widthPixels < 500)
            _maxIcons = 3;
        else if (widthPixels < 600)
            _maxIcons = 4;
        else
            _maxIcons = 5;
        return _maxIcons;
    }

}
