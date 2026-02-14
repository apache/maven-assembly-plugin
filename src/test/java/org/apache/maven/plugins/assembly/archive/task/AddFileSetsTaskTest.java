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
import java.io.IOException;
import java.util.ArrayList;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddFileSetsTaskTest {
    @TempDir
    private File temporaryFolder;

    @Test
    void getFileSetDirectoryShouldReturnAbsoluteSourceDir() throws Exception {
        final File dir = newFolder(temporaryFolder, "junit");

        final FileSet fs = new FileSet();

        fs.setDirectory(dir.getAbsolutePath());

        final File result = new AddFileSetsTask(new ArrayList<>()).getFileSetDirectory(fs, null, null);

        assertEquals(dir.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    void getFileSetDirectoryShouldReturnBasedir() throws Exception {
        final File dir = newFolder(temporaryFolder, "junit");

        final FileSet fs = new FileSet();

        final File result = new AddFileSetsTask(new ArrayList<>()).getFileSetDirectory(fs, dir, null);

        assertEquals(dir.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    void getFileSetDirectoryShouldReturnDirFromBasedirAndSourceDir() throws Exception {
        final File dir = newFolder(temporaryFolder, "junit");

        final String srcPath = "source";

        final File srcDir = new File(dir, srcPath);

        final FileSet fs = new FileSet();

        fs.setDirectory(srcPath);

        final File result = new AddFileSetsTask(new ArrayList<>()).getFileSetDirectory(fs, dir, null);

        assertEquals(srcDir.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    void getFileSetDirectoryShouldReturnDirFromArchiveBasedirAndSourceDir() throws Exception {
        final File dir = newFolder(temporaryFolder, "junit");

        final String srcPath = "source";

        final File srcDir = new File(dir, srcPath);

        final FileSet fs = new FileSet();

        fs.setDirectory(srcPath);

        final File result = new AddFileSetsTask(new ArrayList<>()).getFileSetDirectory(fs, null, dir);

        assertEquals(srcDir.getAbsolutePath(), result.getAbsolutePath());
    }

    @Test
    void addFileSetShouldAddDirectory() throws Exception {
        File basedir = temporaryFolder;

        final FileSet fs = new FileSet();
        fs.setDirectory(newFolder(temporaryFolder, "dir").getName());
        fs.setOutputDirectory("dir2");

        // the logger sends a debug message with this info inside the addFileSet(..) method..
        final Archiver archiver = mock(Archiver.class);
        when(archiver.getOverrideDirectoryMode()).thenReturn(-1);
        when(archiver.getOverrideFileMode()).thenReturn(-1);

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);

        final MavenProject project = new MavenProject(new Model());
        project.setGroupId("GROUPID");
        project.setFile(new File(basedir, "pom.xml"));

        DefaultAssemblyArchiverTest.setupInterpolators(configSource, project);

        final AddFileSetsTask task = new AddFileSetsTask(new ArrayList<>());

        task.setProject(project);

        task.addFileSet(fs, archiver, configSource, null);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource, atLeastOnce()).getFinalName();
        verify(configSource, atLeastOnce()).getMavenSession();

        verify(archiver, times(2)).getOverrideDirectoryMode();
        verify(archiver, times(2)).getOverrideFileMode();
        verify(archiver, atLeastOnce()).addFileSet(any(org.codehaus.plexus.archiver.FileSet.class));
    }

    @Test
    void addFileSetShouldAddDirectoryUsingSourceDirNameForDestDir() throws Exception {
        final FileSet fs = new FileSet();
        final String dirname = "dir";
        fs.setDirectory(dirname);

        final File archiveBaseDir = newFolder(temporaryFolder, "junit");

        // ensure this exists, so the directory addition will proceed.
        final File srcDir = new File(archiveBaseDir, dirname);
        srcDir.mkdirs();

        // the logger sends a debug message with this info inside the addFileSet(..) method..
        final Archiver archiver = mock(Archiver.class);
        when(archiver.getOverrideDirectoryMode()).thenReturn(-1);
        when(archiver.getOverrideFileMode()).thenReturn(-1);

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);

        final MavenProject project = new MavenProject(new Model());
        project.setGroupId("GROUPID");
        DefaultAssemblyArchiverTest.setupInterpolators(configSource, project);

        final AddFileSetsTask task = new AddFileSetsTask(new ArrayList<>());
        task.setProject(project);

        task.addFileSet(fs, archiver, configSource, archiveBaseDir);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource, atLeastOnce()).getFinalName();
        verify(configSource, atLeastOnce()).getMavenSession();

        verify(archiver, times(2)).getOverrideDirectoryMode();
        verify(archiver, times(2)).getOverrideFileMode();
        verify(archiver).addFileSet(any(org.codehaus.plexus.archiver.FileSet.class));
    }

    @Test
    void addFileSetShouldNotAddDirectoryWhenSourceDirNonExistent() throws Exception {
        final FileSet fs = new FileSet();

        fs.setDirectory("dir");
        final File archiveBaseDir = newFolder(temporaryFolder, "junit");

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getFinalName()).thenReturn("finalName");

        final Archiver archiver = mock(Archiver.class);
        when(archiver.getOverrideDirectoryMode()).thenReturn(-1);
        when(archiver.getOverrideFileMode()).thenReturn(-1);

        final MavenProject project = new MavenProject(new Model());
        project.setGroupId("GROUPID");

        DefaultAssemblyArchiverTest.setupInterpolators(configSource, project);

        final AddFileSetsTask task = new AddFileSetsTask(new ArrayList<>());
        task.setProject(project);

        task.addFileSet(fs, archiver, configSource, archiveBaseDir);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource, atLeastOnce()).getFinalName();
        verify(configSource, atLeastOnce()).getMavenSession();

        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
    }

    @Test
    void executeShouldThrowExceptionIfArchiveBasedirProvidedIsNonExistent() throws Exception {
        File archiveBaseDir = new File(temporaryFolder, "archive");
        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getArchiveBaseDirectory()).thenReturn(archiveBaseDir);

        final AddFileSetsTask task = new AddFileSetsTask(new ArrayList<>());

        try {
            task.execute(null, configSource);

            fail("Should throw exception due to non-existent archiveBasedir location that was provided.");
        } catch (final ArchiveCreationException e) {
            // should do this, because it cannot use the provide archiveBasedir.
        }

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource).getArchiveBaseDirectory();
    }

    @Test
    void executeShouldThrowExceptionIfArchiveBasedirProvidedIsNotADirectory() throws Exception {
        File archiveBaseDir = File.createTempFile("junit", null, temporaryFolder);
        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getArchiveBaseDirectory()).thenReturn(archiveBaseDir);

        final AddFileSetsTask task = new AddFileSetsTask(new ArrayList<>());

        try {
            task.execute(null, configSource);

            fail("Should throw exception due to non-directory archiveBasedir location that was provided.");
        } catch (final ArchiveCreationException e) {
            // should do this, because it cannot use the provide archiveBasedir.
        }

        verify(configSource).getArchiveBaseDirectory();
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
