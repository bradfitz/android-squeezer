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

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithTextAndIcon;
import uk.org.ngo.squeezer.itemlist.PluginListActivity;
import uk.org.ngo.squeezer.util.ThemeManager;

public class ViewDialog extends androidx.fragment.app.DialogFragment {

    protected String getTitle() {
        return getContext().getString(R.string.settings_theme_title);
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
                               public int getCount() {
                                   return ThemeManager.Theme.values().length;
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
                                   CheckedTextView textView = (CheckedTextView) LayoutInflater.from(getContext())
                                           .inflate(android.R.layout.select_dialog_singlechoice, parent, false);
                                   ThemeManager.Theme theme = ThemeManager.Theme.values()[position];
                                   textView.setText(theme.getText(getContext()));
                                   textView.setChecked(theme.mThemeId == activity.getThemeId());
                                   return textView;

                               }
                           }, new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int position) {
                                   activity.setTheme(ThemeManager.Theme.values()[position]);
                                   dialog.dismiss();
                               }
                           }
        );
        return builder.create();
    }

}
