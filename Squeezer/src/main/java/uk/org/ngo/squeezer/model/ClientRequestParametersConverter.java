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

import com.bluelinelabs.logansquare.typeconverters.TypeConverter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * Convert between JSON serialised "params" blocks and ClientRequestParameters.
 */
public class ClientRequestParametersConverter implements TypeConverter<ClientRequestParameters> {
    @Override
    public ClientRequestParameters parse(JsonParser jsonParser) throws IOException {
        ClientRequestParameters clientRequestParameters = new ClientRequestParameters();

        if (jsonParser.isExpectedStartArrayToken()) {
            clientRequestParameters.playerId = jsonParser.nextTextValue();
            jsonParser.nextToken();
            if (jsonParser.isExpectedStartArrayToken()) {
                clientRequestParameters.command = jsonParser.nextTextValue();
                clientRequestParameters.start = jsonParser.nextIntValue(0);
                clientRequestParameters.itemsPerResponse = jsonParser.nextIntValue(0);

                String parameter = jsonParser.nextTextValue();
                while (parameter != null) {
                    clientRequestParameters.parameters.add(parameter);
                    parameter = jsonParser.nextTextValue();
                }
                jsonParser.nextToken();
            }
        }

        return clientRequestParameters;
    }

    /**
     * Serialise to
     * <code>"params":["$playerId",["$command",$start,$itemsPerResponse,"$parameters",..."]]</code>.
     * @param object
     * @param fieldName
     * @param writeFieldNameForObject
     * @param jsonGenerator
     * @throws IOException
     */
    @Override
    public void serialize(ClientRequestParameters object, String fieldName,
                          boolean writeFieldNameForObject, JsonGenerator jsonGenerator) throws IOException {
        // Emit <"params:">
        if (writeFieldNameForObject) {
            jsonGenerator.writeFieldName(fieldName);
        }

        // Emit <[>
        jsonGenerator.writeStartArray();

        // Emit <"$playerId",> or <"",>
        if (object.playerId != null) {
            jsonGenerator.writeString(object.playerId);
        } else {
            jsonGenerator.writeString("");
        }

        // Emit <[>
        jsonGenerator.writeStartArray();

        // Emit <"$command", $start, $itemsPerResponse>
        jsonGenerator.writeString(object.command);
        jsonGenerator.writeNumber(object.start);
        jsonGenerator.writeNumber(object.itemsPerResponse);

        // Emit <"$parameters">
        if (object.parameters != null) {
            for (String param : object.parameters) {
                jsonGenerator.writeString(param);
            }
        }

        // Emit <]]>
        jsonGenerator.writeEndArray();
        jsonGenerator.writeEndArray();
    }
}
