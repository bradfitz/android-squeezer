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

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.model.JiveItem;

public class DownloadDialog extends DialogFragment {
    private static final String TAG = DownloadDialog.class.getSimpleName();
    private static final String TITLE_KEY = "TITLE_KEY";
    private DownloadDialogListener callback;

    public DownloadDialog(DownloadDialogListener callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public AlertDialog onCreateDialog(Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(getActivity())
                .setTitle(getString(R.string.download_item, getArguments().getString(TITLE_KEY)))
                .setMultiChoiceItems(new String[]{getString(R.string.DONT_ASK_AGAIN)}, new boolean[]{false}, (dialogInterface, i, b) -> setNegativeButtonText(b))
                .setPositiveButton(R.string.DOWNLOAD, (dialogInterface, i) -> callback.download(isPersistChecked()))
                .setNegativeButton(android.R.string.cancel, (dialogInterface, i) -> callback.cancel(isPersistChecked()))
                .create();
    }

    private void setNegativeButtonText(boolean b) {
        getDialog().getButton(DialogInterface.BUTTON_NEGATIVE).setText(b ? R.string.disable_downloads : android.R.string.cancel);
    }

    private boolean isPersistChecked() {
        return getDialog().getListView().isItemChecked(0);
    }

    @Override
    public AlertDialog getDialog() {
        return (AlertDialog) super.getDialog();
    }

    public static DownloadDialog show(FragmentManager fragmentManager, JiveItem item, DownloadDialogListener callback) {
        DownloadDialog dialog = new DownloadDialog(callback);

        Bundle args = new Bundle();
        args.putString(TITLE_KEY, item.getName());
        dialog.setArguments(args);

        dialog.show(fragmentManager, TAG);
        return dialog;
    }

    public interface DownloadDialogListener {
        void download(boolean persist);
        void cancel(boolean persist);
    }
}
