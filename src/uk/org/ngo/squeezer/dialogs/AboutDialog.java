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

package uk.org.ngo.squeezer.dialogs;

import uk.org.ngo.squeezer.R;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View view = getActivity().getLayoutInflater().inflate(R.layout.about_dialog, null);
        final TextView titleText = (TextView) view.findViewById(R.id.about_title);

        PackageManager pm = getActivity().getPackageManager();
        PackageInfo info;
        try {
            info = pm.getPackageInfo("uk.org.ngo.squeezer", 0);
            titleText.setText(getString(R.string.about_title, info.versionName));
        } catch (NameNotFoundException e) {
            titleText.setText(getString(R.string.app_name));
        }

        Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(R.string.dialog_license, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                new LicenseDialog()
                        .show(getActivity().getSupportFragmentManager(), "LicenseDialog");
            }
        });
        return builder.create();
    }
}