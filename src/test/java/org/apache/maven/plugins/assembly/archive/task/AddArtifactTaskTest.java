package org.apache.maven.plugins.assembly.archive.task;

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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
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
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class AddArtifactTaskTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private MavenProject mainProject;
    
    private AssemblerConfigurationSource configSource;

    @Before
    public void setUp()
        throws IOException
    {
        Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "main" );
        model.setVersion( "1000" );

        this.mainProject = new MavenProject( model );

        this.configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getFinalName() ).thenReturn( "final-name" );
    }
    
    @After
    public void tearDown()
    {
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
    }

    @Test
    public void testShouldAddArchiveFileWithoutUnpacking()
        throws Exception
    {
        String outputLocation = "artifact";

        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        File artifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( artifactFile );

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0222 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0222 );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        
        when( configSource.getProject() ).thenReturn( mainProject );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, mainProject );

        AddArtifactTask task = createTask( artifact );

        task.execute( archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getProject();

        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).addFile( artifactFile, outputLocation );
    }

    @Test
    public void testShouldAddArchiveFileWithDefaultOutputLocation()
        throws Exception
    {
        String artifactId = "myArtifact";
        String version = "1";
        String ext = "jar";
        String outputDir = "tmp/";

        Artifact artifact = mock( Artifact.class );
        ArtifactHandler artifactHandler = mock( ArtifactHandler.class );
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        when( artifactHandler.getExtension() ).thenReturn( ext );
        when( artifact.getArtifactHandler() ).thenReturn( artifactHandler );
        File artifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( artifactFile );

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0222 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0222 );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );

        when( configSource.getProject() ).thenReturn( mainProject );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource, mainProject );

        AddArtifactTask task = new AddArtifactTask( artifact, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );
        task.setOutputDirectory( outputDir );
        task.setFileNameMapping( new DependencySet().getOutputFileNameMapping() );

        Model model = new Model();
        model.setArtifactId( artifactId );
        model.setVersion( version );

        MavenProject project = new MavenProject( model );
        project.setGroupId( "GROUPID" );
        task.setProject( project );

        task.execute( archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getProject();
        
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).addFile( artifactFile, outputDir + artifactId + "-" + version + "." + ext );
    }

    private AddArtifactTask createTask( Artifact artifact )
    {
        AddArtifactTask task = new AddArtifactTask( artifact, new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        task.setFileNameMapping( "artifact" );

        return task;
    }

    @Test
    public void testShouldAddArchiveFileWithUnpack()
        throws Exception
    {
        final int originalDirMode = -1;
        final int originalFileMode = -1;

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( originalDirMode );
        when( archiver.getOverrideFileMode() ).thenReturn( originalFileMode );
        
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, mainProject );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getFile() ).thenReturn( temporaryFolder.newFile() );
        
        AddArtifactTask task = createTask( artifact );
        task.setUnpack( true );

        task.execute( archiver, configSource );
        
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiver ).addArchivedFileSet( any( ArchivedFileSet.class ), isNull( Charset.class ) );
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
    }

    @Test
    public void testShouldAddArchiveFileWithUnpackAndModes()
        throws Exception
    {
        final int directoryMode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        final int fileMode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        final int originalDirMode = -1;
        final int originalFileMode = -1;
        
        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( originalDirMode );
        when( archiver.getOverrideFileMode() ).thenReturn( originalFileMode );
        
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, mainProject );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getFile() ).thenReturn( temporaryFolder.newFile() );

        AddArtifactTask task = createTask( artifact );
        task.setUnpack( true );
        task.setDirectoryMode( directoryMode );
        task.setFileMode( fileMode );

        task.execute( archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiver ).addArchivedFileSet( any( ArchivedFileSet.class ), isNull( Charset.class ) );
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver ).setDirectoryMode( directoryMode );
        verify( archiver ).setFileMode( fileMode );
        verify( archiver ).setDirectoryMode( originalDirMode );
        verify( archiver ).setFileMode( originalFileMode );
    }

    @Test
    public void testShouldAddArchiveFileWithUnpackIncludesAndExcludes()
        throws Exception
    {
        final int originalDirMode = -1;
        final int originalFileMode = -1;
        
        final Archiver archiver = mock( Archiver.class );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( originalDirMode );
        when( archiver.getOverrideFileMode() ).thenReturn( originalFileMode );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );

        String[] includes = { "**/*.txt" };
        String[] excludes = { "**/README.txt" };

        Artifact artifact = mock( Artifact.class );
        when( artifact.getFile() ).thenReturn( temporaryFolder.newFile() );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource, mainProject );

        AddArtifactTask task = createTask( artifact );
        task.setUnpack( true );
        task.setIncludes( Arrays.asList( includes ) );
        task.setExcludes( Arrays.asList( excludes ) );

        task.execute( archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiver ).addArchivedFileSet( any( ArchivedFileSet.class ), isNull( Charset.class ) );
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
    }
}
