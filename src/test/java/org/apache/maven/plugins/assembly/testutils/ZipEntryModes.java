/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.assembly.testutils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the unix mode of every entry from the central directory of a zip file ("external file attributes", upper
 * 16 bits), so tests can see file modes and symlink entries without a dependency on commons-compress.
 */
public final class ZipEntryModes {

    public static final int S_IFMT = 0170000;

    public static final int S_IFLNK = 0120000;

    private ZipEntryModes() {}

    /**
     * Maps entry name to its unix mode ({@code st_mode}, including the file type bits).
     */
    public static Map<String, Integer> read(File zip) throws IOException {
        byte[] data = java.nio.file.Files.readAllBytes(zip.toPath());
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        int eocd = -1;
        for (int i = data.length - 22; i >= 0; i--) {
            if (buf.getInt(i) == 0x06054b50) {
                eocd = i;
                break;
            }
        }
        if (eocd < 0) {
            throw new IOException("Not a zip file: " + zip);
        }

        int entries = buf.getShort(eocd + 10) & 0xffff;
        int pos = buf.getInt(eocd + 16);
        Map<String, Integer> modes = new LinkedHashMap<>();
        for (int i = 0; i < entries; i++) {
            if (buf.getInt(pos) != 0x02014b50) {
                throw new IOException("Corrupt central directory in " + zip);
            }
            int nameLength = buf.getShort(pos + 28) & 0xffff;
            int extraLength = buf.getShort(pos + 30) & 0xffff;
            int commentLength = buf.getShort(pos + 32) & 0xffff;
            int externalAttributes = buf.getInt(pos + 38);
            String name = new String(data, pos + 46, nameLength, StandardCharsets.UTF_8);
            modes.put(name, externalAttributes >>> 16);
            pos += 46 + nameLength + extraLength + commentLength;
        }
        return modes;
    }

    public static boolean isSymlink(int mode) {
        return (mode & S_IFMT) == S_IFLNK;
    }

    /**
     * The permission bits only (for example {@code 0644}).
     */
    public static int permissions(int mode) {
        return mode & 07777;
    }

    /**
     * Convenience for the entry named {@code name}, failing the test when it is absent.
     */
    public static int modeOf(File zip, String name) throws IOException {
        Integer mode = read(zip).get(name);
        if (mode == null) {
            throw new AssertionError(
                    "No entry '" + name + "' in " + zip + ", has: " + read(zip).keySet());
        }
        return mode;
    }
}
