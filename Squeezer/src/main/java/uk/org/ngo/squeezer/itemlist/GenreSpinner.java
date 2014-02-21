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

import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.framework.ItemListActivity;
import uk.org.ngo.squeezer.model.Genre;
import uk.org.ngo.squeezer.service.ISqueezeService;
import uk.org.ngo.squeezer.util.ImageFetcher;

public class GenreSpinner {

    GenreSpinnerCallback callback;

    private final ItemListActivity activity;

    private final Spinner spinner;

    public GenreSpinner(GenreSpinnerCallback callback, ItemListActivity activity, Spinner spinner) {
        this.callback = callback;
        this.activity = activity;
        this.spinner = spinner;
        orderItems(0);
    }

    private void orderItems(int start) {
        if (callback.getService() != null) {
            callback.getService().genres(start, null, genreListCallback);
        }
    }

    private final IServiceItemListCallback<Genre> genreListCallback
            = new IServiceItemListCallback<Genre>() {
        private ItemAdapter<Genre> adapter;

        @Override
        public void onItemsReceived(final int count, final int start, Map<String, String> parameters, final List<Genre> list, Class<Genre> dataType) {
            callback.getUIThreadHandler().post(new Runnable() {
                public void run() {
                    if (adapter == null) {
                        GenreView itemView = new GenreView(activity) {
                            @Override
                            public View getAdapterView(View convertView, ViewGroup parent,
                                    Genre item,
                                    ImageFetcher unused) {
                                return Util.getSpinnerItemView(getActivity(), convertView, parent,
                                        item.getName());
                            }

                            @Override
                            public View getAdapterView(View convertView, ViewGroup parent,
                                    String label) {
                                return Util.getSpinnerItemView(getActivity(), convertView, parent,
                                        label);
                            }

                        };
                        adapter = new ItemAdapter<Genre>(itemView, true, null);
                        spinner.setAdapter(adapter);
                    }
                    adapter.update(count, start, list);
                    spinner.setSelection(adapter.findItem(callback.getGenre()));

                    if (count > start + list.size()) {
                        if ((start + list.size()) % adapter.getPageSize() == 0) {
                            orderItems(start + list.size());
                        }
                    }
                }
            });
        }

        @Override
        public Object getClient() {
            return activity;
        }
    };

    public interface GenreSpinnerCallback {

        ISqueezeService getService();

        Handler getUIThreadHandler();

        Genre getGenre();

        void setGenre(Genre genre);
    }

}
