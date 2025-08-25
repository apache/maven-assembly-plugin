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
package org.apache.maven.plugins.assembly.archive;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.mojos.AbstractAssemblyMojo;
import org.apache.maven.plugins.assembly.testutils.PojoConfigSource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.diags.NoOpArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAssemblyArchiverTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ArchiverManager archiverManager;

    private PlexusContainer container;

    public static void setupInterpolators(AssemblerConfigurationSource configSource) {
        when(configSource.getRepositoryInterpolator()).thenReturn(FixedStringSearchInterpolator.create());
        when(configSource.getCommandLinePropsInterpolator()).thenReturn(FixedStringSearchInterpolator.create());
        when(configSource.getEnvInterpolator()).thenReturn(FixedStringSearchInterpolator.create());
    }

    public static void setupInterpolators(AssemblerConfigurationSource configSource, MavenProject mavenProject) {
        when(configSource.getCommandLinePropsInterpolator()).thenReturn(FixedStringSearchInterpolator.create());
        when(configSource.getEnvInterpolator()).thenReturn(FixedStringSearchInterpolator.create());
        when(configSource.getMainProjectInterpolator())
                .thenReturn(AbstractAssemblyMojo.mainProjectInterpolator(mavenProject));
    }

    @Before
    public void setup() throws PlexusContainerException {
        this.archiverManager = mock(ArchiverManager.class);
        this.container = new DefaultPlexusContainer();
    }

    @Test(expected = InvalidAssemblerConfigurationException.class)
    public void failWhenAssemblyIdIsNull() throws Exception {
        final DefaultAssemblyArchiver archiver = createSubject(Collections.emptyList());
        archiver.createArchive(new Assembly(), "full-name", "zip", null, null);
    }

    @Test
    public void testCreateArchive() throws Exception {
        Archiver archiver = mock(Archiver.class);

        when(archiverManager.getArchiver("zip")).thenReturn(archiver);

        final AssemblyArchiverPhase phase = mock(AssemblyArchiverPhase.class);

        final File outDir = temporaryFolder.newFolder("out");

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getTemporaryRootDirectory()).thenReturn(new File(temporaryFolder.getRoot(), "temp"));
        when(configSource.getOverrideUid()).thenReturn(0);
        when(configSource.getOverrideUserName()).thenReturn("root");
        when(configSource.getOverrideGid()).thenReturn(0);
        when(configSource.getOverrideGroupName()).thenReturn("root");
        when(configSource.getOutputDirectory()).thenReturn(outDir);
        when(configSource.getFinalName()).thenReturn("finalName");
        when(configSource.getWorkingDirectory()).thenReturn(new File("."));

        final Assembly assembly = new Assembly();
        assembly.setId("id");

        final DefaultAssemblyArchiver subject = createSubject(Collections.singletonList(phase));

        subject.createArchive(assembly, "full-name", "zip", configSource, null);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource).getArchiverConfig();
        verify(configSource).getFinalName();
        verify(configSource).getOutputDirectory();
        verify(configSource, atLeastOnce()).getOverrideUid();
        verify(configSource, atLeastOnce()).getOverrideUserName();
        verify(configSource, atLeastOnce()).getOverrideGid();
        verify(configSource, atLeastOnce()).getOverrideGroupName();
        verify(configSource).getTemporaryRootDirectory();
        verify(configSource).getWorkingDirectory();
        verify(configSource).isDryRun();
        verify(configSource).isIgnoreDirFormatExtensions();
        verify(configSource).isIgnorePermissions();
        verify(configSource).isUpdateOnly();

        verify(phase).execute(eq(assembly), any(Archiver.class), eq(configSource));

        verify(archiver).createArchive();
        verify(archiver).setDestFile(new File(outDir, "full-name.zip"));
        verify(archiver, times(2)).setForced(true);
        verify(archiver).setIgnorePermissions(false);
        verify(archiver).setOverrideUid(0);
        verify(archiver).setOverrideUserName("root");
        verify(archiver).setOverrideGid(0);
        verify(archiver).setOverrideGroupName("root");

        verify(archiverManager).getArchiver("zip");
    }

    @Test
    public void testCreateArchiverShouldConfigureArchiver() throws Exception {
        final TestArchiverWithConfig archiver = new TestArchiverWithConfig();

        when(archiverManager.getArchiver("dummy")).thenReturn(archiver);

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);

        final String simpleConfig = "value";

        when(configSource.getArchiverConfig())
                .thenReturn("<configuration><simpleConfig>" + simpleConfig + "</simpleConfig></configuration>");

        final MavenProject project = new MavenProject(new Model());

        when(configSource.getProject()).thenReturn(project);
        when(configSource.getWorkingDirectory()).thenReturn(new File("."));

        when(configSource.isIgnorePermissions()).thenReturn(true);
        setupInterpolators(configSource);

        when(configSource.getOverrideUid()).thenReturn(0);
        when(configSource.getOverrideUserName()).thenReturn("root");
        when(configSource.getOverrideGid()).thenReturn(0);
        when(configSource.getOverrideGroupName()).thenReturn("root");

        final DefaultAssemblyArchiver subject = createSubject(new ArrayList<>());

        subject.createArchiver("dummy", false, "finalName", configSource, null, null);

        assertEquals(simpleConfig, archiver.getSimpleConfig());

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiverManager).getArchiver("dummy");
    }

    @Test
    public void testCreateArchiverShouldCreateTarArchiverWithNoCompression() throws Exception {
        final TestTarArchiver ttArchiver = new TestTarArchiver();

        when(archiverManager.getArchiver("tar")).thenReturn(ttArchiver);

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getTarLongFileMode()).thenReturn(TarLongFileMode.fail.toString());
        when(configSource.getWorkingDirectory()).thenReturn(new File("."));
        when(configSource.isIgnorePermissions()).thenReturn(true);
        when(configSource.getOverrideUid()).thenReturn(0);
        when(configSource.getOverrideUserName()).thenReturn("root");
        when(configSource.getOverrideGid()).thenReturn(0);
        when(configSource.getOverrideGroupName()).thenReturn("root");

        final DefaultAssemblyArchiver subject = createSubject(new ArrayList<>());

        subject.createArchiver("tar", false, "finalName", configSource, null, null);

        assertNull(ttArchiver.compressionMethod);
        assertEquals(TarLongFileMode.fail, ttArchiver.longFileMode);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource).getArchiverConfig();
        verify(configSource, times(2)).getOverrideGid();
        verify(configSource, times(2)).getOverrideGroupName();
        verify(configSource, times(2)).getOverrideUid();
        verify(configSource, times(2)).getOverrideUserName();
        verify(configSource).getTarLongFileMode();
        verify(configSource).getWorkingDirectory();
        verify(configSource).isDryRun();
        verify(configSource).isIgnorePermissions();
        verify(configSource).isUpdateOnly();

        verify(archiverManager).getArchiver("tar");
    }

    @Test
    public void testCreateArchiverShouldCreateWarArchiverWitExpectWebXmlSetToFalse() throws Exception {
        final TestWarArchiver twArchiver = new TestWarArchiver();

        when(archiverManager.getArchiver("war")).thenReturn(twArchiver);

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getOverrideGid()).thenReturn(0);
        when(configSource.getOverrideGroupName()).thenReturn("root");
        when(configSource.getOverrideUid()).thenReturn(0);
        when(configSource.getOverrideUserName()).thenReturn("root");
        when(configSource.getProject()).thenReturn(new MavenProject(new Model()));
        when(configSource.getWorkingDirectory()).thenReturn(new File("."));
        when(configSource.isIgnorePermissions()).thenReturn(true);
        when(configSource.isRecompressZippedFiles()).thenReturn(false);

        final DefaultAssemblyArchiver subject = createSubject(new ArrayList<>());

        subject.createArchiver("war", false, null, configSource, null, null);

        assertNotNull(twArchiver.expectWebXml);
        assertFalse(twArchiver.expectWebXml);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource).getArchiverConfig();
        verify(configSource).getJarArchiveConfiguration();
        verify(configSource).getMavenSession();
        verify(configSource, times(2)).getOverrideGid();
        verify(configSource, times(2)).getOverrideGroupName();
        verify(configSource, times(2)).getOverrideUid();
        verify(configSource, times(2)).getOverrideUserName();
        verify(configSource).getProject();
        verify(configSource).getWorkingDirectory();
        verify(configSource).isDryRun();
        verify(configSource).isIgnorePermissions();
        verify(configSource).isUpdateOnly();

        verify(archiverManager).getArchiver("war");
    }

    @Test
    public void testCreateArchiverShouldCreateZipArchiver() throws Exception {
        final ZipArchiver archiver = new ZipArchiver();

        when(archiverManager.getArchiver("zip")).thenReturn(archiver);

        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.getOverrideGid()).thenReturn(0);
        when(configSource.getOverrideGroupName()).thenReturn("root");
        when(configSource.getOverrideUid()).thenReturn(0);
        when(configSource.getOverrideUserName()).thenReturn("root");
        when(configSource.getWorkingDirectory()).thenReturn(new File("."));
        when(configSource.isIgnorePermissions()).thenReturn(true);
        when(configSource.isRecompressZippedFiles()).thenReturn(false);

        final DefaultAssemblyArchiver subject = createSubject(new ArrayList<>());

        subject.createArchiver("zip", false, null, configSource, null, null);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(configSource).getArchiverConfig();
        verify(configSource, times(2)).getOverrideGid();
        verify(configSource, times(2)).getOverrideGroupName();
        verify(configSource, times(2)).getOverrideUid();
        verify(configSource, times(2)).getOverrideUserName();
        verify(configSource).getWorkingDirectory();
        verify(configSource).isDryRun();
        verify(configSource).isIgnorePermissions();
        verify(configSource).isUpdateOnly();

        verify(archiverManager).getArchiver("zip");
    }

    @Test
    public void testCreateTarArchiverShouldNotInitializeCompression() throws Exception {
        final TestTarArchiver archiver = new TestTarArchiver();

        when(archiverManager.getArchiver("tar")).thenReturn(archiver);

        final DefaultAssemblyArchiver subject = createSubject(new ArrayList<>());

        PojoConfigSource configSource = new PojoConfigSource();
        configSource.setTarLongFileMode(TarLongFileMode.fail.name());
        configSource.setWorkingDirectory(new File(""));
        configSource.setRecompressZippedFiles(false);

        subject.createArchiver("tar", true, "", configSource, null, null);

        assertNull(new TestTarArchiver().compressionMethod);
        assertEquals(TarLongFileMode.fail, archiver.longFileMode);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiverManager).getArchiver("tar");
    }

    @Test
    public void testCreateTarArchiverInvalidFormatShouldFailWithInvalidCompression() throws Exception {

        when(archiverManager.getArchiver("tar.ZZZ")).thenThrow(new NoSuchArchiverException("no archiver"));

        final DefaultAssemblyArchiver subject = createSubject(new ArrayList<>());

        try {
            subject.createArchiver("tar.ZZZ", true, "", null, null, null);

            fail("Invalid compression formats should throw an error.");
        } catch (final NoSuchArchiverException e) {
            // expected.
        }

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(archiverManager).getArchiver("tar.ZZZ");
    }

    private DefaultAssemblyArchiver createSubject(final List<AssemblyArchiverPhase> phases) {
        return new DefaultAssemblyArchiver(archiverManager, phases, Collections.emptyMap(), container);
    }

    private static final class TestTarArchiver extends TarArchiver {

        TarCompressionMethod compressionMethod;

        TarLongFileMode longFileMode;

        @Override
        protected void execute() throws ArchiverException, IOException {
            super.createArchive();
        }

        @Override
        public void setCompression(final TarCompressionMethod mode) {
            compressionMethod = mode;
            super.setCompression(mode);
        }

        @Override
        public void setLongfile(final TarLongFileMode mode) {
            longFileMode = mode;
            super.setLongfile(mode);
        }
    }

    private static final class TestWarArchiver extends WarArchiver {

        Boolean expectWebXml;

        @Override
        public void setExpectWebXml(boolean expectWebXml) {
            this.expectWebXml = expectWebXml;
            super.setExpectWebXml(expectWebXml);
        }
    }

    public static final class TestArchiverWithConfig extends NoOpArchiver {

        private String simpleConfig;

        public String getSimpleConfig() {
            return simpleConfig;
        }

        public String getDuplicateBehavior() {
            return Archiver.DUPLICATES_ADD;
        }
    }
}
