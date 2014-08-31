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

package uk.org.ngo.squeezer.model;


import android.os.Parcel;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.framework.Item;

public class PlayerSyncGroup extends Item {
    /** The IDs of the players in this group. */
    private List<String> mMemberIds;

    /** The names of the players in this group. */
    private List<String> mMemberNames;

    public PlayerSyncGroup(Map<String, String> record) {
        this(record.get("sync_members"), record.get("sync_member_names"));
    }

    public PlayerSyncGroup(String memberIds, String memberNames) {
        mMemberIds = new ArrayList<String>();
        mMemberNames = new ArrayList<String>();

        for (String member : Splitter.on(",").split(memberIds)) {
            mMemberIds.add(member);
        }

        for (String memberName : Splitter.on(",").split(memberNames)) {
            mMemberNames.add(memberName);
        }
    }

    /**
     * Individual sync groups do not have names.
     *
     * @return The string "sync_group".
     */
    @Override
    public String getName() {
        return "sync_group";
    }

    private PlayerSyncGroup(Parcel source) {
        source.readStringList(mMemberIds);
        source.readStringList(mMemberNames);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(mMemberIds);
        dest.writeStringList(mMemberNames);
    }

    public static final Creator<PlayerSyncGroup> CREATOR = new Creator<PlayerSyncGroup>() {
        public PlayerSyncGroup[] newArray(int size) {
            return new PlayerSyncGroup[size];
        }

        public PlayerSyncGroup createFromParcel(Parcel source) {
            return new PlayerSyncGroup(source);
        }
    };
}
