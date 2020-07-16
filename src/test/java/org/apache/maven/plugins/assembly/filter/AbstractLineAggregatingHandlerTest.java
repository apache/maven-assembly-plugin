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

import org.codehaus.plexus.archiver.ArchiveEntry;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.dir.DirectoryArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import static org.codehaus.plexus.components.io.resources.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbstractLineAggregatingHandlerTest
{
    /** A time today, rounded down to the previous minute */
    private static long MODIFIED_TODAY = (System.currentTimeMillis() / TimeUnit.MINUTES.toMillis( 1 )) * TimeUnit.MINUTES.toMillis( 1 );

    /** A time yesterday, rounded down to the previous minute */
    private static long MODIFIED_YESTERDAY = MODIFIED_TODAY - TimeUnit.DAYS.toMillis( 1 );

    /** A time last week, rounded down to the previous minute */
    private static long MODIFIED_LAST_WEEK = MODIFIED_TODAY - TimeUnit.DAYS.toMillis( 7 );

    private final AbstractLineAggregatingHandler handler = new AbstractLineAggregatingHandler()
    {
        @Override
        protected String getOutputPathPrefix( final FileInfo fileInfo ) {
            return "";
        }

        @Override
        protected boolean fileMatches( final FileInfo fileInfo ) {
            return true;
        }
    };

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testHandlerMergesMatchingFiles()
            throws Exception
    {
        // Arrange
        final Archiver archiver = new DirectoryArchiver();
        final FileInfo resource1 = resource( "merged.txt", "text1", MODIFIED_YESTERDAY );
        final FileInfo resource2 = resource( "merged.txt", "text2", MODIFIED_LAST_WEEK );

        // Act
        handler.isSelected( resource1 );
        handler.isSelected( resource2 );
        handler.addToArchive( archiver );

        // Assert

        final ResourceIterator resources = archiver.getResources();
        assertTrue( "Expected at least one resource", resources.hasNext() );

        final ArchiveEntry resource = resources.next();
        assertFalse( "Expected at most one resource", resources.hasNext() );

        try(final BufferedReader in =
            new BufferedReader( new InputStreamReader( resource.getInputStream() ) ) )
        {
            assertEquals( "text1", in.readLine());
            assertEquals( "text2", in.readLine());
            assertNull( in.readLine() );
        }

        assertTrue(
            "Merging old resources should result in old merge",
            resource.getResource().getLastModified() < MODIFIED_TODAY
        );
    }

    private PlexusIoResource resource( final String name, final String text, final long modified ) throws IOException {
        final File file = temporaryFolder.newFile();
        FileUtils.fileWrite( file, text );
        file.setLastModified( modified );
        return createResource( file, name );
    }
}
