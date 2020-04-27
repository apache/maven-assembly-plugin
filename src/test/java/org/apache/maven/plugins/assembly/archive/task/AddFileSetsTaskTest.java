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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class AddFileSetsTaskTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testGetFileSetDirectory_ShouldReturnAbsoluteSourceDir()
        throws Exception
    {
        final File dir = temporaryFolder.newFolder();

        final FileSet fs = new FileSet();

        fs.setDirectory( dir.getAbsolutePath() );

        final File result = new AddFileSetsTask( new ArrayList<FileSet>() ).getFileSetDirectory( fs, null, null );

        assertEquals( dir.getAbsolutePath(), result.getAbsolutePath() );
    }

    @Test
    public void testGetFileSetDirectory_ShouldReturnBasedir()
        throws Exception
    {
        final File dir = temporaryFolder.newFolder();

        final FileSet fs = new FileSet();

        final File result = new AddFileSetsTask( new ArrayList<FileSet>() ).getFileSetDirectory( fs, dir, null );

        assertEquals( dir.getAbsolutePath(), result.getAbsolutePath() );
    }

    @Test
    public void testGetFileSetDirectory_ShouldReturnDirFromBasedirAndSourceDir()
        throws Exception
    {
        final File dir = temporaryFolder.newFolder();

        final String srcPath = "source";

        final File srcDir = new File( dir, srcPath );

        final FileSet fs = new FileSet();

        fs.setDirectory( srcPath );

        final File result = new AddFileSetsTask( new ArrayList<FileSet>() ).getFileSetDirectory( fs, dir, null );

        assertEquals( srcDir.getAbsolutePath(), result.getAbsolutePath() );
    }

    @Test
    public void testGetFileSetDirectory_ShouldReturnDirFromArchiveBasedirAndSourceDir()
        throws Exception
    {
        final File dir = temporaryFolder.newFolder();

        final String srcPath = "source";

        final File srcDir = new File( dir, srcPath );

        final FileSet fs = new FileSet();

        fs.setDirectory( srcPath );

        final File result = new AddFileSetsTask( new ArrayList<FileSet>() ).getFileSetDirectory( fs, null, dir );

        assertEquals( srcDir.getAbsolutePath(), result.getAbsolutePath() );
    }

    @Test
    public void testAddFileSet_ShouldAddDirectory()
        throws Exception
    {
        File basedir = temporaryFolder.getRoot();
        
        final FileSet fs = new FileSet();
        fs.setDirectory( temporaryFolder.newFolder( "dir" ).getName() );
        fs.setOutputDirectory( "dir2" );

        // the logger sends a debug message with this info inside the addFileSet(..) method..
        final Archiver archiver = mock( Archiver.class );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( -1 );
        when( archiver.getOverrideFileMode() ).thenReturn( -1 );
        
        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );

        final MavenProject project = new MavenProject( new Model() );
        project.setGroupId( "GROUPID" );
        project.setFile( new File( basedir, "pom.xml" ) );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setProject( project );

        task.addFileSet( fs, archiver, configSource, null );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        
        verify( archiver, times( 2 ) ).getOverrideDirectoryMode();
        verify( archiver, times( 2 ) ).getOverrideFileMode();
        verify( archiver, atLeastOnce() ) .addFileSet( any( org.codehaus.plexus.archiver.FileSet.class ) );
    }

    @Test
    public void testAddFileSet_ShouldAddDirectoryUsingSourceDirNameForDestDir()
        throws Exception
    {
        final FileSet fs = new FileSet();
        final String dirname = "dir";
        fs.setDirectory( dirname );

        final File archiveBaseDir = temporaryFolder.newFolder();

        // ensure this exists, so the directory addition will proceed.
        final File srcDir = new File( archiveBaseDir, dirname );
        srcDir.mkdirs();

        // the logger sends a debug message with this info inside the addFileSet(..) method..
        final Archiver archiver = mock( Archiver.class );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( -1 );
        when( archiver.getOverrideFileMode() ).thenReturn( -1 );
        
        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );

        final MavenProject project = new MavenProject( new Model() );
        project.setGroupId( "GROUPID" );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );
        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setProject( project );

        task.addFileSet( fs, archiver, configSource, archiveBaseDir );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        
        verify( archiver, times( 2 ) ).getOverrideDirectoryMode();
        verify( archiver, times( 2 ) ).getOverrideFileMode();
        verify( archiver ).addFileSet( any( org.codehaus.plexus.archiver.FileSet.class ) );
    }

    @Test
    public void testAddFileSet_ShouldNotAddDirectoryWhenSourceDirNonExistent()
        throws Exception
    {
        final FileSet fs = new FileSet();

        fs.setDirectory( "dir" );
        final File archiveBaseDir = temporaryFolder.newFolder();

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getFinalName() ).thenReturn( "finalName" );

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( -1 );
        when( archiver.getOverrideFileMode() ).thenReturn( -1 );

        final MavenProject project = new MavenProject( new Model() );
        project.setGroupId( "GROUPID" );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        task.setLogger( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        task.setProject( project );

        task.addFileSet( fs, archiver, configSource, archiveBaseDir );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();

        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
    }

    @Test
    public void testExecute_ShouldThrowExceptionIfArchiveBasedirProvidedIsNonExistent()
        throws Exception
    {
        File archiveBaseDir = new File( temporaryFolder.getRoot(), "archive");
        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getArchiveBaseDirectory() ).thenReturn( archiveBaseDir );

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        try
        {
            task.execute( null, configSource );

            fail( "Should throw exception due to non-existent archiveBasedir location that was provided." );
        }
        catch ( final ArchiveCreationException e )
        {
            // should do this, because it cannot use the provide archiveBasedir.
        }

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getArchiveBaseDirectory();
    }

    @Test
    public void testExecute_ShouldThrowExceptionIfArchiveBasedirProvidedIsNotADirectory()
        throws Exception
    {
        File archiveBaseDir = temporaryFolder.newFile();
        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getArchiveBaseDirectory() ).thenReturn( archiveBaseDir );

        final AddFileSetsTask task = new AddFileSetsTask( new ArrayList<FileSet>() );

        try
        {
            task.execute( null, configSource );

            fail( "Should throw exception due to non-directory archiveBasedir location that was provided." );
        }
        catch ( final ArchiveCreationException e )
        {
            // should do this, because it cannot use the provide archiveBasedir.
        }
        
        verify( configSource ).getArchiveBaseDirectory();
    }

}
