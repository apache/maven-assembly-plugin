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
package org.apache.maven.plugins.assembly.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Kristian Rosenvold
 */
class WindowsLineFeedInputStream extends InputStream {

    private static final int NO_PENDING_BYTE = -2;

    private final InputStream inputStream;

    private final boolean ensureLineFeedAtEndOfFile;

    private boolean injectSlashN = false;

    private boolean eofSeen = false;

    private boolean slashNSeen = false;

    private int pendingByte = NO_PENDING_BYTE;

    WindowsLineFeedInputStream(InputStream in, boolean ensureLineFeedAtEndOfFile) {
        this.inputStream = in;
        this.ensureLineFeedAtEndOfFile = ensureLineFeedAtEndOfFile;
    }

    private int readTarget() throws IOException {
        if (pendingByte != NO_PENDING_BYTE) {
            int result = pendingByte;
            pendingByte = NO_PENDING_BYTE;
            return result;
        }
        if (eofSeen) {
            return -1;
        }

        final int target = this.inputStream.read();
        eofSeen = target == -1;
        return target;
    }

    @Override
    public int read() throws IOException {
        if (injectSlashN) {
            injectSlashN = false;
            slashNSeen = true;
            return '\n';
        }

        int target = readTarget();
        if (target == -1) {
            return eofGame();
        }

        slashNSeen = false;
        if (target == '\r') {
            int next = readTarget();
            if (next != '\n' && next != -1) {
                pendingByte = next;
            }
            injectSlashN = true;
            return '\r';
        }
        if (target == '\n') {
            injectSlashN = true;
            return '\r';
        }
        return target;
    }

    private int eofGame() {
        if (ensureLineFeedAtEndOfFile && !slashNSeen) {
            injectSlashN = true;
            return '\r';
        }
        return -1;
    }

    @Override
    public void close() throws IOException {
        super.close();
        inputStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException("Mark not implemented yet");
    }
}
