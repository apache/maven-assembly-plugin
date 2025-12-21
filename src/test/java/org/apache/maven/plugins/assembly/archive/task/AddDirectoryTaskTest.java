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
package org.apache.maven.plugins.assembly.archive.task;

import java.io.File;
import java.util.Collections;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class AddDirectoryTaskTest {
    @TempDir
    private File temporaryFolder;

    private Archiver archiver;

    @BeforeEach
    void setUp() {
        this.archiver = mock(Archiver.class);
    }

    @Test
    void addDirectoryShouldNotAddDirectoryIfNonExistent() throws Exception {
        final int defaultDirMode = -1;
        final int defaultFileMode = -1;

        when(archiver.getOverrideDirectoryMode()).thenReturn(defaultDirMode);
        when(archiver.getOverrideFileMode()).thenReturn(defaultFileMode);

        AddDirectoryTask task = new AddDirectoryTask(new File(temporaryFolder, "non-existent"));

        task.execute(archiver);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
    }

    @Test
    void addDirectoryShouldAddDirectory() throws Exception {
        final int defaultDirMode = -1;
        final int defaultFileMode = -1;

        when(archiver.getOverrideDirectoryMode()).thenReturn(defaultDirMode);
        when(archiver.getOverrideFileMode()).thenReturn(defaultFileMode);

        AddDirectoryTask task = new AddDirectoryTask(temporaryFolder);
        task.setOutputDirectory("dir");

        task.execute(archiver);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiver).addFileSet(any(FileSet.class));
        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
    }

    @Test
    void addDirectoryShouldAddDirectoryWithDirMode() throws Exception {
        final int dirMode = Integer.parseInt("777", 8);
        final int fileMode = Integer.parseInt("777", 8);
        final int defaultDirMode = -1;
        final int defaultFileMode = -1;

        when(archiver.getOverrideDirectoryMode()).thenReturn(defaultDirMode);
        when(archiver.getOverrideFileMode()).thenReturn(defaultFileMode);

        AddDirectoryTask task = new AddDirectoryTask(temporaryFolder);
        task.setDirectoryMode(dirMode);
        task.setFileMode(fileMode);
        task.setOutputDirectory("dir");

        task.execute(archiver);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiver).addFileSet(any(FileSet.class));
        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
        verify(archiver).setDirectoryMode(dirMode);
        verify(archiver).setFileMode(fileMode);
        verify(archiver).setDirectoryMode(defaultDirMode);
        verify(archiver).setFileMode(defaultFileMode);
    }

    @Test
    void addDirectoryShouldAddDirectoryWithIncludesAndExcludes() throws Exception {
        when(archiver.getOverrideDirectoryMode()).thenReturn(-1);
        when(archiver.getOverrideFileMode()).thenReturn(-1);

        AddDirectoryTask task = new AddDirectoryTask(temporaryFolder);
        task.setIncludes(Collections.singletonList("**/*.txt"));
        task.setExcludes(Collections.singletonList("**/README.txt"));
        task.setOutputDirectory("dir");

        task.execute(archiver);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiver).addFileSet(any(FileSet.class));
        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
    }
}
