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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.PatternExcludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.PatternIncludesArtifactFilter;
import org.apache.maven.shared.artifact.filter.StatisticsReportingArtifactFilter;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.slf4j.Logger;

/**
 *
 */
public final class FilterUtils
{

    private FilterUtils()
    {
    }

    public static Set<MavenProject> filterProjects( final Set<MavenProject> projects, final List<String> includes,
                                                    final List<String> excludes, final boolean actTransitively,
                                                    final Logger logger )
    {
        final List<PatternIncludesArtifactFilter> allFilters = new ArrayList<>();

        final AndArtifactFilter filter = new AndArtifactFilter();

        if ( !includes.isEmpty() )
        {
            final PatternIncludesArtifactFilter includeFilter =
                new PatternIncludesArtifactFilter( includes, actTransitively );

            filter.add( includeFilter );
            allFilters.add( includeFilter );
        }
        if ( !excludes.isEmpty() )
        {
            final PatternExcludesArtifactFilter excludeFilter =
                new PatternExcludesArtifactFilter( excludes, actTransitively );

            filter.add( excludeFilter );
            allFilters.add( excludeFilter );
        }

        Set<MavenProject> result = new LinkedHashSet<>( projects.size() );
        for ( MavenProject project : projects )
        {
            final Artifact artifact = project.getArtifact();

            if ( filter.include( artifact ) )
            {
                result.add( project );
            }
        }

        for ( final PatternIncludesArtifactFilter f : allFilters )
        {
            if ( f != null )
            {
                f.reportMissedCriteria( logger );
            }
        }
        return result;
    }

    public static void filterArtifacts( final Set<Artifact> artifacts, final List<String> includes,
                                        final List<String> excludes, final boolean strictFiltering,
                                        final boolean actTransitively, final Logger logger,
                                        final ArtifactFilter... additionalFilters )
        throws InvalidAssemblerConfigurationException
    {
        final List<ArtifactFilter> allFilters = new ArrayList<>();

        final AndArtifactFilter filter = new AndArtifactFilter();

        if ( additionalFilters != null && additionalFilters.length > 0 )
        {
            for ( final ArtifactFilter additionalFilter : additionalFilters )
            {
                if ( additionalFilter != null )
                {
                    filter.add( additionalFilter );
                }
            }
        }

        if ( !includes.isEmpty() )
        {
            final ArtifactFilter includeFilter = new PatternIncludesArtifactFilter( includes, actTransitively );

            filter.add( includeFilter );

            allFilters.add( includeFilter );
        }

        if ( !excludes.isEmpty() )
        {
            final ArtifactFilter excludeFilter = new PatternExcludesArtifactFilter( excludes, actTransitively );

            filter.add( excludeFilter );

            allFilters.add( excludeFilter );
        }

        // FIXME: Why is this added twice??
        // if ( additionalFilters != null && !additionalFilters.isEmpty() )
        // {
        // allFilters.addAll( additionalFilters );
        // }

        for ( final Iterator<Artifact> it = artifacts.iterator(); it.hasNext(); )
        {
            final Artifact artifact = it.next();

            if ( !filter.include( artifact ) )
            {
                it.remove();

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( artifact.getId() + " was removed by one or more filters." );
                }
            }
        }

        reportFilteringStatistics( allFilters, logger );

        for ( final ArtifactFilter f : allFilters )
        {
            if ( f instanceof StatisticsReportingArtifactFilter )
            {
                final StatisticsReportingArtifactFilter sFilter = (StatisticsReportingArtifactFilter) f;

                if ( strictFiltering && sFilter.hasMissedCriteria() )
                {
                    throw new InvalidAssemblerConfigurationException(
                        "One or more filters had unmatched criteria. Check debug log for more information." );
                }
            }
        }
    }

    public static void reportFilteringStatistics( final Collection<ArtifactFilter> filters, final Logger logger )
    {
        for ( final ArtifactFilter f : filters )
        {
            if ( f instanceof StatisticsReportingArtifactFilter )
            {
                final StatisticsReportingArtifactFilter sFilter = (StatisticsReportingArtifactFilter) f;

                if ( logger.isDebugEnabled() )
                {
                    logger.debug( "Statistics for " + sFilter + "\n" );
                }

                sFilter.reportMissedCriteria( logger );
                sFilter.reportFilteredArtifacts( logger );
            }
        }
    }

    /**
     * Results in a filter including the rootScope and its transitive scopes 
     * 
     * @param rootScope the root scope
     * @return the filter
     */
    public static ScopeFilter newScopeFilter( final String rootScope )
    {
        return newScopeFilter( Collections.singleton( rootScope ) );
    }
    
    /**
     * Results in a filter including all rootScopes and their transitive scopes 
     * 
     * @param rootScopes all root scopes
     * @return the filter
     */
    public static ScopeFilter newScopeFilter( final Collection<String> rootScopes )
    {
        Set<String> scopes = new HashSet<>();
        
        for ( String rootScope : rootScopes )
        {
            if ( Artifact.SCOPE_COMPILE.equals( rootScope ) )
            {
                scopes.addAll( Arrays.asList( "compile", "provided", "system" ) );
            }
            if ( Artifact.SCOPE_PROVIDED.equals( rootScope ) )
            {
                scopes.add( "provided" );
            }
            if ( Artifact.SCOPE_RUNTIME.equals( rootScope ) )
            {
                scopes.addAll( Arrays.asList( "compile", "runtime" ) );
            }
            if ( Artifact.SCOPE_SYSTEM.equals( rootScope ) )
            {
                scopes.add( "system" );
            }
            if ( Artifact.SCOPE_TEST.equals( rootScope ) )
            {
                scopes.addAll( Arrays.asList( "compile", "provided", "runtime", "system", "test" ) );
            }
        }
        
        return ScopeFilter.including( scopes );
    }

}
