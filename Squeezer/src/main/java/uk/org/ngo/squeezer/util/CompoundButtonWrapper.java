/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.util;

import android.widget.CompoundButton;

/**
 * Helper class to prevent {@link CompoundButton.OnCheckedChangeListener#onCheckedChanged(CompoundButton, boolean)}
 * from being called when the checked state is set pragmatically. (i.e. by {@link CompoundButton#setChecked(boolean)})
*/
public class CompoundButtonWrapper {
    private final CompoundButton button;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

    public CompoundButtonWrapper(CompoundButton button) {
        this.button = button;
    }

    public CompoundButton getButton() {
        return button;
    }

    public void setChecked(boolean checked) {
        button.setOnCheckedChangeListener(null);
        button.setChecked(checked);
        button.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    public void setOncheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
        button.setOnCheckedChangeListener(listener);
    }

    public void setEnabled(boolean enabled) {
        button.setEnabled(enabled);
    }
}
