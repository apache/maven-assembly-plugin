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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
public class ManifestCreationFinalizerTest {

    @TempDir
    public File temporaryFolder;

    @Test
    public void testShouldDoNothingWhenArchiveConfigIsNull() throws Exception {
        new ManifestCreationFinalizer(null, null, null).finalizeArchiveCreation(null);
    }

    @Test
    public void testShouldDoNothingWhenArchiverIsNotJarArchiver() throws Exception {
        MavenProject project = new MavenProject(new Model());
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        new ManifestCreationFinalizer(null, project, config).finalizeArchiveCreation(null);
    }

    @Test
    public void testShouldAddManifestWhenArchiverIsJarArchiver() throws Exception {
        MavenProject project = new MavenProject(new Model());
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        File tempDir = temporaryFolder;

        Path manifestFile = tempDir.toPath().resolve("MANIFEST.MF");

        Files.write(manifestFile, Collections.singletonList("Main-Class: Stuff\n"), StandardCharsets.UTF_8);

        config.setManifestFile(manifestFile.toFile());

        JarArchiver archiver = new JarArchiver();

        archiver.setArchiveFinalizers(Collections.singletonList(new ManifestCreationFinalizer(null, project, config)));

        File file = File.createTempFile("junit", null, temporaryFolder);

        archiver.setDestFile(file);

        archiver.createArchive();

        URL resource = new URL("jar:file:" + file.getAbsolutePath() + "!/META-INF/MANIFEST.MF");

        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()));

        StringWriter writer = new StringWriter();

        IOUtils.copy(reader, writer);

        assertTrue(writer.toString().contains("Main-Class: Stuff"));

        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4823678
        ((JarURLConnection) resource.openConnection()).getJarFile().close();
    }

    @Test
    public void testShouldAddManifestEntriesWhenArchiverIsJarArchiver() throws Exception {
        MavenProject project = new MavenProject(new Model());
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        String testKey = "Test-Key";
        String testValue = "test-value";

        config.addManifestEntry(testKey, testValue);

        JarArchiver archiver = new JarArchiver();

        archiver.setArchiveFinalizers(Collections.singletonList(new ManifestCreationFinalizer(null, project, config)));

        File file = File.createTempFile("junit", null, temporaryFolder);

        archiver.setDestFile(file);

        archiver.createArchive();

        URL resource = new URL("jar:file:" + file.getAbsolutePath() + "!/META-INF/MANIFEST.MF");

        BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()));

        StringWriter writer = new StringWriter();

        IOUtils.copy(reader, writer);

        assertTrue(writer.toString().contains(testKey + ": " + testValue));

        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4823678
        ((JarURLConnection) resource.openConnection()).getJarFile().close();
    }
}
