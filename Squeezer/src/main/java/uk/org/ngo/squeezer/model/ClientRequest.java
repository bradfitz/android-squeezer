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

import com.bluelinelabs.logansquare.annotation.JsonField;
import com.bluelinelabs.logansquare.annotation.JsonObject;
import com.google.common.base.Objects;

/**
 * Represents a request to an LMS.
 */
@JsonObject
public class ClientRequest {
    /** Identifier for the request, echoed back by the LMS. */
    @JsonField
    public int id;

    /** The server method to call. */
    @JsonField
    public String method;

    /** The parameters to pass to the method. */
    @JsonField(typeConverter = ClientRequestParametersConverter.class)
    public ClientRequestParameters params;

    public ClientRequest() {}

    public ClientRequest(int id, String method, ClientRequestParameters params) {
        this.id = id;
        this.method = method;
        this.params = params;
    }

    /**
     * Client requests are equal when their fields are equal.
     * {@inheritDoc}
     */
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
        final ClientRequest other = (ClientRequest) o;

        return  Objects.equal(this.id, other.id) &&
                Objects.equal(this.method, other.method) &&
                this.params.equals(other.params);
    }

    @Override
    public int hashCode()
    {
        return com.google.common.base.Objects.hashCode(
                this.id, this.method, this.params);
    }
}
