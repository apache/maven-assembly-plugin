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

    private final InputStream target;

    private final boolean ensureLineFeedAtEndOfFile;

    private boolean slashRSeen = false;

    private boolean slashNSeen = false;

    private boolean injectSlashN = false;

    private boolean eofSeen = false;

    WindowsLineFeedInputStream(InputStream in, boolean ensureLineFeedAtEndOfFile) {
        this.target = in;
        this.ensureLineFeedAtEndOfFile = ensureLineFeedAtEndOfFile;
    }

    private int readWithUpdate() throws IOException {
        final int target = this.target.read();
        eofSeen = target == -1;
        if (eofSeen) {
            return target;
        }
        slashRSeen = target == '\r';
        slashNSeen = target == '\n';
        return target;
    }

    @Override
    public int read() throws IOException {
        if (eofSeen) {
            return eofGame();
        } else if (injectSlashN) {
            injectSlashN = false;
            return '\n';
        } else {
            boolean prevWasSlashR = slashRSeen;
            int target = readWithUpdate();
            if (eofSeen) {
                return eofGame();
            }
            if (target == '\n') {
                if (!prevWasSlashR) {
                    injectSlashN = true;
                    return '\r';
                }
            }
            return target;
        }
    }

    private int eofGame() {
        if (!ensureLineFeedAtEndOfFile) {
            return -1;
        }
        if (!slashNSeen && !slashRSeen) {
            slashRSeen = true;
            return '\r';
        }
        if (!slashNSeen) {
            slashRSeen = false;
            slashNSeen = true;
            return '\n';
        } else {
            return -1;
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        target.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException("Mark not implemented yet");
    }
}
