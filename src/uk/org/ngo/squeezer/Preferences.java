/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
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

package uk.org.ngo.squeezer;

public final class Preferences {
	public static final String NAME = "Squeezer";

	// e.g. "10.0.0.81:9090"
	public static final String KEY_SERVERADDR = "squeezer.serveraddr";

	// The playerId that we were last connected to. e.g. "00:04:20:17:04:7f"
    public static final String KEY_LASTPLAYER = "squeezer.lastplayer";

    public static final String KEY_AUTO_DISCOVER = "squeezer.autodiscover";
    public static final String KEY_AUTO_CONNECT = "squeezer.autoconnect";

    // Do we keep the notification going at top, even when we're not connected?
    public static final String KEY_NOTIFY_OF_CONNECTION = "squeezer.notifyofconnection";

    // Do we scrobble track information?
    public static final String KEY_SCROBBLE = "squeezer.scrobble";

    public static final String KEY_DEBUG_LOGGING = "squeezer.debuglogging";

    // Do we send anonymous usage statistics?
    public static final String KEY_ANALYTICS_ENABLED = "squeezer.analytics.enabled";

	private Preferences() {
	}
}
