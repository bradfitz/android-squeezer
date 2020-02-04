/*
 * Copyright (c) 2019 Kurt Aaholst.  All Rights Reserved.
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
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.Item;

public class ChoicesDialog extends DialogFragment {
    private static final String TAG = DialogFragment.class.getSimpleName();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final BaseActivity activity = (BaseActivity)getActivity();
        final Item item = getArguments().getParcelable(Item.class.getName());
        final int alreadyPopped = getArguments().getInt("alreadyPopped", 0);

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.choices_layout);

        ((TextView)dialog.findViewById(R.id.header)).setText(item.getName());

        RadioGroup radioGroup = dialog.findViewById(R.id.choices);
        for (int i = 0; i < item.choiceStrings.length; i++) {
            RadioButton radioButton = new RadioButton(activity);
            radioButton.setText(item.choiceStrings[i]);
            radioButton.setId(i);
            radioGroup.addView(radioButton);
        }
        radioGroup.check(item.selectedIndex-1);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                activity.action(item.goAction.choices[checkedId], alreadyPopped);
                dismiss();
            }
        });


        return dialog;
    }

    /**
     * Create a dialog to select from choices
     * <p>
     * See http://wiki.slimdevices.com/index.php/SBS_SqueezePlay_interface#Choices_array_in_Do_action
     * and choiceStrings of
     * http://wiki.slimdevices.com/index.php/SBS_SqueezePlay_interface#.3Citem_fields.3E
     */
    public static ChoicesDialog show(BaseActivity activity, Item item, int alreadyPopped) {
        // Create and show the dialog
        ChoicesDialog dialog = new ChoicesDialog();

        Bundle args = new Bundle();
        args.putParcelable(Item.class.getName(), item);
        args.putInt("alreadyPopped", alreadyPopped);
        dialog.setArguments(args);

        dialog.show(activity.getSupportFragmentManager(), TAG);
        return dialog;
    }
}
