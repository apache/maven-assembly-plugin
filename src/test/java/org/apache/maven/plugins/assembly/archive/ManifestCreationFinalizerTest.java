package org.apache.maven.plugins.assembly.archive;

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

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.util.IOUtil;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class ManifestCreationFinalizerTest
{

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testShouldDoNothingWhenArchiveConfigIsNull()
        throws Exception
    {
        new ManifestCreationFinalizer( null, null, null ).finalizeArchiveCreation( null );
    }

    @Test
    public void testShouldDoNothingWhenArchiverIsNotJarArchiver()
        throws Exception
    {
        MavenProject project = new MavenProject( new Model() );
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        new ManifestCreationFinalizer( null, project, config ).finalizeArchiveCreation( null );
    }

    @Test
    public void testShouldAddManifestWhenArchiverIsJarArchiver()
        throws Exception
    {
        MavenProject project = new MavenProject( new Model() );
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        File tempDir = temporaryFolder.getRoot();

        Path manifestFile = tempDir.toPath().resolve("MANIFEST.MF");
        
        Files.write( manifestFile, Arrays.asList( "Main-Class: Stuff\n" ), StandardCharsets.UTF_8 );

        config.setManifestFile( manifestFile.toFile() );

        JarArchiver archiver = new JarArchiver();

        archiver.setArchiveFinalizers(
            Collections.<ArchiveFinalizer>singletonList( new ManifestCreationFinalizer( null, project, config ) ) );

        File file = temporaryFolder.newFile();

        archiver.setDestFile( file );

        archiver.createArchive();

        URL resource = new URL( "jar:file:" + file.getAbsolutePath() + "!/META-INF/MANIFEST.MF" );

        BufferedReader reader = new BufferedReader( new InputStreamReader( resource.openStream() ) );

        StringWriter writer = new StringWriter();

        IOUtil.copy( reader, writer );

        assertTrue( writer.toString().contains( "Main-Class: Stuff" ) );

        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4823678
        ( (JarURLConnection) resource.openConnection() ).getJarFile().close();
    }

    @Test
    public void testShouldAddManifestEntriesWhenArchiverIsJarArchiver()
        throws Exception
    {
        MavenProject project = new MavenProject( new Model() );
        MavenArchiveConfiguration config = new MavenArchiveConfiguration();

        String testKey = "Test-Key";
        String testValue = "test-value";

        config.addManifestEntry( testKey, testValue );

        JarArchiver archiver = new JarArchiver();

        archiver.setArchiveFinalizers(
            Collections.<ArchiveFinalizer>singletonList( new ManifestCreationFinalizer( null, project, config ) ) );

        File file = temporaryFolder.newFile();

        archiver.setDestFile( file );

        archiver.createArchive();

        URL resource = new URL( "jar:file:" + file.getAbsolutePath() + "!/META-INF/MANIFEST.MF" );

        BufferedReader reader = new BufferedReader( new InputStreamReader( resource.openStream() ) );

        StringWriter writer = new StringWriter();

        IOUtil.copy( reader, writer );

        assertTrue( writer.toString().contains( testKey + ": " + testValue ) );

        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4823678
        ( (JarURLConnection) resource.openConnection() ).getJarFile().close();
    }
}
