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

package uk.org.ngo.squeezer.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

/**
 * Display an {@link android.widget.ImageButton} with customised behaviour suitable for use in a
 * {@link android.view.ViewGroup}, such as a list item.
 * <p>
 * The custom behaviour is invoked in {@link #setPressed(boolean)}. Default behaviour is that if any
 * parent views in the ViewGroup are pressed then the button also appears pressed.
 * <p>
 * This has the side effect of making the button's pressed state drawable be overlaid over the
 * parent view's pressed state drawable, appearing to be too bright.
 * <p>
 * This is especially apparent if the user long-presses on the ViewGroup that makes up a row in a
 * list.
 * <p>
 * This class checks to see if any parent views are pressed, and if they are, ignores the press on
 * this view.
 * <p>
 * See Cyril Mottier's discussion of this, and code, in http://android.cyrilmottier.com/?p=525.
 */
public class ListItemImageButton extends ImageButton {

    public ListItemImageButton(Context context) {
        super(context);
    }

    public ListItemImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ListItemImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setPressed(boolean pressed) {
        if (pressed && getParent() instanceof View && ((View) getParent()).isPressed()) {
            return;
        }
        super.setPressed(pressed);
    }
}
