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

import com.google.common.base.Strings;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

public class ScanNetworkTaskTest extends TestCase {
    public void testExtractNameFromBuffer() {
        List<BufferTest> testTable = new ArrayList<>();

        // Success cases.

        // Find NAME if it's the first entry in the buffer.
        testTable.add(new BufferTest(
                "NAME as first entry", "ENAME\04Test", "Test"));

        // Find NAME if it's not the first entry in the buffer.
        testTable.add(new BufferTest(
                "NAME as second entry", "EIPAD\04\01\02\03\04NAME\04Test", "Test"));

        // Single character NAME is OK.
        testTable.add(new BufferTest(
                "NAME is a single char", "EIPAD\04\01\02\03\04NAME\01T", "T"));

        // NAME tuple followed by another tuple is OK.
        testTable.add(new BufferTest(
                "Successor tuple", "EIPAD\04\01\02\03\04NAME\04TestVERS\011", "Test"));

        // Names longer than 127 characters are OK (catch errors as bytes are signed).  Need to
        // use a byte[] here instead of a string, as a string literal \200 and above becomes a
        // 2-byte value when String.getBytes() is called.
        String name = Strings.repeat("a", 128);
        byte [] buffer = ("ENAME\01" + name).getBytes();
        buffer[5] = (byte) 0x80;
        testTable.add(new BufferTest("128 char name", buffer, name));

        //noinspection ReuseOfLocalVariable
        name = Strings.repeat("a", 256);
        //noinspection ReuseOfLocalVariable
        buffer = ("ENAME\01" + name).getBytes();
        buffer[5] = (byte) 0xff;
        testTable.add(new BufferTest("255 char name", buffer, Strings.repeat("a", 255)));

        // Expected failure cases to handle..

        testTable.add(new BufferTest(
                "No NAME block in buffer", "EIPAD\0401\02\03\04", null));
        testTable.add(new BufferTest(
                "NAME block is too short (1)", "EIPAD\04\01\02\03\04N", null));
        testTable.add(new BufferTest(
                "NAME block is too short (2)", "EIPAD\04\01\02\03\04NA", null));
        testTable.add(new BufferTest(
                "NAME block is too short (3)", "EIPAD\04\01\02\03\04NAM", null));
        testTable.add(new BufferTest(
                "NAME block is too short (4)", "EIPAD\04\01\02\03\04NAME", null));
        testTable.add(new BufferTest(
                "NAME block is too short (5)", "EIPAD\04\01\02\03\04NAME\01", null));
        testTable.add(new BufferTest(
                "Corrupted (short) packet", "EIPAD\04\01\02\03\04NAME\10Test 2", null));

        for (BufferTest test : testTable) {
            String actual = ScanNetworkTask.extractNameFromBuffer(test.buffer);
            assertEquals(test.message, test.expected, actual);
        }
    }

    /**
     * Represents a single test of the buffer extraction code.
     */
    private static class BufferTest {
        /** Test message. */
        final String message;

        /** Buffer to test. */
        final byte[] buffer;

        /** Expected value. */
        final String expected;

        BufferTest(String message, byte[] buffer, String expected) {
            this.message = message;
            this.buffer = buffer;
            this.expected = expected;
        }

        BufferTest(String message, String buffer, String expected) {
            this(message, buffer.getBytes(), expected);
        }
    }
}