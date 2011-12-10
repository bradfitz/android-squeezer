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

import uk.org.ngo.squeezer.framework.SqueezerBaseListActivity;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.model.SqueezerPlaylist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.Toast;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlists.IServicePlaylistMaintenanceCallback;
import uk.org.ngo.squeezer.itemlists.IServicePlaylistsCallback;

public class SqueezerPlaylistsActivity extends SqueezerBaseListActivity<SqueezerPlaylist>{
	protected static final int DIALOG_NEW = 0;
	protected static final int DIALOG_RENAME = 1;
	protected static final int DIALOG_DELETE = 2;

	private SqueezerPlaylist currentPlaylist;
	public void setCurrentPlaylist(SqueezerPlaylist currentPlaylist) { this.currentPlaylist = currentPlaylist; }

	private String oldname;

	@Override
	public SqueezerItemView<SqueezerPlaylist> createItemView() {
		return new SqueezerPlaylistView(this);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		getService().registerPlaylistsCallback(playlistsCallback);
		getService().registerPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	}

	@Override
	protected void unregisterCallback() throws RemoteException {
		getService().unregisterPlaylistsCallback(playlistsCallback);
		getService().unregisterPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().playlists(start);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlistsmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_playlists_new:
			showDialog(DIALOG_NEW);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_NEW:
		{
			View form = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
			builder.setView(form);
	        final EditText editText = (EditText) form.findViewById(R.id.edittext);
			builder.setTitle(R.string.new_playlist_title);
			editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			editText.setHint(R.string.new_playlist_hint);
	        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					create(editText.getText().toString());
				}
			});
	        editText.setOnKeyListener(new OnKeyListener() {
	            public boolean onKey(View v, int keyCode, KeyEvent event) {
	                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
	               		create(editText.getText().toString());
						dismissDialog(DIALOG_NEW);
						return true;
	                }
	                return false;
	            }
	        });
        	break;
		}
        case DIALOG_DELETE:
        	{
				builder.setTitle(getString(R.string.delete_title, currentPlaylist.getName()));
				builder.setMessage(R.string.delete__message);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						try {
							getService().playlistsDelete(currentPlaylist);
							orderItems();
						} catch (RemoteException e) {
							Log.e(getTag(), "Error deleting playlist");
						}
					}
				});
			}
			break;
		case DIALOG_RENAME:
			{
				builder.setTitle(getString(R.string.rename_title, currentPlaylist.getName()));
				View form = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
				builder.setView(form);
		        final EditText editText = (EditText) form.findViewById(R.id.edittext);
				editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						rename(editText.getText().toString());
					}
				});
			}
			break;
        }

        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    protected void onPrepareDialog(int id, final Dialog dialog) {
        switch (id) {
        case DIALOG_NEW:
        	{
		        final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
				editText.setText("");
        	}
        	break;
        case DIALOG_DELETE:
        	break;
		case DIALOG_RENAME:
			{
		        final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
				editText.setText(currentPlaylist.getName());
		        editText.setOnKeyListener(new OnKeyListener() {
		            public boolean onKey(View v, int keyCode, KeyEvent event) {
		                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
							rename(editText.getText().toString());
							dialog.dismiss();
							return true;
		                }
		                return false;
		            }
		        });
			}
			break;
        }
    	super.onPrepareDialog(id, dialog);
    }

    private void create(String name) {
   		try {
			getService().playlistsNew(name);
			orderItems();
		} catch (RemoteException e) {
            Log.e(getTag(), "Error saving playlist as '"+ name + "': " + e);
		}
    }

    private void rename(String newname) {
   		try {
   			oldname = currentPlaylist.getName();
			getService().playlistsRename(currentPlaylist, newname);
			currentPlaylist.setName(newname);
			getItemAdapter().notifyDataSetChanged();
		} catch (RemoteException e) {
            Log.e(getTag(), "Error renaming playlist to '"+ newname + "': " + e);
		}
    }


	public static void show(Context context) {
        final Intent intent = new Intent(context, SqueezerPlaylistsActivity.class);
        context.startActivity(intent);
    }

    private final IServicePlaylistsCallback playlistsCallback = new IServicePlaylistsCallback.Stub() {
		public void onPlaylistsReceived(int count, int start, List<SqueezerPlaylist> items) throws RemoteException {
			onItemsReceived(count, start, items);
		}
    };

    private void showServiceMessage(final String msg) {
		getUIThreadHandler().post(new Runnable() {
			public void run() {
				getItemAdapter().notifyDataSetChanged();
				Toast.makeText(SqueezerPlaylistsActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
    }

    private final IServicePlaylistMaintenanceCallback playlistMaintenanceCallback = new IServicePlaylistMaintenanceCallback.Stub() {

		public void onRenameFailed(String msg) throws RemoteException {
			currentPlaylist.setName(oldname);
			showServiceMessage(msg);
		}

		public void onCreateFailed(String msg) throws RemoteException {
			showServiceMessage(msg);
		}

    };

}
