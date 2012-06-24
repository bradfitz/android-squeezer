/*
 * Copyright (c) 2012 Kurt Aaholst <kaaholst@gmail.com>
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

import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.SqueezerItem;

/**
 * <p>
 * Implement this and give it to {@link SqueezerCLIImpl#parseSqueezerList(List, SqueezerListHandler)} for each
 * extended query format command you wish to support.
 * </p>
 * @author Kurt Aaholst
 */
interface SqueezerListHandler<T extends SqueezerItem> {
	/**
	 * @return The type of item this handler can handle
	 */
	Class<T> getDataType();

	/**
	 * Prepare for parsing an extended query format response
	 */
	void clear();

	/**
	 * Called for each item received in the current reply. Just store this internally.
	 * @param record
	 */
	void add(Map<String, String> record);

	/**
	 * Called when the current reply is completely parsed. Pass the information on to your activity now. If there are
	 * any more data, it is automatically ordered by {@link SqueezerCLIImpl#parseSqueezerList(List, SqueezerListHandler)}
	 * @param rescan Set if SqueezeServer is currently doing a scan of the music library.
	 * @param count Total number of result for the current query.
	 * @param max The current configured default maximum list size.
	 * @param start Offset for the current list in total results.
	 * @return
	 */
	boolean processList(boolean rescan, int count, int start, Map<String, String> parameters);
}