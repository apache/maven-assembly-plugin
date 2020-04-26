package org.apache.maven.plugins.assembly.archive;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.mojos.AbstractAssemblyMojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.diags.NoOpArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
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
public class DefaultAssemblyArchiverTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PlexusContainer container;

    public static void setupInterpolators( AssemblerConfigurationSource configSource )
    {
        when( configSource.getRepositoryInterpolator() ).thenReturn( FixedStringSearchInterpolator.create() );
        when( configSource.getCommandLinePropsInterpolator() ).thenReturn( FixedStringSearchInterpolator.create() );
        when( configSource.getEnvInterpolator() ).thenReturn( FixedStringSearchInterpolator.create() );
    }

    public static void setupInterpolators( AssemblerConfigurationSource configSource, MavenProject mavenProject )
    {
        when( configSource.getCommandLinePropsInterpolator() ).thenReturn( FixedStringSearchInterpolator.create() );
        when( configSource.getEnvInterpolator() ).thenReturn( FixedStringSearchInterpolator.create() );
        when( configSource.getMainProjectInterpolator() ).thenReturn( AbstractAssemblyMojo.mainProjectInterpolator( mavenProject ) );
    }

    @Before
    public void setup()
        throws PlexusContainerException
    {
        this.container = new DefaultPlexusContainer();
    }

    @Test( expected = InvalidAssemblerConfigurationException.class )
    public void failWhenAssemblyIdIsNull()
        throws Exception
    {
        final DefaultAssemblyArchiver archiver = createSubject( null, null, null );
        archiver.createArchive( new Assembly(), "full-name", "zip", null, false, null, null );
    }

    @Test
    public void testCreateArchive()
        throws Exception
    {
        Archiver archiver = mock( Archiver.class );
        
        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "zip" ) ).thenReturn( archiver );

        final AssemblyArchiverPhase phase = mock( AssemblyArchiverPhase.class );

        final File outDir = temporaryFolder.newFolder( "out" );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getTemporaryRootDirectory() ).thenReturn( new File ( temporaryFolder.getRoot(), "temp" ) );
        when( configSource.getOverrideUid() ).thenReturn( 0 );
        when( configSource.getOverrideUserName() ).thenReturn( "root" );
        when( configSource.getOverrideGid() ).thenReturn( 0 );
        when( configSource.getOverrideGroupName() ).thenReturn( "root" );
        when( configSource.getOutputDirectory() ).thenReturn( outDir );
        when( configSource.getFinalName() ).thenReturn( "finalName" );
        when( configSource.getWorkingDirectory() ).thenReturn( new File( "." ) );

        final Assembly assembly = new Assembly();
        assembly.setId( "id" );

        final DefaultAssemblyArchiver subject = createSubject( archiverManager, Collections.singletonList( phase ), null );

        subject.createArchive( assembly, "full-name", "zip", configSource, false, null, null );
        
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getArchiverConfig();
        verify( configSource ).getFinalName();
        verify( configSource ).getOutputDirectory();
        verify( configSource, atLeastOnce() ).getOverrideUid();
        verify( configSource, atLeastOnce() ).getOverrideUserName();
        verify( configSource, atLeastOnce() ).getOverrideGid();
        verify( configSource, atLeastOnce() ).getOverrideGroupName();
        verify( configSource ).getTemporaryRootDirectory();
        verify( configSource ).getWorkingDirectory();
        verify( configSource ).isDryRun();
        verify( configSource ).isIgnoreDirFormatExtensions();
        verify( configSource ).isIgnorePermissions();
        verify( configSource, times( 2 ) ).isUpdateOnly();
        
        verify( phase ).execute( eq( assembly ), any( Archiver.class ), eq( configSource ) );

        verify( archiver ).createArchive();
        verify( archiver ).setDestFile( new File( outDir, "full-name.zip" ) );
        verify( archiver, times( 2 ) ).setForced( true );
        verify( archiver ).setIgnorePermissions( false );
        verify( archiver ).setOverrideUid( 0 );
        verify( archiver ).setOverrideUserName( "root" );
        verify( archiver ).setOverrideGid( 0 );
        verify( archiver ).setOverrideGroupName( "root" );
        
        verify( archiverManager ).getArchiver( "zip" );
    }

    @Test
    public void testCreateArchiver_ShouldConfigureArchiver()
        throws Exception
    {
        final TestArchiverWithConfig archiver = new TestArchiverWithConfig();

        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "dummy" ) ).thenReturn( archiver );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );

        final String simpleConfig = "value";

        when( configSource.getArchiverConfig() ).thenReturn(
            "<configuration><simpleConfig>" + simpleConfig + "</simpleConfig></configuration>" );

        final MavenProject project = new MavenProject( new Model() );

        when( configSource.getProject() ).thenReturn( project );
        when( configSource.getWorkingDirectory() ).thenReturn( new File( "." ) );

        when( configSource.isIgnorePermissions() ).thenReturn( true );
        setupInterpolators( configSource );

        when( configSource.getOverrideUid() ).thenReturn( 0 );
        when( configSource.getOverrideUserName() ).thenReturn( "root" );
        when( configSource.getOverrideGid() ).thenReturn( 0 );
        when( configSource.getOverrideGroupName() ).thenReturn( "root" );

        final DefaultAssemblyArchiver subject =
            createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "dummy", false, "finalName", configSource, null, false, null, null );

        assertEquals( simpleConfig, archiver.getSimpleConfig() );
        
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "dummy" );
    }

    @Test
    public void testCreateArchiver_ShouldCreateTarArchiverWithNoCompression()
        throws Exception
    {
        final TestTarArchiver ttArchiver = new TestTarArchiver();

        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( ttArchiver );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getTarLongFileMode() ).thenReturn( TarLongFileMode.fail.toString() );
        when( configSource.getWorkingDirectory() ).thenReturn( new File( "." ) );
        when( configSource.isIgnorePermissions() ).thenReturn( true );
        when( configSource.getOverrideUid() ).thenReturn( 0 );
        when( configSource.getOverrideUserName() ).thenReturn( "root" );
        when( configSource.getOverrideGid() ).thenReturn( 0 );
        when( configSource.getOverrideGroupName() ).thenReturn( "root" );

        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "tar", false, "finalName", configSource, null, false, null, null );

        assertNull( ttArchiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, ttArchiver.longFileMode );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getArchiverConfig();
        verify( configSource, times( 2 ) ).getOverrideGid();
        verify( configSource, times( 2 ) ).getOverrideGroupName();
        verify( configSource, times( 2 ) ).getOverrideUid();
        verify( configSource, times( 2 ) ).getOverrideUserName();
        verify( configSource ).getTarLongFileMode();
        verify( configSource ).getWorkingDirectory();
        verify( configSource ).isDryRun();
        verify( configSource ).isIgnorePermissions();
        verify( configSource, times( 2 ) ).isUpdateOnly();

        verify( archiverManager ).getArchiver( "tar" );
    }

    @Test
    public void testCreateArchiver_ShouldCreateWarArchiverWithIgnoreWebxmlSetToFalse()
        throws Exception
    {
        final TestWarArchiver twArchiver = new TestWarArchiver();

        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "war" ) ).thenReturn( twArchiver );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getOverrideGid() ).thenReturn( 0 );
        when( configSource.getOverrideGroupName() ).thenReturn( "root" );
        when( configSource.getOverrideUid() ).thenReturn( 0 );
        when( configSource.getOverrideUserName() ).thenReturn( "root" );
        when( configSource.getProject() ).thenReturn( new MavenProject( new Model() ) );
        when( configSource.getWorkingDirectory() ).thenReturn( new File( "." ) );
        when( configSource.isIgnorePermissions() ).thenReturn( true );
        
        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "war", false, null, configSource, null, false, null, null );

        assertFalse( twArchiver.ignoreWebxml );
        
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getArchiverConfig();
        verify( configSource ).getJarArchiveConfiguration();
        verify( configSource ).getMavenSession();
        verify( configSource, times( 2 ) ).getOverrideGid();
        verify( configSource, times( 2 ) ).getOverrideGroupName();
        verify( configSource, times( 2 ) ).getOverrideUid();
        verify( configSource, times( 2 ) ).getOverrideUserName();
        verify( configSource ).getProject();
        verify( configSource ).getWorkingDirectory();
        verify( configSource ).isDryRun();
        verify( configSource ).isIgnorePermissions();
        verify( configSource, times( 2 ) ).isUpdateOnly();
        
        verify( archiverManager ).getArchiver( "war" );
    }

    @Test
    public void testCreateArchiver_ShouldCreateZipArchiver()
        throws Exception
    {
        final ZipArchiver archiver = new ZipArchiver();

        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "zip" ) ).thenReturn( archiver );

        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.getOverrideGid() ).thenReturn( 0 );
        when( configSource.getOverrideGroupName() ).thenReturn( "root" );
        when( configSource.getOverrideUid() ).thenReturn( 0 );
        when( configSource.getOverrideUserName() ).thenReturn( "root" );
        when( configSource.getWorkingDirectory() ).thenReturn( new File( "." ) );
        when( configSource.isIgnorePermissions() ).thenReturn( true );

        final DefaultAssemblyArchiver subject =
            createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createArchiver( "zip", false, null, configSource, null, false, null, null );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( configSource ).getArchiverConfig();
        verify( configSource, times( 2 ) ).getOverrideGid();
        verify( configSource, times( 2 ) ).getOverrideGroupName();
        verify( configSource, times( 2 ) ).getOverrideUid();
        verify( configSource, times( 2 ) ).getOverrideUserName();
        verify( configSource ).getWorkingDirectory();
        verify( configSource ).isDryRun();
        verify( configSource ).isIgnorePermissions();
        verify( configSource, times( 2 ) ).isUpdateOnly();
        
        verify( archiverManager ).getArchiver( "zip" );
    }

    @Test
    public void testCreateWarArchiver_ShouldDisableIgnoreWebxmlOption()
        throws Exception
    {
        final TestWarArchiver twArchiver = new TestWarArchiver();

        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "war" ) ).thenReturn( twArchiver );

        final DefaultAssemblyArchiver subject =
            createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createWarArchiver();

        assertFalse( twArchiver.ignoreWebxml );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "war" );
    }

    @Test
    public void testCreateTarArchiver_ShouldNotInitializeCompression()
        throws Exception
    {
        final TestTarArchiver archiver = new TestTarArchiver();
        
        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( archiver );

        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tar", TarLongFileMode.fail );

        assertNull( new TestTarArchiver().compressionMethod );
        assertEquals( TarLongFileMode.fail, archiver.longFileMode );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "tar" );
    }

    @Test
    public void testCreateTarArchiver_TarGzFormat_ShouldInitializeGZipCompression()
        throws Exception
    {
        final TestTarArchiver archiver = new TestTarArchiver();
        
        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( archiver );
        
        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tar.gz", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.gzip, archiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, archiver.longFileMode );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "tar" );
    }

    @Test
    public void testCreateTarArchiver_TgzFormat_ShouldInitializeGZipCompression()
        throws Exception
    {
        final TestTarArchiver archiver = new TestTarArchiver();
        
        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( archiver );
        
        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tgz", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.gzip, archiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, archiver.longFileMode );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "tar" );
    }

    @Test
    public void testCreateTarArchiver_TarBz2Format_ShouldInitializeBZipCompression()
        throws Exception
    {
        final TestTarArchiver archiver = new TestTarArchiver();
        
        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( archiver );

        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tar.bz2", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.bzip2, archiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, archiver.longFileMode );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "tar" );
    }

    @Test
    public void testCreateTarArchiver_Tbz2Format_ShouldInitializeBZipCompression()
        throws Exception
    {
        final TestTarArchiver archiver = new TestTarArchiver();
        
        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( archiver );

        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tbz2", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.bzip2, archiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, archiver.longFileMode );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "tar" );
    }

    @Test
    public void testCreateTarArchiver_TarXzFormat_ShouldInitializeXzCompression()
        throws Exception
    {
        final TestTarArchiver archiver = new TestTarArchiver();

        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( archiver );
        
        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "tar.xz", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.xz, archiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, archiver.longFileMode );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "tar" );
    }

    @Test
    public void testCreateTarArchiver_TXzFormat_ShouldInitializeXzCompression()
        throws Exception
    {
        final TestTarArchiver archiver = new TestTarArchiver();
        
        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( archiver );

        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        subject.createTarArchiver( "txz", TarLongFileMode.fail );

        assertEquals( TarArchiver.TarCompressionMethod.xz, archiver.compressionMethod );
        assertEquals( TarLongFileMode.fail, archiver.longFileMode );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "tar" );
    }

    @Test
    public void testCreateTarArchiver_InvalidFormat_ShouldFailWithInvalidCompression()
        throws Exception
    {
        final TestTarArchiver ttArchiver = new TestTarArchiver();
        
        final ArchiverManager archiverManager = mock( ArchiverManager.class );
        when( archiverManager.getArchiver( "tar" ) ).thenReturn( ttArchiver );

        final DefaultAssemblyArchiver subject = createSubject( archiverManager, new ArrayList<AssemblyArchiverPhase>(), null );

        try
        {
            subject.createTarArchiver( "tar.Z", null );

            fail( "Invalid compression formats should throw an error." );
        }
        catch ( final IllegalArgumentException e )
        {
            // expected.
        }
        
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( archiverManager ).getArchiver( "tar" );
    }

    private DefaultAssemblyArchiver createSubject( final ArchiverManager archiverManager,
                                                   final List<AssemblyArchiverPhase> phases, Logger logger )
    {
        final DefaultAssemblyArchiver subject = new DefaultAssemblyArchiver( archiverManager, phases );

        subject.setContainer( container );

        if ( logger == null )
        {
            logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );
        }

        subject.enableLogging( logger );

        return subject;
    }

    private static final class TestTarArchiver
        extends TarArchiver
    {

        TarCompressionMethod compressionMethod;

        TarLongFileMode longFileMode;

        @Override
        protected void execute()
            throws ArchiverException, IOException
        {
            super.createArchive();
        }

        @Override
        public void setCompression( final TarCompressionMethod mode )
        {
            compressionMethod = mode;
            super.setCompression( mode );
        }

        @Override
        public void setLongfile( final TarLongFileMode mode )
        {
            longFileMode = mode;
            super.setLongfile( mode );
        }

    }

    private static final class TestWarArchiver
        extends WarArchiver
    {

        boolean ignoreWebxml;

        @Override
        public void setIgnoreWebxml( final boolean ignore )
        {
            ignoreWebxml = ignore;
            super.setIgnoreWebxml( ignore );
        }

    }

    public static final class TestArchiverWithConfig
        extends NoOpArchiver
    {

        private String simpleConfig;

        public String getSimpleConfig()
        {
            return simpleConfig;
        }


        public String getDuplicateBehavior()
        {
            return Archiver.DUPLICATES_ADD;
        }
    }

}
