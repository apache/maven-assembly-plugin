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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class FileItemAssemblyPhaseTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @TempDir
    public File temporaryFolder;

    @Test
    public void testExecuteShouldAddNothingWhenNoFileItemsArePresent() throws Exception {
        final AssemblerConfigurationSource macCS = mock(AssemblerConfigurationSource.class);

        final File basedir = temporaryFolder;

        when(macCS.getBasedir()).thenReturn(basedir);

        final Assembly assembly = new Assembly();
        assembly.setId("test");

        new FileItemAssemblyPhase().execute(assembly, null, macCS);

        verify(macCS).getBasedir();
    }

    @Test
    public void testExecuteShouldAddAbsoluteFileNoFilterNoLineEndingConversion() throws Exception {
        final AssemblerConfigurationSource macCS = mock(AssemblerConfigurationSource.class);

        final File basedir = temporaryFolder;

        final File file = newFile(temporaryFolder, "file.txt");
        Files.write(file.toPath(), Collections.singletonList("This is a test file."), StandardCharsets.UTF_8);

        when(macCS.getBasedir()).thenReturn(basedir);
        when(macCS.getProject()).thenReturn(new MavenProject(new Model()));
        when(macCS.getFinalName()).thenReturn("final-name");
        prepareInterpolators(macCS);

        final Archiver macArchiver = mock(Archiver.class);

        final Assembly assembly = new Assembly();
        assembly.setId("test");

        final FileItem fi = new FileItem();
        fi.setSource(file.getAbsolutePath());
        fi.setFiltered(false);
        fi.setLineEnding("keep");
        fi.setFileMode("777");

        assembly.addFile(fi);

        new FileItemAssemblyPhase().execute(assembly, macArchiver, macCS);

        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class), eq("file.txt"), eq(TypeConversionUtils.modeToInt("777", logger)));
    }

    @Test
    public void testExecuteShouldAddRelativeFileNoFilterNoLineEndingConversion() throws Exception {
        final AssemblerConfigurationSource macCS = mock(AssemblerConfigurationSource.class);

        final File basedir = temporaryFolder;

        final File file = newFile(temporaryFolder, "file.txt");
        Files.write(file.toPath(), Collections.singletonList("This is a test file."), StandardCharsets.UTF_8);

        when(macCS.getBasedir()).thenReturn(basedir);
        when(macCS.getProject()).thenReturn(new MavenProject(new Model()));
        when(macCS.getFinalName()).thenReturn("final-name");
        prepareInterpolators(macCS);

        final Archiver macArchiver = mock(Archiver.class);

        final Assembly assembly = new Assembly();
        assembly.setId("test");

        final FileItem fi = new FileItem();
        fi.setSource("file.txt");
        fi.setFiltered(false);
        fi.setLineEnding("keep");
        fi.setFileMode("777");

        assembly.addFile(fi);

        new FileItemAssemblyPhase().execute(assembly, macArchiver, macCS);

        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class), eq("file.txt"), eq(TypeConversionUtils.modeToInt("777", logger)));
    }

    @Test
    public void testExecuteWithOutputDirectory() throws Exception {
        final AssemblerConfigurationSource macCS = mock(AssemblerConfigurationSource.class);

        final File basedir = temporaryFolder;

        final File readmeFile = newFile(temporaryFolder, "README.txt");
        Files.write(
                readmeFile.toPath(),
                Collections.singletonList("This is a test file for README.txt."),
                StandardCharsets.UTF_8);

        final File licenseFile = newFile(temporaryFolder, "LICENSE.txt");
        Files.write(
                licenseFile.toPath(),
                Collections.singletonList("This is a test file for LICENSE.txt."),
                StandardCharsets.UTF_8);

        final File configFile = new File(newFolder(temporaryFolder, "config"), "config.txt");
        Files.write(
                configFile.toPath(),
                Collections.singletonList("This is a test file for config/config.txt"),
                StandardCharsets.UTF_8);

        when(macCS.getBasedir()).thenReturn(basedir);
        when(macCS.getProject()).thenReturn(new MavenProject(new Model()));
        when(macCS.getFinalName()).thenReturn("final-name");
        prepareInterpolators(macCS);

        final Archiver macArchiver = mock(Archiver.class);

        final Assembly assembly = new Assembly();
        assembly.setId("test");
        assembly.setIncludeBaseDirectory(true);

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource("README.txt");
        readmeFileItem.setOutputDirectory("");
        readmeFileItem.setFiltered(false);
        readmeFileItem.setLineEnding("keep");
        readmeFileItem.setFileMode("777");

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource("LICENSE.txt");
        licenseFileItem.setOutputDirectory("/");
        licenseFileItem.setFiltered(false);
        licenseFileItem.setLineEnding("keep");
        licenseFileItem.setFileMode("777");

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource("config/config.txt");
        configFileItem.setOutputDirectory("config");
        configFileItem.setFiltered(false);
        configFileItem.setLineEnding("keep");
        configFileItem.setFileMode("777");

        assembly.addFile(readmeFileItem);
        assembly.addFile(licenseFileItem);
        assembly.addFile(configFileItem);

        new FileItemAssemblyPhase().execute(assembly, macArchiver, macCS);

        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("README.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("LICENSE.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("config/config.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
    }

    @Test
    public void testExecuteWithOutputDirectoryAndDestName() throws Exception {
        final AssemblerConfigurationSource macCS = mock(AssemblerConfigurationSource.class);

        final File basedir = temporaryFolder;

        final File readmeFile = newFile(temporaryFolder, "README.txt");
        Files.write(
                readmeFile.toPath(),
                Collections.singletonList("This is a test file for README.txt."),
                StandardCharsets.UTF_8);

        final File licenseFile = newFile(temporaryFolder, "LICENSE.txt");
        Files.write(
                licenseFile.toPath(),
                Collections.singletonList("This is a test file for LICENSE.txt."),
                StandardCharsets.UTF_8);

        final File configFile = new File(newFolder(temporaryFolder, "config"), "config.txt");
        Files.write(
                configFile.toPath(),
                Collections.singletonList("This is a test file for config/config.txt"),
                StandardCharsets.UTF_8);

        when(macCS.getBasedir()).thenReturn(basedir);
        when(macCS.getProject()).thenReturn(new MavenProject(new Model()));
        when(macCS.getFinalName()).thenReturn("final-name");
        prepareInterpolators(macCS);

        final Archiver macArchiver = mock(Archiver.class);

        final Assembly assembly = new Assembly();
        assembly.setId("test");
        assembly.setIncludeBaseDirectory(true);

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource("README.txt");
        readmeFileItem.setOutputDirectory("");
        readmeFileItem.setDestName("README_renamed.txt");
        readmeFileItem.setFiltered(false);
        readmeFileItem.setLineEnding("keep");
        readmeFileItem.setFileMode("777");

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource("LICENSE.txt");
        licenseFileItem.setOutputDirectory("/");
        licenseFileItem.setDestName("LICENSE_renamed.txt");
        licenseFileItem.setFiltered(false);
        licenseFileItem.setLineEnding("keep");
        licenseFileItem.setFileMode("777");

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource("config/config.txt");
        configFileItem.setDestName("config_renamed.txt");
        configFileItem.setOutputDirectory("config");
        configFileItem.setFiltered(false);
        configFileItem.setLineEnding("keep");
        configFileItem.setFileMode("777");

        assembly.addFile(readmeFileItem);
        assembly.addFile(licenseFileItem);
        assembly.addFile(configFileItem);

        new FileItemAssemblyPhase().execute(assembly, macArchiver, macCS);

        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("README_renamed.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("LICENSE_renamed.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("config/config_renamed.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
    }

    @Test
    public void testExecuteWithOutputDirectoryAndDestNameAndIncludeBaseDirectoryFalse() throws Exception {
        final AssemblerConfigurationSource macCS = mock(AssemblerConfigurationSource.class);

        final File basedir = temporaryFolder;

        final File readmeFile = newFile(temporaryFolder, "README.txt");
        Files.write(
                readmeFile.toPath(),
                Collections.singletonList("This is a test file for README.txt."),
                StandardCharsets.UTF_8);

        final File licenseFile = newFile(temporaryFolder, "LICENSE.txt");
        Files.write(
                licenseFile.toPath(),
                Collections.singletonList("This is a test file for LICENSE.txt."),
                StandardCharsets.UTF_8);

        final File configFile = new File(newFolder(temporaryFolder, "config"), "config.txt");
        Files.write(
                configFile.toPath(),
                Collections.singletonList("This is a test file for config/config.txt"),
                StandardCharsets.UTF_8);

        when(macCS.getBasedir()).thenReturn(basedir);
        when(macCS.getProject()).thenReturn(new MavenProject(new Model()));
        when(macCS.getFinalName()).thenReturn("final-name");
        prepareInterpolators(macCS);

        final Archiver macArchiver = mock(Archiver.class);

        final Assembly assembly = new Assembly();
        assembly.setId("test");
        assembly.setIncludeBaseDirectory(false);

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource("README.txt");
        readmeFileItem.setDestName("README_renamed.txt");
        readmeFileItem.setFiltered(false);
        readmeFileItem.setLineEnding("keep");
        readmeFileItem.setFileMode("777");

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource("LICENSE.txt");
        licenseFileItem.setDestName("LICENSE_renamed.txt");
        licenseFileItem.setFiltered(false);
        licenseFileItem.setLineEnding("keep");
        licenseFileItem.setFileMode("777");

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource("config/config.txt");
        configFileItem.setDestName("config_renamed.txt");
        configFileItem.setOutputDirectory("config");
        configFileItem.setFiltered(false);
        configFileItem.setLineEnding("keep");
        configFileItem.setFileMode("777");

        assembly.addFile(readmeFileItem);
        assembly.addFile(licenseFileItem);
        assembly.addFile(configFileItem);

        new FileItemAssemblyPhase().execute(assembly, macArchiver, macCS);

        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("README_renamed.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("LICENSE_renamed.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
        verify(macArchiver)
                .addResource(
                        any(PlexusIoResource.class),
                        eq("config/config_renamed.txt"),
                        eq(TypeConversionUtils.modeToInt("777", logger)));
    }

    private void prepareInterpolators(AssemblerConfigurationSource configSource) {
        when(configSource.getCommandLinePropsInterpolator()).thenReturn(FixedStringSearchInterpolator.empty());
        when(configSource.getEnvInterpolator()).thenReturn(FixedStringSearchInterpolator.empty());
        when(configSource.getMainProjectInterpolator()).thenReturn(FixedStringSearchInterpolator.empty());
    }

    private static File newFile(File parent, String child) throws IOException {
        File result = new File(parent, child);
        result.createNewFile();
        return result;
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
