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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.NowPlayingActivity;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.dialog.NetworkErrorDialogFragment;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.framework.Window;
import uk.org.ngo.squeezer.itemlist.dialog.ViewDialog;
import uk.org.ngo.squeezer.menu.BaseMenuFragment;
import uk.org.ngo.squeezer.menu.ViewMenuItemFragment;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;

/*
 * The activity's content view scrolls in from the right, and disappear to the left, to provide a
 * spatial component to navigation.
 */
public class PluginListActivity extends BaseListActivity<Plugin>
        implements NetworkErrorDialogFragment.NetworkErrorDialogListener,
        ViewMenuItemFragment.ListActivityWithViewMenu<Plugin, ViewDialog.ArtworkListLayout>{
    private static final int GO = 1;
    static final String FINISH = "FINISH";
    static final String RELOAD = "RELOAD";

    private boolean register;
    private String cmd;
    private Item plugin;
    private Action action;
    Window.WindowStyle windowStyle;
    ViewDialog.ArtworkListLayout listLayout;

    /** The preferred list layout for lists with artwork items */
    ViewDialog.ArtworkListLayout artworkListLayout = null;

    @Override
    public ItemView<Plugin> createItemView() {
        return new PluginView(this, windowStyle, listLayout);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        artworkListLayout = new Preferences(this).getAlbumListLayout();
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        register = extras.getBoolean("register");
        cmd = extras.getString("cmd");
        plugin = extras.getParcelable(Plugin.class.getName());
        action = extras.getParcelable(Action.class.getName());

        if (!register) {
            BaseMenuFragment.add(this, ViewMenuItemFragment.class);
        }
        if (plugin != null && plugin.window != null) {
            applyWindow(plugin.window);
        } else if (register)
            applyWindowStyle(Window.WindowStyle.TEXT_ONLY);

        findViewById(R.id.input_view).setVisibility((hasInput()) ? View.VISIBLE : View.GONE);
        if (hasInput()) {
            ImageButton inputButton = (ImageButton) findViewById(R.id.input_button);
            final EditText inputText = (EditText) findViewById(R.id.plugin_input);
            int inputType = EditorInfo.TYPE_CLASS_TEXT;
            int inputImage = R.drawable.ic_keyboard_return;

            switch (action.getInputType()) {
                case TEXT:
                    break;
                case SEARCH:
                    inputImage = R.drawable.ic_menu_search;
                    break;
                case EMAIL:
                    inputType |= EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS;
                    break;
                case PASSWORD:
                    inputType |= EditorInfo.TYPE_TEXT_VARIATION_PASSWORD;
                    break;
            }
            inputText.setInputType(inputType);
            inputButton.setImageResource(inputImage);
            inputText.setHint(plugin.input.title);
            inputText.setText(plugin.inputValue);
            inputText.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN)
                            && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        clearAndReOrderItems(inputText.getText().toString());
                        return true;
                    }
                    return false;
                }
            });

            inputButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getService() != null) {
                        clearAndReOrderItems(inputText.getText().toString());
                    }
                }
            });
        }
    }

    @Override
    protected AbsListView setupListView(AbsListView listView) {
        if (listLayout == ViewDialog.ArtworkListLayout.grid && !(listView instanceof GridView)) {
            listView = switchListView(listView, R.layout.item_grid);
        }
        if (listLayout != ViewDialog.ArtworkListLayout.grid && (listView instanceof GridView)) {
            listView = switchListView(listView, R.layout.item_list);
        }
        return super.setupListView(listView);
    }

    private AbsListView switchListView(AbsListView listView, @LayoutRes int resource) {
        ViewGroup parent = (ViewGroup) listView.getParent();
        int i1 = parent.indexOfChild(listView);
        parent.removeViewAt(i1);
        listView = (AbsListView) getLayoutInflater().inflate(resource, parent, false);
        parent.addView(listView, i1);
        return listView;
    }

    private void updateHeader(String headerText) {
        TextView header = (TextView) findViewById(R.id.header);
        header.setText(headerText);
        header.setVisibility(View.VISIBLE);
    }

    private void updateSubHeader(String headerText) {
        TextView header = (TextView) findViewById(R.id.sub_header);
        header.setText(headerText);
        findViewById(R.id.sub_header_container).setVisibility(View.VISIBLE);
    }

    private void updateHeader(Window window) {
        if (!TextUtils.isEmpty(window.text)) {
            updateHeader(window.text);
        }
        if (!TextUtils.isEmpty(window.textarea)) {
            updateSubHeader(window.textarea);
        }
    }

    private void applyWindow(Window window) {
        applyWindowStyle(register ? Window.WindowStyle.TEXT_ONLY : window.windowStyle());
        updateHeader(window);
    }

    private void applyWindowStyle(Window.WindowStyle windowStyle) {
        ViewDialog.ArtworkListLayout listLayout = (windowStyle == Window.WindowStyle.ICON_TEXT && artworkListLayout == ViewDialog.ArtworkListLayout.grid)
                ? ViewDialog.ArtworkListLayout.grid
                : ViewDialog.ArtworkListLayout.list;
        if (windowStyle != this.windowStyle || listLayout != this.listLayout) {
            this.windowStyle = windowStyle;
            ((PluginView) getItemView()).setWindowStyle(windowStyle, listLayout);
            getItemAdapter().notifyDataSetChanged();
        }
        if (listLayout != this.listLayout) {
            this.listLayout = listLayout;
            setListView(setupListView(getListView()));
        }
    }


    private void clearAndReOrderItems(String inputString) {
        if (getService() != null && !TextUtils.isEmpty(inputString)) {
            plugin.inputValue = inputString;
            clearAndReOrderItems();
        }
    }

    private boolean hasInput() {
        return plugin != null && plugin.hasInput();
    }

    @Override
    protected boolean needPlayer() {
        // Most of the the times we actualle do need a player, but we have to return false, because
        // if we need to register on SN, it is before we can get the players
        return false;
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        if (plugin != null) {
            if (action == null || (plugin.hasInput() && !plugin.isInputReady())) {
                showContent();
            } else if (plugin.doAction) {
                action(plugin, plugin.goAction);
                finish();
            } else
                service.pluginItems(start, plugin, action, this);
        } else if (cmd != null) {
            service.pluginItems(start, cmd, this);
        } else if (register) {
            service.register(this);
        }
    }

    public void onEventMainThread(HandshakeComplete event) {
        super.onEventMainThread(event);
        if (plugin != null && plugin.hasSubItems()) {
            getItemAdapter().update(plugin.subItems.size(), 0, plugin.subItems);
        }
    }


    @Override
    public void onItemsReceived(int count, int start, final Map<String, Object> parameters, List<Plugin> items, Class<Plugin> dataType) {
        final Window window = Item.extractWindow(Util.getRecord(parameters, "window"), null);
        if (window != null) {
            runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        applyWindow(window);
                    }
                });
        }
        if (parameters.containsKey("title")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateHeader(Util.getString(parameters, "title"));
                }
            });
        }

        if (parameters.containsKey("goNow")) {
            Action.NextWindow nextWindow = Action.NextWindow.fromString(Util.getString(parameters, "goNow"));
            switch (nextWindow.nextWindow) {
                case nowPlaying:
                    NowPlayingActivity.show(this);
                    break;
                case playlist:
                    CurrentPlaylistActivity.show(this);
                    break;
                case home:
                    HomeActivity.show(this);
                    break;
            }
            finish();
        }

        // The documentation says "Returned with value 1 if there was a network error accessing
        // the content source.". In practice (with at least the Napster and Pandora plugins) the
        // value is an error message suitable for displaying to the user.
        if (parameters.containsKey("networkerror")) {
            Resources resources = getResources();
            ISqueezeService service = getService();
            String playerName;

            if (service == null) {
                playerName = "Unknown";
            } else {
                playerName = service.getActivePlayer().getName();
            }

            String errorMsg = Util.getString(parameters, "networkerror");

            String errorMessage = String.format(resources.getString(R.string.server_error),
                    playerName, errorMsg);
            NetworkErrorDialogFragment networkErrorDialogFragment =
                    NetworkErrorDialogFragment.newInstance(errorMessage);
            networkErrorDialogFragment.show(getSupportFragmentManager(), "networkerror");
        }

        super.onItemsReceived(count, start, parameters, items, dataType);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GO) {
            if (resultCode == RESULT_OK) {
                if (FINISH.equals(data.getAction())) {
                    finish();
                }
                else if (RELOAD.equals(data.getAction())) {
                    clearAndReOrderItems();
                }
            }
        }
    }

    @Override
    public void showViewDialog() {
        new ViewDialog().show(getSupportFragmentManager(), "ViewDialog");
    }

    public ViewDialog.ArtworkListLayout getListLayout() {
        return artworkListLayout;
    }

    public void setListLayout(ViewDialog.ArtworkListLayout listLayout) {
        new Preferences(this).setAlbumListLayout(artworkListLayout = listLayout);
        applyWindowStyle(windowStyle);
    }

    /**
     * The user dismissed the network error dialog box. There's nothing more to do, so finish
     * the activity.
     */
    @Override
    public void onDialogDismissed(DialogInterface dialog) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    public static void register(Activity activity) {
        final Intent intent = new Intent(activity, PluginListActivity.class);
        intent.putExtra("register", true);
        activity.startActivity(intent);
    }

    /**
     * Start a new {@link PluginListActivity} to perform the supplied <code>action</code>.
     * <p>
     * If the action requires input, we initially get the input.
     * <p>
     * When input is ready or the actyion does not require input, the action is performed. If it
     * is a <code>do</code> action it is performed via {@link #action(Item, Action)}i, and the
     * activity is finished.
     * <p>
     * Otherwise it is a <code>go</code> action, and items are ordered asynchronously via
     * {@link ISqueezeService#pluginItems(int, Item, Action, IServiceItemListCallback)}
     *
     * @see #orderPage(ISqueezeService, int)
     */
    public static void show(Activity activity, Item plugin, Action action) {
        final Intent intent = getPluginListIntent(activity);
        intent.putExtra(Plugin.class.getName(), plugin);
        intent.putExtra(Action.class.getName(), action);
        activity.startActivityForResult(intent, GO);
    }

    public static void show(Activity activity, Item plugin) {
        final Intent intent = getPluginListIntent(activity);
        intent.putExtra(Plugin.class.getName(), plugin);
        activity.startActivityForResult(intent, GO);
    }

    @NonNull
    private static Intent getPluginListIntent(Activity activity) {
        Intent intent = new Intent(activity, PluginListActivity.class);
        if (activity instanceof PluginListActivity && ((PluginListActivity)activity).register) {
            intent.putExtra("register", true);
        }
        return intent;
    }

}
