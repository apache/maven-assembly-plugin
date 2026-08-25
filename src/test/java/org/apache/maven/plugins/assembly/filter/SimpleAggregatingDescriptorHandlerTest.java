package org.apache.maven.plugins.assembly.filter;

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

import junit.framework.TestCase;

import org.apache.maven.plugins.assembly.testutils.TestFileManager;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResourceCollection;
import org.codehaus.plexus.util.IOUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SimpleAggregatingDescriptorHandlerTest
    extends TestCase
{

    private final TestFileManager fileManager =
        new TestFileManager( "simpleAggregatingDescriptorHandler.test", ".zip" );

    @Override
    public void tearDown()
        throws IOException
    {
        fileManager.cleanUp();
    }

    public void testShouldAggregateMatchingFilesIntoOutputPath()
        throws Exception
    {
        final SimpleAggregatingDescriptorHandler handler = new SimpleAggregatingDescriptorHandler();
        handler.setFilePattern( ".*/file\\.txt" );
        handler.setOutputPath( "file.txt" );

        final File baseDir = new File( fileManager.createTempDir(), "src" );
        baseDir.mkdirs();
        new File( baseDir, "a" ).mkdirs();
        new File( baseDir, "b" ).mkdirs();

        writeFile( new File( baseDir, "a/file.txt" ), "file A\n" );
        writeFile( new File( baseDir, "b/file.txt" ), "file B\n" );

        final ZipArchiver archiver = new ZipArchiver();
        final File archiveFile = fileManager.createTempFile();
        archiver.setDestFile( archiveFile );
        archiver.addResources( resourceCollection( baseDir, handler ) );
        archiver.setArchiveFinalizers( Collections.<ArchiveFinalizer>singletonList( handler ) );

        archiver.createArchive();

        ZipFile zf = null;
        try
        {
            zf = new ZipFile( archiveFile );
            final ZipEntry entry = zf.getEntry( "file.txt" );
            assertNotNull( "aggregated file.txt should be present in the archive", entry );

            final String content = readStream( zf.getInputStream( entry ) );

            assertTrue( "aggregated content should contain file A", content.contains( "file A" ) );
            assertTrue( "aggregated content should contain file B", content.contains( "file B" ) );

            zf.close();
            zf = null;
        }
        finally
        {
            closeQuietly( zf );
        }
    }

    public void testShouldNotIncludeAggregatedSourcesInArchive()
        throws Exception
    {
        final SimpleAggregatingDescriptorHandler handler = new SimpleAggregatingDescriptorHandler();
        handler.setFilePattern( ".*/file\\.txt" );
        handler.setOutputPath( "file.txt" );

        final File baseDir = new File( fileManager.createTempDir(), "src" );
        baseDir.mkdirs();
        new File( baseDir, "a" ).mkdirs();
        new File( baseDir, "b" ).mkdirs();

        writeFile( new File( baseDir, "a/file.txt" ), "file A\n" );
        writeFile( new File( baseDir, "b/file.txt" ), "file B\n" );

        final ZipArchiver archiver = new ZipArchiver();
        final File archiveFile = fileManager.createTempFile();
        archiver.setDestFile( archiveFile );
        archiver.addResources( resourceCollection( baseDir, handler ) );
        archiver.setArchiveFinalizers( Collections.<ArchiveFinalizer>singletonList( handler ) );

        archiver.createArchive();

        ZipFile zf = null;
        try
        {
            zf = new ZipFile( archiveFile );
            int fileCount = 0;
            final Enumeration<? extends ZipEntry> entries = zf.entries();
            while ( entries.hasMoreElements() )
            {
                final ZipEntry e = entries.nextElement();
                if ( !e.isDirectory() )
                {
                    fileCount++;
                }
            }
            assertEquals( "the matching sources should be excluded, only the aggregated file.txt should remain",
                          1, fileCount );

            zf.close();
            zf = null;
        }
        finally
        {
            closeQuietly( zf );
        }
    }

    private PlexusIoFileResourceCollection resourceCollection( final File baseDir, final FileSelector selector )
    {
        final PlexusIoFileResourceCollection collection = new PlexusIoFileResourceCollection();
        collection.setBaseDir( baseDir );
        collection.setIncludes( new String[] { "**/file.txt" } );
        collection.setFileSelectors( new FileSelector[] { selector } );
        return collection;
    }

    private void writeFile( final File file, final String content )
        throws IOException
    {
        file.getParentFile().mkdirs();
        final FileOutputStream out = new FileOutputStream( file );
        out.write( content.getBytes( "UTF-8" ) );
        out.close();
    }

    private String readStream( final InputStream in )
        throws IOException
    {
        try
        {
            return IOUtil.toString( in );
        }
        finally
        {
            IOUtil.close( in );
        }
    }

    private void closeQuietly( final ZipFile zf )
    {
        if ( zf != null )
        {
            try
            {
                zf.close();
            }
            catch ( final IOException e )
            {
                // Suppressed.
            }
        }
    }
}
