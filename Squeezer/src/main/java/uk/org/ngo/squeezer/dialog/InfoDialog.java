/*
 * Copyright (c) 2012 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.dialog;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;

public class InfoDialog extends DialogFragment {
    private static final String TAG = DialogFragment.class.getSimpleName();
    private static final String TEXT_RESOURCE_KEY = "TEXT_RESOURCE_KEY";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Builder builder = new Builder(getActivity());
        builder.setMessage(Html.fromHtml((String) getText(getArguments().getInt(TEXT_RESOURCE_KEY))));
        builder.setPositiveButton(android.R.string.ok, null);
        return builder.create();
    }

    public static InfoDialog show(FragmentManager fragmentManager, int textResourceId) {
        // Remove any currently showing dialog
        Fragment prev = fragmentManager.findFragmentByTag(TAG);
        if (prev != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(prev);
            fragmentTransaction.commit();
        }

        // Create and show the dialog
        InfoDialog dialog = new InfoDialog();

        Bundle args = new Bundle();
        args.putInt(TEXT_RESOURCE_KEY, textResourceId);
        dialog.setArguments(args);

        dialog.show(fragmentManager, TAG);
        return dialog;
    }
}
