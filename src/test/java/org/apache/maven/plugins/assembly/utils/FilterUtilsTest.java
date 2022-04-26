package org.apache.maven.plugins.assembly.utils;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.project.MavenProject;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

@RunWith( MockitoJUnitRunner.class )
public class FilterUtilsTest
{

    @Test
    public void testFilterArtifacts_ShouldThrowExceptionUsingStrictModeWithUnmatchedInclude()
    {
        final Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "group" );
        when( artifact.getArtifactId() ).thenReturn( "artifact" );
        when( artifact.getBaseVersion() ).thenReturn( "version" );
        when( artifact.getType() ).thenReturn( "jar" );

        final List<String> includes = new ArrayList<>();

        includes.add( "other.group:other-artifact:type:version" );

        final List<String> excludes = Collections.emptyList();

        final Set<Artifact> artifacts = new HashSet<>();
        artifacts.add( artifact );

        try
        {
            FilterUtils.filterArtifacts( artifacts, includes, excludes, true, false, LoggerFactory.getLogger( getClass() ) );

            fail( "Should fail because of unmatched include." );
        }
        catch ( final InvalidAssemblerConfigurationException e )
        {
            // expected.
        }
    }

    @Test
    public void testFilterArtifacts_ShouldNotRemoveArtifactDirectlyIncluded()
        throws Exception
    {
        verifyArtifactInclusion( "group", "artifact", "group:artifact", null, null, null );
        verifyArtifactInclusion( "group", "artifact", "group:artifact:jar", null, null, null );
    }

    @Test
    public void testFilterArtifacts_ShouldNotRemoveArtifactTransitivelyIncluded()
        throws Exception
    {
        verifyArtifactInclusion( "group", "artifact", "group:dependentArtifact", null,
                                 Arrays.asList( "current:project:jar:1.0", "group:dependentArtifact:jar:version" ),
                                 null );
    }

    @Test
    public void testFilterArtifacts_ShouldRemoveArtifactTransitivelyExcluded()
        throws Exception
    {
        verifyArtifactExclusion( "group", "artifact", null, "group:dependentArtifact",
                                 Arrays.asList( "current:project:jar:1.0", "group:dependentArtifact:jar:version" ),
                                 null );
    }

    @Test
    public void testFilterArtifacts_ShouldRemoveArtifactDirectlyExcluded()
        throws Exception
    {
        verifyArtifactExclusion( "group", "artifact", null, "group:artifact", null, null );
        verifyArtifactExclusion( "group", "artifact", null, "group:artifact:jar", null, null );
    }

    @Test
    public void testFilterArtifacts_ShouldNotRemoveArtifactNotIncludedAndNotExcluded()
        throws Exception
    {
        verifyArtifactInclusion( "group", "artifact", null, null, null, null );
        verifyArtifactInclusion( "group", "artifact", null, null, null, null );
    }

    @Test
    public void testFilterArtifacts_ShouldRemoveArtifactExcludedByAdditionalFilter()
        throws Exception
    {
        final ArtifactFilter filter = new ArtifactFilter()
        {

            public boolean include( final Artifact artifact )
            {
                return false;
            }

        };

        verifyArtifactExclusion( "group", "artifact", "fail:fail", null, null, filter );
    }

    @Test
    public void testFilterProjects_ShouldNotRemoveProjectDirectlyIncluded()
    {
        verifyProjectInclusion( "group", "artifact", "group:artifact", null, null );
        verifyProjectInclusion( "group", "artifact", "group:artifact:jar", null, null );
    }

    @Test
    public void testFilterProjects_ShouldNotRemoveProjectTransitivelyIncluded()
    {
        verifyProjectInclusion( "group", "artifact", "group:dependentArtifact", null,
                                Arrays.asList( "current:project:jar:1.0", "group:dependentArtifact:jar:version" ) );
    }

    @Test
    public void testFilterProjects_ShouldRemoveProjectTransitivelyExcluded()
    {
        verifyProjectExclusion( "group", "artifact", null, "group:dependentArtifact",
                                Arrays.asList( "current:project:jar:1.0", "group:dependentArtifact:jar:version" ) );
    }

    @Test
    public void testFilterProjects_ShouldRemoveProjectDirectlyExcluded()
    {
        verifyProjectExclusion( "group", "artifact", null, "group:artifact", null );
        verifyProjectExclusion( "group", "artifact", null, "group:artifact:jar", null );
    }

    @Test
    public void testFilterProjects_ShouldNotRemoveProjectNotIncludedAndNotExcluded()
    {
        verifyProjectInclusion( "group", "artifact", null, null, null );
        verifyProjectInclusion( "group", "artifact", null, null, null );
    }

    @Test
    public void testTransitiveScopes()
    {
        assertThat( FilterUtils.newScopeFilter( "compile" ).getIncluded(),
                    Matchers.containsInAnyOrder( "compile", "provided", "system" ) );

        assertThat( FilterUtils.newScopeFilter( "provided" ).getIncluded(), Matchers.containsInAnyOrder( "provided" ) );

        assertThat( FilterUtils.newScopeFilter( "system" ).getIncluded(), Matchers.containsInAnyOrder( "system" ) );

        assertThat( FilterUtils.newScopeFilter( "runtime" ).getIncluded(),
                    Matchers.containsInAnyOrder( "compile", "runtime" ) );

        assertThat( FilterUtils.newScopeFilter( "test" ).getIncluded(),
                    Matchers.containsInAnyOrder( "compile", "provided", "runtime", "system", "test" ) );
    }
    
    private void verifyArtifactInclusion( final String groupId, final String artifactId, final String inclusionPattern,
                                          final String exclusionPattern, final List<String> depTrail,
                                          final ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true,
                                 additionalFilter );
    }

    private void verifyArtifactExclusion( final String groupId, final String artifactId, final String inclusionPattern,
                                          final String exclusionPattern, final List<String> depTrail,
                                          final ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        verifyArtifactFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false,
                                 additionalFilter );
    }

    private void verifyArtifactFiltering( final String groupId, final String artifactId, final String inclusionPattern,
                                          final String exclusionPattern, final List<String> depTrail,
                                          final boolean verifyInclusion, final ArtifactFilter additionalFilter )
        throws InvalidAssemblerConfigurationException
    {
        Artifact artifact = mock( Artifact.class );

        // this is always enabled, for verification purposes.
        when( artifact.getDependencyConflictId() ).thenReturn( groupId + ":" + artifactId + ":jar" );
        when( artifact.getGroupId() ).thenReturn( groupId );
        when( artifact.getArtifactId() ).thenReturn( artifactId );
        when( artifact.getBaseVersion() ).thenReturn( "version" );
        when( artifact.getType() ).thenReturn( "jar" );

        if ( depTrail != null )
        {
            when( artifact.getDependencyTrail() ).thenReturn( depTrail );
        }

        List<String> inclusions;
        if ( inclusionPattern != null )
        {
            inclusions = Collections.singletonList( inclusionPattern );
        }
        else
        {
            inclusions = Collections.emptyList();
        }

        List<String> exclusions;
        if ( exclusionPattern != null )
        {
            exclusions = Collections.singletonList( exclusionPattern );
        }
        else
        {
            exclusions = Collections.emptyList();
        }

        final Set<Artifact> artifacts = new HashSet<>( Collections.singleton( artifact ) );

        FilterUtils.filterArtifacts( artifacts, inclusions, exclusions, false, depTrail != null, LoggerFactory.getLogger( getClass() ),
                                     additionalFilter );

        if ( verifyInclusion )
        {
            assertEquals( 1, artifacts.size() );
            assertEquals( artifact.getDependencyConflictId(),
                          artifacts.iterator().next().getDependencyConflictId() );
        }
        else
        {
            // just make sure this trips, to meet the mock's expectations.
            artifact.getDependencyConflictId();

            assertTrue( artifacts.isEmpty() );
        }
    }

    private void verifyProjectInclusion( final String groupId, final String artifactId, final String inclusionPattern,
                                         final String exclusionPattern, final List<String> depTrail )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, true );
    }

    private void verifyProjectExclusion( final String groupId, final String artifactId, final String inclusionPattern,
                                         final String exclusionPattern, final List<String> depTrail )
    {
        verifyProjectFiltering( groupId, artifactId, inclusionPattern, exclusionPattern, depTrail, false );
    }

    private void verifyProjectFiltering( final String groupId, final String artifactId, final String inclusionPattern,
                                         final String exclusionPattern, final List<String> depTrail,
                                         final boolean verifyInclusion )
    {
        final Artifact artifact = mock( Artifact.class );

        // this is always enabled, for verification purposes.
        when( artifact.getGroupId() ).thenReturn( groupId );
        when( artifact.getArtifactId() ).thenReturn( artifactId );
        when( artifact.getBaseVersion() ).thenReturn( "version" );
        when( artifact.getType() ).thenReturn( "jar" );

        if ( depTrail != null )
        {
            when( artifact.getDependencyTrail() ).thenReturn( depTrail );
        }

        MavenProject project = mock( MavenProject.class );
        when( project.getId() ).thenReturn( "group:artifact:jar:1.0" );
        when( project.getArtifact() ).thenReturn( artifact );

        final Set<MavenProject> projects = new HashSet<>();
        projects.add( project );

        List<String> inclusions;
        if ( inclusionPattern != null )
        {
            inclusions = Collections.singletonList( inclusionPattern );
        }
        else
        {
            inclusions = Collections.emptyList();
        }

        List<String> exclusions;
        if ( exclusionPattern != null )
        {
            exclusions = Collections.singletonList( exclusionPattern );
        }
        else
        {
            exclusions = Collections.emptyList();
        }

        Set<MavenProject> result =
            FilterUtils.filterProjects( projects, inclusions, exclusions, depTrail != null, LoggerFactory.getLogger( getClass() ) );
        
        if ( verifyInclusion )
        {
            assertEquals( 1, result.size() );
            assertEquals( project.getId(), result.iterator().next().getId() );
        }
        else
        {
            assertTrue( result.isEmpty() );
        }
    }
}
