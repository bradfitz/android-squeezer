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
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.HomeActivity;
import uk.org.ngo.squeezer.NowPlayingActivity;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.dialog.NetworkErrorDialogFragment;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseItem;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.framework.Window;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;

/*
 * The activity's content view scrolls in from the right, and disappear to the left, to provide a
 * spatial component to navigation.
 */
public class PluginListActivity extends BaseListActivity<Plugin>
        implements NetworkErrorDialogFragment.NetworkErrorDialogListener {

    private boolean register;
    private String cmd;
    private Plugin plugin;
    private Action action;

    @Override
    public ItemView<Plugin> createItemView() {
        return new PluginView(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        assert extras != null;
        register = extras.getBoolean("register");
        cmd = extras.getString("cmd");
        plugin = extras.getParcelable(Plugin.class.getName());
        action = extras.getParcelable(Action.class.getName());

        findViewById(R.id.input_view).setVisibility((hasInput()) ? View.VISIBLE : View.GONE);
        if (hasInput()) {
            ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
            final EditText searchCriteriaText = (EditText) findViewById(R.id.plugin_input);

            searchCriteriaText.setHint(plugin.input.title);
            searchCriteriaText.setText(action.getInputValue());
            searchCriteriaText.setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN)
                            && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        clearAndReOrderItems(searchCriteriaText.getText().toString());
                        return true;
                    }
                    return false;
                }
            });

            searchButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getService() != null) {
                        clearAndReOrderItems(searchCriteriaText.getText().toString());
                    }
                }
            });
        }
    }

    private void updateHeader(String headerText) {
        TextView header = (TextView) findViewById(R.id.header);
        header.setText(headerText);
        header.setVisibility(View.VISIBLE);
    }

    private void updateSubHeader(String headerText) {
        TextView header = (TextView) findViewById(R.id.sub_header);
        header.setText(headerText);
        header.setVisibility(View.VISIBLE);
    }

    private void updateHeader(Window window) {
        updateHeader(window.text);
        if (!TextUtils.isEmpty(window.textarea)) {
            updateSubHeader(window.textarea);
        }
    }


    private void clearAndReOrderItems(String searchString) {
        if (getService() != null && !TextUtils.isEmpty(searchString)) {
            action.action.params.put(action.inputParam, searchString);
            clearAndReOrderItems();
        }
    }

    private boolean hasInput() {
        return plugin != null && plugin.hasInput();
    }

    private boolean isInputReady() {
        return (!hasInput() || action.isInputReady());
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        if (register) {
            service.register(this);
        } else if (plugin != null) {
            if (isInputReady())
                service.pluginItems(start, plugin, action, this);
        } else {
            service.pluginItems(start, cmd, this);
        }
    }


    @Override
    public void onItemsReceived(int count, int start, final Map<String, Object> parameters, List<Plugin> items, Class<Plugin> dataType) {
        Object baseRecord = parameters.get("base");
        if (baseRecord != null) {
            final BaseItem baseItem = new BaseItem((Map<String, Object>) baseRecord);
            if (baseItem.window != null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateHeader(baseItem.window);
                    }
                });
            }
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

    public static void apps(Activity activity) {
        show(activity, "apps");
    }

    public static void radios(Activity activity) {
        show(activity, "radios");
    }

    public static void favorites(Activity activity) {
        show(activity, "favorites");
    }

    public static void register(Activity activity) {
        final Intent intent = new Intent(activity, PluginListActivity.class);
        intent.putExtra("register", true);
        activity.startActivity(intent);
    }

    private static void show(Activity activity, String plugin) {
        final Intent intent = new Intent(activity, PluginListActivity.class);
        intent.putExtra("cmd", plugin);
        activity.startActivity(intent);
    }

    public static void show(Activity activity, Plugin plugin, Action action) {
        final Intent intent = new Intent(activity, PluginListActivity.class);
        intent.putExtra(Plugin.class.getName(), plugin);
        intent.putExtra(Action.class.getName(), action);
        activity.startActivity(intent);
    }

    public static void show(Activity activity, Plugin plugin) {
        show(activity, plugin, plugin.goAction);
    }

}
