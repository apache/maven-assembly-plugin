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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.diags.TrackingArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssemblyProxyArchiverTest {
    @TempDir
    private File temporaryFolder;

    @Test
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    void addFileSetSkipWhenSourceIsAssemblyWorkDir() throws Exception {
        final File sources = temporaryFolder;

        final File workdir = new File(sources, "workdir");

        final TrackingArchiver tracker = new TrackingArchiver();
        final AssemblyProxyArchiver archiver = new AssemblyProxyArchiver("", tracker, null, null, null, workdir);

        archiver.setForced(true);

        final DefaultFileSet fs = new DefaultFileSet();
        fs.setDirectory(workdir);

        archiver.addFileSet(fs);

        assertTrue(tracker.added.isEmpty());
    }

    @Test
    @Timeout(value = 5000, unit = TimeUnit.MILLISECONDS)
    void addFileSetAddExcludeWhenSourceContainsAssemblyWorkDir() throws Exception {
        final File sources = temporaryFolder;

        final File workdir = new File(sources, "workdir");
        workdir.mkdir();

        Files.write(
                sources.toPath().resolve("test-included.txt"),
                Collections.singletonList("This is included"),
                StandardCharsets.UTF_8);
        Files.write(
                workdir.toPath().resolve("test-excluded.txt"),
                Collections.singletonList("This is excluded"),
                StandardCharsets.UTF_8);

        final TrackingArchiver tracker = new TrackingArchiver();
        final AssemblyProxyArchiver archiver = new AssemblyProxyArchiver("", tracker, null, null, null, workdir);

        archiver.setForced(true);

        final DefaultFileSet fs = new DefaultFileSet();
        fs.setDirectory(sources);

        archiver.addFileSet(fs);

        assertEquals(1, tracker.added.size());

        final TrackingArchiver.Addition addition = tracker.added.get(0);
        assertNotNull(addition.excludes);
        assertEquals(1, addition.excludes.length);
        assertEquals(workdir.getName(), addition.excludes[0]);
    }

    @Test
    void addFileNoPermsCallAcceptFilesOnlyOnce() throws Exception {
        final Archiver delegate = mock(Archiver.class);

        final CounterSelector counter = new CounterSelector(true);
        final List<FileSelector> selectors = new ArrayList<>();
        selectors.add(counter);

        final AssemblyProxyArchiver archiver =
                new AssemblyProxyArchiver("", delegate, null, selectors, null, new File("."));
        archiver.setForced(true);

        final File inputFile = File.createTempFile("junit", null, temporaryFolder);
        archiver.addFile(inputFile, "file.txt");

        assertEquals(1, counter.getCount());
        verify(delegate).addFile(inputFile, "file.txt");
        verify(delegate).setForced(true);
    }

    @Test
    @SuppressWarnings("deprecation")
    void addDirectoryNoPermsCallAcceptFilesOnlyOnce() throws Exception {
        final Archiver delegate = new JarArchiver();

        final File output = File.createTempFile("junit", null, temporaryFolder);

        delegate.setDestFile(output);

        final CounterSelector counter = new CounterSelector(true);
        final List<FileSelector> selectors = new ArrayList<>();
        selectors.add(counter);

        final AssemblyProxyArchiver archiver =
                new AssemblyProxyArchiver("", delegate, null, selectors, null, new File("."));

        archiver.setForced(true);

        final File dir = newFolder(temporaryFolder, "junit");
        Files.write(
                dir.toPath().resolve("file.txt"), Collections.singletonList("This is a test."), StandardCharsets.UTF_8);

        archiver.addDirectory(dir);

        archiver.createArchive();

        assertEquals(1, counter.getCount());
    }

    @Test
    void assemblyWorkDir() {
        final Archiver delegate = mock(Archiver.class);
        final List<FileSelector> selectors = new ArrayList<>();

        final AssemblyProxyArchiver archiver = new AssemblyProxyArchiver(
                "prefix", delegate, null, selectors, null, new File(temporaryFolder, "module1"));

        FileSet fileSet = mock(FileSet.class);
        when(fileSet.getDirectory()).thenReturn(temporaryFolder);
        when(fileSet.getStreamTransformer()).thenReturn(mock(InputStreamTransformer.class));

        archiver.addFileSet(fileSet);

        ArgumentCaptor<FileSet> delFileSet = ArgumentCaptor.forClass(FileSet.class);
        verify(delegate).addFileSet(delFileSet.capture());

        assertEquals(delFileSet.getValue().getDirectory(), fileSet.getDirectory());

        String[] excludes = delFileSet.getValue().getExcludes();
        assertNotNull(excludes);
        assertEquals("module1", excludes[0]);

        assertEquals(delFileSet.getValue().getFileMappers(), fileSet.getFileMappers());
        assertEquals(delFileSet.getValue().getFileSelectors(), fileSet.getFileSelectors());
        String[] includes = delFileSet.getValue().getIncludes();
        assertNotNull(includes);
        assertEquals(0, includes.length);

        assertEquals("prefix/", delFileSet.getValue().getPrefix());
        assertEquals(delFileSet.getValue().getStreamTransformer(), fileSet.getStreamTransformer());
    }

    private static final class CounterSelector implements FileSelector {

        private int count = 0;

        private boolean answer = false;

        CounterSelector(final boolean answer) {
            this.answer = answer;
        }

        int getCount() {
            return count;
        }

        public boolean isSelected(final FileInfo fileInfo) throws IOException {
            if (fileInfo.isFile()) {
                count++;
                System.out.println("Counting file: " + fileInfo.getName() + ". Current count: " + count);
            }

            return answer;
        }
    }

    private static File newFolder(File root, String... subDirs) throws IOException {
        String subFolder = String.join("/", subDirs);
        File result = new File(root, subFolder);
        if (!result.mkdirs()) {
            throw new IOException("Couldn't create folders " + root);
        }
        return result;
    }
}
