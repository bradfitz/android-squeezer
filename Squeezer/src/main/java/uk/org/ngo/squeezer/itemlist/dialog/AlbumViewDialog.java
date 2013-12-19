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
package uk.org.ngo.squeezer.itemlist.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.itemlist.AlbumListActivity;
import uk.org.ngo.squeezer.service.ServerString;

public class AlbumViewDialog extends DialogFragment {

    private static final int POSITION_SORT_LABEL = AlbumListLayout.values().length;

    private static final int POSITION_SORT_START = POSITION_SORT_LABEL + 1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlbumListActivity activity = (AlbumListActivity) getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getServerString(ServerString.ALBUM_DISPLAY_OPTIONS));
        builder.setAdapter(new BaseAdapter() {
                               @Override
                               public boolean areAllItemsEnabled() {
                                   return false;
                               }

                               @Override
                               public boolean isEnabled(int position) {
                                   return (position != POSITION_SORT_LABEL);
                               }

                               @Override
                               public int getCount() {
                                   return AlbumListLayout.values().length + 1 + AlbumsSortOrder
                                           .values().length;
                               }

                               @Override
                               public Object getItem(int i) {
                                   return null;
                               }

                               @Override
                               public long getItemId(int i) {
                                   return i;
                               }


                               @Override
                               public View getView(int position, View convertView,
                                       ViewGroup parent) {
                                   if (position < POSITION_SORT_LABEL) {
                                       CheckedTextView textView = (CheckedTextView) activity
                                               .getLayoutInflater()
                                               .inflate(android.R.layout.select_dialog_singlechoice,
                                                       parent, false);
                                       AlbumListLayout listLayout = AlbumListLayout
                                               .values()[position];
                                       textView.setCompoundDrawablesWithIntrinsicBounds(
                                               listLayout.icon, 0, 0, 0);
                                       textView.setText(
                                               activity.getServerString(listLayout.serverString));
                                       textView.setChecked(listLayout == activity.getListLayout());
                                       return textView;
                                   } else if (position > POSITION_SORT_LABEL) {
                                       CheckedTextView textView = (CheckedTextView) activity
                                               .getLayoutInflater()
                                               .inflate(android.R.layout.select_dialog_singlechoice,
                                                       parent, false);
                                       position -= POSITION_SORT_START;
                                       AlbumsSortOrder sortOrder = AlbumsSortOrder
                                               .values()[position];
                                       textView.setText(
                                               activity.getServerString(sortOrder.serverString));
                                       textView.setChecked(sortOrder == activity.getSortOrder());
                                       return textView;
                                   }

                                   TextView textView = new TextView(activity, null,
                                           android.R.attr.listSeparatorTextViewStyle);
                                   textView.setText(getString(R.string.choose_sort_order,
                                           activity.getItemAdapter().getQuantityString(2)));
                                   return textView;
                               }
                           }, new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int position) {
                                   if (position < POSITION_SORT_LABEL) {
                                       activity.setListLayout(AlbumListLayout.values()[position]);
                                       dialog.dismiss();
                                   } else if (position > POSITION_SORT_LABEL) {
                                       position -= POSITION_SORT_START;
                                       activity.setSortOrder(AlbumsSortOrder.values()[position]);
                                       dialog.dismiss();
                                   }
                               }
                           }
        );
        return builder.create();
    }

    /**
     * Supported album list layouts.
     */
    public enum AlbumListLayout {
        grid(R.drawable.ic_action_view_as_grid, ServerString.SWITCH_TO_GALLERY),
        list(R.drawable.ic_action_view_as_list, ServerString.SWITCH_TO_EXTENDED_LIST);

        /**
         * The icon to use for this layout
         */
        private int icon;

        /**
         * The text to use for this layout
         */
        private ServerString serverString;

        private AlbumListLayout(int icon, ServerString serverString) {
            this.serverString = serverString;
            this.icon = icon;
        }
    }

    /**
     * Sort order strings supported by the server.
     * <p/>
     * Values must correspond with the string expected by the server. Any '__' in the strings will
     * be removed.
     */
    public enum AlbumsSortOrder {
        __new(ServerString.BROWSE_NEW_MUSIC),
        album(ServerString.ALBUM),
        artflow(ServerString.SORT_ARTISTYEARALBUM),
        artistalbum(ServerString.SORT_ARTISTALBUM),
        yearalbum(ServerString.SORT_YEARALBUM),
        yearartistalbum(ServerString.SORT_YEARARTISTALBUM);

        private ServerString serverString;

        private AlbumsSortOrder(ServerString serverString) {
            this.serverString = serverString;
        }
    }
}
