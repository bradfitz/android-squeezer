/*
 * Copyright (c) 2019 Kurt Aaholst <kaaholst@gmail.com>
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

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AlertDialog.Builder;

public class AlertEventDialog extends DialogFragment {
    private static final String TAG = AlertEventDialog.class.getSimpleName();
    private static final String TITLE_KEY = "TITLE_KEY";
    private static final String MESSAGE_KEY = "MESSAGE_KEY";

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Builder(getActivity())
                .setTitle(getArguments().getString(TITLE_KEY))
                .setMessage(getArguments().getString(MESSAGE_KEY))
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }

    public static AlertEventDialog show(FragmentManager fragmentManager, String title, String text) {
        AlertEventDialog dialog = new AlertEventDialog();

        Bundle args = new Bundle();
        args.putString(TITLE_KEY, title);
        args.putString(MESSAGE_KEY, text);
        dialog.setArguments(args);

        dialog.show(fragmentManager, TAG);
        return dialog;
    }
}
