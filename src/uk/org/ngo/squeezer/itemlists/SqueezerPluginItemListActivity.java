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

package uk.org.ngo.squeezer.itemlists;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.model.SqueezerPlugin;
import uk.org.ngo.squeezer.model.SqueezerPluginItem;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ImageButton;

/*
 * The activity's content view scrolls in from the right, and disappear to the left, to provide a
 * spatial component to navigation.
 */
public class SqueezerPluginItemListActivity extends SqueezerBaseListActivity<SqueezerPluginItem>{
	private SqueezerPlugin plugin;
	private SqueezerPluginItem parent;
	private String search;

	@Override
	public SqueezerItemView<SqueezerPluginItem> createItemView() {
		return new SqueezerPluginItemView(this);
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            plugin = extras.getParcelable(SqueezerPlugin.class.getName());
            parent = extras.getParcelable(SqueezerPluginItem.class.getName());
            findViewById(R.id.search_view).setVisibility(
                    plugin.isSearchable() ? View.VISIBLE : View.GONE);

            ImageButton searchButton = (ImageButton) findViewById(R.id.search_button);
            final EditText searchCriteriaText = (EditText) findViewById(R.id.search_input);

            searchCriteriaText.setOnKeyListener(new OnKeyListener() {
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
                public void onClick(View v) {
                    if (getService() != null) {
                        clearAndReOrderItems(searchCriteriaText.getText().toString());
                    }
                }
            });
        }
	}

    public SqueezerPlugin getPlugin() {
        return plugin;
    }

    private void clearAndReOrderItems(String searchString) {
		if (getService() != null && !(plugin.isSearchable() && (searchString == null || searchString.length() == 0))) {
			search = searchString;
            super.clearAndReOrderItems();
		}
	}

	@Override
	public void clearAndReOrderItems() {
		clearAndReOrderItems(search);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerPluginItemListCallback(pluginItemListCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterPluginItemListCallback(pluginItemListCallback);
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().pluginItems(start, plugin, parent, search);
    }


    public void show(SqueezerPluginItem pluginItem) {
        final Intent intent = new Intent(this, SqueezerPluginItemListActivity.class);
        intent.putExtra(plugin.getClass().getName(), plugin);
        intent.putExtra(pluginItem.getClass().getName(), pluginItem);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    public static void show(Activity activity, SqueezerPlugin plugin) {
        final Intent intent = new Intent(activity, SqueezerPluginItemListActivity.class);
        intent.putExtra(plugin.getClass().getName(), plugin);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    private final IServicePluginItemListCallback pluginItemListCallback = new IServicePluginItemListCallback.Stub() {
		public void onPluginItemsReceived(int count, int start, @SuppressWarnings("rawtypes") final Map parameters, List<SqueezerPluginItem> items) throws RemoteException {
			if (parameters.containsKey("title")) {
				getUIThreadHandler().post(new Runnable() {
					public void run() {
						setTitle((String) parameters.get("title"));
					}
				});
			}

            // Automatically fetch subitems, if this is the only item.
            // TODO: Seen an NPE here (before adding the != null) check. Find out
            // why count == 1 might be true, but items.get(0) might return null.
            if (count == 1 && items.get(0) != null && items.get(0).isHasitems()) {
                parent = items.get(0);
				getUIThreadHandler().post(new Runnable() {
					public void run() {
						clearAndReOrderItems();
					}
				});
				return;
			}
			onItemsReceived(count, start, items);
		}
    };


    // Shortcuts for operations for plugin items

    public boolean play(SqueezerPluginItem item) throws RemoteException {
    	return pluginPlaylistControl(PluginPlaylistControlCmd.play, item);
    }

    public boolean load(SqueezerPluginItem item) throws RemoteException {
    	return pluginPlaylistControl(PluginPlaylistControlCmd.load, item);
    }

    public boolean insert(SqueezerPluginItem item) throws RemoteException {
    	return pluginPlaylistControl(PluginPlaylistControlCmd.insert, item);
    }

    public boolean add(SqueezerPluginItem item) throws RemoteException {
    	return pluginPlaylistControl(PluginPlaylistControlCmd.add, item);
    }

    private boolean pluginPlaylistControl(PluginPlaylistControlCmd cmd, SqueezerPluginItem item) throws RemoteException {
        if (getService() == null) {
            return false;
        }
        getService().pluginPlaylistControl(plugin, cmd.name(), item.getId());
        return true;
    }

    private enum PluginPlaylistControlCmd {
    	play,
    	load,
    	add,
        insert
    }

}
