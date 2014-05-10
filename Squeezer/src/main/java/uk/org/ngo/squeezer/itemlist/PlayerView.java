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

package uk.org.ngo.squeezer.itemlist;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.model.PlayerState;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class PlayerView extends BaseItemView<Player> {

    private static final Map<String, Integer> modelIcons = initializeModelIcons();

    public PlayerView(PlayerListActivity activity) {
        super(activity);

        setViewParams(EnumSet.of(ViewParams.ICON));
        setLoadingViewParams(EnumSet.of(ViewParams.ICON));
    }

    @Override
    public View getAdapterView(View convertView, ViewGroup parent, EnumSet<ViewParams> viewParams) {
        return getAdapterView(convertView, parent, viewParams, R.layout.list_item_player);
    }

    public void bindView(View view, Player item, ImageFetcher imageFetcher) {
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        viewHolder.text1.setText(item.getName());
        viewHolder.icon.setImageResource(getModelIcon(item.getModel()));

        ImageButton power_button = (ImageButton) view.findViewById(R.id.power_button);
        PlayerListActivity activity = (PlayerListActivity) getActivity();
        PlayerState playerState = activity.getPlayerState(item.getId());
        power_button.setVisibility(playerState == null ? View.GONE : View.VISIBLE);
        if (playerState != null) {
            power_button.setAlpha(playerState.isPoweredOn() ? 1.0F : 0.5F);
            power_button.setTag(item);
            power_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Player player = (Player) view.getTag();
                    getActivity().getService().togglePower(player);
                }
            });
        }
    }

    public void onItemSelected(int index, Player item) {
    }

    public String getQuantityString(int quantity) {
        return getActivity().getResources().getQuantityString(R.plurals.player, quantity);
    }

    private static Map<String, Integer> initializeModelIcons() {
        Map<String, Integer> modelIcons = new HashMap<String, Integer>();
        modelIcons.put("baby", R.drawable.ic_baby);
        modelIcons.put("boom", R.drawable.ic_boom);
        modelIcons.put("fab4", R.drawable.ic_fab4);
        modelIcons.put("receiver", R.drawable.ic_receiver);
        modelIcons.put("controller", R.drawable.ic_controller);
        modelIcons.put("sb1n2", R.drawable.ic_sb1n2);
        modelIcons.put("sb3", R.drawable.ic_sb3);
        modelIcons.put("slimp3", R.drawable.ic_slimp3);
        modelIcons.put("softsqueeze", R.drawable.ic_softsqueeze);
        modelIcons.put("squeezeplay", R.drawable.ic_squeezeplay);
        modelIcons.put("transporter", R.drawable.ic_transporter);
        modelIcons.put("squeezeplayer", R.drawable.ic_squeezeplayer);
        return modelIcons;
    }

    private static int getModelIcon(String model) {
        Integer icon = modelIcons.get(model);
        return (icon != null ? icon : R.drawable.ic_blank);
    }
}
