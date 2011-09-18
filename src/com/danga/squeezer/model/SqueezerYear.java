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

package com.danga.squeezer.model;

import java.util.Map;

import android.os.Parcel;

import com.danga.squeezer.framework.SqueezerItem;

public class SqueezerYear extends SqueezerItem {

	public SqueezerYear(Map<String, String> record) {
		setId(record.get("year"));
	}

	public static final Creator<SqueezerYear> CREATOR = new Creator<SqueezerYear>() {
		public SqueezerYear[] newArray(int size) {
			return new SqueezerYear[size];
		}

		public SqueezerYear createFromParcel(Parcel source) {
			return new SqueezerYear(source);
		}
	};
	private SqueezerYear(Parcel source) {
		setId(source.readString());
	}
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(getId());
	}

	@Override
	public String getName() {
		return getId();
	}

	@Override
	public String toString() {
		return "year=" + getId();
	}

}
