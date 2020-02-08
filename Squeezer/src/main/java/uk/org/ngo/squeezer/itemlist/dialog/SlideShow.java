/*
 * Copyright (c) 2019 Kurt Aaholst.  All Rights Reserved.
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

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.Action;
import uk.org.ngo.squeezer.framework.BaseActivity;
import uk.org.ngo.squeezer.itemlist.IServiceItemListCallback;
import uk.org.ngo.squeezer.model.Plugin;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class SlideShow extends DialogFragment implements IServiceItemListCallback<Plugin> {
    private static final String TAG = SlideShow.class.getSimpleName();
    private static final int DELAY = 10_000;
    private ImageView artwork;
    private Uri[] images;
    private int nextImage;

    private final Handler handler = new Handler();
    private final Runnable nextSlideTask = new Runnable() {
        @Override
        public void run() {
            nextSlide();
            handler.postDelayed(this, DELAY);
        }
    };

    private void nextSlide() {
        ImageFetcher.getInstance(getContext()).loadImage(images[nextImage], artwork);
        nextImage = ++nextImage % images.length;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BaseActivity activity = (BaseActivity)getActivity();
        Action action = getArguments().getParcelable(Action.class.getName());

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.show_artwork);
        artwork = dialog.findViewById(R.id.artwork);
        artwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (images != null && images.length > 0) {
                    nextSlide();
                }
            }
        });

        Rect rect = new Rect();
        Window window = dialog.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(rect);
        int size = Math.min(rect.width(), rect.height());
        window.setLayout(size, size);

        // FIXME Image wont get fetched (and thus not displayed) after orientation change
        if (activity.getService()!= null) {
            activity.getService().pluginItems(action, this);
        }

        return dialog;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        handler.removeCallbacks(nextSlideTask);
    }

    @Override
    public void onItemsReceived(int count, int start, Map<String, Object> parameters, List<Plugin> items, Class<Plugin> dataType) {
        Object[] item_data = (Object[]) parameters.get("loop_loop");
        if (item_data != null && item_data.length > 0) {
            nextImage = 0;
            images = new Uri[item_data.length];
            for (int i = 0; i < item_data.length; i++) {
                Object item_d = item_data[i];
                Map<String, Object> record = (Map<String, Object>) item_d;
                record.put("urlPrefix", parameters.get("urlPrefix"));
                Uri artworkId = Util.getImageUrl(record, "image");
                images[i] = artworkId;
            }
            nextSlide();
            handler.postDelayed(nextSlideTask, DELAY);
        }
    }

    @Override
    public Object getClient() {
        return getActivity();
    }

    /**
     * Create a dialog to show artwork.
     * <p>
     * We call {@link ISqueezeService#pluginItems(Action, IServiceItemListCallback)} with the
     * supplied <code>action</code> to asynchronously order an artwork id or URL. When the response
     * arrives we load the artwork into the dialog.
     * <p>
     * See Slim/Control/Queries.pm in the slimserver code
     */
    public static SlideShow show(BaseActivity activity, Action action) {
        // Create and show the dialog
        SlideShow dialog = new SlideShow();

        Bundle args = new Bundle();
        args.putParcelable(Action.class.getName(), action);
        dialog.setArguments(args);

        dialog.show(activity.getSupportFragmentManager(), TAG);
        return dialog;
    }
}
