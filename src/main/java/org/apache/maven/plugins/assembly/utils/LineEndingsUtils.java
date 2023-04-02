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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;

import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;

/**
 * Line Ending class which contains convenience methods to change line endings.
 */
public final class LineEndingsUtils {

    private LineEndingsUtils() {
        // prevent creations of instances.
    }

    /**
     * Converts the line endings of a file, writing a new file. The encoding of reading and writing can be specified.
     *
     * @param source      The source file, not null
     * @param dest        The destination file, not null
     * @param lineEndings This is the result of the getLineEndingChars(..) method in this utility class; the actual
     *                    line-ending characters, not null.
     * @param atEndOfFile The end-of-file line ending, if true then the resulting file will have a new line at the end
     *                    even if the input didn't have one, if false then the resulting file will have no new line at
     *                    the end even if the input did have one, null to determine whether to have a new line at the
     *                    end of the file based on the input file
     * @param encoding    The encoding to use, null for platform encoding
     * @throws IOException .
     */
    public static void convertLineEndings(
            final File source, File dest, LineEndings lineEndings, final Boolean atEndOfFile, String encoding)
            throws IOException {
        // MASSEMBLY-637, MASSEMBLY-96
        // find characters at the end of the file
        // needed to preserve the last line ending
        // only check for LF (as CRLF also ends in LF)
        String eofChars = "";
        if (atEndOfFile == null) {
            if (source.length() >= 1) {
                try (RandomAccessFile raf = new RandomAccessFile(source, "r")) {
                    raf.seek(source.length() - 1);
                    byte last = raf.readByte();
                    if (last == '\n') {
                        eofChars = lineEndings.getLineEndingCharacters();
                    }
                }
            }
        } else if (atEndOfFile) {
            eofChars = lineEndings.getLineEndingCharacters();
        }

        try (BufferedReader in = getBufferedReader(source, encoding);
                BufferedWriter out = getBufferedWriter(dest, encoding)) {
            String line = in.readLine();
            while (line != null) {
                out.write(line);
                line = in.readLine();
                if (line != null) {
                    out.write(lineEndings.getLineEndingCharacters());
                } else {
                    out.write(eofChars);
                }
            }
        }
    }

    private static BufferedReader getBufferedReader(File source, String encoding) throws IOException {
        if (encoding == null) {
            // platform encoding
            return new BufferedReader(new InputStreamReader(new FileInputStream(source)));
        } else {
            // MASSEMBLY-371
            return new BufferedReader(new InputStreamReader(new FileInputStream(source), encoding));
        }
    }

    private static BufferedWriter getBufferedWriter(File dest, String encoding) throws IOException {
        if (encoding == null) {
            // platform encoding
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest)));
        } else {
            // MASSEMBLY-371
            return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), encoding));
        }
    }

    /**
     * Converts the line endings of a file, writing a new file. The encoding of reading and writing can be specified.
     *
     * @param in          The source reader
     * @param lineEndings This is the result of the getLineEndingChars(..) method in this utility class; the actual
     *                    line-ending characters, not null.
     * @return an input stream that enforces a specifi line ending style
     */
    @SuppressWarnings("resource")
    public static InputStream lineEndingConverter(InputStream in, LineEndings lineEndings) throws IOException {
        return lineEndings.isNewLine()
                ? new LinuxLineFeedInputStream(in, false)
                : lineEndings.isCrLF() ? new WindowsLineFeedInputStream(in, false) : in;
    }

    public static LineEndings getLineEnding(/* nullable */ String lineEnding) throws AssemblyFormattingException {
        LineEndings result = LineEndings.keep;
        if (lineEnding != null) {
            try {
                result = LineEndings.valueOf(lineEnding);
            } catch (IllegalArgumentException e) {
                throw new AssemblyFormattingException("Illegal lineEnding specified: '" + lineEnding + "'", e);
            }
        }
        return result;
    }

    /**
     * Returns the appopriate line ending characters for the specified style
     *
     * @param lineEnding The name of the line ending style,
     *                   see org.apache.maven.plugin.assembly.utils.LineEndings#valueOf
     * @return The proper line ending characters
     * @throws AssemblyFormattingException
     */
    public static String getLineEndingCharacters(/* nullable */ String lineEnding) throws AssemblyFormattingException {
        String value = lineEnding;

        if (lineEnding != null) {
            try {
                value = LineEndings.valueOf(lineEnding).getLineEndingCharacters();
            } catch (IllegalArgumentException e) {
                throw new AssemblyFormattingException("Illegal lineEnding specified: '" + lineEnding + "'", e);
            }
        }

        return value;
    }
}
