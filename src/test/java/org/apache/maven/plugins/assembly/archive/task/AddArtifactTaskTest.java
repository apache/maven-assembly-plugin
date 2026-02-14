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
import java.util.Arrays;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddArtifactTaskTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @TempDir
    private File temporaryFolder;

    private MavenProject mainProject;

    private AssemblerConfigurationSource configSource;

    @BeforeEach
    void setUp() throws IOException {
        Model model = new Model();
        model.setGroupId("group");
        model.setArtifactId("main");
        model.setVersion("1000");

        this.mainProject = new MavenProject(model);

        this.configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getFinalName()).thenReturn("final-name");
    }

    @AfterEach
    void tearDown() {
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource, atLeastOnce()).getFinalName();
        verify(configSource, atLeastOnce()).getMavenSession();
    }

    @Test
    void shouldAddArchiveFileWithoutUnpacking() throws Exception {
        String outputLocation = "artifact";

        Artifact artifact = mock(Artifact.class);
        when(artifact.getGroupId()).thenReturn("GROUPID");
        File artifactFile = File.createTempFile("junit", null, temporaryFolder);
        when(artifact.getFile()).thenReturn(artifactFile);

        final Archiver archiver = mock(Archiver.class);
        when(archiver.getOverrideDirectoryMode()).thenReturn(0222);
        when(archiver.getOverrideFileMode()).thenReturn(0222);
        when(archiver.getDestFile()).thenReturn(new File("junk"));

        when(configSource.getProject()).thenReturn(mainProject);
        DefaultAssemblyArchiverTest.setupInterpolators(configSource, mainProject);

        AddArtifactTask task = createTask(artifact);

        task.execute(archiver, configSource);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource, atLeastOnce()).getProject();

        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
        verify(archiver, atLeastOnce()).getDestFile();
        verify(archiver).addFile(artifactFile, outputLocation);
    }

    @Test
    void shouldAddArchiveFileWithDefaultOutputLocation() throws Exception {
        String artifactId = "myArtifact";
        String version = "1";
        String ext = "jar";
        String outputDir = "tmp/";

        Artifact artifact = mock(Artifact.class);
        ArtifactHandler artifactHandler = mock(ArtifactHandler.class);
        when(artifact.getGroupId()).thenReturn("GROUPID");
        when(artifactHandler.getExtension()).thenReturn(ext);
        when(artifact.getArtifactHandler()).thenReturn(artifactHandler);
        File artifactFile = File.createTempFile("junit", null, temporaryFolder);
        when(artifact.getFile()).thenReturn(artifactFile);

        final Archiver archiver = mock(Archiver.class);
        when(archiver.getOverrideDirectoryMode()).thenReturn(0222);
        when(archiver.getOverrideFileMode()).thenReturn(0222);
        when(archiver.getDestFile()).thenReturn(new File("junk"));

        when(configSource.getProject()).thenReturn(mainProject);

        DefaultAssemblyArchiverTest.setupInterpolators(configSource, mainProject);

        AddArtifactTask task = new AddArtifactTask(artifact, null);
        task.setOutputDirectory(outputDir);
        task.setFileNameMapping(new DependencySet().getOutputFileNameMapping());

        Model model = new Model();
        model.setArtifactId(artifactId);
        model.setVersion(version);

        MavenProject project = new MavenProject(model);
        project.setGroupId("GROUPID");
        task.setProject(project);

        task.execute(archiver, configSource);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource, atLeastOnce()).getProject();

        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
        verify(archiver, atLeastOnce()).getDestFile();
        verify(archiver).addFile(artifactFile, outputDir + artifactId + "-" + version + "." + ext);
    }

    private AddArtifactTask createTask(Artifact artifact) {
        AddArtifactTask task = new AddArtifactTask(artifact, null);

        task.setFileNameMapping("artifact");

        return task;
    }

    @Test
    void shouldAddArchiveFileWithUnpack() throws Exception {
        final int originalDirMode = -1;
        final int originalFileMode = -1;

        final Archiver archiver = mock(Archiver.class);
        when(archiver.getDestFile()).thenReturn(new File("junk"));
        when(archiver.getOverrideDirectoryMode()).thenReturn(originalDirMode);
        when(archiver.getOverrideFileMode()).thenReturn(originalFileMode);

        DefaultAssemblyArchiverTest.setupInterpolators(configSource, mainProject);

        Artifact artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(File.createTempFile("junit", null, temporaryFolder));

        AddArtifactTask task = createTask(artifact);
        task.setUnpack(true);

        task.execute(archiver, configSource);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiver).addArchivedFileSet(any(ArchivedFileSet.class), isNull());
        verify(archiver, atLeastOnce()).getDestFile();
        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
    }

    @Test
    void shouldAddArchiveFileWithUnpackAndModes() throws Exception {
        final int directoryMode = TypeConversionUtils.modeToInt("777", logger);
        final int fileMode = TypeConversionUtils.modeToInt("777", logger);
        final int originalDirMode = -1;
        final int originalFileMode = -1;

        final Archiver archiver = mock(Archiver.class);
        when(archiver.getDestFile()).thenReturn(new File("junk"));
        when(archiver.getOverrideDirectoryMode()).thenReturn(originalDirMode);
        when(archiver.getOverrideFileMode()).thenReturn(originalFileMode);

        DefaultAssemblyArchiverTest.setupInterpolators(configSource, mainProject);

        Artifact artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(File.createTempFile("junit", null, temporaryFolder));

        AddArtifactTask task = createTask(artifact);
        task.setUnpack(true);
        task.setDirectoryMode(directoryMode);
        task.setFileMode(fileMode);

        task.execute(archiver, configSource);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiver).addArchivedFileSet(any(ArchivedFileSet.class), isNull());
        verify(archiver, atLeastOnce()).getDestFile();
        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
        verify(archiver).setDirectoryMode(directoryMode);
        verify(archiver).setFileMode(fileMode);
        verify(archiver).setDirectoryMode(originalDirMode);
        verify(archiver).setFileMode(originalFileMode);
    }

    @Test
    void shouldAddArchiveFileWithUnpackIncludesAndExcludes() throws Exception {
        final int originalDirMode = -1;
        final int originalFileMode = -1;

        final Archiver archiver = mock(Archiver.class);
        when(archiver.getOverrideDirectoryMode()).thenReturn(originalDirMode);
        when(archiver.getOverrideFileMode()).thenReturn(originalFileMode);
        when(archiver.getDestFile()).thenReturn(new File("junk"));

        String[] includes = {"**/*.txt"};
        String[] excludes = {"**/README.txt"};

        Artifact artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(File.createTempFile("junit", null, temporaryFolder));

        DefaultAssemblyArchiverTest.setupInterpolators(configSource, mainProject);

        AddArtifactTask task = createTask(artifact);
        task.setUnpack(true);
        task.setIncludes(Arrays.asList(includes));
        task.setExcludes(Arrays.asList(excludes));

        task.execute(archiver, configSource);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiver).addArchivedFileSet(any(ArchivedFileSet.class), isNull());
        verify(archiver, atLeastOnce()).getDestFile();
        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
    }
}
