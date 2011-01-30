package com.danga.squeezer.itemlists;

import java.util.HashMap;
import java.util.Map;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.R;
import com.danga.squeezer.SqueezerBaseActivity;
import com.danga.squeezer.SqueezerBaseItemView;
import com.danga.squeezer.model.SqueezerPlayer;

public class SqueezerPlayerView extends SqueezerBaseItemView<SqueezerPlayer> {
	private LayoutInflater layoutInflater;
	private static final Map<String, Integer> modelIcons = initializeModelIcons();

	public SqueezerPlayerView(SqueezerBaseActivity activity) {
		super(activity);
		layoutInflater = activity.getLayoutInflater();
	}

	public View getAdapterView(View convertView, SqueezerPlayer item) {
		ViewHolder viewHolder;

		if (convertView == null || convertView.getTag() == null) {
			convertView = layoutInflater.inflate(R.layout.icon_large_row_layout, null);
			viewHolder = new ViewHolder();
			viewHolder.label = (TextView) convertView.findViewById(R.id.label);
			viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
			convertView.setTag(viewHolder);
		} else
			viewHolder = (ViewHolder) convertView.getTag();

		viewHolder.label.setText((CharSequence) item.getName());
		viewHolder.icon.setImageResource(getModelIcon(item.getModel()));

		return convertView;
	}

	public void updateAdapterView(View view, SqueezerPlayer item) {
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

	private static class ViewHolder {
		TextView label;
		ImageView icon;
	}

}
