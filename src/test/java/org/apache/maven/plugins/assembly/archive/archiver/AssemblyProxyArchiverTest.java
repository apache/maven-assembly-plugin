package org.apache.maven.plugins.assembly.archive.archiver;

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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnull;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.FileSet;
import org.codehaus.plexus.archiver.diags.TrackingArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class AssemblyProxyArchiverTest
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private static final Logger logger = new ConsoleLogger( Logger.LEVEL_DEBUG, "test" );

    @Test( timeout = 5000 )
    public void addFileSet_SkipWhenSourceIsAssemblyWorkDir()
        throws IOException, ArchiverException
    {
        final File sources = temporaryFolder.getRoot();

        final File workdir = new File( sources, "workdir" );

        final TrackingArchiver tracker = new TrackingArchiver();
        final AssemblyProxyArchiver archiver =
            new AssemblyProxyArchiver( "", tracker, null, null, null, workdir, logger );

        archiver.setForced( true );

        final DefaultFileSet fs = new DefaultFileSet();
        fs.setDirectory( workdir );

        archiver.addFileSet( fs );

        assertTrue( tracker.added.isEmpty() );
    }

    @Test( timeout = 5000 )
    public void addFileSet_addExcludeWhenSourceContainsAssemblyWorkDir()
        throws IOException, ArchiverException
    {
        final File sources = temporaryFolder.getRoot();

        final File workdir = new File( sources, "workdir" );
        workdir.mkdir();

        Files.write( sources.toPath().resolve( "test-included.txt" ), Arrays.asList( "This is included" ),
                     StandardCharsets.UTF_8 );
        Files.write( workdir.toPath().resolve( "test-excluded.txt" ), Arrays.asList( "This is excluded" ),
                     StandardCharsets.UTF_8 );

        final TrackingArchiver tracker = new TrackingArchiver();
        final AssemblyProxyArchiver archiver =
            new AssemblyProxyArchiver( "", tracker, null, null, null, workdir, logger );

        archiver.setForced( true );

        final DefaultFileSet fs = new DefaultFileSet();
        fs.setDirectory( sources );

        archiver.addFileSet( fs );

        assertEquals( 1, tracker.added.size() );

        final TrackingArchiver.Addition addition = tracker.added.get( 0 );
        assertNotNull( addition.excludes );
        assertEquals( 1, addition.excludes.length );
        assertEquals( workdir.getName(), addition.excludes[0] );
    }

    @Test
    public void addFile_NoPerms_CallAcceptFilesOnlyOnce()
        throws IOException, ArchiverException
    {
        final Archiver delegate = mock( Archiver.class );

        final CounterSelector counter = new CounterSelector( true );
        final List<FileSelector> selectors = new ArrayList<>();
        selectors.add( counter );

        final AssemblyProxyArchiver archiver =
            new AssemblyProxyArchiver( "", delegate, null, selectors, null, new File( "." ),
                                       new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        archiver.setForced( true );

        final File inputFile = temporaryFolder.newFile();
        archiver.addFile( inputFile, "file.txt" );

        assertEquals( 1, counter.getCount() );
        verify( delegate ).addFile( inputFile, "file.txt" );
        verify( delegate ).setForced( true );
    }

    @Test
    public void addDirectory_NoPerms_CallAcceptFilesOnlyOnce()
        throws IOException, ArchiverException
    {
        final Archiver delegate = new JarArchiver();

        final File output = temporaryFolder.newFile();

        delegate.setDestFile( output );

        final CounterSelector counter = new CounterSelector( true );
        final List<FileSelector> selectors = new ArrayList<>();
        selectors.add( counter );

        final AssemblyProxyArchiver archiver =
            new AssemblyProxyArchiver( "", delegate, null, selectors, null, new File( "." ),
                                       new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );

        archiver.setForced( true );

        final File dir = temporaryFolder.newFolder();
        Files.write( dir.toPath().resolve( "file.txt" ), Arrays.asList( "This is a test." ), StandardCharsets.UTF_8 );

        archiver.addDirectory( dir );

        archiver.createArchive();

        assertEquals( 1, counter.getCount() );
    }
    
    @Test
    public void assemblyWorkDir() 
    {
        final Archiver delegate = mock( Archiver.class );
        final List<FileSelector> selectors = new ArrayList<>();

        final AssemblyProxyArchiver archiver =
            new AssemblyProxyArchiver( "prefix", delegate, null, selectors, null,
                                       new File( temporaryFolder.getRoot(), "module1" ),
                                       new ConsoleLogger( Logger.LEVEL_DEBUG, "test" ) );
        
        FileSet fileSet = mock( FileSet.class );
        when( fileSet.getDirectory() ).thenReturn( temporaryFolder.getRoot() ); 
        when( fileSet.getStreamTransformer() ).thenReturn( mock( InputStreamTransformer.class ) );
        
        archiver.addFileSet( fileSet );

        ArgumentCaptor<FileSet> delFileSet = ArgumentCaptor.forClass( FileSet.class );
        verify( delegate ).addFileSet( delFileSet.capture() );
        
        assertThat( delFileSet.getValue().getDirectory(), is( fileSet.getDirectory() ) );
        assertThat( delFileSet.getValue().getExcludes(), is( new String[] { "module1" } ) );
        assertThat( delFileSet.getValue().getFileMappers(), is( fileSet.getFileMappers() ) );
        assertThat( delFileSet.getValue().getFileSelectors(), is( fileSet.getFileSelectors() ) );
        assertThat( delFileSet.getValue().getIncludes(), is(  new String[0] ) );
        assertThat( delFileSet.getValue().getPrefix(), is( "prefix/" ) );
        assertThat( delFileSet.getValue().getStreamTransformer(), is( fileSet.getStreamTransformer() ) );
    }
    

    private static final class CounterSelector
        implements FileSelector
    {

        private int count = 0;

        private boolean answer = false;

        public CounterSelector( final boolean answer )
        {
            this.answer = answer;
        }

        public int getCount()
        {
            return count;
        }

        public boolean isSelected( final @Nonnull FileInfo fileInfo )
            throws IOException
        {
            if ( fileInfo.isFile() )
            {
                count++;
                System.out.println( "Counting file: " + fileInfo.getName() + ". Current count: " + count );
            }

            return answer;
        }

    }

}
