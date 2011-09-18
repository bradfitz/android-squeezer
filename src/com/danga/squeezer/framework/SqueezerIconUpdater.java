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

package com.danga.squeezer.framework;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import com.danga.squeezer.R;

public class SqueezerIconUpdater<T extends SqueezerItem> {
	private final ScheduledThreadPoolExecutor backgroundExecutor = new ScheduledThreadPoolExecutor(1);
	private final SqueezerBaseActivity activity;

	public SqueezerIconUpdater(SqueezerBaseActivity activity) {
		this.activity = activity;
	}

	private SqueezerBaseActivity getActivity() {
		return activity;
	}

	public void updateIcon(final ImageView icon, final Object item, final String urlString) {
		icon.setImageResource(R.drawable.icon_album_noart);

		if (urlString == null || urlString.length() == 0) {
			icon.setTag(null);
			return;
		}

		icon.setTag(item);
		backgroundExecutor.execute(new Runnable() {
			public void run() {
				if (icon.getTag() != item) {
	                // Bail out before fetch the resource if the item for
	                // album art has changed since this Runnable got scheduled.
	                return;
				}
				try {
					URL url = new URL(urlString);
					final Bitmap bitmap = decodeUrl(url, icon.getHeight());
					getActivity().getUIThreadHandler().post(new Runnable() {
						public void run() {
							if (icon.getTag() == item) {
	                            // Only set the image if the item art hasn't changed since we
	                            // started and finally fetched the image over the network
	                            // and decoded it.
								icon.setImageBitmap(bitmap);
							}
						}

					});
				} catch (MalformedURLException e) {
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), "Error getting drawable from url='" + urlString +"': " + e);
				}
			}
		});
	}

	private static Bitmap decodeUrl(URL url, int imageSize) throws IOException{
	    Bitmap b = null;

	    //Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

		InputStream inputStream = (InputStream) url.getContent();
        BitmapFactory.decodeStream(inputStream, null, o);
        inputStream.close();

        //Find the correct scale value. It should be the power of 2.
        int width_tmp=o.outWidth/2, height_tmp=o.outHeight/2;
        int scale=1;
        while(width_tmp> imageSize && height_tmp > imageSize){
            width_tmp/=2;
            height_tmp/=2;
            scale*=2;
        }

        //Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
		inputStream = (InputStream) url.getContent();
        b = BitmapFactory.decodeStream(inputStream, null, o2);
        inputStream.close();
	    return b;
	}

}