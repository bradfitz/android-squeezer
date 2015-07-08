/*
 * Copyright (c) 2015 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.service;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;

abstract class BaseClient implements IClient {

    protected final ConnectionState mConnectionState = new ConnectionState();

    /** Executor for off-main-thread work. */
    @NonNull
    protected final ScheduledThreadPoolExecutor mExecutor = new ScheduledThreadPoolExecutor(1);

    /** Shared event bus for status changes. */
    @NonNull protected final EventBus mEventBus;

    protected final int mPageSize = Squeezer.getContext().getResources().getInteger(R.integer.PageSize);

    BaseClient(EventBus eventBus) {
        mEventBus = eventBus;
    }

    public void initialize() {
        mEventBus.postSticky(new ConnectionChanged(ConnectionState.DISCONNECTED));
    }

}
