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

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.dialog.NetworkErrorDialogFragment;
import uk.org.ngo.squeezer.framework.BaseListActivity;
import uk.org.ngo.squeezer.framework.ItemView;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.model.PluginItem;
import uk.org.ngo.squeezer.service.ISqueezeService;

/*
 * The activity's content view scrolls in from the right, and disappear to the left, to provide a
 * spatial component to navigation.
 */
public class PluginListActivity extends BaseListActivity<Plugin>
        implements NetworkErrorDialogFragment.NetworkErrorDialogListener {

    private Plugin plugin;

    private String search;

    @Override
    public ItemView<Plugin> createItemView() {
        return new RadioView(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            plugin = extras.getParcelable(Plugin.class.getName());
            findViewById(R.id.search_view).setVisibility(
                    plugin.isSearchable() ? View.VISIBLE : View.GONE);

            ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
            final EditText searchCriteriaText = (EditText) findViewById(R.id.search_input);

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

    public Plugin getPlugin() {
        return plugin;
    }

    private void clearAndReOrderItems(String searchString) {
        if (getService() != null && !(plugin.isSearchable() && (searchString == null
                || searchString.length() == 0))) {
            search = searchString;
            super.clearAndReOrderItems();
        }
    }

    @Override
    protected void orderPage(@NonNull ISqueezeService service, int start) {
        service.apps(start, this);
    }


    @Override
    public void onItemsReceived(int count, int start, final Map<String, String> parameters, List<Plugin> items, Class<Plugin> dataType) {
        if (parameters.containsKey("title")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateHeader(parameters.get("title"));
                }
            });
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

            String errorMsg = parameters.get("networkerror");

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

    private boolean pluginPlaylistControl(@PluginPlaylistControlCmd String cmd, PluginItem item) {
        if (getService() == null) {
            return false;
        }
        getService().pluginPlaylistControl(plugin, cmd, item.getId());
        return true;
    }

    @StringDef({PLUGIN_PLAYLIST_PLAY, PLUGIN_PLAYLIST_PLAY_NOW, PLUGIN_PLAYLIST_ADD_TO_END,
            PLUGIN_PLAYLIST_PLAY_AFTER_CURRENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PluginPlaylistControlCmd {}
    public static final String PLUGIN_PLAYLIST_PLAY = "play";
    public static final String PLUGIN_PLAYLIST_PLAY_NOW = "load";
    public static final String PLUGIN_PLAYLIST_ADD_TO_END = "add";
    public static final String PLUGIN_PLAYLIST_PLAY_AFTER_CURRENT = "insert";

}
