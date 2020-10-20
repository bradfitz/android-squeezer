/*
 * Copyright (c) 2020 Kurt Aaholst <kaaholst@gmail.com>
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

import android.content.ClipData;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.view.GestureDetectorCompat;
import androidx.palette.graphics.Palette;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.framework.BaseItemView;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.widget.AnimationEndListener;
import uk.org.ngo.squeezer.widget.OnSwipeListener;
import uk.org.ngo.squeezer.widget.UndoBarController;

class CurrentPlaylistItemView extends JiveItemView {
    private static final int ANIMATION_DURATION = 200;

    private final CurrentPlaylistActivity activity;

    public CurrentPlaylistItemView(CurrentPlaylistActivity activity) {
        super(activity, activity.window.windowStyle);
        this.activity = activity;
    }

    @Override
    public BaseItemView.ViewHolder createViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public void bindView(View view, JiveItem item) {
        super.bindView(view, item);
        view.setBackgroundResource(getActivity().getAttributeValue(R.attr.selectableItemBackground));
        ViewHolder viewHolder = (ViewHolder) view.getTag();

        if (viewHolder.position == activity.getItemAdapter().getSelectedIndex()) {
            viewHolder.text1.setTextAppearance(getActivity(), R.style.SqueezerTextAppearance_ListItem_Primary_Highlight);
            viewHolder.text2.setTextAppearance(getActivity(), R.style.SqueezerTextAppearance_ListItem_Secondary_Highlight);
        } else {
            viewHolder.text1.setTextAppearance(getActivity(), R.style.SqueezerTextAppearance_ListItem_Primary);
            viewHolder.text2.setTextAppearance(getActivity(), R.style.SqueezerTextAppearance_ListItem_Secondary);
        }

        view.setAlpha(viewHolder.position == activity.getDraggedIndex() ? 0 : 1);

        final GestureDetectorCompat detector = new GestureDetectorCompat(getActivity(), new OnSwipeListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                view.setPressed(true);
                return super.onDown(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                activity.setDraggedIndex(viewHolder.position);
                view.setPressed(false);
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                view.setActivated(true);
                view.startDrag(data, shadowBuilder, null, 0);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                onItemSelected(view, viewHolder.position, item);
                return true;
            }

            @Override
            public boolean onSwipeLeft() {
                removeItem(view, viewHolder.position, item);
                return true;
            }

            @Override
            public boolean onSwipeRight() {
                removeItem(view, viewHolder.position, item);
                return true;
            }
        });

        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                view.setPressed(false);
                view.performClick();
            }
            return detector.onTouchEvent(event);
        });
    }

    @Override
    public void onIcon(ViewHolder viewHolder) {
        if (viewHolder.position == activity.getItemAdapter().getSelectedIndex() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Drawable icon = viewHolder.icon.getDrawable();
            if (icon instanceof TransitionDrawable) {
                icon = ((TransitionDrawable) icon).getDrawable(1);
            }

            Drawable marker = AppCompatResources.getDrawable(activity, R.drawable.ic_action_nowplaying);
            Palette colorPalette = Palette.from(((BitmapDrawable) icon).getBitmap()).generate();
            marker.setTint(colorPalette.getDominantSwatch().getBodyTextColor());

            LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{icon, marker});
            layerDrawable.setLayerGravity(1, Gravity.CENTER);

            viewHolder.icon.setImageDrawable(layerDrawable);
        }
    }

    private void removeItem(View view, int position, JiveItem item) {
        final AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(new ScaleAnimation(1F, 1F, 1F, 0.5F));
        animationSet.addAnimation(new AlphaAnimation(1F, 0F));
        animationSet.setDuration(ANIMATION_DURATION);
        animationSet.setAnimationListener(new AnimationEndListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                activity.getItemAdapter().removeItem(position);
                UndoBarController.show(activity, activity.getString(R.string.JIVE_POPUP_REMOVING_FROM_PLAYLIST, item.getName()), new UndoBarController.UndoListener() {
                    @Override
                    public void onUndo() {
                        activity.getItemAdapter().insertItem(position, item);
                    }

                    @Override
                    public void onDone() {
                        ISqueezeService service = activity.getService();
                        if (service != null) {
                            service.playlistRemove(position);
                            activity.skipPlaylistChanged();
                        }
                    }
                });
            }
        });

        view.startAnimation(animationSet);


    }

    @Override
    public boolean isSelectable(JiveItem item) {
        return true;
    }

    /**
     * Jumps to whichever song the user chose.
     */
    @Override
    public boolean onItemSelected(View view, int index, JiveItem item) {
        ISqueezeService service = getActivity().getService();
        if (service != null) {
            getActivity().getService().playlistIndex(index);
        }
        return false;
    }
}
