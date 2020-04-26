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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@RunWith( MockitoJUnitRunner.class )
public class AddDependencySetsTaskTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testAddDependencySet_ShouldInterpolateDefaultOutputFileNameMapping()
        throws Exception
    {
        final String outDir = "tmp/";
        final String mainAid = "main";
        final String mainGid = "org.maingrp";
        final String mainVer = "9";
        final String depAid = "dep";
        final String depGid = "org.depgrp";
        final String depVer = "1";
        final String depExt = "war";

        final DependencySet ds = new DependencySet();
        ds.setOutputDirectory( outDir );
        ds.setDirectoryMode( Integer.toString( 10, 8 ) );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        final Model mainModel = new Model();
        mainModel.setArtifactId( mainAid );
        mainModel.setGroupId( mainGid );
        mainModel.setVersion( mainVer );

        final MavenProject mainProject = new MavenProject( mainModel );
        
        Artifact mainArtifact = mock( Artifact.class );
        mainProject.setArtifact( mainArtifact );

        final Model depModel = new Model();
        depModel.setArtifactId( depAid );
        depModel.setGroupId( depGid );
        depModel.setVersion( depVer );
        depModel.setPackaging( depExt );

        final MavenProject depProject = new MavenProject( depModel );

        Artifact depArtifact = mock( Artifact.class );
        ArtifactHandler artifactHandler = mock( ArtifactHandler.class );
        when( artifactHandler.getExtension() ).thenReturn( depExt );
        when( depArtifact.getArtifactHandler() ).thenReturn( artifactHandler );
        final File newFile = temporaryFolder.newFile();
        when( depArtifact.getFile() ).thenReturn( newFile );

        depProject.setArtifact( depArtifact );

        ProjectBuildingResult pbr = mock( ProjectBuildingResult.class );
        when( pbr.getProject() ).thenReturn( depProject );
        
        final ProjectBuilder projectBuilder = mock( ProjectBuilder.class );
        when( projectBuilder.build( any( Artifact.class ), any( ProjectBuildingRequest.class ) ) ).thenReturn( pbr );

        final MavenSession session = mock( MavenSession.class );
        when( session.getProjectBuildingRequest() ).thenReturn( mock( ProjectBuildingRequest.class ) );
        when( session.getExecutionProperties() ).thenReturn( new Properties() );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getFinalName() ).thenReturn( mainAid + "-" + mainVer );
        when( configSource.getProject() ).thenReturn( mainProject );
        when( configSource.getMavenSession() ).thenReturn( session );

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0222 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0222 );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource, mainProject );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), Collections.singleton( depArtifact ),
                                       depProject, projectBuilder, logger );

        task.addDependencySet( ds, archiver, configSource );
        
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();
        
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).addFile( newFile, outDir + depAid + "-" + depVer + "." + depExt, 10 );
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver ).setDirectoryMode( 10 );
        verify( archiver ).setDirectoryMode( 146 );
        verify( archiver ).setFileMode( 10 );
        verify( archiver ).setFileMode( 146 );

        verify( session ).getProjectBuildingRequest();
        verify( session, times( 2 ) ).getExecutionProperties();
        
        verify( projectBuilder ).build( any( Artifact.class ), any( ProjectBuildingRequest.class ) );
    }

    @Test
    public void testAddDependencySet_ShouldNotAddDependenciesWhenProjectHasNone()
        throws Exception
    {
        final MavenProject project = new MavenProject( new Model() );

        final DependencySet ds = new DependencySet();
        ds.setOutputDirectory( "/out" );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), null, project, null, logger );

        task.addDependencySet( ds, null, null );
    }

    // TODO: Find a better way of testing the project-stubbing behavior when a ProjectBuildingException takes place.
    @Test
    public void testAddDependencySet_ShouldNotAddDependenciesWhenProjectIsStubbed()
        throws Exception
    {
        final MavenProject project = new MavenProject( new Model() );

        final ProjectBuildingException pbe = new ProjectBuildingException( "test", "Test error.", new Throwable() );

        final String aid = "test-dep";
        final String version = "2.0-SNAPSHOT";
        final String type = "jar";

        final File file = new File( "dep-artifact.jar" );

        Artifact depArtifact = mock( Artifact.class );
        when( depArtifact.getArtifactId() ).thenReturn( aid );
        when( depArtifact.getBaseVersion() ).thenReturn( version );
        when( depArtifact.getFile() ).thenReturn( file );
        ArtifactHandler artifactHandler = mock( ArtifactHandler.class );
        when( artifactHandler.getExtension() ).thenReturn( type );
        when( depArtifact.getArtifactHandler() ).thenReturn( artifactHandler );

        final File destFile = new File( "assembly-dep-set.zip" );

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( destFile );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0222 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0222 );

        final ProjectBuilder projectBuilder = mock( ProjectBuilder.class );
        when( projectBuilder.build( any(Artifact.class), any(ProjectBuildingRequest.class) ) ).thenThrow( pbe );
        
        final MavenSession session = mock( MavenSession.class );
        when( session.getProjectBuildingRequest() ).thenReturn( mock( ProjectBuildingRequest.class ) );
        when( session.getExecutionProperties() ).thenReturn( new Properties() );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getFinalName() ).thenReturn( "final-name" );
        when( configSource.getMavenSession() ).thenReturn( session );
        when( configSource.getProject() ).thenReturn( project );
        

        final DependencySet ds = new DependencySet();
        ds.setOutputDirectory( "/out" );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( ds ), Collections.singleton( depArtifact ),
                                       project, projectBuilder, logger );

        task.addDependencySet( ds, archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();
        
        verify( archiver ).addFile( file, "out/" + aid + "-" + version + "." + type );
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();

        verify( session ).getProjectBuildingRequest();
        verify( session, times( 2 ) ).getExecutionProperties();

        verify( projectBuilder ).build( any(Artifact.class), any(ProjectBuildingRequest.class) );
    }

    @Test
    public void testAddDependencySet_ShouldAddOneDependencyFromProjectWithoutUnpacking()
        throws Exception
    {
        verifyOneDependencyAdded( "out", false );
    }

    @Test
    public void testAddDependencySet_ShouldAddOneDependencyFromProjectUnpacked()
        throws Exception
    {
        verifyOneDependencyAdded( "out", true );
    }

    private void verifyOneDependencyAdded( final String outputLocation, final boolean unpack )
        throws AssemblyFormattingException, ArchiverException, ArchiveCreationException, IOException,
        InvalidAssemblerConfigurationException, ProjectBuildingException
    {
        final MavenProject project = new MavenProject( new Model() );

        final DependencySet ds = new DependencySet();
        ds.setOutputDirectory( outputLocation );
        ds.setOutputFileNameMapping( "artifact" );
        ds.setUnpack( unpack );
        ds.setScope( Artifact.SCOPE_COMPILE );

        ds.setDirectoryMode( Integer.toString( 10, 8 ) );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        final MavenSession session = mock( MavenSession.class );
        when( session.getProjectBuildingRequest() ).thenReturn( mock( ProjectBuildingRequest.class ) );
        when( session.getExecutionProperties() ).thenReturn( new Properties() );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getMavenSession() ).thenReturn( session );
        when( configSource.getFinalName() ).thenReturn( "final-name" );
        
        Artifact artifact = mock( Artifact.class );
        final File artifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( artifactFile );

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0222 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0222 );

        if ( unpack )
        {
            
        }
        else
        {
            when( configSource.getProject() ).thenReturn( project );
        }


        final MavenProject depProject = new MavenProject( new Model() );

        ProjectBuildingResult pbr = mock( ProjectBuildingResult.class );
        when( pbr.getProject() ).thenReturn( depProject );
        
        final ProjectBuilder projectBuilder = mock( ProjectBuilder.class );
        when( projectBuilder.build( any( Artifact.class ), any( ProjectBuildingRequest.class ) ) ).thenReturn( pbr );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task = new AddDependencySetsTask( Collections.singletonList( ds ),
                                                                      Collections.singleton(
                                                                          artifact ), project,
                                                                      projectBuilder, logger );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        task.addDependencySet( ds, archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver ).setFileMode( 10 );
        verify( archiver ).setFileMode( 146 );
        verify( archiver ).setDirectoryMode( 10 );
        verify( archiver ).setDirectoryMode( 146 );
        
        verify( session ).getProjectBuildingRequest();
        verify( session, atLeastOnce() ).getExecutionProperties();
        
        verify( projectBuilder ).build( any( Artifact.class ), any( ProjectBuildingRequest.class ) );
        
        if ( unpack )
        {
            verify( archiver ).addArchivedFileSet( any( ArchivedFileSet.class ), isNull( Charset.class ) );
        }
        else
        {
            verify( archiver ).addFile( artifactFile, outputLocation + "/artifact", 10 );
            verify( configSource, atLeastOnce() ).getProject();
        }
    }

    @Test
    public void testGetDependencyArtifacts_ShouldGetOneDependencyArtifact()
        throws Exception
    {
        final MavenProject project = new MavenProject( new Model() );

        Artifact artifact = mock( Artifact.class );
        project.setArtifacts( Collections.singleton( artifact ) );

        final DependencySet dependencySet = new DependencySet();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task = new AddDependencySetsTask( Collections.singletonList( dependencySet ),
                                                                      Collections.singleton(
                                                                      artifact ), project,
                                                                      null, logger );

        final Set<Artifact> result = task.resolveDependencyArtifacts( dependencySet );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( artifact, result.iterator().next() );
    }

    @Test
    public void testGetDependencyArtifacts_ShouldFilterOneDependencyArtifactViaInclude()
        throws Exception
    {
        final MavenProject project = new MavenProject( new Model() );

        final Set<Artifact> artifacts = new HashSet<>();

        Artifact am1 = mock( Artifact.class );
        when( am1.getGroupId() ).thenReturn( "group" );
        when( am1.getArtifactId() ).thenReturn( "artifact" );
        when( am1.getId() ).thenReturn( "group:artifact:1.0:jar" );
        artifacts.add( am1 );

        Artifact am2 = mock( Artifact.class );
        when( am2.getGroupId() ).thenReturn( "group2" );
        when( am2.getArtifactId() ).thenReturn( "artifact2" );
        when( am2.getId() ).thenReturn( "group2:artifact2:1.0:jar" );
        when( am2.getDependencyConflictId() ).thenReturn( "group2:artifact2:jar" );
        artifacts.add( am2 );

        final DependencySet dependencySet = new DependencySet();

        dependencySet.addInclude( "group:artifact" );
        dependencySet.setUseTransitiveFiltering( true );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ), artifacts, project, null, logger );

        final Set<Artifact> result = task.resolveDependencyArtifacts( dependencySet );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( am1, result.iterator().next() );
    }

    @Test
    public void testGetDependencyArtifacts_ShouldIgnoreTransitivePathFilteringWhenIncludeNotTransitive()
        throws Exception
    {
        final MavenProject project = new MavenProject( new Model() );

        final Set<Artifact> artifacts = new HashSet<>();

        Artifact am1 = mock( Artifact.class );
        when( am1.getGroupId() ).thenReturn( "group" );
        when( am1.getArtifactId() ).thenReturn( "artifact" );
        when( am1.getId() ).thenReturn( "group:artifact:1.0:jar" );
        artifacts.add( am1 );

        Artifact am2 = mock( Artifact.class );
        when( am2.getGroupId() ).thenReturn( "group2" );
        when( am2.getArtifactId() ).thenReturn( "artifact2" );
        when( am2.getId() ).thenReturn( "group2:artifact2:1.0:jar" );
        when( am2.getDependencyConflictId() ).thenReturn( "group2:artifact2:jar" );
        artifacts.add( am2 );

        final DependencySet dependencySet = new DependencySet();

        dependencySet.addInclude( "group:artifact" );
        dependencySet.setUseTransitiveFiltering( false );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        final AddDependencySetsTask task =
            new AddDependencySetsTask( Collections.singletonList( dependencySet ), artifacts, project, null, logger );

        final Set<Artifact> result = task.resolveDependencyArtifacts( dependencySet );

        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertSame( am1, result.iterator().next() );
    }

}
