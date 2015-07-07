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

package uk.org.ngo.squeezer.util;

import com.bluelinelabs.logansquare.LoganSquare;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.org.ngo.squeezer.model.ClientRequestParameters;
import uk.org.ngo.squeezer.model.ClientRequest;

public class JsonTest extends TestCase {
    public void testSeralization() {
        List<JsonSerializeTest> testTable = new ArrayList<>();

        testTable.add(
                new JsonSerializeTest(
                        "No parameters",
                        new ClientRequest(0, "slim.request",
                                new ClientRequestParameters("a_player", "albums", 0, 2)),
                        "{\"id\":0,\"method\":\"slim.request\",\"params\":[\"a_player\",[\"albums\",0,2]]}"));
        testTable.add(
                new JsonSerializeTest(
                        "One parameter",
                        new ClientRequest(0, "slim.request",
                                new ClientRequestParameters("a_player", "albums", 0, 2, "tags:alyj")),
                        "{\"id\":0,\"method\":\"slim.request\",\"params\":[\"a_player\",[\"albums\",0,2,\"tags:alyj\"]]}"));
        testTable.add(
                new JsonSerializeTest(
                        "Two parameters",
                        new ClientRequest(0, "slim.request",
                                new ClientRequestParameters("a_player", "albums", 0, 2, "tags:alyj", "sort:albums")),
                        "{\"id\":0,\"method\":\"slim.request\",\"params\":[\"a_player\",[\"albums\",0,2,\"tags:alyj\",\"sort:albums\"]]}"));

        for (JsonSerializeTest test : testTable) {
            String serializedJson = null;
            ClientRequest parsedClientRequest = null;
            String roundtripJson = null;

            try {
                // Serialize test.clientRequest, parse it back in to an object, and serialize
                // that to ensure that parsing, serialization, and roundtripping works.
                serializedJson = LoganSquare.serialize(test.clientRequest);
                parsedClientRequest = LoganSquare.parse(serializedJson, ClientRequest.class);
                roundtripJson = LoganSquare.serialize(parsedClientRequest);
            } catch (IOException e) {
                e.printStackTrace();
            }

            // ClientRequest should have serialized to expected JSON
            assertEquals(test.message, test.expected, serializedJson);

            // Parsing the generated JSON should give back an identical request.
            assertEquals(test.message + " parsing works", test.clientRequest, parsedClientRequest);

            // Round tripping the new client should give back the same JSON as earlier.
            assertEquals(test.message + " roundtripJson", test.expected, roundtripJson);
        }
    }

    private class JsonSerializeTest {
        public String message;
        public ClientRequest clientRequest;
        public String expected;

        public JsonSerializeTest(String message, ClientRequest clientRequest, String expected) {
            this.message = message;
            this.clientRequest = clientRequest;
            this.expected = expected;
        }
    }
}
