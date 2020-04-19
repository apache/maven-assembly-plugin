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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.archive.task.testutils.MockAndControlForAddArtifactTask;
import org.apache.maven.plugins.assembly.archive.task.testutils.MockAndControlForAddDependencySetsTask;
import org.apache.maven.plugins.assembly.archive.task.testutils.MockAndControlForAddFileSetsTask;
import org.apache.maven.plugins.assembly.artifact.DependencyResolver;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ModuleSetAssemblyPhaseTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final Logger logger = new ConsoleLogger( Logger.LEVEL_INFO, "test" );

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchOutputDir()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setOutputDirectory( "outdir" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchInclude()
    {
        final ModuleSources sources = new ModuleSources();
        sources.addInclude( "**/included.txt" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldCatchExclude()
    {
        final ModuleSources sources = new ModuleSources();
        sources.addExclude( "**/excluded.txt" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertTrue( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldNotCatchFileMode()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setFileMode( "777" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertFalse( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testIsDeprecatedModuleSourcesConfigPresent_ShouldNotCatchDirMode()
    {
        final ModuleSources sources = new ModuleSources();
        sources.setDirectoryMode( "777" );

        final ModuleSetAssemblyPhase phase = createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null );

        assertFalse( phase.isDeprecatedModuleSourcesConfigPresent( sources ) );
    }

    @Test
    public void testCreateFileSet_ShouldUseModuleDirOnlyWhenOutDirIsNull()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final Model model = new Model();
        model.setArtifactId( "artifact" );

        final MavenProject project = new MavenProject( model );

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        macTask.expectGetFinalName( null );

        final FileSet fs = new FileSet();

        final ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        final File basedir = temporaryFolder.getRoot();

        final MavenProject artifactProject = new MavenProject( new Model() );
        artifactProject.setFile( new File( basedir, "pom.xml" ) );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getArtifactId() ).thenReturn( "artifact" );

        artifactProject.setArtifact( artifact );

        DefaultAssemblyArchiverTest.setupInterpolators( macTask.configSource );

        mm.replayAll();

        final FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources,
                                                                                                artifactProject,
                                                                                                macTask.configSource );

        assertEquals( "artifact/", result.getOutputDirectory() );

        mm.verifyAll();
    }

    @Test
    public void testCreateFileSet_ShouldPrependModuleDirWhenOutDirIsProvided()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final Model model = new Model();
        model.setArtifactId( "artifact" );

        final MavenProject project = new MavenProject( model );

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        macTask.expectGetFinalName( null );

        final FileSet fs = new FileSet();
        fs.setOutputDirectory( "out" );

        final ModuleSources sources = new ModuleSources();
        sources.setIncludeModuleDirectory( true );

        final MavenProject artifactProject = new MavenProject( new Model() );

        final File basedir = temporaryFolder.getRoot();

        artifactProject.setFile( new File( basedir, "pom.xml" ) );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getArtifactId() ).thenReturn( "artifact" );

        artifactProject.setArtifact( artifact );
        DefaultAssemblyArchiverTest.setupInterpolators( macTask.configSource );

        mm.replayAll();

        final FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources,
                                                                                                artifactProject,
                                                                                                macTask.configSource );

        assertEquals( "artifact/out/", result.getOutputDirectory() );

        mm.verifyAll();
    }

    @Test
    public void testCreateFileSet_ShouldAddExcludesForSubModulesWhenExcludeSubModDirsIsTrue()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, null );

        macTask.expectGetFinalName( null );

        final FileSet fs = new FileSet();

        final ModuleSources sources = new ModuleSources();
        sources.setExcludeSubModuleDirectories( true );

        final Model model = new Model();
        model.setArtifactId( "artifact" );

        model.addModule( "submodule" );

        final MavenProject project = new MavenProject( model );

        final File basedir = temporaryFolder.getRoot();

        project.setFile( new File( basedir, "pom.xml" ) );

        Artifact artifact = mock( Artifact.class );

        project.setArtifact( artifact );
        DefaultAssemblyArchiverTest.setupInterpolators( macTask.configSource );

        mm.replayAll();

        final FileSet result =
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).createFileSet( fs, sources, project,
                                                                                                macTask.configSource );

        assertEquals( 1, result.getExcludes().size() );
        assertEquals( "submodule/**", result.getExcludes().get( 0 ) );

        mm.verifyAll();
    }

    @Test
    public void testExecute_ShouldSkipIfNoModuleSetsFound()
        throws Exception
    {
        final Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        createPhase( null, null ).execute( assembly, null, null );
    }

    @Test
    public void testExecute_ShouldAddOneModuleSetWithOneModuleInIt()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, project );

        final MavenProject module = createProject( "group", "module", "version", project );

        Artifact artifact = mock( Artifact.class );
        final File moduleArtifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( moduleArtifactFile );
        module.setArtifact( artifact );

        final List<MavenProject> projects = new ArrayList<>();

        projects.add( module );

        macTask.expectGetReactorProjects( projects );
        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectGetMode( 0777, 0777 );

        final int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        macTask.expectAddFile( moduleArtifactFile, "out/artifact", mode );

        final Assembly assembly = new Assembly();
        assembly.setIncludeBaseDirectory( false );

        final ModuleSet ms = new ModuleSet();

        final ModuleBinaries bin = new ModuleBinaries();

        bin.setOutputFileNameMapping( "artifact" );
        bin.setOutputDirectory( "out" );
        bin.setFileMode( "777" );
        bin.setUnpack( false );
        bin.setIncludeDependencies( false );

        ms.setBinaries( bin );

        assembly.addModuleSet( ms );

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        macTask.expectResolveDependencySets();
        DefaultAssemblyArchiverTest.setupInterpolators( macTask.configSource );

        mm.replayAll();

        final ModuleSetAssemblyPhase phase = createPhase( logger, macTask.dependencyResolver, null );
        phase.execute( assembly, macTask.archiver, macTask.configSource );

        mm.verifyAll();
    }

    @Test
    public void testAddModuleBinaries_ShouldReturnImmediatelyWhenBinariesIsNull()
        throws Exception
    {
        createPhase( null, null ).addModuleBinaries( null, null, null, null, null, null );
    }

    @Test
    public void testAddModuleBinaries_ShouldFilterPomModule()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

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

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleBinaries( null, null, binaries,
                                                                                                projects,
                                                                                                macTask.archiver,
                                                                                                macTask.configSource );

        mm.verifyAll();
    }

    @Test
    public void testAddModuleBinaries_ShouldAddOneModuleAttachmentArtifactAndNoDeps()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm, null );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getClassifier() ).thenReturn( "test" );
        final File artifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( artifactFile );

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectGetMode( 0222, 0222 );
        macTask.expectAddFile( artifactFile, "out/artifact",
                               TypeConversionUtils.modeToInt( "777",
                                                              new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

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

        macTask.expectResolveDependencySets();
        DefaultAssemblyArchiverTest.setupInterpolators( macTask.configSource );

        mm.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, macTask.dependencyResolver, null ).addModuleBinaries( null, null, binaries, projects,
                                                                                   macTask.archiver,
                                                                                   macTask.configSource );

        mm.verifyAll();
    }

    @Test
    public void testAddModuleBinaries_ShouldFailWhenOneModuleDoesntHaveAttachmentWithMatchingClassifier()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );
        
        Artifact artifact = mock( Artifact.class );
        when( artifact.getClassifier() ).thenReturn( "test" );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setAttachmentClassifier( "test" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifact );

        final Set<MavenProject> projects = singleton( project );

        mm.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        try
        {
            createPhase( logger, null ).addModuleBinaries( null, null, binaries, projects, macTask.archiver,
                                                           macTask.configSource );

            fail( "Should throw an invalid configuration exception because of module with missing attachment." );
        }
        catch ( final InvalidAssemblerConfigurationException e )
        {
            assertEquals( "Cannot find attachment with classifier: test in module project: group:artifact:jar:version. "
                + "Please exclude this module from the module-set.", e.getMessage());
            // should throw this because of missing attachment.
        }

        mm.verifyAll();
    }

    @Test
    public void testAddModuleBinaries_ShouldAddOneModuleArtifactAndNoDeps()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        Artifact artifact = mock( Artifact.class );
        final File artifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( artifactFile );

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectAddFile( artifactFile, "out/artifact",
                               TypeConversionUtils.modeToInt( "777",
                                                              new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );
        macTask.expectGetMode( 0222, 0222 );

        final ModuleBinaries binaries = new ModuleBinaries();

        binaries.setIncludeDependencies( false );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifact );

        final Set<MavenProject> projects = singleton( project );

        macTask.expectResolveDependencySets();
        DefaultAssemblyArchiverTest.setupInterpolators( macTask.configSource );

        mm.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, macTask.dependencyResolver, null ).addModuleBinaries( null, null, binaries, projects,
                                                                                   macTask.archiver,
                                                                                   macTask.configSource );

        mm.verifyAll();
    }

    @Test
    public void testAddModuleArtifact_ShouldThrowExceptionWhenArtifactFileIsNull()
        throws Exception
    {
        Artifact artifact = mock( Artifact.class );
        try
        {
            createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ),
                         null ).addModuleArtifact( artifact, null, null, null, null );

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
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAddArtifactTask macTask = new MockAndControlForAddArtifactTask( mm );

        Artifact artifact = mock( Artifact.class );
        final File artifactFile = temporaryFolder.newFile();
        when( artifact.getFile() ).thenReturn( artifactFile );

        final MavenProject project = createProject( "group", "artifact", "version", null );
        project.setArtifact( artifact );

        macTask.expectGetFinalName( "final-name" );
        macTask.expectGetDestFile( new File( "junk" ) );
        macTask.expectGetMode( 0222, 0222 );

        macTask.expectAddFile( artifactFile, "out/artifact",
                               TypeConversionUtils.modeToInt( "777",
                                                              new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) ) );

        final ModuleBinaries binaries = new ModuleBinaries();
        binaries.setOutputDirectory( "out" );
        binaries.setOutputFileNameMapping( "artifact" );
        binaries.setUnpack( false );
        binaries.setFileMode( "777" );
        DefaultAssemblyArchiverTest.setupInterpolators( macTask.configSource );

        mm.replayAll();

        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ),
                     null ).addModuleArtifact( artifact, project, macTask.archiver,
                                               macTask.configSource, binaries );

        mm.verifyAll();
    }

    @Test
    public void testAddModuleSourceFileSets_ShouldReturnImmediatelyIfSourcesIsNull()
        throws Exception
    {
        createPhase( new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ), null ).addModuleSourceFileSets( null, null, null,
                                                                                                      null );
    }

    @Test
    public void testAddModuleSourceFileSets_ShouldAddOneSourceDirectory()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MockAndControlForAddFileSetsTask macTask = new MockAndControlForAddFileSetsTask( mm );

        final MavenProject project = createProject( "group", "artifact", "version", null );

        macTask.expectGetProject( project );

        project.setArtifact( mock( Artifact.class ) );

        final Set<MavenProject> projects = singleton( project );

        final ModuleSources sources = new ModuleSources();

        final FileSet fs = new FileSet();
        fs.setDirectory( "/src" );
        fs.setDirectoryMode( "777" );
        fs.setFileMode( "777" );

        sources.addFileSet( fs );

        macTask.expectGetArchiveBaseDirectory();

        final int mode = TypeConversionUtils.modeToInt( "777", new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        final int[] modes = { -1, -1, mode, mode };

        macTask.expectAdditionOfSingleFileSet( project, "final-name", false, modes, 1, true, false );
        DefaultAssemblyArchiverTest.setupInterpolators( macTask.configSource );

        mm.replayAll();

        final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

        createPhase( logger, null ).addModuleSourceFileSets( sources, projects, macTask.archiver,
                                                             macTask.configSource );

        mm.verifyAll();
    }

    @Test
    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsOnlyCurrentProject()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mm, project );

        final List<MavenProject> projects = Collections.singletonList( project );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    @Test
    public void testGetModuleProjects_ShouldReturnNothingWhenReactorContainsTwoSiblingProjects()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mm, project );

        final MavenProject project2 = createProject( "group", "artifact2", "version", null );

        final List<MavenProject> projects = new ArrayList<>();
        projects.add( project );
        projects.add( project2 );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
    }

    @Test
    public void testGetModuleProjects_ShouldReturnModuleOfCurrentProject()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mm, project );

        final MavenProject project2 = createProject( "group", "artifact2", "version", project );

        final List<MavenProject> projects = new ArrayList<>();
        projects.add( project );
        projects.add( project2 );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertFalse( moduleProjects.isEmpty() );

        final MavenProject result = moduleProjects.iterator().next();

        assertEquals( "artifact2", result.getArtifactId() );

        mm.verifyAll();
    }

    @Test
    public void testGetModuleProjects_ShouldReturnDescendentModulesOfCurrentProject()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mm, project );

        final MavenProject project2 = createProject( "group", "artifact2", "version", project );
        final MavenProject project3 = createProject( "group", "artifact3", "version", project2 );

        final List<MavenProject> projects = new ArrayList<>();
        projects.add( project );
        projects.add( project2 );
        projects.add( project3 );

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertEquals( 2, moduleProjects.size() );

        final List<MavenProject> check = new ArrayList<>();
        check.add( project2 );
        check.add( project3 );

        verifyResultIs( check, moduleProjects );

        mm.verifyAll();
    }

    @Test
    public void testGetModuleProjects_ShouldExcludeModuleAndDescendentsTransitively()
        throws Exception
    {
        final EasyMockSupport mm = new EasyMockSupport();

        final MavenProject project = createProject( "group", "artifact", "version", null );

        final MockAndControlForAddDependencySetsTask macTask =
            new MockAndControlForAddDependencySetsTask( mm, project );

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

        macTask.expectGetReactorProjects( projects );

        final ModuleSet moduleSet = new ModuleSet();
        moduleSet.setIncludeSubModules( true );

        moduleSet.addExclude( "group:artifact2" );

        mm.replayAll();

        final Set<MavenProject> moduleProjects =
            ModuleSetAssemblyPhase.getModuleProjects( moduleSet, macTask.configSource, logger );

        assertTrue( moduleProjects.isEmpty() );

        mm.verifyAll();
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

    private ModuleSetAssemblyPhase createPhase( final Logger logger,
                                                final MockAndControlForAddDependencySetsTask macTask )
    {
        ProjectBuilder projectBuilder = null;

        if ( macTask != null )
        {
            projectBuilder = macTask.projectBuilder;
        }

        DependencyResolver dr = EasyMock.createMock( DependencyResolver.class );
        return new ModuleSetAssemblyPhase( projectBuilder, dr, logger );
    }

    private ModuleSetAssemblyPhase createPhase( final Logger logger, DependencyResolver dr,
                                                ProjectBuilder projectBuilder1 )
    {
        return new ModuleSetAssemblyPhase( projectBuilder1, dr, logger );
    }
}
