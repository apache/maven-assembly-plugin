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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class FileItemAssemblyPhaseTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @Test
    public void testExecute_ShouldAddNothingWhenNoFileItemsArePresent()
        throws Exception
    {
        final AssemblerConfigurationSource macCS = mock( AssemblerConfigurationSource.class );

        final File basedir = temporaryFolder.getRoot();

        when( macCS.getBasedir()).thenReturn( basedir );

        final Logger macLogger = mock( Logger.class );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        createPhase( macLogger ).execute( assembly, null, macCS );

        verify( macCS ).getBasedir();
    }

    @Test
    public void testExecute_ShouldAddAbsoluteFileNoFilterNoLineEndingConversion()
        throws Exception
    {
        final AssemblerConfigurationSource macCS = mock( AssemblerConfigurationSource.class );

        final File basedir = temporaryFolder.getRoot();

        final File file = temporaryFolder.newFile( "file.txt" );
        Files.write( file.toPath(), Arrays.asList( "This is a test file." ), StandardCharsets.UTF_8 );

        when( macCS.getBasedir() ).thenReturn( basedir );
        when( macCS.getProject() ).thenReturn( new MavenProject( new Model() ) );
        when( macCS.getFinalName() ) .thenReturn( "final-name" );
        prepareInterpolators( macCS );

        final Logger macLogger = mock( Logger.class );

        final Archiver macArchiver = mock( Archiver.class );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        final FileItem fi = new FileItem();
        fi.setSource( file.getAbsolutePath() );
        fi.setFiltered( false );
        fi.setLineEnding( "keep" );
        fi.setFileMode( "777" );

        assembly.addFile( fi );

        createPhase( macLogger ).execute( assembly, macArchiver, macCS );

        verify( macArchiver ).addResource( any( PlexusIoResource.class ),
                                           eq( "file.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
    }

    @Test
    public void testExecute_ShouldAddRelativeFileNoFilterNoLineEndingConversion()
        throws Exception
    {
        final AssemblerConfigurationSource macCS = mock( AssemblerConfigurationSource.class );

        final File basedir = temporaryFolder.getRoot();

        final File file = temporaryFolder.newFile( "file.txt" );
        Files.write( file.toPath(), Arrays.asList( "This is a test file." ), StandardCharsets.UTF_8 );

        when( macCS.getBasedir() ).thenReturn( basedir );
        when( macCS.getProject() ).thenReturn( new MavenProject( new Model() ) );
        when( macCS.getFinalName() ) .thenReturn( "final-name" );
        prepareInterpolators( macCS );

        final Logger macLogger = mock( Logger.class );

        final Archiver macArchiver = mock( Archiver.class );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );

        final FileItem fi = new FileItem();
        fi.setSource( "file.txt" );
        fi.setFiltered( false );
        fi.setLineEnding( "keep" );
        fi.setFileMode( "777" );

        assembly.addFile( fi );

        createPhase( macLogger ).execute( assembly, macArchiver, macCS );

        verify( macArchiver ).addResource( any( PlexusIoResource.class ),
                                           eq( "file.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
    }

    @Test
    public void testExecute_WithOutputDirectory()
        throws Exception
    {
        final AssemblerConfigurationSource macCS = mock( AssemblerConfigurationSource.class );

        final File basedir = temporaryFolder.getRoot();

        final File readmeFile = temporaryFolder.newFile( "README.txt" );
        Files.write( readmeFile.toPath(), Arrays.asList( "This is a test file for README.txt." ), StandardCharsets.UTF_8 );

        final File licenseFile = temporaryFolder.newFile( "LICENSE.txt" );
        Files.write( licenseFile.toPath(), Arrays.asList( "This is a test file for LICENSE.txt." ), StandardCharsets.UTF_8 );

        final File configFile = new File( temporaryFolder.newFolder( "config" ), "config.txt" );
        Files.write( configFile.toPath(), Arrays.asList( "This is a test file for config/config.txt" ), StandardCharsets.UTF_8 );

        when( macCS.getBasedir() ).thenReturn( basedir );
        when( macCS.getProject() ).thenReturn( new MavenProject( new Model() ) );
        when( macCS.getFinalName() ) .thenReturn( "final-name" );
        prepareInterpolators( macCS );

        final Logger macLogger = mock( Logger.class );

        final Archiver macArchiver = mock( Archiver.class );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( true );

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource( "README.txt" );
        readmeFileItem.setOutputDirectory( "" );
        readmeFileItem.setFiltered( false );
        readmeFileItem.setLineEnding( "keep" );
        readmeFileItem.setFileMode( "777" );

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource( "LICENSE.txt" );
        licenseFileItem.setOutputDirectory( "/" );
        licenseFileItem.setFiltered( false );
        licenseFileItem.setLineEnding( "keep" );
        licenseFileItem.setFileMode( "777" );

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource( "config/config.txt" );
        configFileItem.setOutputDirectory( "config" );
        configFileItem.setFiltered( false );
        configFileItem.setLineEnding( "keep" );
        configFileItem.setFileMode( "777" );

        assembly.addFile( readmeFileItem );
        assembly.addFile( licenseFileItem );
        assembly.addFile( configFileItem );

        createPhase( macLogger ).execute( assembly, macArchiver, macCS );

        verify( macArchiver ).addResource( any( PlexusIoResource.class ),
                                           eq( "README.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
        verify( macArchiver ).addResource( any( PlexusIoResource.class ),
                                           eq( "LICENSE.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
        verify( macArchiver ).addResource( any( PlexusIoResource.class ),
                                           eq( "config/config.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
    
    }

    @Test
    public void testExecute_WithOutputDirectoryAndDestName()
        throws Exception
    {
        final AssemblerConfigurationSource macCS = mock( AssemblerConfigurationSource.class );

        final File basedir = temporaryFolder.getRoot();

        final File readmeFile = temporaryFolder.newFile( "README.txt" );
        Files.write( readmeFile.toPath(), Arrays.asList( "This is a test file for README.txt." ), StandardCharsets.UTF_8 );

        final File licenseFile = temporaryFolder.newFile( "LICENSE.txt" );
        Files.write( licenseFile.toPath(), Arrays.asList( "This is a test file for LICENSE.txt." ), StandardCharsets.UTF_8 );

        final File configFile = new File( temporaryFolder.newFolder( "config" ), "config.txt" );
        Files.write( configFile.toPath(), Arrays.asList( "This is a test file for config/config.txt" ), StandardCharsets.UTF_8 );

        when( macCS.getBasedir() ).thenReturn( basedir );
        when( macCS.getProject() ).thenReturn( new MavenProject( new Model() ) );
        when( macCS.getFinalName() ) .thenReturn( "final-name" );
        prepareInterpolators( macCS );

        final Logger macLogger = mock( Logger.class );

        final Archiver macArchiver = mock( Archiver.class );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( true );

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource( "README.txt" );
        readmeFileItem.setOutputDirectory( "" );
        readmeFileItem.setDestName( "README_renamed.txt" );
        readmeFileItem.setFiltered( false );
        readmeFileItem.setLineEnding( "keep" );
        readmeFileItem.setFileMode( "777" );

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource( "LICENSE.txt" );
        licenseFileItem.setOutputDirectory( "/" );
        licenseFileItem.setDestName( "LICENSE_renamed.txt" );
        licenseFileItem.setFiltered( false );
        licenseFileItem.setLineEnding( "keep" );
        licenseFileItem.setFileMode( "777" );

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource( "config/config.txt" );
        configFileItem.setDestName( "config_renamed.txt" );
        configFileItem.setOutputDirectory( "config" );
        configFileItem.setFiltered( false );
        configFileItem.setLineEnding( "keep" );
        configFileItem.setFileMode( "777" );

        assembly.addFile( readmeFileItem );
        assembly.addFile( licenseFileItem );
        assembly.addFile( configFileItem );

        createPhase( macLogger ).execute( assembly, macArchiver, macCS );

        verify( macArchiver ).addResource( any( PlexusIoResource.class ), 
                                           eq( "README_renamed.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
        verify( macArchiver ).addResource( any( PlexusIoResource.class ), 
                                           eq( "LICENSE_renamed.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
        verify( macArchiver ).addResource( any( PlexusIoResource.class ), 
                                           eq( "config/config_renamed.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
    }

    @Test
    public void testExecute_WithOutputDirectoryAndDestNameAndIncludeBaseDirectoryFalse()
        throws Exception
    {
        final AssemblerConfigurationSource macCS = mock( AssemblerConfigurationSource.class );

        final File basedir = temporaryFolder.getRoot();

        final File readmeFile = temporaryFolder.newFile( "README.txt" );
        Files.write( readmeFile.toPath(), Arrays.asList( "This is a test file for README.txt." ), StandardCharsets.UTF_8 );

        final File licenseFile = temporaryFolder.newFile( "LICENSE.txt" );
        Files.write( licenseFile.toPath(), Arrays.asList( "This is a test file for LICENSE.txt." ), StandardCharsets.UTF_8 );

        final File configFile = new File( temporaryFolder.newFolder( "config" ), "config.txt" );
        Files.write( configFile.toPath(), Arrays.asList( "This is a test file for config/config.txt" ), StandardCharsets.UTF_8 );

        when( macCS.getBasedir() ).thenReturn( basedir );
        when( macCS.getProject() ).thenReturn( new MavenProject( new Model() ) );
        when( macCS.getFinalName() ) .thenReturn( "final-name" );
        prepareInterpolators( macCS );

        final Logger macLogger = mock( Logger.class );

        final Archiver macArchiver = mock( Archiver.class );

        final Assembly assembly = new Assembly();
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );

        final FileItem readmeFileItem = new FileItem();
        readmeFileItem.setSource( "README.txt" );
        readmeFileItem.setDestName( "README_renamed.txt" );
        readmeFileItem.setFiltered( false );
        readmeFileItem.setLineEnding( "keep" );
        readmeFileItem.setFileMode( "777" );

        final FileItem licenseFileItem = new FileItem();
        licenseFileItem.setSource( "LICENSE.txt" );
        licenseFileItem.setDestName( "LICENSE_renamed.txt" );
        licenseFileItem.setFiltered( false );
        licenseFileItem.setLineEnding( "keep" );
        licenseFileItem.setFileMode( "777" );

        final FileItem configFileItem = new FileItem();
        configFileItem.setSource( "config/config.txt" );
        configFileItem.setDestName( "config_renamed.txt" );
        configFileItem.setOutputDirectory( "config" );
        configFileItem.setFiltered( false );
        configFileItem.setLineEnding( "keep" );
        configFileItem.setFileMode( "777" );

        assembly.addFile( readmeFileItem );
        assembly.addFile( licenseFileItem );
        assembly.addFile( configFileItem );

        createPhase( macLogger ).execute( assembly, macArchiver, macCS );

        verify( macArchiver ).addResource( any( PlexusIoResource.class ), 
                                           eq( "README_renamed.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
        verify( macArchiver ).addResource( any( PlexusIoResource.class ),
                                           eq( "LICENSE_renamed.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
        verify( macArchiver ).addResource( any( PlexusIoResource.class ), 
                                           eq( "config/config_renamed.txt" ),
                                           eq( TypeConversionUtils.modeToInt( "777",
                                                                              new ConsoleLogger( Logger.LEVEL_DEBUG,
                                                                                                 "test" ) ) ) );
    }

    private FileItemAssemblyPhase createPhase( final Logger logger )
    {
        final FileItemAssemblyPhase phase = new FileItemAssemblyPhase();
        phase.enableLogging( logger );

        return phase;
    }

    private void prepareInterpolators( AssemblerConfigurationSource configSource )
    {
        when( configSource.getCommandLinePropsInterpolator() ).thenReturn( FixedStringSearchInterpolator.empty() );
        when( configSource.getEnvInterpolator() ).thenReturn( FixedStringSearchInterpolator.empty() );
        when( configSource.getMainProjectInterpolator() ).thenReturn( FixedStringSearchInterpolator.empty() );
    }

}
