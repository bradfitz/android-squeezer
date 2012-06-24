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

package uk.org.ngo.squeezer.itemlists;

import uk.org.ngo.squeezer.framework.SqueezerItemListActivity;
import uk.org.ngo.squeezer.model.SqueezerPlugin;
import android.view.ContextMenu;

import uk.org.ngo.squeezer.R;

public class SqueezerApplicationView extends SqueezerPluginView {
	public SqueezerApplicationView(SqueezerItemListActivity activity) {
		super(activity);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.application, quantity);
	}

	public void onItemSelected(int index, SqueezerPlugin item) {
		//TODO what to do?
	}

	public void setupContextMenu(ContextMenu menu, int index, SqueezerPlugin item) {
		//TODO what to do?
	}


}
