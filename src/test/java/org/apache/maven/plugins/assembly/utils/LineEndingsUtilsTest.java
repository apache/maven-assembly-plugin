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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LineEndingsUtilsTest {

    private static final String CRLF = "\r\n";

    private static final String LF = "\n";

    @Test
    public void shouldWorkCauseWeTestJdkEnumConversion() {
        LineEndings lineEnding = LineEndings.valueOf("windows");
        assertEquals(CRLF, lineEnding.getLineEndingCharacters());
    }

    @Test
    public void shouldReturnDosLineEnding() {
        assertEquals(CRLF, LineEndings.windows.getLineEndingCharacters());
        assertEquals(CRLF, LineEndings.dos.getLineEndingCharacters());
        assertEquals(CRLF, LineEndings.crlf.getLineEndingCharacters());
    }

    @Test
    public void shouldReturnUnixLineEnding() {
        assertEquals(LF, LineEndings.unix.getLineEndingCharacters());
        assertEquals(LF, LineEndings.lf.getLineEndingCharacters());
    }

    @Test
    public void shouldReturnNullAsLineEndingForKeep() {
        assertNull(LineEndings.keep.getLineEndingCharacters());
    }

    @Test
    public void testGetLineEndingCharsShouldReturnDosLineEnding() throws AssemblyFormattingException {
        assertEquals("\r\n", LineEndingsUtils.getLineEndingCharacters("windows"));
        assertEquals("\r\n", LineEndingsUtils.getLineEndingCharacters("dos"));
        assertEquals("\r\n", LineEndingsUtils.getLineEndingCharacters("crlf"));
    }

    @Test
    public void testGetLineEndingCharsShouldReturnUnixLineEnding() throws AssemblyFormattingException {
        assertEquals("\n", LineEndingsUtils.getLineEndingCharacters("unix"));
        assertEquals("\n", LineEndingsUtils.getLineEndingCharacters("lf"));
    }

    @Test
    public void testGetLineEndingCharsShouldReturnNullLineEnding() throws AssemblyFormattingException {
        assertNull(LineEndingsUtils.getLineEndingCharacters("keep"));
    }

    @Test
    public void testGetLineEndingCharsShouldThrowFormattingExceptionWithInvalidHint() {
        assertThrows(AssemblyFormattingException.class, () -> LineEndingsUtils.getLineEndingCharacters("invalid"));
    }

    @Test
    public void testConvertLineEndingsShouldReplaceLFWithCRLF() throws IOException {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.";

        testConversion(test, check, LineEndings.crlf, null);
    }

    @Test
    public void testConvertLineEndingsShouldReplaceLFWithCRLFAtEOF() throws IOException {
        String test = "This is a \ntest.\n";
        String check = "This is a \r\ntest.\r\n";

        testConversion(test, check, LineEndings.crlf, null);
    }

    @Test
    public void testConvertLineEndingsShouldReplaceCRLFWithLF() throws IOException {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.";

        testConversion(test, check, LineEndings.lf, null);
    }

    @Test
    public void testConvertLineEndingsShouldReplaceCRLFWithLFAtEOF() throws IOException {
        String test = "This is a \r\ntest.\r\n";
        String check = "This is a \ntest.\n";

        testConversion(test, check, LineEndings.lf, null);
    }

    @Test
    public void testConvertLineEndingsShouldReplaceLFWithLF() throws IOException {
        String test = "This is a \ntest.";
        String check = "This is a \ntest.";

        testConversion(test, check, LineEndings.lf, null);
    }

    @Test
    public void testConvertLineEndingsShouldReplaceLFWithLFAtEOF() throws IOException {
        String test = "This is a \ntest.\n";
        String check = "This is a \ntest.\n";

        testConversion(test, check, LineEndings.lf, null);
    }

    @Test
    public void testConvertLineEndingsShouldReplaceCRLFWithCRLF() throws IOException {
        String test = "This is a \r\ntest.";
        String check = "This is a \r\ntest.";

        testConversion(test, check, LineEndings.crlf, null);
    }

    @Test
    public void testConvertLineEndingsShouldReplaceCRLFWithCRLFAtEOF() throws IOException {
        String test = "This is a \r\ntest.\r\n";
        String check = "This is a \r\ntest.\r\n";

        testConversion(test, check, LineEndings.crlf, null);
    }

    @Test
    public void testConvertLineEndingsLFToCRLFNoEOFForceEOF() throws IOException {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.\r\n";

        testConversion(test, check, LineEndings.crlf, true);
    }

    @Test
    public void testConvertLineEndingsLFToCRLFWithEOFForceEOF() throws IOException {
        String test = "This is a \ntest.\n";
        String check = "This is a \r\ntest.\r\n";

        testConversion(test, check, LineEndings.crlf, true);
    }

    @Test
    public void testConvertLineEndingsLFToCRLFNoEOFStripEOF() throws IOException {
        String test = "This is a \ntest.";
        String check = "This is a \r\ntest.";

        testConversion(test, check, LineEndings.crlf, false);
    }

    @Test
    public void testConvertLineEndingsLFToCRLFWithEOFStripEOF() throws IOException {
        String test = "This is a \ntest.\n";
        String check = "This is a \r\ntest.";

        testConversion(test, check, LineEndings.crlf, false);
    }

    @Test
    public void testConvertLineEndingsCRLFToLFNoEOFForceEOF() throws IOException {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.\n";

        testConversion(test, check, LineEndings.lf, true);
    }

    @Test
    public void testConvertLineEndingsCRLFToLFWithEOFForceEOF() throws IOException {
        String test = "This is a \r\ntest.\r\n";
        String check = "This is a \ntest.\n";

        testConversion(test, check, LineEndings.lf, true);
    }

    @Test
    public void testConvertLineEndingsCRLFToLFNoEOFStripEOF() throws IOException {
        String test = "This is a \r\ntest.";
        String check = "This is a \ntest.";

        testConversion(test, check, LineEndings.lf, false);
    }

    @Test
    public void testConvertLineEndingsCRLFToLFWithEOFStripEOF() throws IOException {
        String test = "This is a \r\ntest.\r\n";
        String check = "This is a \ntest.";

        testConversion(test, check, LineEndings.lf, false);
    }

    private void testConversion(String test, String check, LineEndings lineEndingChars, Boolean eof)
            throws IOException {
        File source = Files.createTempFile("line-conversion-test-in.", "").toFile();
        source.deleteOnExit();
        File dest = Files.createTempFile("line-conversion-test-out.", "").toFile();
        dest.deleteOnExit();

        try (StringReader sourceReader = new StringReader(test);
                FileWriter sourceWriter = new FileWriter(source)) {
            IOUtils.copy(sourceReader, sourceWriter);
        }

        // Using platform encoding for the conversion tests in this class is OK
        LineEndingsUtils.convertLineEndings(source, dest, lineEndingChars, eof, null);

        try (FileReader destReader = new FileReader(dest);
                StringWriter destWriter = new StringWriter()) {
            IOUtils.copy(destReader, destWriter);
            assertEquals(check, destWriter.toString());
        }
    }
}
