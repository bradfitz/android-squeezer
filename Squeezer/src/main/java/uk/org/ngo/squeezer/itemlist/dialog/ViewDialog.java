/*
 * Copyright (c) 2011 Kurt Aaholst <kaaholst@gmail.com>
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

import android.content.Context;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.EnumWithTextAndIcon;
import uk.org.ngo.squeezer.framework.Item;
import uk.org.ngo.squeezer.service.ServerString;

public class ViewDialog extends BaseViewDialog<Item, ViewDialog.ArtworkListLayout> {

    @Override
    protected String getTitle() {
        return ServerString.ALBUM_DISPLAY_OPTIONS.getLocalizedString();
    }

    /**
     * Supported album list layouts.
     */
    public enum ArtworkListLayout implements EnumWithTextAndIcon {
        grid(R.attr.ic_action_view_as_grid, ServerString.SWITCH_TO_GALLERY),
        list(R.attr.ic_action_view_as_list, ServerString.SWITCH_TO_EXTENDED_LIST);

        /**
         * The icon to use for this layout
         */
        private final int iconAttribute;

        @Override
        public int getIconAttribute() {
            return iconAttribute;
        }

        /**
         * The text to use for this layout
         */
        private final ServerString serverString;

        @Override
        public String getText(Context context) {
            return serverString.getLocalizedString();
        }

        ArtworkListLayout(int iconAttribute, ServerString serverString) {
            this.serverString = serverString;
            this.iconAttribute = iconAttribute;
        }
    }
}
