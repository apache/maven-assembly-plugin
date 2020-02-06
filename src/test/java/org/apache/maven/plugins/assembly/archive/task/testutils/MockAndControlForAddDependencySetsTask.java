package org.apache.maven.plugins.assembly.archive.task.testutils;

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

import junit.framework.Assert;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Profile;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugins.assembly.artifact.DependencyResolver;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.DependencyResolutionResult;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.archiver.ArchivedFileSet;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.easymock.EasyMock;
import org.easymock.classextension.EasyMockSupport;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

public class MockAndControlForAddDependencySetsTask
{

    public final Archiver archiver;

    public final AssemblerConfigurationSource configSource;

    public final ProjectBuilder projectBuilder;

    public final ArchiverManager archiverManager;

    public final DependencyResolver dependencyResolver;

    private final MavenProject project;

    private final MavenSession session;
    
    private final ProjectBuildingRequest projectBuildingRequest;

    public MockAndControlForAddDependencySetsTask( final EasyMockSupport mockManager )
    {
        this( mockManager, null );
    }

    public MockAndControlForAddDependencySetsTask( final EasyMockSupport mockManager, final MavenProject project )
    {
        this.project = project;

        this.session = mockManager.createMock( MavenSession.class );
        
        this.projectBuildingRequest = mockManager.createMock( ProjectBuildingRequest.class );

        archiver = mockManager.createMock( Archiver.class );
        
        configSource = mockManager.createMock( AssemblerConfigurationSource.class );

        projectBuilder = mockManager.createMock( ProjectBuilder.class );

        archiverManager = mockManager.createMock( ArchiverManager.class );

        dependencyResolver = mockManager.createMock( DependencyResolver.class );

        enableDefaultExpectations();
    }

    private void enableDefaultExpectations()
    {
        expect( configSource.getProject() ).andReturn( project ).anyTimes();
        expect( session.getProjectBuildingRequest() ).andReturn( projectBuildingRequest ).anyTimes();
        expect( session.getSystemProperties() ).andReturn( new Properties() ).anyTimes();
        expect( session.getUserProperties() ).andReturn( new Properties() ).anyTimes();
        expect( session.getExecutionProperties() ).andReturn( new Properties() ).anyTimes();

        expect( projectBuildingRequest.isProcessPlugins() )
                .andReturn( false ).anyTimes();
        expect( projectBuildingRequest.getProfiles() )
                .andReturn( Collections.<Profile>emptyList() ).anyTimes();
        expect( projectBuildingRequest.getActiveProfileIds() )
                .andReturn( Collections.<String>emptyList() ).anyTimes();
        expect( projectBuildingRequest.getInactiveProfileIds() )
                .andReturn( Collections.<String>emptyList() ).anyTimes();
        expect( projectBuildingRequest.getSystemProperties() )
                .andReturn( new Properties() ).anyTimes();
        expect( projectBuildingRequest.getUserProperties() )
                .andReturn( new Properties() ).anyTimes();
        expect( projectBuildingRequest.getRemoteRepositories() )
                .andReturn( new ArrayList<ArtifactRepository>() ).anyTimes();
        expect( projectBuildingRequest.getPluginArtifactRepositories() )
                .andReturn( Collections.<ArtifactRepository>emptyList() ).anyTimes();
        expect( projectBuildingRequest.getRepositorySession() )
                .andReturn( new MavenRepositorySystemSession() ).anyTimes();
        expect( projectBuildingRequest.getLocalRepository() )
                .andReturn( new MavenArtifactRepository() ).anyTimes();
        expect( projectBuildingRequest.getBuildStartTime() )
                .andReturn( new Date() ).anyTimes();
        expect( projectBuildingRequest.getProject() )
                .andReturn( project ).anyTimes();
        expect( projectBuildingRequest.isResolveDependencies() )
                .andReturn( false ).anyTimes();
        expect( projectBuildingRequest.getValidationLevel() )
                .andReturn( ModelBuildingRequest.VALIDATION_LEVEL_STRICT ).anyTimes();

        expectGetSession( session );
    }

    public void expectAddArchivedFileSet()
    {
        try
        {
            archiver.addArchivedFileSet( (File) anyObject(), (String) anyObject(), (String[]) anyObject(),
                                         (String[]) anyObject() );
            EasyMock.expectLastCall().anyTimes();
            archiver.addArchivedFileSet( (ArchivedFileSet) anyObject(), (Charset) anyObject() );
            EasyMock.expectLastCall().anyTimes();

        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectModeChange( final int originalDirMode, final int originalFileMode, final int dirMode,
                                  final int fileMode, final int numberOfChanges )
    {
        expectGetMode( originalDirMode, originalFileMode );
        // one of the changes will occur below, when we restore the original mode.
        if ( numberOfChanges > 1 )
        {
            for ( int i = 1; i < numberOfChanges; i++ )
            {
                archiver.setDirectoryMode( dirMode );
                archiver.setFileMode( fileMode );
            }
        }

        archiver.setDirectoryMode( originalDirMode );
        archiver.setFileMode( originalFileMode );
    }

    public void expectGetMode( final int originalDirMode, final int originalFileMode )
    {
        archiver.setFileMode( anyInt() );
        EasyMock.expectLastCall().anyTimes();
        expect( archiver.getOverrideDirectoryMode() ).andReturn( originalDirMode );
        expect( archiver.getOverrideFileMode() ).andReturn( originalFileMode );
        archiver.setDirectoryMode( anyInt() );
        EasyMock.expectLastCall().anyTimes();

    }

    public void expectAddFile( final File file, final String outputLocation )
    {
        try
        {
            archiver.addFile( file, outputLocation );
        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectAddFile( final File file, final String outputLocation, final int fileMode )
    {
        try
        {
            archiver.addFile( file, outputLocation, fileMode );
        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectAddAnyFile()
    {
        try
        {
            archiver.addFile( (File) anyObject(), (String) anyObject(), anyInt() );
        }
        catch ( final ArchiverException e )
        {
            Assert.fail( "Should never happen." );
        }
    }

    public void expectGetReactorProjects( final List<MavenProject> projects )
    {
        expect( configSource.getReactorProjects() ).andReturn( projects ).anyTimes();
    }

    public void expectCSGetFinalName( final String finalName )
    {
        expect( configSource.getFinalName() ).andReturn( finalName ).anyTimes();
    }

    public void expectGetDestFile( final File destFile )
    {
        expect( archiver.getDestFile() ).andReturn( destFile ).anyTimes();
    }

    public void expectCSGetRepositories( final ArtifactRepository localRepo,
                                         final List<ArtifactRepository> remoteRepos )
    {
        expect( configSource.getLocalRepository() ).andReturn( localRepo ).anyTimes();
        expect( configSource.getRemoteRepositories() ).andReturn( remoteRepos ).anyTimes();
    }

    public void expectBuildFromRepository( final ProjectBuildingException error )
    {
        try
        {
            expect( projectBuilder.build( (Artifact) anyObject(), (ProjectBuildingRequest) anyObject() ) ).andThrow(
                error );
//            projectBuilderCtl.setThrowable( error, MockControl.ONE_OR_MORE );
        }
        catch ( final ProjectBuildingException e )
        {
            Assert.fail( "should never happen" );
        }
    }

    public void expectBuildFromRepository( final MavenProject project )
    {
        ProjectBuildingResult pbr = new ProjectBuildingResult()
        {
            @Override
            public String getProjectId()
            {
                return null;
            }

            @Override
            public File getPomFile()
            {
                return null;
            }

            @Override
            public MavenProject getProject()
            {
                return project;
            }

            @Override
            public List<ModelProblem> getProblems()
            {
                return null;
            }

            @Override
            public DependencyResolutionResult getDependencyResolutionResult()
            {
                return null;
            }
        };

        try
        {
            expect( projectBuilder.build( (Artifact) anyObject(), (ProjectBuildingRequest) anyObject() ) ).andReturn(
                pbr ).anyTimes();
        }
        catch ( final ProjectBuildingException e )
        {
            Assert.fail( "should never happen" );
        }
    }

    public void expectGetSession( final MavenSession session )
    {
        expect( configSource.getMavenSession() ).andReturn( session ).anyTimes();
    }

    public void expectResolveDependencySets()
        throws DependencyResolutionException
    {
        expect( dependencyResolver.resolveDependencySets( (Assembly) anyObject(),
                                                          (AssemblerConfigurationSource) anyObject(),
                                                          (List<DependencySet>) anyObject() ) ).andReturn(
            new LinkedHashMap<DependencySet, Set<Artifact>>() ).anyTimes();

    }


}
