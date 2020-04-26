package org.apache.maven.plugins.assembly.archive.phase;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.plugins.assembly.repository.RepositoryAssembler;
import org.apache.maven.plugins.assembly.repository.RepositoryBuilderConfigSource;
import org.apache.maven.plugins.assembly.repository.model.RepositoryInfo;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class RepositoryAssemblyPhaseTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private RepositoryAssemblyPhase phase;
    
    private RepositoryAssembler repositoryAssembler;
    
    @Before
    public void setUp()
    {
        this.repositoryAssembler = mock( RepositoryAssembler.class );
        this.phase = new RepositoryAssemblyPhase( repositoryAssembler );
        this.phase.enableLogging( mock( Logger.class ) );
    }
    
    @Test
    public void testExecute_ShouldNotIncludeRepositoryIfNonSpecifiedInAssembly()
        throws Exception
    {
        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getTemporaryRootDirectory() ).thenReturn( temporaryFolder.getRoot() );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        this.phase.execute( assembly, null, configSource );
        
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getTemporaryRootDirectory();
        
        verifyZeroInteractions( repositoryAssembler );
    }

    @Test
    public void testExecute_ShouldIncludeOneRepository()
        throws Exception
    {
        final File tempRoot = temporaryFolder.getRoot();

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getCommandLinePropsInterpolator() ).thenReturn( FixedStringSearchInterpolator.empty() );
        when( configSource.getEnvInterpolator() ).thenReturn( FixedStringSearchInterpolator.empty() );
        when( configSource.getFinalName() ).thenReturn( "final-name" );
        when( configSource.getMainProjectInterpolator() ).thenReturn( FixedStringSearchInterpolator.empty() );
        when( configSource.getProject() ).thenReturn( new MavenProject( new Model() ) );
        when( configSource.getTemporaryRootDirectory() ).thenReturn( tempRoot );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        final Repository repo = new Repository();
        repo.setOutputDirectory( "out" );
        repo.setDirectoryMode( "777" );
        repo.setFileMode( "777" );
        assembly.addRepository( repo );

        final int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        final int defaultDirMode = -1;
        final int defaultFileMode = -1;

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( defaultDirMode );
        when( archiver.getOverrideFileMode() ).thenReturn( defaultFileMode );

        this.phase.execute( assembly, archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getCommandLinePropsInterpolator();
        verify( configSource ).getEnvInterpolator();
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource ).getMainProjectInterpolator();
        verify( configSource ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();
        verify( configSource, atLeastOnce() ).getTemporaryRootDirectory();
        
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver ).setDirectoryMode( mode );
        verify( archiver ).setFileMode( mode );
        verify( archiver ).setDirectoryMode( defaultDirMode );
        verify( archiver ).setFileMode( defaultFileMode );
        verify( archiver ).addFileSet( any( FileSet.class ) );        

        verify( repositoryAssembler ).buildRemoteRepository( any( File.class ), any( RepositoryInfo.class ),
                                                             any( RepositoryBuilderConfigSource.class ) );
    }
}
