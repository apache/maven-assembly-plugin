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

import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugins.assembly.artifact.DependencyResolver;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class DependencySetAssemblyPhaseTest
{
    private DependencySetAssemblyPhase phase;
    
    private DependencyResolver dependencyResolver;
    
    @Before
    public void setUp()
    {
        this.dependencyResolver = mock( DependencyResolver.class );
        
        this.phase = new DependencySetAssemblyPhase( null, dependencyResolver, null );
    }

    @Test
    public void testExecute_ShouldAddOneDependencyFromProject()
        throws AssemblyFormattingException, ArchiveCreationException, IOException,
        InvalidAssemblerConfigurationException, DependencyResolutionException
    {
        final String outputLocation = "/out";

        final MavenProject project = newMavenProject( "group", "project", "0" );

        Artifact artifact = mock( Artifact.class );
        project.setArtifact( artifact );

        final DependencySet ds = new DependencySet();
        ds.setUseProjectArtifact( false );
        ds.setOutputDirectory( outputLocation );
        ds.setOutputFileNameMapping( "${artifact.artifactId}" );
        ds.setUnpack( false );
        ds.setScope( Artifact.SCOPE_COMPILE );
        ds.setFileMode( Integer.toString( 10, 8 ) );

        final Assembly assembly = new Assembly();

        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );
        assembly.addDependencySet( ds );

        project.setArtifacts( Collections.singleton( artifact ) );

        when( dependencyResolver.resolveDependencySets( eq( assembly ),
                                                        isNull(AssemblerConfigurationSource.class),
                                                        anyListOf( DependencySet.class ) ) ).thenReturn( new LinkedHashMap<DependencySet, Set<Artifact>>() );
        
        this.phase.execute( assembly, null, null );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( dependencyResolver ).resolveDependencySets( eq( assembly ),
                                                            isNull(AssemblerConfigurationSource.class),
                                                            anyListOf( DependencySet.class ) );
    }

    @Test
    public void testExecute_ShouldNotAddDependenciesWhenProjectHasNone()
        throws Exception
    {
        final Assembly assembly = new Assembly();
        assembly.setId( "test" );
        assembly.setIncludeBaseDirectory( false );
        
        when( dependencyResolver.resolveDependencySets( eq( assembly ), 
                                                        isNull( AssemblerConfigurationSource.class ),
                                                        anyListOf( DependencySet.class ) ) ).thenReturn( new LinkedHashMap<DependencySet, Set<Artifact>>() );

        this.phase.execute( assembly, null, null );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( dependencyResolver ).resolveDependencySets( eq( assembly ),
                                                            isNull( AssemblerConfigurationSource.class ),
                                                            anyListOf( DependencySet.class ) );        
    }
    
    private MavenProject newMavenProject( final String groupId, final String artifactId, final String version )
    {
        final Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );

        return new MavenProject( model );
    }
}
