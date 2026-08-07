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
package org.apache.maven.plugins.assembly.filter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.resources.DefaultPlexusIoFileResourceCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleAggregatingDescriptorHandlerTest {

    @TempDir
    private Path temporaryFolder;

    @Test
    void shouldAggregateMatchingFilesIntoOutputPath() throws Exception {
        final SimpleAggregatingDescriptorHandler handler = new SimpleAggregatingDescriptorHandler();
        handler.setFilePattern(".*/file\\.txt");
        handler.setOutputPath("file.txt");

        final Path baseDir = Files.createDirectories(temporaryFolder.resolve("src"));
        Files.createDirectories(baseDir.resolve("a"));
        Files.createDirectories(baseDir.resolve("b"));
        Files.write(baseDir.resolve("a/file.txt"), "file A\n".getBytes(StandardCharsets.UTF_8));
        Files.write(baseDir.resolve("b/file.txt"), "file B\n".getBytes(StandardCharsets.UTF_8));

        final ZipArchiver archiver = new ZipArchiver();
        final File archiveFile = new File(temporaryFolder.toFile(), "archive.zip");
        archiver.setDestFile(archiveFile);
        archiver.addResources(resourceCollection(baseDir.toFile(), handler));
        archiver.setArchiveFinalizers(Collections.singletonList(handler));

        archiver.createArchive();

        try (ZipFile zf = new ZipFile(archiveFile)) {
            final ZipEntry entry = zf.getEntry("file.txt");
            assertTrue(entry != null, "aggregated file.txt should be present in the archive");

            final String content = new String(readAllBytes(zf.getInputStream(entry)), StandardCharsets.UTF_8);

            assertTrue(content.contains("file A"), "aggregated content should contain file A");
            assertTrue(content.contains("file B"), "aggregated content should contain file B");
        }
    }

    @Test
    void shouldNotIncludeAggregatedSourcesInArchive() throws Exception {
        final SimpleAggregatingDescriptorHandler handler = new SimpleAggregatingDescriptorHandler();
        handler.setFilePattern(".*/file\\.txt");
        handler.setOutputPath("file.txt");

        final Path baseDir = Files.createDirectories(temporaryFolder.resolve("src"));
        Files.createDirectories(baseDir.resolve("a"));
        Files.createDirectories(baseDir.resolve("b"));
        Files.write(baseDir.resolve("a/file.txt"), "file A\n".getBytes(StandardCharsets.UTF_8));
        Files.write(baseDir.resolve("b/file.txt"), "file B\n".getBytes(StandardCharsets.UTF_8));

        final ZipArchiver archiver = new ZipArchiver();
        final File archiveFile = new File(temporaryFolder.toFile(), "archive.zip");
        archiver.setDestFile(archiveFile);
        archiver.addResources(resourceCollection(baseDir.toFile(), handler));
        archiver.setArchiveFinalizers(Collections.singletonList(handler));

        archiver.createArchive();

        try (ZipFile zf = new ZipFile(archiveFile)) {
            final List<? extends ZipEntry> entries = Collections.list(zf.entries());

            assertEquals(
                    1,
                    entries.stream().filter(e -> !e.isDirectory()).count(),
                    "the matching sources should be excluded, only the aggregated file.txt should remain");
        }
    }

    private DefaultPlexusIoFileResourceCollection resourceCollection(final File baseDir, final FileSelector selector)
            throws IOException {
        final DefaultPlexusIoFileResourceCollection collection = new DefaultPlexusIoFileResourceCollection();
        collection.setBaseDir(baseDir);
        collection.setIncludes(new String[] {"**/file.txt"});
        collection.setFileSelectors(new FileSelector[] {selector});
        return collection;
    }

    private byte[] readAllBytes(final java.io.InputStream in) throws IOException {
        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        final byte[] buffer = new byte[4096];
        for (int n = in.read(buffer); n != -1; n = in.read(buffer)) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
}
