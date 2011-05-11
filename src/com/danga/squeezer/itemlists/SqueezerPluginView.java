package com.danga.squeezer.itemlists;

import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.danga.squeezer.R;
import com.danga.squeezer.framework.SqueezerItemListActivity;
import com.danga.squeezer.model.SqueezerPlugin;
import com.danga.squeezer.service.ISqueezeService;

public abstract class SqueezerPluginView extends SqueezerIconicItemView<SqueezerPlugin> {
	private LayoutInflater layoutInflater;

	public SqueezerPluginView(SqueezerItemListActivity activity) {
		super(activity);
		layoutInflater = activity.getLayoutInflater();
	}

	@Override
	public View getAdapterView(View convertView, SqueezerPlugin item) {
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
		updateIcon(viewHolder.icon, item, getIconUrl(item.getIcon()));

		return convertView;
	}

	private String getIconUrl(String icon) {
		if (icon == null) return null;

		ISqueezeService service = getActivity().getService();
		if (service == null) return null;
			
		try {
			return service.getIconUrl(icon);
		} catch (RemoteException e) {
			Log.e(getClass().getSimpleName(), "Error requesting icon url: " + e);
			return null;
		}
	}

	private static class ViewHolder {
		TextView label;
		ImageView icon;
	}

}
