package org.apache.maven.plugins.assembly.artifact;

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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.LegacyLocalRepositoryManager;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.PlexusTestCase;

public class DefaultDependencyResolverTest
    extends PlexusTestCase
{

    private DefaultDependencyResolver resolver;

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        resolver = (DefaultDependencyResolver) lookup( DependencyResolver.class );
    }
    
    protected MavenSession newMavenSession( MavenProject project )
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        MavenRepositorySystemSession repoSession = new MavenRepositorySystemSession();
        
        repoSession.setLocalRepositoryManager( LegacyLocalRepositoryManager.wrap( new StubArtifactRepository( "target/local-repo" ),
                                                                                  null ) );
        MavenSession session = new MavenSession( getContainer(), repoSession, request, result );
        session.setCurrentProject( project );
        session.setProjects( Arrays.asList( project ) );
        return session;
    }


    public void test_getDependencySetResolutionRequirements_transitive()
        throws DependencyResolutionException
    {
        final DependencySet ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_SYSTEM );
        ds.setUseTransitiveDependencies( true );

        final MavenProject project = createMavenProject( "main-group", "main-artifact", "1", null );

        Set<Artifact> dependencyArtifacts = new HashSet<>();
        dependencyArtifacts.add( newArtifact( "g.id", "a-id", "1" ) );
        Set<Artifact> artifacts = new HashSet<>( dependencyArtifacts );
        artifacts.add( newArtifact( "g.id", "a-id-2", "2" ) );
        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( dependencyArtifacts );

        final ResolutionManagementInfo info = new ResolutionManagementInfo();
        resolver.updateDependencySetResolutionRequirements( ds, info, project );
        assertEquals( artifacts, info.getArtifacts() );
    }

    public void test_getDependencySetResolutionRequirements_nonTransitive()
        throws DependencyResolutionException
    {
        final DependencySet ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_SYSTEM );
        ds.setUseTransitiveDependencies( false );

        final MavenProject project = createMavenProject( "main-group", "main-artifact", "1", null );

        Set<Artifact> dependencyArtifacts = new HashSet<>();
        dependencyArtifacts.add( newArtifact( "g.id", "a-id", "1" ) );
        Set<Artifact> artifacts = new HashSet<>( dependencyArtifacts );
        artifacts.add( newArtifact( "g.id", "a-id-2", "2" ) );
        project.setArtifacts( artifacts );
        project.setDependencyArtifacts( dependencyArtifacts );

        final ResolutionManagementInfo info = new ResolutionManagementInfo();
        resolver.updateDependencySetResolutionRequirements( ds, info, project );
        assertEquals( dependencyArtifacts, info.getArtifacts() );
    }

    public void test_getModuleSetResolutionRequirements_withoutBinaries()
        throws DependencyResolutionException
    {
        final File rootDir = new File( "root" );
        final MavenProject project = createMavenProject( "main-group", "main-artifact", "1", rootDir );
        final MavenProject module1 =
            createMavenProject( "main-group", "module-1", "1", new File( rootDir, "module-1" ) );
        final MavenProject module2 =
            createMavenProject( "main-group", "module-2", "1", new File( rootDir, "module-2" ) );

        project.getModel().addModule( module1.getArtifactId() );
        project.getModel().addModule( module2.getArtifactId() );

        final ResolutionManagementInfo info = new ResolutionManagementInfo();

        final ModuleSet ms = new ModuleSet();
        ms.setBinaries( null );

        resolver.updateModuleSetResolutionRequirements( ms, new DependencySet(), info, null );
        assertTrue( info.getArtifacts().isEmpty() );
    }

    public void test_getModuleSetResolutionRequirements_includeDeps()
        throws DependencyResolutionException
    {
        final File rootDir = new File( "root" );
        final MavenProject project = createMavenProject( "main-group", "main-artifact", "1", rootDir );
        final MavenProject module1 =
            createMavenProject( "main-group", "module-1", "1", new File( rootDir, "module-1" ) );
        final MavenProject module2 =
            createMavenProject( "main-group", "module-2", "1", new File( rootDir, "module-2" ) );

        Set<Artifact> module1Artifacts = Collections.singleton( newArtifact( "group.id", "module-1-dep", "1" ) );
        Set<Artifact> module2Artifacts = Collections.singleton( newArtifact( "group.id", "module-2-dep", "1" ) );
        module1.setArtifacts( module1Artifacts );
        module2.setArtifacts( module2Artifacts );

        project.getModel().addModule( module1.getArtifactId() );
        project.getModel().addModule( module2.getArtifactId() );

        final AssemblerConfigurationSource cs = mock( AssemblerConfigurationSource.class );
        when( cs.getReactorProjects() ).thenReturn( Arrays.asList( project, module1, module2 ) );
        when( cs.getProject() ).thenReturn( project );

        final ResolutionManagementInfo info = new ResolutionManagementInfo();

        final ModuleSet ms = new ModuleSet();
        final ModuleBinaries mb = new ModuleBinaries();
        mb.setIncludeDependencies( true );
        ms.setBinaries( mb );
        ms.addInclude( "*:module-1" );

        resolver.updateModuleSetResolutionRequirements( ms, new DependencySet(), info, cs );
        assertEquals( module1Artifacts, info.getArtifacts() );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( cs ).getReactorProjects();
        verify( cs ).getProject();
    }

    private MavenProject createMavenProject( final String groupId, final String artifactId, final String version,
                                             final File basedir )
    {
        final Model model = new Model();

        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );
        model.setPackaging( "pom" );

        final MavenProject project = new MavenProject( model );

        final Artifact pomArtifact = newArtifact( groupId, artifactId, version );
        project.setArtifact( pomArtifact );
        project.setArtifacts( new HashSet<Artifact>() );
        project.setDependencyArtifacts( new HashSet<Artifact>() );

        project.setFile( new File( basedir, "pom.xml" ) );

        return project;
    }

    private Artifact newArtifact( final String groupId, final String artifactId, final String version )
    {
        return new DefaultArtifact( groupId, artifactId, VersionRange.createFromVersion( version ), "compile", "jar",
                                    null, new DefaultArtifactHandler() );
    }

}
