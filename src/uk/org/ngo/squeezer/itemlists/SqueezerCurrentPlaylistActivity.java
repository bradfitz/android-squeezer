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
import uk.org.ngo.squeezer.model.SqueezerSong;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import uk.org.ngo.squeezer.R;

public class SqueezerCurrentPlaylistActivity extends SqueezerAbstractSongListActivity {
	protected static final int DIALOG_MOVE = 0;
	protected static final int DIALOG_SAVE = 1;

	private static final int PLAYLIST_CONTEXTMENU_PLAY_ITEM = 0;
	private static final int PLAYLIST_CONTEXTMENU_REMOVE_ITEM = 1;
	private static final int PLAYLIST_CONTEXTMENU_MOVE_UP = 2;
	private static final int PLAYLIST_CONTEXTMENU_MOVE_DOWN = 3;
	private static final int PLAYLIST_CONTEXTMENU_MOVE = 4;

	private int fromIndex;

	public static void show(Context context) {
	    final Intent intent = new Intent(context, SqueezerCurrentPlaylistActivity.class);
	    context.startActivity(intent);
	}

	@Override
	public SqueezerItemView<SqueezerSong> createItemView() {
		return new SqueezerSongView(this) {

			@Override
			public void onItemSelected(int index, SqueezerSong item) throws RemoteException {
				getActivity().getService().playlistIndex(index);
				getActivity().finish();
			}

			@Override
			public void setupContextMenu(ContextMenu menu, int index, SqueezerSong item) {
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_PLAY_ITEM, 1, R.string.CONTEXTMENU_PLAY_ITEM);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_REMOVE_ITEM, 2, R.string.PLAYLIST_CONTEXTMENU_REMOVE_ITEM);
				if (index > 0)
					menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE_UP, 3, R.string.PLAYLIST_CONTEXTMENU_MOVE_UP);
				if (index < getAdapter().getCount()-1)
					menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE_DOWN, 4, R.string.PLAYLIST_CONTEXTMENU_MOVE_DOWN);
				menu.add(Menu.NONE, PLAYLIST_CONTEXTMENU_MOVE, 5, R.string.PLAYLIST_CONTEXTMENU_MOVE);
			}

			@Override
			public boolean doItemContext(MenuItem menuItem, int index, SqueezerSong selectedItem) throws RemoteException {
				switch (menuItem.getItemId()) {
				case PLAYLIST_CONTEXTMENU_PLAY_ITEM:
					getService().playlistIndex(index);
					return true;
				case PLAYLIST_CONTEXTMENU_REMOVE_ITEM:
					getService().playlistRemove(index);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE_UP:
					getService().playlistMove(index, index-1);
					orderItems();
					return true;
				case PLAYLIST_CONTEXTMENU_MOVE_DOWN:
					getService().playlistMove(index, index+1);
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
	protected void orderPage(int start) throws RemoteException {
		getService().currentPlaylist(start);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.currentplaylistmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_item_playlist_clear:
			if (getService() != null)
				try {
					getService().playlistClear();
					finish();
				} catch (RemoteException e) {
					Log.e(getTag(), "Error trying to clear playlist: " + e);
				}
			return true;
		case R.id.menu_item_playlist_save:
			showDialog(DIALOG_SAVE);
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View form = getLayoutInflater().inflate(R.layout.edittext_dialog, null);
		builder.setView(form);
        final EditText editText = (EditText) form.findViewById(R.id.edittext);

        switch (id) {
		case DIALOG_SAVE:
			{
				builder.setTitle(R.string.save_playlist_title);
				editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
				editText.setHint(R.string.save_playlist_hint);
		        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
	               		try {
							getService().playlistSave(editText.getText().toString());
						} catch (RemoteException e) {
			                Log.e(getTag(), "Error saving playlist as '"+ editText.getText() + "': " + e);
						}
					}
				});
		        editText.setOnKeyListener(new OnKeyListener() {
		            public boolean onKey(View v, int keyCode, KeyEvent event) {
		                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
		               		try {
								getService().playlistSave(editText.getText().toString());
								dismissDialog(DIALOG_SAVE);
							} catch (RemoteException e) {
				                Log.e(getTag(), "Error saving playlist as '"+ editText.getText() + "': " + e);
							}
							return true;
		                }
		                return false;
		            }
		        });
			}
			break;
		case DIALOG_MOVE:
			{
				builder.setTitle(getString(R.string.move_to_dialog_title, fromIndex));
				editText.setInputType(InputType.TYPE_CLASS_NUMBER);
				editText.setHint(R.string.move_to_index_hint);
		        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
		               	int targetIndex = Util.parseDecimalInt(editText.getText().toString(), -1);
		               	if (targetIndex > 0 && targetIndex <= getItemAdapter().getCount()) {
		               		try {
								getService().playlistMove(fromIndex-1, targetIndex-1);
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
        final EditText editText = (EditText) dialog.findViewById(R.id.edittext);
        switch (id) {
		case DIALOG_SAVE:
	        editText.setText("");
			break;
		case DIALOG_MOVE:
			{
				dialog.setTitle(getString(R.string.move_to_dialog_title, fromIndex));
		        editText.setText("");
		        editText.setOnKeyListener(new OnKeyListener() {
		            public boolean onKey(View v, int keyCode, KeyEvent event) {
		                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
			               	int targetIndex = Util.parseDecimalInt(editText.getText().toString(), -1);
			               	if (targetIndex > 0 && targetIndex <= getItemAdapter().getCount()) {
			               		try {
									getService().playlistMove(fromIndex-1, targetIndex-1);
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

}
