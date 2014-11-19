/*
 * Copyright (c) 2014 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer.service.event;

import android.support.annotation.NonNull;

import uk.org.ngo.squeezer.model.PlayerState;

/** Event sent when the repeat status of the active player has changed. */
public class RepeatStatusChanged {
    /** True if the previous repeat status was unknown. */
    public final boolean mInitial;

    /** The new repeat status. */
    @NonNull
    public final PlayerState.RepeatStatus mRepeatStatus;

    public RepeatStatusChanged(boolean initial, @NonNull PlayerState.RepeatStatus repeatStatus) {
        mInitial = initial;
        mRepeatStatus = repeatStatus;
    }
}
