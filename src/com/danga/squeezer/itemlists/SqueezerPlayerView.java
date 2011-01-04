package com.danga.squeezer.itemlists;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.model.SqueezerPlayer;

public class SqueezerPlayerView extends SqueezerBaseItemView<SqueezerPlayer> {
	private int rowLayout = R.layout.icon_large_row_layout;
	private int iconId = R.id.icon;
	private int text1Id = R.id.label;
	private static final Map<String, Integer> modelIcons = initializeModelIcons();

	public SqueezerPlayerView(Activity activity) {
		super(activity);
	}

	public View getAdapterView(View convertView, SqueezerPlayer item) {
		View row = getActivity().getLayoutInflater().inflate(rowLayout, null);

		TextView label = (TextView) row.findViewById(text1Id);
		label.setText((CharSequence) item.getName());

		ImageView icon = (ImageView) row.findViewById(iconId);
		icon.setImageResource(getModelIcon(item.getModel()));

		return (row);
	}

	public String getQuantityString(int quantity) {
		return getActivity().getResources().getQuantityString(R.plurals.player, quantity);
	}

	private static Map<String, Integer> initializeModelIcons() {
		Map<String, Integer> modelIcons = new HashMap<String, Integer>();
		modelIcons.put("baby", R.drawable.icon_baby);
		modelIcons.put("boom", R.drawable.icon_boom);
		modelIcons.put("fab4", R.drawable.icon_fab4);
		modelIcons.put("receiver", R.drawable.icon_receiver);
		modelIcons.put("controller", R.drawable.icon_controller);
		modelIcons.put("sb1n2", R.drawable.icon_sb1n2);
		modelIcons.put("sb3", R.drawable.icon_sb3);
		modelIcons.put("slimp3", R.drawable.icon_slimp3);
		modelIcons.put("softsqueeze", R.drawable.icon_softsqueeze);
		modelIcons.put("squeezeplay", R.drawable.icon_squeezeplay);
		modelIcons.put("transporter", R.drawable.icon_transporter);
		return modelIcons;
	}

	private int getModelIcon(String model) {
		Integer icon = modelIcons.get(model);
		return (icon != null ? icon : R.drawable.icon_blank);
	}

}
