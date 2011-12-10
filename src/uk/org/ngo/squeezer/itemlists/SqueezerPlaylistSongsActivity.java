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

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.SqueezerItemView;
import uk.org.ngo.squeezer.model.SqueezerPlaylist;
import uk.org.ngo.squeezer.model.SqueezerSong;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.InputType;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.Toast;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlists.IServicePlaylistMaintenanceCallback;

public class SqueezerPlaylistSongsActivity extends SqueezerAbstractSongListActivity {
	protected static final int DIALOG_MOVE = 0;
	protected static final int DIALOG_RENAME = 1;
	protected static final int DIALOG_DELETE = 2;

	private static final int PLAYLIST_CONTEXTMENU_PLAY_ITEM = 0;
	private static final int PLAYLIST_CONTEXTMENU_ADD_ITEM = 1;
	private static final int PLAYLIST_CONTEXTMENU_INSERT_ITEM = 2;
	private static final int PLAYLIST_CONTEXTMENU_REMOVE_ITEM = 3;
	private static final int PLAYLIST_CONTEXTMENU_MOVE_UP = 4;
	private static final int PLAYLIST_CONTEXTMENU_MOVE_DOWN = 5;
	private static final int PLAYLIST_CONTEXTMENU_MOVE = 6;

	public static void show(Context context, SqueezerPlaylist playlist) {
	    final Intent intent = new Intent(context, SqueezerPlaylistSongsActivity.class);
	    intent.putExtra(playlist.getClass().getName(), playlist);
	    context.startActivity(intent);
	}

	private SqueezerPlaylist playlist;
	private String oldname;
	private int fromIndex;

	@Override
	public SqueezerItemView<SqueezerSong> createItemView() {
		return new SqueezerSongView(this) {
			@Override
			public void setupContextMenu(ContextMenu menu, int index, SqueezerSong item) {
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_PLAY_ITEM, 1, R.string.CONTEXTMENU_PLAY_ITEM);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_ADD_ITEM, 2, R.string.CONTEXTMENU_ADD_ITEM);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_INSERT_ITEM, 3, R.string.CONTEXTMENU_INSERT_ITEM);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_REMOVE_ITEM, 4, R.string.PLAYLIST_CONTEXTMENU_REMOVE_ITEM);
				if (index > 0)
					menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE_UP, 5, R.string.PLAYLIST_CONTEXTMENU_MOVE_UP);
				if (index < getAdapter().getCount()-1)
					menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE_DOWN, 6, R.string.PLAYLIST_CONTEXTMENU_MOVE_DOWN);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE, 7, R.string.PLAYLIST_CONTEXTMENU_MOVE);
			}

			@Override
			public boolean doItemContext(MenuItem menuItem, int index, SqueezerSong selectedItem) throws RemoteException {
				switch (menuItem.getItemId()) {
				case PLAYLIST_CONTEXTMENU_PLAY_ITEM:
					play(selectedItem);
					return true;
				case PLAYLIST_CONTEXTMENU_ADD_ITEM:
					add(selectedItem);
					return true;
				case PLAYLIST_CONTEXTMENU_INSERT_ITEM:
					insert(selectedItem);
					return true;
				case PLAYLIST_CONTEXTMENU_REMOVE_ITEM:
					getService().playlistsRemove(playlist, index);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE_UP:
					getService().playlistsMove(playlist, index, index-1);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE_DOWN:
					getService().playlistsMove(playlist, index, index+1);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE:
					fromIndex = index;
					showDialog(DIALOG_MOVE);
					return true;
				}
				return false;
			};
		};
	}

	@Override
	public void prepareActivity(Bundle extras) {
		if (extras != null)
			for (String key : extras.keySet()) {
				if (SqueezerPlaylist.class.getName().equals(key)) {
					playlist = extras.getParcelable(key);
				} else
					Log.e(getTag(), "Unexpected extra value: " + key + "("
							+ extras.get(key).getClass().getName() + ")");
			}
	}

	@Override
	protected void orderPage(int start) throws RemoteException {
		getService().playlistSongs(start, playlist);
	}

	@Override
	protected void registerCallback() throws RemoteException {
		super.registerCallback();
		getService().registerPlaylistMaintenanceCallback(playlistMaintenanceCallback);
	};

	@Override
	protected void unregisterCallback() throws RemoteException {
		super.unregisterCallback();
		getService().unregisterPlaylistMaintenanceCallback(playlistMaintenanceCallback);

	};

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlistmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_playlists_delete:
			showDialog(DIALOG_DELETE);
			return true;
		case R.id.menu_item_playlists_rename:
			showDialog(DIALOG_RENAME);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id) {
        case DIALOG_DELETE:
			builder.setTitle(getString(R.string.delete_title, playlist.getName()));
        	builder.setMessage(R.string.delete__message);
	        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
               		try {
						getService().playlistsDelete(playlist);
						finish();
					} catch (RemoteException e) {
		                Log.e(getTag(), "Error deleting playlist");
					}
				}
			});
			break;
		case DIALOG_RENAME:
			{
				builder.setTitle(getString(R.string.rename_title, playlist.getName()));
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
		case DIALOG_MOVE:
			{
				View form = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
				builder.setView(form);
		        final EditText editText = (EditText) form.findViewById(R.id.edittext);
				builder.setTitle(getString(R.string.move_to_dialog_title, fromIndex));
				editText.setInputType(InputType.TYPE_CLASS_NUMBER);
				editText.setHint(R.string.move_to_index_hint);
		        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
		               	int targetIndex = Util.parseDecimalInt(editText.getText().toString(), -1);
		               	if (targetIndex > 0 && targetIndex <= getItemAdapter().getCount()) {
		               		try {
								getService().playlistsMove(playlist, fromIndex-1, targetIndex-1);
								orderItems();
							} catch (RemoteException e) {
				                Log.e(getTag(), "Error moving song from '"+ fromIndex + "' to '" +targetIndex + "': " + e);
							}
		               	}
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
        case DIALOG_DELETE:
        	break;
		case DIALOG_RENAME:
			{
		        final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
		        editText.setText(playlist.getName());
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
		case DIALOG_MOVE:
			{
				dialog.setTitle(getString(R.string.move_to_dialog_title, fromIndex));
		        final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
		        editText.setText("");
		        editText.setOnKeyListener(new OnKeyListener() {
		            public boolean onKey(View v, int keyCode, KeyEvent event) {
		                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
			               	int targetIndex = Util.parseDecimalInt(editText.getText().toString(), -1);
			               	if (targetIndex > 0 && targetIndex <= getItemAdapter().getCount()) {
			               		try {
									getService().playlistsMove(playlist, fromIndex-1, targetIndex-1);
									orderItems();
									dialog.dismiss();
								} catch (RemoteException e) {
					                Log.e(getTag(), "Error moving song from '"+ fromIndex + "' to '" +targetIndex + "': " + e);
								}
			               	}
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

    private void rename(String newname) {
   		try {
   	    	oldname = playlist.getName();
			getService().playlistsRename(playlist, newname);
   	    	playlist.setName(newname);
		} catch (RemoteException e) {
            Log.e(getTag(), "Error renaming playlist to '"+ newname + "': " + e);
		}
    }

    private void showServiceMessage(final String msg) {
		getUIThreadHandler().post(new Runnable() {
			public void run() {
				Toast.makeText(SqueezerPlaylistSongsActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
    }

    private final IServicePlaylistMaintenanceCallback playlistMaintenanceCallback = new IServicePlaylistMaintenanceCallback.Stub() {

		public void onRenameFailed(String msg) throws RemoteException {
			playlist.setName(oldname);
			showServiceMessage(msg);
		}

		public void onCreateFailed(String msg) throws RemoteException {
			showServiceMessage(msg);
		}

    };

}
