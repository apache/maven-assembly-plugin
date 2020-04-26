package org.apache.maven.plugins.assembly.interpolation;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.io.AssemblyReadException;
import org.apache.maven.plugins.assembly.io.DefaultAssemblyReader;
import org.apache.maven.plugins.assembly.io.DefaultAssemblyReaderTest;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.testutils.PojoConfigSource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.PropertiesBasedValueSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith( MockitoJUnitRunner.class )
public class AssemblyInterpolatorTest
{
    @Test
    public void testDependencySetOutputFileNameMappingsAreNotInterpolated()
        throws IOException, AssemblyInterpolationException, AssemblyReadException,
        InvalidAssemblerConfigurationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final MavenProject project = new MavenProject( model );

        final Assembly assembly = new Assembly();

        // artifactId is blacklisted, but packaging is not.
        final String outputFileNameMapping = "${artifactId}.${packaging}";

        final DependencySet set = new DependencySet();
        set.setOutputFileNameMapping( outputFileNameMapping );

        assembly.addDependencySet( set );

        final PojoConfigSource configSourceStub = new PojoConfigSource();

        configSourceStub.setRootInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setEnvironmentInterpolator( FixedStringSearchInterpolator.create() );

        configSourceStub.setMavenProject( project );
        final Assembly outputAssembly = roundTripInterpolation( assembly, configSourceStub );

        final List<DependencySet> outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        final DependencySet outputSet = outputDependencySets.get( 0 );

        assertEquals( "${artifactId}.${packaging}", outputSet.getOutputFileNameMapping() );
    }

    @Test
    public void testDependencySetOutputDirectoryIsNotInterpolated()
        throws IOException, AssemblyInterpolationException, AssemblyReadException,
        InvalidAssemblerConfigurationException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Assembly assembly = new Assembly();

        final String outputDirectory = "${artifactId}.${packaging}";

        final DependencySet set = new DependencySet();
        set.setOutputDirectory( outputDirectory );

        assembly.addDependencySet( set );

        final PojoConfigSource configSourceStub = new PojoConfigSource();

        configSourceStub.setRootInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setEnvironmentInterpolator( FixedStringSearchInterpolator.create() );

        final MavenProject project = new MavenProject( model );
        configSourceStub.setMavenProject( project );
        final Assembly outputAssembly = roundTripInterpolation( assembly, configSourceStub );

        final List<DependencySet> outputDependencySets = outputAssembly.getDependencySets();
        assertEquals( 1, outputDependencySets.size() );

        final DependencySet outputSet = outputDependencySets.get( 0 );

        assertEquals( "${artifactId}.${packaging}", outputSet.getOutputDirectory() );
    }

    private Assembly roundTripInterpolation( Assembly assembly, AssemblerConfigurationSource configSource )
        throws IOException, AssemblyReadException, InvalidAssemblerConfigurationException
    {
        final StringReader stringReader = DefaultAssemblyReaderTest.writeToStringReader( assembly );
        return new DefaultAssemblyReader().readAssembly( stringReader, "testLocation", null, configSource );
    }

    @Test
    public void testShouldResolveModelGroupIdInAssemblyId()
        throws AssemblyInterpolationException, InvalidAssemblerConfigurationException, AssemblyReadException,
        IOException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );

        final MavenProject project = new MavenProject( model );
        final PojoConfigSource configSourceStub = new PojoConfigSource();

        configSourceStub.setRootInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setEnvironmentInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setMavenProject( project );
        final Assembly outputAssembly = roundTripInterpolation( assembly, configSourceStub );
        assertEquals( "assembly.group.id", outputAssembly.getId() );
    }

    @Test
    public void testShouldResolveModelPropertyBeforeModelGroupIdInAssemblyId()
        throws AssemblyInterpolationException, InvalidAssemblerConfigurationException, AssemblyReadException,
        IOException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        final PojoConfigSource configSourceStub = new PojoConfigSource();

        configSourceStub.setRootInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setEnvironmentInterpolator( FixedStringSearchInterpolator.create() );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );

        final MavenProject project = new MavenProject( model );
        configSourceStub.setMavenProject( project );
        final Assembly result = roundTripInterpolation( assembly, configSourceStub );

        assertEquals( "assembly.other.id", result.getId() );
    }

    @Test
    public void testShouldResolveContextValueBeforeModelPropertyOrModelGroupIdInAssemblyId()
        throws Exception
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Properties props = new Properties();
        props.setProperty( "groupId", "other.id" );

        model.setProperties( props );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${groupId}" );

        final Properties execProps = new Properties();
        execProps.setProperty( "groupId", "still.another.id" );

        final AssemblerConfigurationSource cs = mock( AssemblerConfigurationSource.class );
        when( cs.getRepositoryInterpolator() ).thenReturn( FixedStringSearchInterpolator.create() );
        when( cs.getCommandLinePropsInterpolator() ).thenReturn( FixedStringSearchInterpolator.create( new PropertiesBasedValueSource( execProps ) ) );
        when( cs.getEnvInterpolator() ).thenReturn( FixedStringSearchInterpolator.empty() );

        final MavenProject project = new MavenProject( model );
        when( cs.getProject() ) .thenReturn( project );

        final Assembly result = roundTripInterpolation( assembly, cs );

        assertEquals( "assembly.still.another.id", result.getId() );
    }

    @Test
    public void testShouldNotTouchUnresolvedExpression()
        throws AssemblyInterpolationException, InvalidAssemblerConfigurationException, AssemblyReadException,
        IOException
    {
        final Model model = new Model();
        model.setArtifactId( "artifact-id" );
        model.setGroupId( "group.id" );
        model.setVersion( "1" );
        model.setPackaging( "jar" );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${unresolved}" );

        final PojoConfigSource configSourceStub = new PojoConfigSource();

        configSourceStub.setRootInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setEnvironmentInterpolator( FixedStringSearchInterpolator.create() );

        final MavenProject project = new MavenProject( model );
        configSourceStub.setMavenProject( project );
        final Assembly result = roundTripInterpolation( assembly, configSourceStub );
        assertEquals( "assembly.${unresolved}", result.getId() );
    }

    @Test
    public void testShouldInterpolateMultiDotProjectExpression()
        throws AssemblyInterpolationException, InvalidAssemblerConfigurationException, AssemblyReadException,
        IOException
    {
        final Build build = new Build();
        build.setFinalName( "final-name" );

        final Model model = new Model();
        model.setBuild( build );

        final Assembly assembly = new Assembly();

        assembly.setId( "assembly.${project.build.finalName}" );

        final PojoConfigSource configSourceStub = new PojoConfigSource();

        configSourceStub.setRootInterpolator( FixedStringSearchInterpolator.create() );
        configSourceStub.setEnvironmentInterpolator( FixedStringSearchInterpolator.create() );

        final MavenProject project = new MavenProject( model );
        configSourceStub.setMavenProject( project );
        final Assembly result = roundTripInterpolation( assembly, configSourceStub );
        assertEquals( "assembly.final-name", result.getId() );
    }

}
