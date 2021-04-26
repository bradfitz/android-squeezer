/*
 * Copyright (c) 2014 Kurt Aaholst <kaaholst@gmail.com>
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

package uk.org.ngo.squeezer.download;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.util.AsyncTask;

/**
 * An activity which gives the option, using a dialog theme, to cancel pending
 * Squeezer downloads.
 */
public class CancelDownloadsActivity extends Activity {
    private static final String TAG = CancelDownloadsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cancel_downloads);
        findViewById(R.id.cancel_button).setOnClickListener(view -> finish());
        findViewById(R.id.ok_button).setOnClickListener(view -> {
            cancelDownloads();
            finish();
        });
    }

    private void cancelDownloads() {
        Log.i(TAG, "cancelDownloads");
        new CancelDownloadsTask(this).execute();
    }

    static class CancelDownloadsTask extends AsyncTask<Void, Void, Void> {
        final DownloadDatabase downloadDatabase;
        final DownloadManager downloadManager;

        public CancelDownloadsTask(Context context) {
            downloadDatabase = new DownloadDatabase(context);
            downloadManager =(DownloadManager)context.getSystemService(Context.DOWNLOAD_SERVICE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            downloadDatabase.iterateDownloadEntries(entry -> {
                if (entry.downloadId != -1) {
                    downloadManager.remove(entry.downloadId);
                }
                downloadDatabase.remove(entry.url);
            });
            return null;
        }
    }

}
