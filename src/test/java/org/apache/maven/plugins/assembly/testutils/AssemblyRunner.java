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
package org.apache.maven.plugins.assembly.testutils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiver;
import org.apache.maven.plugins.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugins.assembly.archive.phase.FileItemAssemblyPhase;
import org.apache.maven.plugins.assembly.archive.phase.FileSetAssemblyPhase;
import org.apache.maven.plugins.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugins.assembly.io.DefaultAssemblyReader;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.mojos.AbstractAssemblyMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenReaderFilter;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.build.BuildContext;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Runs an assembly descriptor through the real {@link DefaultAssemblyArchiver} (real archivers, the
 * {@code files} and {@code fileSets} phases) against a project directory, without a mojo or Maven session.
 * Unit-test replacement for ITs that only check what a descriptor puts into the archive.
 */
public final class AssemblyRunner {

    private final File projectDir;

    private final PojoConfigSource configSource = new PojoConfigSource();

    private final MavenProject project;

    private final PlexusContainer container;

    private static PlexusContainer sharedContainer;

    // scanning the class path takes about a second, so the archiver components are looked up once per JVM
    private static synchronized PlexusContainer container() throws Exception {
        if (sharedContainer == null) {
            DefaultContainerConfiguration containerConfiguration = new DefaultContainerConfiguration();
            containerConfiguration.setClassPathScanning(PlexusConstants.SCANNING_INDEX);
            containerConfiguration.setAutoWiring(true);
            sharedContainer = new DefaultPlexusContainer(containerConfiguration);
        }
        return sharedContainer;
    }

    public AssemblyRunner(File projectDir) throws Exception {
        this.projectDir = projectDir;
        Model model = new Model();
        model.setGroupId("test");
        model.setArtifactId("it");
        model.setVersion("1.0-SNAPSHOT");
        Build build = new Build();
        build.setDirectory(new File(projectDir, "target").getAbsolutePath());
        build.setOutputDirectory(new File(projectDir, "target/classes").getAbsolutePath());
        model.setBuild(build);
        project = new MavenProject(model);
        project.setFile(new File(projectDir, "pom.xml"));
        container = container();

        configSource.setMavenProject(project);
        configSource.setBasedir(projectDir);
        configSource.setWorkingDirectory(new File(projectDir, "target/assembly/work"));
        configSource.setOutputDirectory(new File(projectDir, "target"));
        configSource.setTemporaryRootDirectory(new File(projectDir, "target/archiver-tmp"));
        configSource.setFinalName("it-1.0-SNAPSHOT");
        configSource.setEncoding("UTF-8");
        configSource.setTarLongFileMode("warn");
        configSource.setIgnoreDirFormatExtensions(true);
        configSource.setRootInterpolator(FixedStringSearchInterpolator.empty());
        configSource.setEnvInterpolator(FixedStringSearchInterpolator.empty());
        configSource.setEnvironmentInterpolator(FixedStringSearchInterpolator.empty());
        configSource.setMainProjectInterpolator(AbstractAssemblyMojo.mainProjectInterpolator(project));
    }

    /**
     * Wires the real maven-filtering reader filter and a session that exposes the JVM system properties, so that
     * {@code <filtered>true</filtered>} resources are interpolated as they are in a build.
     */
    public AssemblyRunner withFiltering() throws Exception {
        MavenSession session = mock(MavenSession.class);
        when(session.getSystemProperties()).thenReturn(System.getProperties());
        when(session.getUserProperties()).thenReturn(new Properties());
        when(session.getExecutionProperties()).thenReturn(System.getProperties());
        configSource.setMavenSession(session);
        configSource.setMavenReaderFilter(container.lookup(MavenReaderFilter.class));
        return this;
    }

    public PojoConfigSource configSource() {
        return configSource;
    }

    public MavenProject project() {
        return project;
    }

    public File projectDir() {
        return projectDir;
    }

    /**
     * Writes {@code content} to {@code relativePath} below the project directory (UTF-8).
     */
    public File write(String relativePath, String content) throws IOException {
        File f = new File(projectDir, relativePath);
        Files.createDirectories(f.getParentFile().toPath());
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    /**
     * Parses the descriptor the way the plugin does: read, then interpolate.
     */
    public Assembly parse(String descriptorXml) throws Exception {
        return new DefaultAssemblyReader()
                .readAssembly(new StringReader(descriptorXml), "test descriptor", projectDir, configSource);
    }

    /**
     * Assembly descriptor with the given id, a single {@code dir}-style format placeholder and the body inserted
     * after {@code includeBaseDirectory}.
     */
    public static String descriptor(String id, boolean includeBaseDirectory, String... body) {
        StringBuilder sb = new StringBuilder();
        sb.append("<assembly><id>").append(id).append("</id>");
        sb.append("<formats><format>dir</format></formats>");
        sb.append("<includeBaseDirectory>").append(includeBaseDirectory).append("</includeBaseDirectory>");
        for (String b : body) {
            sb.append(b);
        }
        return sb.append("</assembly>").toString();
    }

    /**
     * Builds the assembly in the given format and returns the produced archive (or directory).
     */
    public File build(String descriptorXml, String format) throws Exception {
        Assembly assembly = parse(descriptorXml);
        ArchiverManager archiverManager = container.lookup(ArchiverManager.class);
        List<AssemblyArchiverPhase> phases = Arrays.asList(new FileItemAssemblyPhase(), new FileSetAssemblyPhase());
        DefaultAssemblyArchiver archiver = new DefaultAssemblyArchiver(
                archiverManager,
                phases,
                container.lookupMap(ContainerDescriptorHandler.class),
                container,
                new BasicComponentConfigurator(),
                mock(BuildContext.class));
        String fullName = configSource.getFinalName() + "-" + assembly.getId();
        return archiver.createArchive(assembly, fullName, format, configSource, null);
    }

    /**
     * Relative, '/'-separated paths of everything below {@code root} (directories carry a trailing '/'), sorted.
     */
    public static Set<String> listing(File root) throws IOException {
        try (Stream<java.nio.file.Path> walk = Files.walk(root.toPath())) {
            return walk.filter(p -> !p.equals(root.toPath()))
                    .map(p -> root.toPath().relativize(p).toString().replace(File.separatorChar, '/')
                            + (Files.isDirectory(p, java.nio.file.LinkOption.NOFOLLOW_LINKS) ? "/" : ""))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }
}
