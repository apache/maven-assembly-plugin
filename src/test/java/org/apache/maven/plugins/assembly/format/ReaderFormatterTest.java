package org.apache.maven.plugins.assembly.format;

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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.testutils.PojoConfigSource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.DefaultMavenReaderFilter;
import org.apache.maven.shared.filtering.MavenReaderFilter;
import org.apache.maven.shared.filtering.MavenReaderFilterRequest;
import org.codehaus.plexus.archiver.resources.PlexusIoVirtualFileResource;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Test;
import org.mockito.ArgumentCaptor;


public class ReaderFormatterTest
{
    @Test
    public void lineDosFeed()
        throws IOException, AssemblyFormattingException
    {
        final PojoConfigSource cfg = getPojoConfigSource();
        InputStreamTransformer fileSetTransformers = ReaderFormatter.getFileSetTransformers( cfg, true, Collections.<String>emptySet(), "dos" );
        InputStream fud = fileSetTransformers.transform( dummyResource(), payload( "This is a\ntest." ) );
        assertEquals( "This is a\r\ntest.", readResultStream( fud ) );
    }

    @Test
    public void lineDosFeed_withoutFiltering()
        throws IOException, AssemblyFormattingException
    {
        final PojoConfigSource cfg = getPojoConfigSource();
        InputStreamTransformer fileSetTransformers = ReaderFormatter.getFileSetTransformers( cfg, false, Collections.<String>emptySet(), "dos" );
        InputStream fud = fileSetTransformers.transform( dummyResource(), payload( "This is a\ntest." ) );
        assertEquals( "This is a\r\ntest.", readResultStream( fud ) );
    }

    @Test
    public void lineUnixFeedWithInterpolation()
        throws IOException, AssemblyFormattingException
    {
        final PojoConfigSource cfg = getPojoConfigSource();
        InputStreamTransformer fileSetTransformers = ReaderFormatter.getFileSetTransformers( cfg, true, Collections.<String>emptySet(), "unix" );
        InputStream fud = fileSetTransformers.transform( dummyResource(), payload(
            "This is a test for project: ${artifactId} @artifactId@." ) );
        assertEquals( "This is a test for project: anArtifact anArtifact.", readResultStream( fud ) );
    }

    @Test
    public void nonFilteredFileExtensions() throws Exception
    {
        final PojoConfigSource cfg = getPojoConfigSource();
        Set<String> nonFilteredFileExtensions = new HashSet<>( Arrays.asList( "jpg", "tar.gz" ) );
        InputStreamTransformer transformer = ReaderFormatter.getFileSetTransformers( cfg, true, nonFilteredFileExtensions, "unix" );

        final InputStream is = new ByteArrayInputStream( new byte[0] );
        PlexusIoResource resource = mock( PlexusIoResource.class );

        when( resource.getName() ).thenReturn( "file.jpg", "file.tar.gz", "file.txt", "file.nojpg", "file.gz", "file" );
        assertThat( transformer.transform( resource, is ), sameInstance( is ) );
        assertThat( transformer.transform( resource, is ), sameInstance( is ) );
        assertThat( transformer.transform( resource, is ), not( sameInstance( is ) ) );
        assertThat( transformer.transform( resource, is ), not( sameInstance( is ) ) );
        assertThat( transformer.transform( resource, is ), not( sameInstance( is ) ) );
        assertThat( transformer.transform( resource, is ), not( sameInstance( is ) ) );
    }

    @Test
    public void additionalProperties() throws Exception
    {
        final MavenReaderFilter mavenReaderFilter = mock( MavenReaderFilter.class );

        final PojoConfigSource cfg = getPojoConfigSource();
        cfg.setMavenReaderFilter( mavenReaderFilter );
        Properties additionalProperties = new Properties();
        cfg.setAdditionalProperties( additionalProperties );
        
        InputStreamTransformer transformer =  ReaderFormatter.getFileSetTransformers( cfg, true, Collections.<String>emptySet(), "unix" );
        
        final InputStream inputStream = new ByteArrayInputStream( new byte[0] );
        PlexusIoResource resource = mock( PlexusIoResource.class );
        when( resource.getName() ).thenReturn( "file.txt" );

        transformer.transform( resource, inputStream );

        ArgumentCaptor<MavenReaderFilterRequest> filteringRequest = 
                        ArgumentCaptor.forClass(MavenReaderFilterRequest.class);
        verify( mavenReaderFilter ).filter( filteringRequest.capture() );
        assertThat( filteringRequest.getValue().getAdditionalProperties(), sameInstance( additionalProperties ) );
    }

    private MavenProject createBasicMavenProject()
    {
        final Model model = new Model();
        model.setArtifactId( "anArtifact" );
        model.setGroupId( "group" );
        model.setVersion( "version" );

        return new MavenProject( model );
    }


    private String readResultStream( InputStream fud )
        throws IOException
    {
        byte[] actual = new byte[100];
        int read = IOUtils.read( fud, actual );
        return new String( actual, 0, read );
    }

    private ByteArrayInputStream payload( String payload )
    {
        return new ByteArrayInputStream( payload.getBytes() );
    }

    private PojoConfigSource getPojoConfigSource()
    {
        final PojoConfigSource cfg = new PojoConfigSource();
        cfg.setEncoding( "UTF-8" );
        DefaultMavenReaderFilter mavenReaderFilter = new DefaultMavenReaderFilter();
        mavenReaderFilter.enableLogging( new ConsoleLogger( 2, "fud" ) );
        cfg.setMavenReaderFilter( mavenReaderFilter );
        cfg.setEscapeString( null );
        cfg.setMavenProject( createBasicMavenProject() );
        return cfg;
    }

    private PlexusIoVirtualFileResource dummyResource()
    {
        return new PlexusIoVirtualFileResource( new File( "fud" ), "fud" )
        {
        };
    }
}