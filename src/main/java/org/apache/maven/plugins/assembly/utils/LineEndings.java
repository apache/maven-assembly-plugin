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

/**
 * Enumeration to keep the different line ending types we support.
 *
 * @author Karl-Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
public enum LineEndings {
    keep(null),
    dos("\r\n"),
    windows("\r\n"),
    unix("\n"),
    crlf("\r\n"),
    lf("\n");

    private final String lineEndingCharacters;

    LineEndings(String lineEndingCharacters) {
        this.lineEndingCharacters = lineEndingCharacters;
    }

    public boolean isNewLine() {
        return this == unix || this == lf;
    }

    public boolean isCrLF() {
        return this == windows || this == crlf || this == dos;
    }

    public String getLineEndingCharacters() {
        return this.lineEndingCharacters;
    }
}
