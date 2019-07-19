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
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.codehaus.plexus.PlexusTestCase;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.classextension.EasyMockSupport;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.repository.LocalArtifactRequest;
import org.sonatype.aether.repository.LocalArtifactResult;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.LocalRepositoryManager;

public class DefaultDependencyResolverTest
    extends PlexusTestCase
{

    private DefaultDependencyResolver resolver;
    private DefaultArtifactDescriptorReader defaultArtifactDescriptorReader;
    private Map<String, Model> modelCache = new HashMap<>();
    
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        resolver = (DefaultDependencyResolver) lookup( DependencyResolver.class );
        defaultArtifactDescriptorReader = (DefaultArtifactDescriptorReader) lookup(ArtifactDescriptorReader.class);
        modelCache.clear();
    }

    protected MavenSession newMavenSession( MavenProject project )
    {
        return newMavenSession( project, LegacyLocalRepositoryManager.wrap( new StubArtifactRepository( "target/local-repo" ),
                                                                                          null ) );
    }

    protected MavenSession newMavenSession( MavenProject project, LocalRepositoryManager repositoryManager )
    {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        MavenExecutionResult result = new DefaultMavenExecutionResult();

        MavenRepositorySystemSession repoSession = new MavenRepositorySystemSession();
        
        repoSession.setLocalRepositoryManager( repositoryManager );
        MavenSession session = new MavenSession( getContainer(), repoSession, request, result );
        session.setCurrentProject( project );
        session.setProjects( Arrays.asList( project ) );
        return session;
    }

    protected MavenSession newMavenSession( MavenProject project, final EasyMockSupport nm ) throws ModelBuildingException
    {
        LocalRepositoryManager mock = nm.createMock( LocalRepositoryManager.class );
        expect( mock.find( (RepositorySystemSession) anyObject(), (LocalArtifactRequest) anyObject() ) )
                .andAnswer( new IAnswer<LocalArtifactResult>() {
                    @Override
                    public LocalArtifactResult answer () throws Throwable
                    {
                        Object[] arguments = EasyMock.getCurrentArguments();
                        LocalArtifactRequest localArtifactRequest = (LocalArtifactRequest) arguments[1];
                        LocalArtifactResult result = new LocalArtifactResult( localArtifactRequest );
                        org.sonatype.aether.artifact.Artifact artifact = localArtifactRequest.getArtifact();
                        return result.setAvailable( true )
                                .setFile( new File( artifact.getGroupId() + '.' + artifact.getArtifactId() + '.'
                                        + artifact.getVersion() + '.' + artifact.getExtension() ) );
                    }
                } ).atLeastOnce();
        expect( mock.getRepository() ).andReturn( new LocalRepository( new File( "dummy-repository" ) ) ).atLeastOnce();

        ModelBuilder modelBuilder = nm.createMock( ModelBuilder.class );
        expect( modelBuilder.build( (ModelBuildingRequest) anyObject() ) )
                .andAnswer( new IAnswer<ModelBuildingResult>() {
                    @Override
                    public ModelBuildingResult answer () throws Throwable
                    {
                        ModelBuildingRequest argument = (ModelBuildingRequest) EasyMock.getCurrentArguments()[0];
                        ModelBuildingResult modelBuildingResult = nm.createMock( ModelBuildingResult.class );
                        File pomFile = ( (FileModelSource) argument.getModelSource() ).getPomFile();
                        Model model = modelCache.get( pomFile.getName() );
                        if ( model == null )
                        {
                            model = new Model();
                        }
                        expect( modelBuildingResult.getEffectiveModel() ).andReturn( model ).atLeastOnce();
                        replay( modelBuildingResult );
                        return modelBuildingResult;
                    }
                } ).atLeastOnce();
        defaultArtifactDescriptorReader.setModelBuilder( modelBuilder );
        return newMavenSession( project, mock );
    }

    public void test_getDependencySetResolutionRequirements_transitive ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( null, Artifact.SCOPE_COMPILE, true );
    }

    public void test_getDependencySetResolutionRequirements_transitive_with_test_dependency ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( null, Artifact.SCOPE_TEST, true );
    }

    public void test_getDependencySetResolutionRequirements_transitive_with_test_dependency_scope_runtime ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( Artifact.SCOPE_COMPILE, Artifact.SCOPE_TEST, false );
    }

    public void test_getDependencySetResolutionRequirements_transitive_with_test_dependency_scope_compile_runtime ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( Artifact.SCOPE_COMPILE_PLUS_RUNTIME, Artifact.SCOPE_TEST, false );
    }

    public void test_getDependencySetResolutionRequirements_transitive_with_test_dependency_scope_test ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( Artifact.SCOPE_TEST, Artifact.SCOPE_TEST, true );
    }

    public void test_2ndLevelDependencyResolution_no_scope_compile_compile ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( null, Artifact.SCOPE_COMPILE, true, Artifact.SCOPE_COMPILE, true );
    }

    public void test_2ndLevelDependencyResolution_compile_compile_compile ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( Artifact.SCOPE_COMPILE, Artifact.SCOPE_COMPILE, true, Artifact.SCOPE_COMPILE, true );
    }

    public void test_2ndLevelDependencyResolution_compile_test_compile ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( Artifact.SCOPE_COMPILE, Artifact.SCOPE_TEST, false, Artifact.SCOPE_COMPILE, false );
    }

    public void test_2ndLevelDependencyResolution_compile_test_test ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( Artifact.SCOPE_COMPILE, Artifact.SCOPE_TEST, false, Artifact.SCOPE_TEST, false );
    }

    public void test_2ndLevelDependencyResolution_provided_provided_test ()
            throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution( Artifact.SCOPE_PROVIDED, Artifact.SCOPE_PROVIDED, true, Artifact.SCOPE_TEST, false );
    }

    private void runDependencyResolution(final String dependencySetScope, String dependencyScope, boolean added) throws DependencyResolutionException, ModelBuildingException
    {
        runDependencyResolution(dependencySetScope, dependencyScope, added, null, false);
    }

    private void runDependencyResolution(final String dependencySetScope, String dependencyScope, boolean added, String secondLevelDependencyScope, boolean secondLevelDependencyAdded)
        throws DependencyResolutionException, ModelBuildingException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final DependencySet ds = new DependencySet();
        ds.setScope( dependencySetScope );
        ds.setUseTransitiveDependencies( true );

        final MavenProject project = createMavenProject( "main-group", "main-artifact", "1", null );
        final Artifact mainArtifact = newArtifact( "main-group", "main-artifact", "1" );
        final Artifact depArtifact = newArtifact( "dep-group", "dep-artifact", "2", dependencyScope );

        project.setArtifacts( Collections.singleton( mainArtifact ) );
        project.setDependencyArtifacts( Collections.singleton( depArtifact ) );
        project.setDependencies(
                Collections.singletonList( newDependency( "dep-group", "dep-artifact", "2", dependencyScope ) ) );
        Set<Artifact> expected = new HashSet<>();
        expected.add( mainArtifact );
        if (added) {
            expected.add( depArtifact );
        }
        if (secondLevelDependencyScope != null) {
            Model depModel = new Model();
            depModel.addDependency(
                    newDependency( "other-dep-group", "other-dep-artifact", "3", secondLevelDependencyScope ) );
            modelCache.put( "dep-group.dep-artifact.2.pom", depModel );
        }
        if (secondLevelDependencyAdded) {
            expected.add( newArtifact( "other-dep-group", "other-dep-artifact", "3", secondLevelDependencyScope ) );
        }

        MavenSession mavenSession = newMavenSession( project, mm );
        mm.replayAll();

        final ResolutionManagementInfo info = new ResolutionManagementInfo();
        resolver.updateDependencySetResolutionRequirements( ds, info, mavenSession, project );
        assertEquals( expected, info.getArtifacts() );
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
        resolver.updateDependencySetResolutionRequirements( ds, info, newMavenSession( project ), project );
        assertEquals( dependencyArtifacts, info.getArtifacts() );
    }

    public void test_getModuleSetResolutionRequirements_withoutBinaries()
        throws DependencyResolutionException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final AssemblerConfigurationSource cs = mm.createMock( AssemblerConfigurationSource.class );

        final File rootDir = new File( "root" );
        final MavenProject project = createMavenProject( "main-group", "main-artifact", "1", rootDir );
        final MavenProject module1 =
            createMavenProject( "main-group", "module-1", "1", new File( rootDir, "module-1" ) );
        final MavenProject module2 =
            createMavenProject( "main-group", "module-2", "1", new File( rootDir, "module-2" ) );

        project.getModel().addModule( module1.getArtifactId() );
        project.getModel().addModule( module2.getArtifactId() );

        expect( cs.getReactorProjects() ).andReturn( Arrays.asList( project, module1, module2 ) ).anyTimes();
        expect( cs.getProject() ).andReturn( project ).anyTimes();
        expect( cs.getMavenSession() ).andReturn( newMavenSession( project ) ).anyTimes();

        final ResolutionManagementInfo info = new ResolutionManagementInfo();

        final ModuleSet ms = new ModuleSet();
        ms.setBinaries( null );

        mm.replayAll();

        resolver.updateModuleSetResolutionRequirements( ms, new DependencySet(), info, cs );
        assertTrue( info.getArtifacts().isEmpty() );

        mm.verifyAll();
    }

    public void test_getModuleSetResolutionRequirements_includeDeps()
        throws DependencyResolutionException
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final AssemblerConfigurationSource cs = mm.createMock( AssemblerConfigurationSource.class );

        final File rootDir = new File( "root" );
        final MavenProject project = createMavenProject( "main-group", "main-artifact", "1", rootDir );
        final MavenProject module1 =
            createMavenProject( "main-group", "module-1", "1", new File( rootDir, "module-1" ) );
        final MavenProject module2 =
            createMavenProject( "main-group", "module-2", "1", new File( rootDir, "module-2" ) );

        Set<Artifact> module1Artifacts = Collections.singleton( newArtifact( "main-group", "module-1", "1" ) );
        Set<Artifact> module2Artifacts = Collections.singleton( newArtifact( "main-group", "module-2", "1" ) );
        module1.setArtifacts( module1Artifacts );
        module2.setArtifacts( module2Artifacts );

        project.getModel().addModule( module1.getArtifactId() );
        project.getModel().addModule( module2.getArtifactId() );

        expect( cs.getReactorProjects() ).andReturn( Arrays.asList( project, module1, module2 ) ).anyTimes();
        expect( cs.getProject() ).andReturn( project ).anyTimes();
        expect( cs.getMavenSession() ).andReturn( newMavenSession( project ) ).anyTimes();

        final ResolutionManagementInfo info = new ResolutionManagementInfo();

        final ModuleSet ms = new ModuleSet();
        final ModuleBinaries mb = new ModuleBinaries();
        mb.setIncludeDependencies( true );
        ms.setBinaries( mb );
        ms.addInclude( "*:module-1" );

        mm.replayAll();

        resolver.updateModuleSetResolutionRequirements( ms, new DependencySet(), info, cs );
        assertEquals( module1Artifacts, info.getArtifacts() );

        mm.verifyAll();
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

    private Artifact newArtifact( final String groupId, final String artifactId, final String version) 
    {
        return newArtifact(groupId, artifactId, version, "compile");
    }

    private Artifact newArtifact( final String groupId, final String artifactId, final String version, final String scope )
    {
        return new DefaultArtifact( groupId, artifactId, VersionRange.createFromVersion( version ), scope, "jar",
                                    null, new DefaultArtifactHandler() );
    }

    private Dependency newDependency( final String groupId, final String artifactId, final String version, final String scope )
    {
        Dependency dep = new Dependency();
        dep.setArtifactId( artifactId );
        dep.setGroupId( groupId );
        dep.setVersion( version );
        dep.setScope( scope );
        dep.setType( "jar" );
        return dep;
    }

}
