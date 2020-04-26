package org.apache.maven.plugins.assembly.io;

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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.interpolation.AssemblyInterpolator;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Component;
import org.apache.maven.plugins.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Writer;
import org.apache.maven.plugins.assembly.model.io.xpp3.ComponentXpp3Reader;
import org.apache.maven.plugins.assembly.model.io.xpp3.ComponentXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.InterpolationState;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class DefaultAssemblyReaderTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private AssemblerConfigurationSource configSource;

    public static StringReader writeToStringReader( Assembly assembly )
        throws IOException
    {
        final StringWriter sw = new StringWriter();
        final AssemblyXpp3Writer assemblyWriter = new AssemblyXpp3Writer();

        assemblyWriter.write( sw, assembly );

        return new StringReader( sw.toString() );
    }

    @Before
    public void setUp()
    {
        configSource = mock( AssemblerConfigurationSource.class );
    }

    @Test
    public void testIncludeSiteInAssembly_ShouldFailIfSiteDirectoryNonExistent()
        throws Exception
    {
        final File siteDir = File.createTempFile( "assembly-reader.", ".test" );
        siteDir.delete();

        when( configSource.getSiteDirectory() ).thenReturn( siteDir );

        final Assembly assembly = new Assembly();

        try
        {
            new DefaultAssemblyReader().includeSiteInAssembly( assembly, configSource );

            fail( "Should fail when site directory is non-existent." );
        }
        catch ( final InvalidAssemblerConfigurationException e )
        {
            // this should happen.
        }
    }

    @Test
    public void testIncludeSiteInAssembly_ShouldAddSiteDirFileSetWhenDirExists()
        throws Exception
    {
        final File siteDir = temporaryFolder.getRoot();

        when( configSource.getSiteDirectory() ).thenReturn( siteDir );

        final Assembly assembly = new Assembly();

        new DefaultAssemblyReader().includeSiteInAssembly( assembly, configSource );

        final List<FileSet> fileSets = assembly.getFileSets();

        assertNotNull( fileSets );
        assertEquals( 1, fileSets.size() );

        final FileSet fs = fileSets.get( 0 );

        assertEquals( siteDir.getPath(), fs.getDirectory() );
    }

    @Test
    public void testMergeComponentWithAssembly_ShouldAddOneFileSetToExistingListOfTwo()
    {
        final Assembly assembly = new Assembly();

        FileSet fs = new FileSet();
        fs.setDirectory( "/dir" );

        assembly.addFileSet( fs );

        fs = new FileSet();
        fs.setDirectory( "/other-dir" );
        assembly.addFileSet( fs );

        fs = new FileSet();
        fs.setDirectory( "/third-dir" );

        final Component component = new Component();

        component.addFileSet( fs );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        final List<FileSet> fileSets = assembly.getFileSets();

        assertNotNull( fileSets );
        assertEquals( 3, fileSets.size() );

        final FileSet rfs1 = fileSets.get( 0 );
        assertEquals( "/dir", rfs1.getDirectory() );

        final FileSet rfs2 = fileSets.get( 1 );
        assertEquals( "/other-dir", rfs2.getDirectory() );

        final FileSet rfs3 = fileSets.get( 2 );
        assertEquals( "/third-dir", rfs3.getDirectory() );

    }

    @Test
    public void testMergeComponentWithAssembly_ShouldAddOneFileItemToExistingListOfTwo()
    {
        final Assembly assembly = new Assembly();

        FileItem fi = new FileItem();
        fi.setSource( "file" );

        assembly.addFile( fi );

        fi = new FileItem();
        fi.setSource( "file2" );

        assembly.addFile( fi );

        fi = new FileItem();
        fi.setSource( "file3" );

        final Component component = new Component();

        component.addFile( fi );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        final List<FileItem> fileItems = assembly.getFiles();

        assertNotNull( fileItems );
        assertEquals( 3, fileItems.size() );

        final FileItem rf1 = fileItems.get( 0 );
        assertEquals( "file", rf1.getSource() );

        final FileItem rf2 = fileItems.get( 1 );
        assertEquals( "file2", rf2.getSource() );

        final FileItem rf3 = fileItems.get( 2 );
        assertEquals( "file3", rf3.getSource() );

    }

    @Test
    public void testMergeComponentWithAssembly_ShouldAddOneDependencySetToExistingListOfTwo()
    {
        final Assembly assembly = new Assembly();

        DependencySet ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_RUNTIME );

        assembly.addDependencySet( ds );

        ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_COMPILE );

        assembly.addDependencySet( ds );

        final Component component = new Component();

        ds = new DependencySet();
        ds.setScope( Artifact.SCOPE_SYSTEM );

        component.addDependencySet( ds );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        final List<DependencySet> depSets = assembly.getDependencySets();

        assertNotNull( depSets );
        assertEquals( 3, depSets.size() );

        assertEquals( Artifact.SCOPE_RUNTIME, depSets.get( 0 ).getScope() );
        assertEquals( Artifact.SCOPE_COMPILE, depSets.get( 1 ).getScope() );
        assertEquals( Artifact.SCOPE_SYSTEM, depSets.get( 2 ).getScope() );
    }

    @Test
    public void testMergeComponentWithAssembly_ShouldAddOneRepositoryToExistingListOfTwo()
    {
        final Assembly assembly = new Assembly();

        Repository repo = new Repository();
        repo.setScope( Artifact.SCOPE_RUNTIME );

        assembly.addRepository( repo );

        repo = new Repository();
        repo.setScope( Artifact.SCOPE_COMPILE );

        assembly.addRepository( repo );

        final Component component = new Component();

        repo = new Repository();
        repo.setScope( Artifact.SCOPE_SYSTEM );

        component.addRepository( repo );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        final List<Repository> depSets = assembly.getRepositories();

        assertNotNull( depSets );
        assertEquals( 3, depSets.size() );

        assertEquals( Artifact.SCOPE_RUNTIME, depSets.get( 0 ).getScope() );
        assertEquals( Artifact.SCOPE_COMPILE, depSets.get( 1 ).getScope() );
        assertEquals( Artifact.SCOPE_SYSTEM, depSets.get( 2 ).getScope() );
    }

    @Test
    public void testMergeComponentWithAssembly_ShouldAddOneContainerDescriptorHandlerToExistingListOfTwo()
    {
        final Assembly assembly = new Assembly();

        ContainerDescriptorHandlerConfig cfg = new ContainerDescriptorHandlerConfig();
        cfg.setHandlerName( "one" );

        assembly.addContainerDescriptorHandler( cfg );

        cfg = new ContainerDescriptorHandlerConfig();
        cfg.setHandlerName( "two" );

        assembly.addContainerDescriptorHandler( cfg );

        final Component component = new Component();

        cfg = new ContainerDescriptorHandlerConfig();
        cfg.setHandlerName( "three" );

        component.addContainerDescriptorHandler( cfg );

        new DefaultAssemblyReader().mergeComponentWithAssembly( component, assembly );

        final List<ContainerDescriptorHandlerConfig> result = assembly.getContainerDescriptorHandlers();

        assertNotNull( result );
        assertEquals( 3, result.size() );

        final Iterator<ContainerDescriptorHandlerConfig> it = result.iterator();
        assertEquals( "one", it.next().getHandlerName() );
        assertEquals( "two", it.next().getHandlerName() );
        assertEquals( "three", it.next().getHandlerName() );
    }

    @Test
    public void testMergeComponentsWithMainAssembly_ShouldAddOneFileSetToAssembly()
        throws Exception
    {
        final Component component = new Component();

        final FileSet fileSet = new FileSet();
        fileSet.setDirectory( "/dir" );

        component.addFileSet( fileSet );

        final File componentFile = temporaryFolder.newFile();

        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( componentFile ), "UTF-8" ) )
        {
            final ComponentXpp3Writer componentWriter = new ComponentXpp3Writer();

            componentWriter.write( writer, component );
        }

        final String filename = componentFile.getName();

        final Assembly assembly = new Assembly();
        assembly.addComponentDescriptor( filename );

        final File basedir = componentFile.getParentFile();

        final MavenProject project = new MavenProject();

        when( configSource.getProject() ).thenReturn( project );
        when( configSource.getBasedir() ).thenReturn( basedir );
        DefaultAssemblyArchiverTest.setupInterpolators( configSource );
        InterpolationState is = new InterpolationState();
        ComponentXpp3Reader.ContentTransformer componentIp =
            AssemblyInterpolator.componentInterpolator( FixedStringSearchInterpolator.create(), is,
                                                        new ConsoleLogger( Logger.LEVEL_DEBUG, "console" ) );

        new DefaultAssemblyReader().mergeComponentsWithMainAssembly( assembly, null, configSource, componentIp );

        final List<FileSet> fileSets = assembly.getFileSets();

        assertNotNull( fileSets );
        assertEquals( 1, fileSets.size() );

        final FileSet fs = fileSets.get( 0 );

        assertEquals( "/dir", fs.getDirectory() );
    }

    @Test
    public void testReadAssembly_ShouldReadAssemblyWithoutComponentsInterpolationOrSiteDirInclusion()
        throws Exception
    {
        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        final Assembly result = doReadAssembly( assembly );

        assertEquals( assembly.getId(), result.getId() );
    }

    @Test
    public void testReadAssembly_ShouldReadAssemblyWithSiteDirInclusionFromAssemblyWithoutComponentsOrInterpolation()
        throws Exception
    {
        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        assembly.setIncludeSiteDirectory( true );

        final StringReader sr = writeToStringReader( assembly );

        final File siteDir = temporaryFolder.newFolder( "site" );

        when( configSource.getSiteDirectory() ).thenReturn( siteDir );

        final File basedir = temporaryFolder.getRoot();

        when( configSource.getBasedir() ).thenReturn( basedir );

        final Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        final MavenProject project = new MavenProject( model );

        when( configSource.getProject() ).thenReturn( project );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource );


        final Assembly result = new DefaultAssemblyReader().readAssembly( sr, "testLocation", null, configSource );

        assertEquals( assembly.getId(), result.getId() );

        final List<FileSet> fileSets = result.getFileSets();

        assertEquals( 1, fileSets.size() );

        assertEquals( "/site", fileSets.get( 0 ).getOutputDirectory() );
    }

    @Test
    public void testReadAssembly_ShouldReadAssemblyWithComponentWithoutSiteDirInclusionOrInterpolation()
        throws Exception
    {
        final File componentsFile = temporaryFolder.newFile();

        final File basedir = componentsFile.getParentFile();
        final String componentsFilename = componentsFile.getName();

        final Component component = new Component();

        final FileSet fs = new FileSet();
        fs.setDirectory( "/dir" );

        component.addFileSet( fs );

        try ( Writer fw = new OutputStreamWriter( new FileOutputStream( componentsFile ), "UTF-8" ) )
        {
            new ComponentXpp3Writer().write( fw, component );
        }

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        assembly.addComponentDescriptor( componentsFilename );

        final StringReader sr = writeToStringReader( assembly );

        when( configSource.getBasedir() ).thenReturn( basedir );

        final Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        final MavenProject project = new MavenProject( model );
        when( configSource.getProject() ).thenReturn( project );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource );

        final Assembly result = new DefaultAssemblyReader().readAssembly( sr, "testLocation", null, configSource );

        assertEquals( assembly.getId(), result.getId() );

        final List<FileSet> fileSets = result.getFileSets();

        assertEquals( 1, fileSets.size() );

        assertEquals( "/dir", fileSets.get( 0 ).getDirectory() );
    }

    @Test
    public void testReadAssembly_ShouldReadAssemblyWithComponentInterpolationWithoutSiteDirInclusionOrAssemblyInterpolation()
        throws Exception
    {
        final File componentsFile = temporaryFolder.newFile();

        final File basedir = componentsFile.getParentFile();
        final String componentsFilename = componentsFile.getName();

        final Component component = new Component();

        final FileSet fs = new FileSet();
        fs.setDirectory( "${groupId}-dir" );

        component.addFileSet( fs );

        try( Writer fw = new OutputStreamWriter( new FileOutputStream( componentsFile ), "UTF-8" ) )
        {
            new ComponentXpp3Writer().write( fw, component );
        }

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        assembly.addComponentDescriptor( componentsFilename );

        final StringReader sr = writeToStringReader( assembly );

        when( configSource.getBasedir() ).thenReturn( basedir );

        final Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        final MavenProject project = new MavenProject( model );

        when( configSource.getProject() ).thenReturn( project );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource );

        final Assembly result = new DefaultAssemblyReader().readAssembly( sr, "testLocation", null, configSource );

        assertEquals( assembly.getId(), result.getId() );

        final List<FileSet> fileSets = result.getFileSets();

        assertEquals( 1, fileSets.size() );

        assertEquals( "group-dir", fileSets.get( 0 ).getDirectory() );
    }

    @Test
    public void testReadAssembly_ShouldReadAssemblyWithInterpolationWithoutComponentsOrSiteDirInclusion()
        throws Exception
    {
        final Assembly assembly = new Assembly();
        assembly.setId( "${groupId}-assembly" );

        final Assembly result = doReadAssembly( assembly );

        assertEquals( "group-assembly", result.getId() );
    }

    private Assembly doReadAssembly( Assembly assembly )
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        final StringReader sr = writeToStringReader( assembly );

        final File basedir = temporaryFolder.getRoot();

        when( configSource.getBasedir() ).thenReturn( basedir );

        final Model model = new Model();
        model.setGroupId( "group" );
        model.setArtifactId( "artifact" );
        model.setVersion( "version" );

        final MavenProject project = new MavenProject( model );

        when( configSource.getProject() ).thenReturn( project );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource );

        return new DefaultAssemblyReader().readAssembly( sr, "testLocation", null, configSource );
    }

    @Test
    public void testGetAssemblyFromDescriptorFile_ShouldReadAssembly()
        throws Exception
    {
        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        final FileSet fs = new FileSet();
        fs.setDirectory( "/dir" );

        assembly.addFileSet( fs );

        final File assemblyFile = temporaryFolder.newFile();

        final File basedir = assemblyFile.getParentFile();

        when( configSource.getBasedir() ).thenReturn( basedir );

        when( configSource.getProject() ).thenReturn( new MavenProject( new Model() ) );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource );

        try ( Writer writer = new OutputStreamWriter( new FileOutputStream( assemblyFile ), "UTF-8" ) )
        {
            new AssemblyXpp3Writer().write( writer, assembly );
        }

        final Assembly result = new DefaultAssemblyReader().getAssemblyFromDescriptorFile( assemblyFile, configSource );

        assertEquals( assembly.getId(), result.getId() );
    }

    @Test
    public void testGetAssemblyForDescriptorReference_ShouldReadBinaryAssemblyRef()
        throws Exception
    {
        final File basedir = temporaryFolder.getRoot();

        when( configSource.getBasedir() ).thenReturn( basedir );

        when( configSource.getProject() ).thenReturn( new MavenProject( new Model() ) );

        DefaultAssemblyArchiverTest.setupInterpolators( configSource );

        final Assembly result = new DefaultAssemblyReader().getAssemblyForDescriptorReference( "bin", configSource );

        assertEquals( "bin", result.getId() );
    }

    @Test
    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromSingleFile()
        throws Exception
    {
        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        final FileSet fs = new FileSet();
        fs.setDirectory( "/dir" );

        assembly.addFileSet( fs );

        final File basedir = temporaryFolder.getRoot();

        final List<String> files = writeAssembliesToFile( Collections.singletonList( assembly ), basedir );

        final String assemblyFile = files.get( 0 );

        final List<Assembly> assemblies = performReadAssemblies( basedir, new String[] { assemblyFile }, null, null );

        assertNotNull( assemblies );
        assertEquals( 1, assemblies.size() );

        final Assembly result = assemblies.get( 0 );

        assertEquals( assembly.getId(), result.getId() );
    }

    @Test
    public void testReadAssemblies_ShouldFailWhenSingleDescriptorFileMissing()
        throws Exception
    {
        final File basedir = temporaryFolder.getRoot();

        try
        {
            performReadAssemblies( basedir, null, null, null, false );

            fail( "Should fail when descriptor file is missing and ignoreDescriptors == false" );
        }
        catch ( final AssemblyReadException e )
        {
            // expected.
        }
    }

    @Test
    public void testReadAssemblies_ShouldIgnoreMissingSingleDescriptorFileWhenIgnoreIsConfigured()
        throws Exception
    {
        final File basedir = temporaryFolder.getRoot();

        try
        {
            performReadAssemblies( basedir, null, null, null, true );
        }
        catch ( final AssemblyReadException e )
        {
            fail( "Setting ignoreMissingDescriptor == true (true flag in performReadAssemblies, above) should NOT "
                      + "produce an exception." );
        }
    }

    @Test
    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromFileArray()
        throws Exception
    {
        final Assembly assembly1 = new Assembly();
        assembly1.setId( "test" );

        final Assembly assembly2 = new Assembly();
        assembly2.setId( "test2" );

        final List<Assembly> assemblies = new ArrayList<>();
        assemblies.add( assembly1 );
        assemblies.add( assembly2 );

        final File basedir = temporaryFolder.getRoot();

        final List<String> files = writeAssembliesToFile( assemblies, basedir );

        final List<Assembly> results =
            performReadAssemblies( basedir, files.toArray( new String[files.size()] ), null, null );

        assertNotNull( results );
        assertEquals( 2, results.size() );

        final Assembly result1 = assemblies.get( 0 );

        assertEquals( assembly1.getId(), result1.getId() );

        final Assembly result2 = assemblies.get( 1 );

        assertEquals( assembly2.getId(), result2.getId() );
    }

    @Test
    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromMultipleRefs()
        throws Exception
    {
        final File basedir = temporaryFolder.getRoot();

        final List<Assembly> assemblies =
            performReadAssemblies( basedir, null, new String[]{ "bin", "src" }, null );

        assertNotNull( assemblies );
        assertEquals( 2, assemblies.size() );

        final Assembly result = assemblies.get( 0 );

        assertEquals( "bin", result.getId() );

        final Assembly result2 = assemblies.get( 1 );

        assertEquals( "src", result2.getId() );
    }

    @Test
    public void testReadAssemblies_ShouldGetAssemblyDescriptorFromDirectory()
        throws Exception
    {
        final Assembly assembly1 = new Assembly();
        assembly1.setId( "test" );

        final Assembly assembly2 = new Assembly();
        assembly2.setId( "test2" );

        final List<Assembly> assemblies = new ArrayList<>();
        assemblies.add( assembly1 );
        assemblies.add( assembly2 );

        final File basedir = temporaryFolder.getRoot();

        writeAssembliesToFile( assemblies, basedir );

        final List<Assembly> results = performReadAssemblies( basedir, null, null, basedir );

        assertNotNull( results );
        assertEquals( 2, results.size() );

        final Assembly result1 = assemblies.get( 0 );

        assertEquals( assembly1.getId(), result1.getId() );

        final Assembly result2 = assemblies.get( 1 );

        assertEquals( assembly2.getId(), result2.getId() );
    }

    @Test
    public void testReadAssemblies_ShouldGetTwoAssemblyDescriptorsFromDirectoryWithThreeFiles()
        throws Exception
    {
        final Assembly assembly1 = new Assembly();
        assembly1.setId( "test" );

        final Assembly assembly2 = new Assembly();
        assembly2.setId( "test2" );

        final List<Assembly> assemblies = new ArrayList<>();
        assemblies.add( assembly1 );
        assemblies.add( assembly2 );

        final File basedir = temporaryFolder.getRoot();

        writeAssembliesToFile( assemblies, basedir );

        Files.write( basedir.toPath().resolve( "readme.txt" ),
                     Arrays.asList( "This is just a readme file, not a descriptor." ), 
                     StandardCharsets.UTF_8 );

        final List<Assembly> results = performReadAssemblies( basedir, null, null, basedir );

        assertNotNull( results );
        assertEquals( 2, results.size() );

        final Assembly result1 = assemblies.get( 0 );

        assertEquals( assembly1.getId(), result1.getId() );

        final Assembly result2 = assemblies.get( 1 );

        assertEquals( assembly2.getId(), result2.getId() );
    }

    private List<String> writeAssembliesToFile( final List<Assembly> assemblies, final File dir )
        throws IOException
    {
        final List<String> files = new ArrayList<>();

        for ( final Assembly assembly : assemblies )
        {
            final File assemblyFile = new File( dir, assembly.getId() + ".xml" );

            try ( Writer writer = new OutputStreamWriter( new FileOutputStream( assemblyFile ), "UTF-8" ) )
            {
                new AssemblyXpp3Writer().write( writer, assembly );
            }

            files.add( assemblyFile.getAbsolutePath() );
        }

        return files;
    }

    private List<Assembly> performReadAssemblies( final File basedir, final String[] descriptors,
                                                  final String[] descriptorRefs, final File descriptorDir )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        return performReadAssemblies( basedir, descriptors, descriptorRefs, descriptorDir, false );
    }

    private List<Assembly> performReadAssemblies( final File basedir, final String[] descriptors,
                                                  final String[] descriptorRefs, final File descriptorDir,
                                                  final boolean ignoreMissing )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        when( configSource.getDescriptorReferences() ).thenReturn( descriptorRefs );

        when( configSource.getDescriptors() ).thenReturn( descriptors );

        when( configSource.getDescriptorSourceDirectory() ).thenReturn( descriptorDir );

        when( configSource.getBasedir() ).thenReturn( basedir ); //.atLeastOnce();

        if ( descriptors == null && descriptorRefs == null && descriptorDir == null )
        {
            when( configSource.isIgnoreMissingDescriptor() ).thenReturn( ignoreMissing ); //.atLeastOnce();
        }
        
        if ( !ignoreMissing )
        {
            when( configSource.getProject() ).thenReturn( new MavenProject( new Model() ) ); //.atLeastOnce();

            DefaultAssemblyArchiverTest.setupInterpolators( configSource );
        }

        return new DefaultAssemblyReader().readAssemblies( configSource );
    }

}
