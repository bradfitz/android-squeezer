/*
 * Copyright (c) 2013 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.itemlist;

import uk.org.ngo.squeezer.R;

/**
 * A specialisation of {@link PluginItemView} with a custom {@code getQuantityMethod()} so that the
 * activity title is displayed correctly.
 */
public class FavoritesView extends PluginItemView {

    public FavoritesView(PluginItemListActivity activity) {
        super(activity);
    }

    @Override
    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.favorites, quantity);
    }
}
