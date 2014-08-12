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

package uk.org.ngo.squeezer.framework;

import uk.org.ngo.squeezer.util.ImageFetcher;

/**
 * Specialization of {@link ItemAdapter} where the items can't be selected.
 *
 * @param <T> Denotes the class of the items in the Adapter
 */
public class DisabledItemListAdapter<T extends Item> extends ItemAdapter<T> {

    public DisabledItemListAdapter(ItemView<T> itemView, ImageFetcher imageFetcher) {
        super(itemView, imageFetcher);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true; // Should be false, but then there is no divider
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }

}
