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
package org.apache.maven.plugins.assembly.archive.archiver;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.exceptions.EmptyArchiveException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IgnoringEmptyArchiveArchiverTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoringEmptyArchiveArchiverTest.class);

    @TempDir
    private File tempDir;

    @Test
    void createArchiveThrowsOnEmptyArchiveWithoutTheWrapper() {
        final JarArchiver delegate = new JarArchiver();
        delegate.setDestFile(new File(tempDir, "plain.jar"));
        delegate.setForced(true);

        assertThrows(EmptyArchiveException.class, delegate::createArchive);
    }

    @Test
    void createArchiveIgnoresEmptyArchive() throws Exception {
        final JarArchiver delegate = new JarArchiver();
        final File destFile = new File(tempDir, "empty.jar");
        delegate.setDestFile(destFile);
        delegate.setForced(true);

        final Archiver archiver = new IgnoringEmptyArchiveArchiver(delegate, LOGGER);

        archiver.createArchive();

        assertFalse(destFile.exists(), "no archive should have been produced for an empty assembly");
    }

    @Test
    void createArchiveStillProducesArchiveWithContent() throws Exception {
        final JarArchiver delegate = new JarArchiver();
        final File destFile = new File(tempDir, "nonEmpty.jar");
        delegate.setDestFile(destFile);
        delegate.setForced(true);
        delegate.addFile(createFile("a-file.txt"), "a-file.txt");

        final Archiver archiver = new IgnoringEmptyArchiveArchiver(delegate, LOGGER);

        archiver.createArchive();

        assertTrue(destFile.exists(), "a non-empty assembly should still be archived");
    }

    private File createFile(final String name) throws IOException {
        final File file = new File(tempDir, name);
        FileUtils.fileWrite(file, "content");
        return file;
    }
}
