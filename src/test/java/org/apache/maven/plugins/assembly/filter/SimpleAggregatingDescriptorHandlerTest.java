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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.diags.NoOpArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimpleAggregatingDescriptorHandlerTest {

    private SimpleAggregatingDescriptorHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SimpleAggregatingDescriptorHandler();
        handler.setFilePattern(".*file.txt");
        handler.setOutputPath("file.txt");
    }

    @Test
    void finalizeArchiveCreationShouldAggregateResourcesVisitedAfterFinalizersRun() throws Exception {
        PlexusIoResource a = resource("a/file.txt", "file A");
        PlexusIoResource b = resource("b/file.txt", "file B");

        List<String> lines = finalizeWith(new WalkingArchiver(handler, a, b));

        assertEquals(1, count(lines, "file A"), lines.toString());
        assertEquals(1, count(lines, "file B"), lines.toString());
    }

    @Test
    void shouldAggregateFilesWithTheSameNameFromDifferentSources() throws Exception {
        // <file> entries are checked when added, and are named after the file alone
        assertFalse(handler.isSelected(info("file.txt", "file A")));
        assertFalse(handler.isSelected(info("file.txt", "file B")));
        // unpacked dependencies carry the same archive-internal path
        PlexusIoResource one = resource("META-INF/file.txt", "from jar 1");
        PlexusIoResource two = resource("META-INF/file.txt", "from jar 2");

        List<String> lines = finalizeWith(new WalkingArchiver(handler, one, two));

        for (String expected : new String[] {"file A", "file B", "from jar 1", "from jar 2"}) {
            assertEquals(1, count(lines, expected), lines.toString());
        }
    }

    @Test
    void shouldNotDuplicateContentWhenResourcesAreWalkedAgain() throws Exception {
        PlexusIoResource a = resource("a/file.txt", "file A");
        WalkingArchiver archiver = new WalkingArchiver(handler, a);

        // another finalizer's walk before ours
        archiver.walk();

        List<String> lines = finalizeWith(archiver);
        // execute() walks again after the aggregate was written
        archiver.walk();

        assertEquals(1, count(lines, "file A"), lines.toString());
    }

    @Test
    void shouldNotCarryContentOverToTheNextArchive() throws Exception {
        finalizeWith(new WalkingArchiver(handler, resource("a/file.txt", "file A")));
        // the walk of the first archive's execute()
        assertFalse(handler.isSelected(resource("a/file.txt", "file A")));

        List<String> lines = finalizeWith(new WalkingArchiver(handler, resource("b/file.txt", "file B")));

        assertEquals(1, count(lines, "file B"), lines.toString());
        assertEquals(0, count(lines, "file A"), lines.toString());
    }

    private static long count(List<String> lines, String line) {
        return lines.stream().filter(line::equals).count();
    }

    private List<String> finalizeWith(WalkingArchiver archiver) throws IOException {
        handler.finalizeArchiveCreation(archiver);
        assertNotNull(archiver.aggregate, "aggregate should have been added");
        return Files.readAllLines(archiver.aggregate.toPath(), StandardCharsets.UTF_8);
    }

    private static PlexusIoResource resource(String name, String content) throws IOException {
        PlexusIoResource r = mock(PlexusIoResource.class);
        when(r.getName()).thenReturn(name);
        when(r.isFile()).thenReturn(true);
        when(r.getContents()).thenAnswer(i -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return r;
    }

    private static FileInfo info(String name, String content) {
        return new FileInfo() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public boolean isFile() {
                return true;
            }

            @Override
            public boolean isDirectory() {
                return false;
            }

            @Override
            public boolean isSymbolicLink() {
                return false;
            }

            @Override
            public java.io.InputStream getContents() {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    /** Hands out its resources by running them past the handler, as the archiver does. */
    private static final class WalkingArchiver extends NoOpArchiver {
        private final SimpleAggregatingDescriptorHandler handler;
        private final List<PlexusIoResource> resources = new ArrayList<>();
        File aggregate;

        WalkingArchiver(SimpleAggregatingDescriptorHandler handler, PlexusIoResource... resources) {
            this.handler = handler;
            this.resources.addAll(Arrays.asList(resources));
        }

        void walk() {
            for (ResourceIterator it = getResources(); it.hasNext(); ) {
                it.next();
            }
        }

        @Override
        public ResourceIterator getResources() throws ArchiverException {
            return new ResourceIterator() {
                private int index;

                @Override
                public boolean hasNext() {
                    return index < resources.size();
                }

                @Override
                public ArchiveEntry next() {
                    try {
                        handler.isSelected(resources.get(index++));
                    } catch (IOException e) {
                        throw new ArchiverException(e.getMessage(), e);
                    }
                    return null;
                }
            };
        }

        @Override
        public void addFile(File inputFile, String destFileName) {
            aggregate = inputFile;
        }
    }
}
