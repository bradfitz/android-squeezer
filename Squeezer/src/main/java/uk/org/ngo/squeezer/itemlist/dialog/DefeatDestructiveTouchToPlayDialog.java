/*
 * Copyright (c) 2020 Kurt Aaholst.  All Rights Reserved.
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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import uk.org.ngo.squeezer.R;

public class DefeatDestructiveTouchToPlayDialog extends BaseChoicesDialog {
    /**
     * Activities that host this dialog must implement this interface.
     */
    public interface DefeatDestructiveTouchToPlayDialogHost {
        FragmentManager getSupportFragmentManager();
        String getDefeatDestructiveTTP();
        void setDefeatDestructiveTTP(@NonNull String option);
    }

    private DefeatDestructiveTouchToPlayDialogHost host;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        host = (DefeatDestructiveTouchToPlayDialogHost) context;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] options = {
            getString(R.string.SETUP_DEFEAT_DESTRUCTIVE_TTP_0),
            getString(R.string.SETUP_DEFEAT_DESTRUCTIVE_TTP_1),
            getString(R.string.SETUP_DEFEAT_DESTRUCTIVE_TTP_2),
            getString(R.string.SETUP_DEFEAT_DESTRUCTIVE_TTP_3),
            getString(R.string.SETUP_DEFEAT_DESTRUCTIVE_TTP_4)
        };
        return createDialog(
                getString(R.string.SETUP_DEFEAT_DESTRUCTIVE_TTP),
                getString(R.string.SETUP_DEFEAT_DESTRUCTIVE_TTP_DESC),
                Integer.parseInt(host.getDefeatDestructiveTTP()),
                options
        );
    }

    @Override
    protected void onSelectOption(int checkedId) {
        host.setDefeatDestructiveTTP(String.valueOf(checkedId));
    }

    /**
     * Create a dialog to select the defeat destructive touch-to-play option.
     *
     * @param host The hosting activity must implement {@link DefeatDestructiveTouchToPlayDialogHost} to provide
     *            the information that the dialog needs.
     */
    public static DefeatDestructiveTouchToPlayDialog show(DefeatDestructiveTouchToPlayDialogHost host) {
        DefeatDestructiveTouchToPlayDialog dialog = new DefeatDestructiveTouchToPlayDialog();

        dialog.show(host.getSupportFragmentManager(), DefeatDestructiveTouchToPlayDialog.class.getSimpleName());

        return dialog;
    }
}
