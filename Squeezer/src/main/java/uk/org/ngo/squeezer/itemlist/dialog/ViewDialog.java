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
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithTextAndIcon;
import uk.org.ngo.squeezer.itemlist.PluginListActivity;
import uk.org.ngo.squeezer.util.ThemeManager;

public class ViewDialog extends androidx.fragment.app.DialogFragment {
    private static final int POSITION_THEME_LABEL = ArtworkListLayout.values().length;
    private static final int POSITION_THEME_START = POSITION_THEME_LABEL + 1;


    protected String getTitle() {
        return getContext().getString(R.string.ALBUM_DISPLAY_OPTIONS);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final PluginListActivity activity = (PluginListActivity) getActivity();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getTitle());
        builder.setAdapter(new BaseAdapter() {
                               @Override
                               public boolean areAllItemsEnabled() {
                                   return false;
                               }

                               @Override
                               public boolean isEnabled(int position) {
                                   return (position != POSITION_THEME_LABEL);
                               }

                               @Override
                               public int getCount() {
                                   return ArtworkListLayout.values().length + 1 + ThemeManager.Theme.values().length;
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
                               public View getView(int position, View convertView, ViewGroup parent) {
                                   if (position < POSITION_THEME_LABEL) {
                                       CheckedTextView textView = (CheckedTextView) LayoutInflater.from(getContext())
                                               .inflate(android.R.layout.select_dialog_singlechoice, parent, false);
                                       ArtworkListLayout listLayout = ArtworkListLayout.values()[position];
                                       textView.setCompoundDrawablesWithIntrinsicBounds(getIcon(listLayout), 0, 0, 0);
                                       textView.setText(listLayout.getText(getActivity()));
                                       textView.setChecked(listLayout == activity.getPreferredListLayout());
                                       return textView;
                                   } else if (position > POSITION_THEME_LABEL) {
                                       CheckedTextView textView = (CheckedTextView) LayoutInflater.from(getContext())
                                               .inflate(android.R.layout.select_dialog_singlechoice, parent, false);
                                       position -= POSITION_THEME_START;
                                       ThemeManager.Theme theme = ThemeManager.Theme.values()[position];
                                       textView.setText(theme.getText(getContext()));
                                       textView.setChecked(theme.mThemeId == activity.getThemeId());
                                       return textView;
                                   }

                                   TextView textView = new TextView(getActivity(), null, android.R.attr.listSeparatorTextViewStyle);
                                   textView.setText(getString(R.string.settings_theme_title));
                                   return textView;

                               }
                           }, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int position) {
                                   if (position < POSITION_THEME_LABEL) {
                                       activity.setPreferredListLayout(ArtworkListLayout.values()[position]);
                                       dialog.dismiss();
                                   } else if (position > POSITION_THEME_LABEL) {
                                       position -= POSITION_THEME_START;
                                       activity.setTheme(ThemeManager.Theme.values()[position]);
                                       dialog.dismiss();
                                   }
                               }
                           }
        );
        return builder.create();
    }

    private int getIcon(ArtworkListLayout listLayout) {
        TypedValue v = new TypedValue();
        getActivity().getTheme().resolveAttribute(listLayout.getIconAttribute(), v, true);
        return v.resourceId;
    }

    /**
     * Supported list layouts.
     */
    public enum ArtworkListLayout implements EnumWithTextAndIcon {
        grid(R.attr.ic_action_view_as_grid, R.string.SWITCH_TO_GALLERY),
        list(R.attr.ic_action_view_as_list, R.string.SWITCH_TO_EXTENDED_LIST);

        /**
         * The icon to use for this layout
         */
        private final int iconAttribute;

        @Override
        public int getIconAttribute() {
            return iconAttribute;
        }

        /**
         * The text to use for this layout
         */
        @StringRes
        private final int serverString;

        @Override
        public String getText(Context context) {
            return context.getString(serverString);
        }

        ArtworkListLayout(int iconAttribute, @StringRes int serverString) {
            this.serverString = serverString;
            this.iconAttribute = iconAttribute;
        }
    }
}
