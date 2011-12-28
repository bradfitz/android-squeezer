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

package com.danga.squeezer.actionbarcompat;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * An abstract class that handles some common action bar-related functionality in the app. This
 * class provides functionality useful for both phones and tablets, and does not require any Android
 * 3.0-specific features, although it uses them if available.
 *
 * Two implementations of this class are {@link ActionBarHelperBase} for a pre-Honeycomb version of
 * the action bar, and {@link ActionBarHelperHoneycomb}, which uses the built-in ActionBar features
 * in Android 3.0 and later.
 */
public abstract class ActionBarHelper {
    protected Activity mActivity;
    protected Menu mOptionsMenu;

    /**
     * Factory method for creating {@link ActionBarHelper} objects for a
     * given activity. Depending on which device the app is running, either a basic helper or
     * Honeycomb-specific helper will be returned.
     */
    public static ActionBarHelper createInstance(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new ActionBarHelperICS(activity);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return new ActionBarHelperHoneycomb(activity);
        } else {
            return new ActionBarHelperBase(activity);
        }
    }

    protected ActionBarHelper(Activity activity) {
        mActivity = activity;
    }

    /**
     * Action bar helper code to be run in {@link Activity#onCreate(android.os.Bundle)}.
     */
    public void onCreate(Bundle savedInstanceState) {
    }

    /**
     * Action bar helper code to be run in {@link Activity#onPostCreate(android.os.Bundle)}.
     */
    public void onPostCreate(Bundle savedInstanceState) {
    }

    /**
     * Action bar helper code to be run in {@link Activity#onTitleChanged(CharSequence, int)}.
     */
    protected void onTitleChanged(CharSequence title, int color) {
    }

    /**
     * Return the menu item with a particular identifier.
     * <p>NOTE<br>
     * When using the action bar helper, it is necessary to use this in stead of {@link Menu#findItem(int)}
     * because this one knows which items are on the action bar.
     * 
     * @param idThe identifier to find.
     * @return The menu item object, or null if there is no item with this identifier.
     */
    abstract public MenuItem findItem(int id);

    /**
     * Set the icon for the "home" action. 
     * @param resId Resource ID referring to the drawable to use as an icon
     * @return The current instance for call chaining 
     */
    public abstract ActionBarHelper setIcon(int resId);

    /**
     * Set the icon for the "home" action. 
     * @param icon Drawable to use as an icon
     * @return The current instance for call chaining 
     */
    public abstract ActionBarHelper setIcon(Drawable icon);
    
    /**
     * Sets the indeterminate loading state of the item with ID {@link R.id.menu_refresh}.
     * (where the item ID was menu_refresh).
     */
    public abstract void setRefreshActionItemState(boolean refreshing);

    /**
     * Returns a {@link MenuInflater} for use when inflating menus. The implementation of this
     * method in {@link ActionBarHelperBase} returns a wrapped menu inflater that can read
     * action bar metadata from a menu resource pre-Honeycomb.
     */
    public MenuInflater getMenuInflater(MenuInflater superMenuInflater) {
        return new WrappedMenuInflater(mActivity, superMenuInflater);
    }

    /**
     * A {@link android.view.MenuInflater}.
     */
    protected class WrappedMenuInflater extends MenuInflater {
        protected MenuInflater mInflater;

        public WrappedMenuInflater(Context context, MenuInflater inflater) {
            super(context);
            mInflater = inflater;
        }

        @Override
        public void inflate(int menuRes, Menu menu) {
            mOptionsMenu = menu;
            mInflater.inflate(menuRes, menu);
        }
    }

}
