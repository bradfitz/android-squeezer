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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import uk.org.ngo.squeezer.R;
import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.InflateException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * A class that implements the action bar pattern for pre-Honeycomb devices.
 */
public class ActionBarHelperBase extends ActionBarHelper {
    private static final String MENU_RES_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String MENU_ATTR_ID = "id";
    private static final String MENU_ATTR_SHOW_AS_ACTION = "showAsAction";

    public Set<Integer> optionsMenuResources;
    private SparseArray<View> mActionBarItems ;
    private Menu mSimpleMenu;
    private ViewGroup mActionBarCompat;
    protected Drawable mHomeIcon;

    protected ActionBarHelperBase(Activity activity) {
        super(activity);
    }

    /**{@inheritDoc}*/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        mActivity.requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
    }

    /**{@inheritDoc}*/
    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        // Prepare the action bar with an app icon area and a view control area.
        mActivity.getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
                R.layout.actionbar_compat);
        setupActionBar();

        // Force an inflation of the options menu into our simple menu.
        // We also store the menu resources which participates in the options
        // menu. This is necessary because when inflating something other than
        // the options menu (e.g. a context menu), no action should be perform-
        // ed by the action bar helpers.
        mSimpleMenu = new SimpleMenu(this);
        optionsMenuResources = new HashSet<Integer>();
        mActivity.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, mSimpleMenu);
        mActivity.onPrepareOptionsMenu(mSimpleMenu);

        // Populate the action bar with the configured items
        mActionBarItems = new SparseArray<View>();
        for (int i = 0; i < mSimpleMenu.size(); i++) {
            SimpleMenuItem item = (SimpleMenuItem) mSimpleMenu.getItem(i);
            if (item.isActionBar())
                mActionBarItems.put(item.getItemId(), addActionItemCompatFromMenuItem(item));
        }
    }
    
    /**
     * Sets up the compatibility action bar with the given title.
     */
    private void setupActionBar() {
        mActionBarCompat = getActionBarCompat();
        if (mActionBarCompat == null) {
            return;
        }

        LinearLayout.LayoutParams springLayoutParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.FILL_PARENT);
        springLayoutParams.weight = 1;

        // Add Home button
        SimpleMenu tempMenu = new SimpleMenu(this);
        SimpleMenuItem homeItem = new SimpleMenuItem(
                tempMenu, android.R.id.home, 0, mActivity.getString(R.string.app_name));
        homeItem.setIcon(mHomeIcon != null ? mHomeIcon : mActivity.getResources().getDrawable(mActivity.getApplicationInfo().icon));
        addActionItemCompatFromMenuItem(homeItem);

        // Add title text
        TextView titleText = new TextView(mActivity, null, R.attr.actionbarCompatTitleStyle);
        titleText.setLayoutParams(springLayoutParams);
        titleText.setText(mActivity.getTitle());
        mActionBarCompat.addView(titleText);
    }

    /**{@inheritDoc}*/
    @Override
    public void setRefreshActionItemState(boolean refreshing) {
        View refreshButton = mActivity.findViewById(R.id.actionbar_compat_item_refresh);
        View refreshIndicator = mActivity.findViewById(
                R.id.actionbar_compat_item_refresh_progress);

        if (refreshButton != null) {
            refreshButton.setVisibility(refreshing ? View.GONE : View.VISIBLE);
        }
        if (refreshIndicator != null) {
            refreshIndicator.setVisibility(refreshing ? View.VISIBLE : View.GONE);
        }
    }

    /**{@inheritDoc}*/
    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        TextView titleView = (TextView) mActivity.findViewById(R.id.actionbar_compat_title);
        if (titleView != null) {
            titleView.setText(title);
        }
    }
    
    /**{@inheritDoc}*/
    @Override
    public ActionBarHelper setIcon(int resId) {
        this.mHomeIcon = mActivity.getResources().getDrawable(resId);
        return this;
    }

    /**{@inheritDoc}*/
    @Override
    public ActionBarHelper setIcon(Drawable icon) {
        this.mHomeIcon = icon;
        return this;
    }
    
    @Override
    public MenuItem findItem(int id) {
        return mSimpleMenu.findItem(id);
    }

    /**
     * Sets whether the menu item is enabled. 
     * Disabling a menu item will not allow it to be invoked via its shortcut. 
     * The menu item will still be visible.
     * <p>
     * Finds out whether the item is on the action bar or the options menu, and set the
     * invokable state on the proper element.
     * 
     * @param item The menu item  to set the invokable state for
     * @param enabled If true then the item will be invokable; if false it is won't be invokable.
     */
    public void setEnabled(SimpleMenuItem item, boolean enabled) {
        if (item.isActionBar()) {
            // If mActionBarItems is null, the state is being set from onCreateOptionsMenu.
            // Just skip it now, the state will be set in addActionItemCompatFromMenuItem.
            if (mActionBarItems != null) {
                mActionBarItems.get(item.getItemId()).setEnabled(enabled);
            }
        } else if (mOptionsMenu != null) {
            mOptionsMenu.findItem(item.getItemId()).setEnabled(enabled);
        }
    }

    /**
     * Set visibility of a menu item.<br>
     * Finds out whether the item is on the action bar or the options menu, and set the
     * visibility on the proper element.
     * 
     * @param item The menu item to set the visibility for
     * @param visible If true then the item will be visible; if false it is hidden.
     */
    public void setVisible(SimpleMenuItem item, boolean visible) {
        if (item.isActionBar()) {
            // If mActionBarItems is null, the visibility is being set from onCreateOptionsMenu.
            // Just skip it now, the visibility will be set in addActionItemCompatFromMenuItem.
            if (mActionBarItems != null) {
                mActionBarItems.get(item.getItemId()).setVisibility(visible ? View.VISIBLE : View.GONE);
            }
        } else if (mOptionsMenu != null) {
            mOptionsMenu.findItem(item.getItemId()).setVisible(visible);
        }
    }


    /**
     * Returns a {@link android.view.MenuInflater} that can read action bar metadata on
     * pre-Honeycomb devices.
     */
    @Override
    public MenuInflater getMenuInflater(MenuInflater superMenuInflater) {
        return new WrappedMenuInflater(mActivity, superMenuInflater);
    }

    /**
     * Returns the {@link android.view.ViewGroup} for the action bar on phones (compatibility action
     * bar). Can return null, and will return null on Honeycomb.
     */
    private ViewGroup getActionBarCompat() {
        return (ViewGroup) mActivity.findViewById(R.id.actionbar_compat);
    }

    /**
     * Adds an action button to the compatibility action bar, using menu information from a {@link
     * android.view.MenuItem}. If the menu item ID is <code>menu_refresh</code>, the menu item's
     * state can be changed to show a loading spinner using
     * {@link #setRefreshActionItemState(boolean)}.
     */
    private View addActionItemCompatFromMenuItem(final MenuItem item) {
        final int itemId = item.getItemId();

        if (mActionBarCompat == null) {
            return null;
        }

        // Create the button
        ImageButton actionButton = new ImageButton(mActivity, null,
                itemId == android.R.id.home
                        ? R.attr.actionbarCompatItemHomeStyle
                        : R.attr.actionbarCompatItemStyle);
        actionButton.setLayoutParams(new ViewGroup.LayoutParams(
                (int) mActivity.getResources().getDimension(
                        itemId == android.R.id.home
                                ? R.dimen.actionbar_compat_button_home_width
                                : R.dimen.actionbar_compat_button_width),
                ViewGroup.LayoutParams.FILL_PARENT));
        if (itemId == R.id.menu_refresh) {
            actionButton.setId(R.id.actionbar_compat_item_refresh);
        }
        actionButton.setImageDrawable(item.getIcon());
        actionButton.setScaleType(ImageView.ScaleType.CENTER);
        actionButton.setContentDescription(item.getTitle());
        actionButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mActivity.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
            }
        });

        mActionBarCompat.addView(actionButton);

        if (itemId == R.id.menu_refresh) {
            // Refresh buttons should be stateful, and allow for indeterminate progress indicators,
            // so add those.
            ProgressBar indicator = new ProgressBar(mActivity, null,
                    R.attr.actionbarCompatProgressIndicatorStyle);

            final int buttonWidth = mActivity.getResources().getDimensionPixelSize(
                    R.dimen.actionbar_compat_button_width);
            final int buttonHeight = mActivity.getResources().getDimensionPixelSize(
                    R.dimen.actionbar_compat_height);
            final int progressIndicatorWidth = buttonWidth / 2;

            LinearLayout.LayoutParams indicatorLayoutParams = new LinearLayout.LayoutParams(
                    progressIndicatorWidth, progressIndicatorWidth);
            indicatorLayoutParams.setMargins(
                    (buttonWidth - progressIndicatorWidth) / 2,
                    (buttonHeight - progressIndicatorWidth) / 2,
                    (buttonWidth - progressIndicatorWidth) / 2,
                    0);
            indicator.setLayoutParams(indicatorLayoutParams);
            indicator.setVisibility(View.GONE);
            indicator.setId(R.id.actionbar_compat_item_refresh_progress);
            mActionBarCompat.addView(indicator);
        }

        // Set the initial state from the menu item.
        actionButton.setEnabled(item.isEnabled());
        actionButton.setVisibility(item.isVisible() ? View.VISIBLE : View.GONE);
        return actionButton;
    }


    /**
     * A {@link android.view.MenuInflater} that reads action bar metadata.
     */
    private class WrappedMenuInflater extends ActionBarHelper.WrappedMenuInflater {

        public WrappedMenuInflater(Context context, MenuInflater inflater) {
            super(context, inflater);
        }

        @Override
        public void inflate(int menuRes, Menu menu) {
            mInflater.inflate(menuRes, menu);
            if (menu == mSimpleMenu) {
                loadActionBarMetadata(menuRes);
                optionsMenuResources.add(menuRes);
            } else if (optionsMenuResources.contains(menuRes)) {
                mOptionsMenu = menu;
                // Hide on-screen action items from the options menu.
                for (int i = 0; i < mSimpleMenu.size(); i++) {
                    SimpleMenuItem item = (SimpleMenuItem) mSimpleMenu.getItem(i);
                    MenuItem menuItem = menu.findItem(item.getItemId());
                    // Items populated by a fragment may not be found
                    if (menuItem != null) {
                        if (item.isActionBar()) {
                            menuItem.setVisible(false);
                        } else {
                            menuItem.setVisible(item.isVisible());
                            menuItem.setEnabled(item.isEnabled());
                        }
                    }
                }
            }
        }

        /**
         * Loads action bar metadata from a menu resource, storing a list of menu item IDs that
         * should be shown on-screen (i.e. those with showAsAction set to always or ifRoom).
         * @param menuResId
         */
        private void loadActionBarMetadata(int menuResId) {
            XmlResourceParser parser = null;
            try {
                parser = mActivity.getResources().getXml(menuResId);

                int eventType = parser.getEventType();
                int itemId;
                int showAsAction;

                boolean eof = false;
                while (!eof) {
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            if (!parser.getName().equals("item")) {
                                break;
                            }

                            itemId = parser.getAttributeResourceValue(MENU_RES_NAMESPACE,
                                    MENU_ATTR_ID, 0);
                            if (itemId == 0) {
                                break;
                            }

                            showAsAction = parser.getAttributeIntValue(MENU_RES_NAMESPACE,
                                    MENU_ATTR_SHOW_AS_ACTION, -1);
                            ((SimpleMenuItem)mSimpleMenu.findItem(itemId)).setShowAsAction(showAsAction);
                            break;

                        case XmlPullParser.END_DOCUMENT:
                            eof = true;
                            break;
                    }

                    eventType = parser.next();
                }
            } catch (XmlPullParserException e) {
                throw new InflateException("Error inflating menu XML", e);
            } catch (IOException e) {
                throw new InflateException("Error inflating menu XML", e);
            } finally {
                if (parser != null) {
                    parser.close();
                }
            }
        }

    }

}
