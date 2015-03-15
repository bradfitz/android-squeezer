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

public class ConnectionChanged {
    /** Has the network connection to the media server been made? */
    public final boolean mIsConnected;

    /** True if this is the very first callback after a new initial connect attempt. */
    public final boolean mPostConnect;

    /** The server disconnected before handshake completed, indicating that login failed. */
    public final boolean mLoginFailed;

    public ConnectionChanged(boolean isConnected, boolean postConnect, boolean loginFailed) {
        mIsConnected = isConnected;
        mPostConnect = postConnect;
        mLoginFailed = loginFailed;
    }

    @Override
    public String toString() {
        return "ConnectionChanged{" +
                "mIsConnected=" + mIsConnected +
                ", mPostConnect=" + mPostConnect +
                ", mLoginFailed=" + mLoginFailed +
                '}';
    }
}
