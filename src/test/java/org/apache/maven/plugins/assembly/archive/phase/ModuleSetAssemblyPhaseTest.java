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

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.artifact.DependencyResolver;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.logging.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class ModuleSetAssemblyPhaseTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private ModuleSetAssemblyPhase phase;
    
    private DependencyResolver dependencyResolver;
    
    private ProjectBuilder projectBuilder;

    private Logger logger;
    
    @Before
    public void setUp()
    {
        this.dependencyResolver = mock( DependencyResolver.class );
        
        this.logger = mock( Logger.class );
        
        this.phase = new ModuleSetAssemblyPhase( projectBuilder, dependencyResolver, logger );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchOutputDir()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setOutputDirectory( "outdir" );

        assertTrue( this.phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchInclude()
    {
        final ModuleSources sources = new ModuleSources();
        sources.addInclude( "**/included.txt" );

        assertTrue( this.phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchExclude()
    {
        final ModuleSources sources = new ModuleSources();
        sources.addExclude( "**/excluded.txt" );

        assertTrue( this.phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldNotCatchFileMode()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setFileMode( "777" );

        assertFalse( this.phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldNotCatchDirMode()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setDirectoryMode( "777" );

        assertFalse( this.phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testCreateFileSet_ShouldUseModuleDirOnlyWhenOutDirIsNull()
        throws Exception
    {
        final Model model = new Model();
        model.setArtifactId( "artifact" );

        final MavenProject project = new MavenProject( model );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getProject() ).thenReturn( project );

        final FileSet fs = new FileSet();

        final ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        final File basedir = temporaryFolder.getRoot();

        final MavenProject artifactProject = new MavenProject( new Model() );
        artifactProject.setGroupId( "GROUPID" );
        artifactProject.setFile( new File( basedir, "pom.xml" ) );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        when( artifact.getArtifactId() ).thenReturn( "artifact" );

        artifactProject.setArtifact( artifact );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        final FileSet result = this.phase.createFileSet( fs, sources, artifactProject, configSource );

        assertEquals( "artifact/", result.getOutputDirectory() );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();
    }

    @Test
    public void testCreateFileSet_ShouldPrependModuleDirWhenOutDirIsProvided()
        throws Exception
    {
        final Model model = new Model();
        model.setArtifactId( "artifact" );

        final MavenProject project = new MavenProject( model );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getProject() ).thenReturn( project );

        final FileSet fs = new FileSet();
        fs.setOutputDirectory( "out" );

        final ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        final MavenProject artifactProject = new MavenProject( new Model() );
        artifactProject.setGroupId( "GROUPID" );

        final File basedir = temporaryFolder.getRoot();

        artifactProject.setFile( new File( basedir, "pom.xml" ) );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        when( artifact.getArtifactId() ).thenReturn( "artifact" );

        artifactProject.setArtifact( artifact );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project /* or artifactProject */ );

        final FileSet result = this.phase.createFileSet( fs, sources, artifactProject, configSource );

        assertEquals( "artifact/out/", result.getOutputDirectory() );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();
    }

    @Test
    public void testCreateFileSet_ShouldAddExcludesForSubModulesWhenExcludeSubModDirsIsTrue()
        throws Exception
    {
        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );

        final FileSet fs = new FileSet();

        final ModuleSources sources = new ModuleSources();
        sources.setExcludeSubModuleDirectories( true );

        final Model model = new Model();
        model.setArtifactId( "artifact" );

        model.addModule( "submodule" );

        final MavenProject project = new MavenProject( model );

        final File basedir = temporaryFolder.getRoot();
        project.setGroupId( "GROUPID" );
        project.setFile( new File( basedir, "pom.xml" ) );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );

        project.setArtifact( artifact );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        final FileSet result = this.phase.createFileSet( fs, sources, project, configSource );

        assertEquals( 1, result.getExcludes().size() );
        assertEquals( "submodule/**", result.getExcludes().get( 0 ) );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();
    }

    @Test
    public void testExecute_ShouldSkipIfNoModuleSetsFound()
        throws Exception
    {
        final Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        this.phase.execute( assembly, null, null );
    }

    @Test
    public void testExecute_ShouldAddOneModuleSetWithOneModuleInIt()
        throws Exception
    {
        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MavenProject module = createProject( "group", "module", "version", project );

        Artifact artifact = mock( Artifact.class );
        final File moduleArtifactFile = temporaryFolder.newFile();
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        when( artifact.getFile() ).thenReturn( moduleArtifactFile );
        module.setArtifact( artifact );

        final List<MavenProject> projects = new ArrayList<>();
        projects.add( module );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getReactorProjects() ).thenReturn( projects );
        when( configSource.getFinalName() ).thenReturn( "final-name" );
        when( configSource.getProject() ).thenReturn( project );
        
        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0777 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0777 );

        final ModuleBinaries bin = new ModuleBinaries();
        bin.setOutputFileNameMapping( "artifact" );
        bin.setOutputDirectory( "out" );
        bin.setFileMode( "777" );
        bin.setUnpack( false );
        bin.setIncludeDependencies( false );

        final ModuleSet ms = new ModuleSet();
        ms.setBinaries( bin );

        final Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );
        assembly.addModuleSet( ms );

        when( dependencyResolver.resolveDependencySets( eq( assembly ), 
                                                        eq( ms ),
                                                        eq( configSource ),
                                                        anyListOf( DependencySet.class ) ) ).thenReturn( new LinkedHashMap<DependencySet, Set<Artifact>>() );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, module );

        this.phase.execute( assembly, archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();
        verify( configSource, atLeastOnce() ).getReactorProjects();

        verify( archiver ).addFile( moduleArtifactFile, "out/artifact", 511 );
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver, times( 2 ) ).setFileMode( 511 );

        verify( dependencyResolver ).resolveDependencySets( eq( assembly ), 
                                                            eq( ms ),
                                                            eq( configSource ), 
                                                            anyListOf( DependencySet.class ) );
    }

    @Test
    public void testAddModuleBinaries_ShouldReturnImmediatelyWhenBinariesIsNull()
        throws Exception
    {
        this.phase.addModuleBinaries( null, null, null, null, null, null );
    }

    @Test
    public void testAddModuleBinaries_ShouldFilterPomModule()
        throws Exception
    {
        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setPackaging( "pom" );

        Artifact artifact = mock( Artifact.class );
        project.setArtifact( artifact );

        final Set<MavenProject> projects = singleton( project );
        
        this.phase.addModuleBinaries( null, null, binaries, projects, null, null );
    }

    @Test
    public void testAddModuleBinaries_ShouldAddOneModuleAttachmentArtifactAndNoDeps()
        throws Exception
    {
        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getFinalName() ).thenReturn( "final-name" );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        when( artifact.getClassifier() ).thenReturn( "test" );
        final File artifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( artifactFile );

        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0222 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0222 );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( false );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setAttachmentClassifier( "test" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.addAttachedArtifact( artifact );

        final Set<MavenProject> projects = singleton( project );

        when( dependencyResolver.resolveDependencySets( isNull( Assembly.class ), 
                                                        isNull( ModuleSet.class ),
                                                        eq( configSource ),
                                                        anyListOf( DependencySet.class ) ) ).thenReturn( new LinkedHashMap<DependencySet, Set<Artifact>>() );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        this.phase.addModuleBinaries( null, null, binaries, projects, archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();

        verify( archiver ).addFile( artifactFile, "out/artifact", 511 );
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver ).setFileMode( 511 );
        verify( archiver ).setFileMode( 146 );

        verify( dependencyResolver ).resolveDependencySets( isNull( Assembly.class ), 
                                                            isNull( ModuleSet.class ),
                                                            eq( configSource ), 
                                                            anyListOf( DependencySet.class ) );
    }

    @Test
    public void testAddModuleBinaries_ShouldFailWhenOneModuleDoesntHaveAttachmentWithMatchingClassifier()
        throws Exception
    {
        Artifact artifact = mock( Artifact.class );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setAttachmentClassifier( "test" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifact );

        final Set<MavenProject> projects = singleton( project );

        try
        {
            
            this.phase.addModuleBinaries( null, null, binaries, projects, null, null );

            fail( "Should throw an invalid configuration exception because of module with missing attachment." );
        }
        catch ( final InvalidAssemblerConfigurationException e )
        {
            assertEquals( "Cannot find attachment with classifier: test in module project: group:artifact:jar:version. "
                + "Please exclude this module from the module-set.", e.getMessage());
            // should throw this because of missing attachment.
        }
    }

    @Test
    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndNoDeps()
        throws Exception
    {
        Artifact artifact = mock( Artifact.class );
        final File artifactFile = temporaryFolder.newFile();
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        when( artifact.getFile() ).thenReturn( artifactFile );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getFinalName() ).thenReturn( "final-name" );
        
        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0222 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0222 );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( false );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifact );

        final Set<MavenProject> projects = singleton( project );

        when( dependencyResolver.resolveDependencySets( isNull( Assembly.class ), 
                                                        isNull( ModuleSet.class ),
                                                        any( AssemblerConfigurationSource.class ),
                                                        anyListOf( DependencySet.class ) ) ).thenReturn( new LinkedHashMap<DependencySet, Set<Artifact>>() );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        this.phase.addModuleBinaries( null, null, binaries, projects, archiver, configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();
        
        verify( dependencyResolver ).resolveDependencySets( isNull( Assembly.class ), 
                                                            isNull( ModuleSet.class ),
                                                            any( AssemblerConfigurationSource.class ),
                                                            anyListOf( DependencySet.class ) );

        verify( archiver ).addFile( artifactFile, "out/artifact", 511 );
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver ).setFileMode( 511 );
        verify( archiver ).setFileMode( 146);
    }

    @Test
    public void testAddModuleArtifact_ShouldThrowExceptionWhenArtifactFileIsNull()
        throws Exception
    {
        Artifact artifact = mock( Artifact.class );
        try
        {
            this.phase.addModuleArtifact( artifact, null, null, null, null );

            fail( "Expected ArchiveCreationException since artifact file is null." );
        }
        catch ( final ArchiveCreationException e )
        {
            // expected
        }
    }

    @Test
    public void testAddModuleArtifact_ShouldAddOneArtifact()
        throws Exception
    {
        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        final File artifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( artifactFile );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifact );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getFinalName() ).thenReturn( "final-name" );
        
        final Archiver archiver = mock( Archiver.class );
        when( archiver.getDestFile() ).thenReturn( new File( "junk" ) );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( 0222 );
        when( archiver.getOverrideFileMode() ).thenReturn( 0222 );

        final ModuleBinaries binaries = new ModuleBinaries();
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        this.phase.addModuleArtifact( artifact, project, archiver, configSource, binaries );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getMavenSession();
        verify( configSource, atLeastOnce() ).getProject();

        verify( archiver ).addFile( artifactFile, "out/artifact", 511 );
        verify( archiver, atLeastOnce() ).getDestFile();
        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
        verify( archiver ).setFileMode( 511 );
        verify( archiver ).setFileMode( 146 );
    }

    @Test
    public void testAddModuleSourceFileSets_ShouldReturnImmediatelyIfSourcesIsNull()
        throws Exception
    {
        this.phase.addModuleSourceFileSets( null, null, null, null );
    }

    @Test
    public void testAddModuleSourceFileSets_ShouldAddOneSourceDirectory()
        throws Exception
    {
        final MavenProject project = createProject( "group", "artifact", "version", null );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getFinalName() ).thenReturn( "final-name" );
        when( configSource.getProject() ).thenReturn( project );
        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "GROUPID" );
        project.setArtifact( artifact );

        final Set<MavenProject> projects = singleton( project );

        final FileSet fs = new FileSet();
        fs.setDirectory( "/src" );
        fs.setDirectoryMode( "777" );
        fs.setFileMode( "777" );

        final ModuleSources sources = new ModuleSources();
        sources.addFileSet( fs );

        // the logger sends a debug message with this info inside the addFileSet(..) method..
        final Archiver archiver = mock( Archiver.class );
        when( archiver.getOverrideDirectoryMode() ).thenReturn( -1 );
        when( archiver.getOverrideFileMode() ).thenReturn( -1 );
        
        DefaultAssemblyArchiverTest.setupInterpolators( configSource, project );

        when( logger.isDebugEnabled() ).thenReturn( true );

        this.phase.addModuleSourceFileSets( sources, projects, archiver,
                                                             configSource );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getArchiveBaseDirectory();
        verify( configSource, atLeastOnce() ).getFinalName();
        verify( configSource, atLeastOnce() ).getProject();
        verify( configSource, atLeastOnce() ).getMavenSession();

        verify( archiver ).getOverrideDirectoryMode();
        verify( archiver ).getOverrideFileMode();
    }

    @Test
    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsOnlyCurrentProject()
        throws Exception
    {
        final MavenProject project = createProject( "group", "artifact", "version", null );

        final List<MavenProject> projects = Collections.singletonList( project );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getProject() ).thenReturn( project );
        when( configSource.getReactorProjects() ).thenReturn( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getReactorProjects();
        verify( configSource, atLeastOnce() ).getProject();
    }

    @Test
    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsTwoSiblingProjects()
        throws Exception
    {
        final MavenProject project = createProject( "group", "artifact", "version", null );
        final MavenProject project2 = createProject( "group", "artifact2", "version", null );

        final List<MavenProject> projects = new ArrayList<>();
        projects.add( project );
        projects.add( project2 );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getReactorProjects() ).thenReturn( projects );
        when( configSource.getProject() ).thenReturn( project );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getReactorProjects();
        verify( configSource, atLeastOnce() ).getProject();
    }

    @Test
    public void testGetModuleProjects_ShouldReturnModuleOfCurrentProject()
        throws Exception
    {
        final MavenProject project = createProject( "group", "artifact", "version", null );
        final MavenProject project2 = createProject( "group", "artifact2", "version", project );

        final List<MavenProject> projects = new ArrayList<>();
        projects.add( project );
        projects.add( project2 );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getReactorProjects() ).thenReturn( projects );
        when( configSource.getProject() ).thenReturn( project );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, configSource, logger );

        assertFalse( moduleProjects.isEmpty() );

        final MavenProject result = moduleProjects.iterator().next();

        assertEquals( "artifact2", result.getArtifactId() );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getReactorProjects();
        verify( configSource, atLeastOnce() ).getProject();
    }

    @Test
    public void testGetModuleProjects_ShouldReturnDescendentModulesOfCurrentProject()
        throws Exception
    {
        final MavenProject project = createProject( "group", "artifact", "version", null );
        final MavenProject project2 = createProject( "group", "artifact2", "version", project );
        final MavenProject project3 = createProject( "group", "artifact3", "version", project2 );

        final List<MavenProject> projects = new ArrayList<>();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getReactorProjects() ).thenReturn( projects );
        when( configSource.getProject() ).thenReturn( project );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, configSource, logger );

        assertEquals( 2, moduleProjects.size() );

        final List<MavenProject> check = new ArrayList<>();
        check.add( project2 );
        check.add( project3 );

        verifyResultIs( check, moduleProjects );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getReactorProjects();
        verify( configSource, atLeastOnce() ).getProject();
    }

    @Test
    public void testGetModuleProjects_ShouldExcludeModuleAndDescendentsTransitively()
        throws Exception
    {
        final MavenProject project = createProject( "group", "artifact", "version", null );

        Artifact artifact1 = mock( Artifact.class );
        project.setArtifact( artifact1 );

        final MavenProject project2 = createProject( "group", "artifact2", "version", project );
        Artifact artifact2 = mock( Artifact.class );
        when( artifact2.getGroupId() ).thenReturn( "group" );
        when( artifact2.getArtifactId() ).thenReturn( "artifact2" );
        when( artifact2.getId() ).thenReturn( "group:artifact2:version:jar" );
        when( artifact2.getDependencyConflictId() ).thenReturn( "group:artifact2:jar" );
        project2.setArtifact( artifact2 );

        final MavenProject project3 = createProject( "group", "artifact3", "version", project2 );
        Artifact artifact3 = mock( Artifact.class );
        when( artifact3.getGroupId() ).thenReturn( "group" );
        when( artifact3.getArtifactId() ).thenReturn( "artifact3" );
        when( artifact3.getId() ).thenReturn( "group:artifact3:version:jar" );
        when( artifact3.getDependencyConflictId() ).thenReturn( "group:artifact3:jar" );
        when( artifact3.getDependencyTrail() ).thenReturn( Arrays.asList( project2.getId(), project.getId() ) );
        project3.setArtifact( artifact3 );

        final List<MavenProject> projects = new ArrayList<>();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getReactorProjects() ).thenReturn( projects );
        when( configSource.getProject() ).thenReturn( project );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        moduleSet.addExclude( "group:artifact2" );

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getReactorProjects();
        verify( configSource, atLeastOnce() ).getProject();
    }

    private void verifyResultIs( final List<MavenProject> check, final Set<MavenProject> moduleProjects )
    {
        boolean failed = false;

        final Set<MavenProject> checkTooMany = new HashSet<>( moduleProjects );
        checkTooMany.removeAll( check );

        if ( !checkTooMany.isEmpty() )
        {
            failed = true;

            System.out.println( "Unexpected projects in output: " );

            for ( final MavenProject project : checkTooMany )
            {
                System.out.println( project.getId() );
            }
        }

        final Set<MavenProject> checkTooFew = new HashSet<>( check );
        checkTooFew.removeAll( moduleProjects );

        if ( !checkTooFew.isEmpty() )
        {
            failed = true;

            System.out.println( "Expected projects missing from output: " );

            for ( final MavenProject project : checkTooMany )
            {
                System.out.println( project.getId() );
            }
        }

        if ( failed )
        {
            fail( "See system output for more information." );
        }
    }

    private MavenProject createProject( final String groupId, final String artifactId, final String version,
                                        final MavenProject parentProject )
    {
        final Model model = new Model();
        model.setArtifactId( artifactId );
        model.setGroupId( groupId );
        model.setVersion( version );

        final MavenProject project = new MavenProject( model );

        File pomFile;
        if ( parentProject == null )
        {
            final File basedir = temporaryFolder.getRoot();
            pomFile = new File( basedir, "pom.xml" );
        }
        else
        {
            final File parentBase = parentProject.getBasedir();
            pomFile = new File( parentBase, artifactId + "/pom.xml" );

            parentProject.getModel().addModule( artifactId );
            project.setParent( parentProject );
        }

        project.setFile( pomFile );

        return project;
    }
}
