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
package org.apache.maven.plugins.assembly.archive.phase;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class FileSetAssemblyPhaseTest {
    private FileSetAssemblyPhase phase;

    @BeforeEach
    public void setUp() {
        this.phase = new FileSetAssemblyPhase();
    }

    @Test
    public void testShouldNotFailWhenNoFileSetsSpecified() throws Exception {
        final Assembly assembly = new Assembly();
        assembly.setId("test");

        this.phase.execute(assembly, null, null);
    }

    @Test
    public void testShouldAddOneFileSet() throws Exception {
        final Assembly assembly = new Assembly();

        assembly.setId("test");
        assembly.setIncludeBaseDirectory(false);

        final FileSet fs = new FileSet();
        fs.setOutputDirectory("/out");
        fs.setDirectory("/input");
        fs.setFileMode("777");
        fs.setDirectoryMode("777");

        assembly.addFileSet(fs);

        final MavenProject project = new MavenProject(new Model());
        project.setGroupId("GROUPID");

        final int dirMode = Integer.parseInt("777", 8);
        final int fileMode = Integer.parseInt("777", 8);

        final int[] modes = {-1, -1, dirMode, fileMode};

        // the logger sends a debug message with this info inside the addFileSet(..) method..
        final Archiver archiver = mock(Archiver.class);
        when(archiver.getOverrideDirectoryMode()).thenReturn(modes[0]);
        when(archiver.getOverrideFileMode()).thenReturn(modes[1]);

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getProject()).thenReturn(project);
        when(configSource.getFinalName()).thenReturn("final-name");

        DefaultAssemblyArchiverTest.setupInterpolators(configSource, project);

        this.phase.execute(assembly, archiver, configSource);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource).getArchiveBaseDirectory();
        verify(configSource, atLeastOnce()).getFinalName();
        verify(configSource, atLeastOnce()).getMavenSession();
        verify(configSource, atLeastOnce()).getProject();

        verify(archiver).getOverrideDirectoryMode();
        verify(archiver).getOverrideFileMode();
    }
}
