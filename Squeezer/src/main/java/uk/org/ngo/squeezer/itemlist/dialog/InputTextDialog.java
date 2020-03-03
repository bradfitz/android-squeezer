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
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.framework.Item;

public class InputTextDialog extends BaseEditTextDialog {
    private BaseActivity activity;
    private Item item;
    private int alreadyPopped;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = (BaseActivity)getActivity();
        item = getArguments().getParcelable(Item.class.getName());
        alreadyPopped = getArguments().getInt("alreadyPopped", 0);

        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(item.getName());
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editTextLayout.setHint(item.input.title);
        editText.setText(item.input.initialText);

        return dialog;
    }

    @Override
    protected boolean commit(String inputString) {
        item.inputValue = inputString;
        activity.action(item, item.goAction, alreadyPopped);
        return true;
    }

    /**
     * Create a dialog to input text before proceding with the actions
     * <p>
     * See http://wiki.slimdevices.com/index.php/SBS_SqueezePlay_interface#.3Cinput_fields.3E
     */
    public static void show(BaseActivity activity, Item item, int alreadyPopped) {
        InputTextDialog dialog = new InputTextDialog();

        Bundle args = new Bundle();
        args.putParcelable(Item.class.getName(), item);
        args.putInt("alreadyPopped", alreadyPopped);
        dialog.setArguments(args);

        dialog.show(activity.getSupportFragmentManager(), DialogFragment.class.getSimpleName());
    }
}
