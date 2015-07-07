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

package uk.org.ngo.squeezer.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.google.common.base.Objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@JsonObject
public class ClientRequestParameters {
    /** String identifier for the player the request is for. Null indicates a server request. */
    @JsonField
    public String playerId;

    /** The command to send. */
    @JsonField
    public String command;

    @JsonField
    public int start;

    @JsonField
    public int itemsPerResponse;

    @JsonField
    public List<String> parameters = new ArrayList<>(6);

    public ClientRequestParameters() {}

    public ClientRequestParameters(String playerId, String command,
                                   int start, int itemsPerResponse) {
        this.playerId = playerId;
        this.command = command;
        this.start = start;
        this.itemsPerResponse = itemsPerResponse;
    }

    public ClientRequestParameters(String playerId, String command,
                                   int start, int itemsPerResponse, String... parameters) {
        this.playerId = playerId;
        this.command = command;
        this.start = start;
        this.itemsPerResponse = itemsPerResponse;
        this.parameters = Arrays.asList(parameters);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == null)
        {
            return false;
        }
        if (getClass() != o.getClass())
        {
            return false;
        }
        final ClientRequestParameters other = (ClientRequestParameters) o;

        return  Objects.equal(this.playerId, other.playerId) &&
                Objects.equal(this.command, other.command) &&
                this.start == other.start &&
                this.itemsPerResponse == other.itemsPerResponse &&
                this.parameters.equals(other.parameters);
    }

    @Override
    public int hashCode()
    {
        return com.google.common.base.Objects.hashCode(
                this.playerId, this.command, this.start, this.itemsPerResponse, this.parameters);
    }

}
