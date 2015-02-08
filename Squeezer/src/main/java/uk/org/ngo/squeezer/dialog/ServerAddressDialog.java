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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import uk.org.ngo.squeezer.NowPlayingFragment;
import uk.org.ngo.squeezer.R;

public class ServerAddressDialog extends DialogFragment {
    private ServerAddressView form;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        form = new ServerAddressView(activity);
        builder.setView(form);
        builder.setTitle(R.string.settings_serveraddr_title);


        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                form.savePreferences();
                ((NowPlayingFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.now_playing_fragment)).startVisibleConnection();
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        form.onDismiss();
        super.onDismiss(dialog);
    }
}
