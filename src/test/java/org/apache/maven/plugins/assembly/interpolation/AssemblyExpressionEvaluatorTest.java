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
package org.apache.maven.plugins.assembly.interpolation;

import java.util.Properties;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.testutils.PojoConfigSource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.PropertiesBasedValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.WARN)
@ExtendWith(MockitoExtension.class)
class AssemblyExpressionEvaluatorTest {
    private final PojoConfigSource configSourceStub = new PojoConfigSource();

    @Test
    void shouldResolveModelGroupId() throws Exception {
        final Model model = new Model();
        model.setArtifactId("artifact-id");
        model.setGroupId("group.id");
        model.setVersion("1");
        model.setPackaging("jar");

        configSourceStub.setMavenProject(new MavenProject(model));
        setupInterpolation();

        final Object result = new AssemblyExpressionEvaluator(configSourceStub).evaluate("assembly.${groupId}");

        assertEquals("assembly.group.id", result);
    }

    private void setupInterpolation() {
        configSourceStub.setRootInterpolator(FixedStringSearchInterpolator.create());
        configSourceStub.setEnvironmentInterpolator(FixedStringSearchInterpolator.create());
        configSourceStub.setEnvInterpolator(FixedStringSearchInterpolator.create());
    }

    @Test
    void shouldResolveModelPropertyBeforeModelGroupId() throws Exception {
        final Model model = new Model();
        model.setArtifactId("artifact-id");
        model.setGroupId("group.id");
        model.setVersion("1");
        model.setPackaging("jar");

        final Properties props = new Properties();
        props.setProperty("groupId", "other.id");

        model.setProperties(props);

        configSourceStub.setMavenProject(new MavenProject(model));
        setupInterpolation();

        final Object result = new AssemblyExpressionEvaluator(configSourceStub).evaluate("assembly.${groupId}");

        assertEquals("assembly.other.id", result);
    }

    @Test
    void shouldResolveContextValueBeforeModelPropertyOrModelGroupIdInAssemblyId() throws Exception {
        final Model model = new Model();
        model.setArtifactId("artifact-id");
        model.setGroupId("group.id");
        model.setVersion("1");
        model.setPackaging("jar");

        final Properties props = new Properties();
        props.setProperty("groupId", "other.id");

        model.setProperties(props);

        final Properties execProps = new Properties();
        execProps.setProperty("groupId", "still.another.id");

        PropertiesBasedValueSource cliProps = new PropertiesBasedValueSource(execProps);

        AssemblerConfigurationSource cs = mock(AssemblerConfigurationSource.class);
        when(cs.getCommandLinePropsInterpolator()).thenReturn(FixedStringSearchInterpolator.create(cliProps));
        when(cs.getRepositoryInterpolator()).thenReturn(FixedStringSearchInterpolator.create());
        when(cs.getEnvInterpolator()).thenReturn(FixedStringSearchInterpolator.create());
        when(cs.getProject()).thenReturn(new MavenProject(model));

        final Object result = new AssemblyExpressionEvaluator(cs).evaluate("assembly.${groupId}");

        assertEquals("assembly.still.another.id", result);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(cs).getCommandLinePropsInterpolator();
        verify(cs).getRepositoryInterpolator();
        verify(cs).getEnvInterpolator();
        verify(cs).getProject();
    }

    @Test
    void shouldReturnUnchangedInputForUnresolvedExpression() throws Exception {
        final Model model = new Model();
        model.setArtifactId("artifact-id");
        model.setGroupId("group.id");
        model.setVersion("1");
        model.setPackaging("jar");

        configSourceStub.setMavenProject(new MavenProject(model));
        setupInterpolation();

        final Object result = new AssemblyExpressionEvaluator(configSourceStub).evaluate("assembly.${unresolved}");

        assertEquals("assembly.${unresolved}", result);
    }

    @Test
    void shouldInterpolateMultiDotProjectExpression() throws Exception {
        final Build build = new Build();
        build.setFinalName("final-name");

        final Model model = new Model();
        model.setBuild(build);

        configSourceStub.setMavenProject(new MavenProject(model));
        setupInterpolation();

        final Object result =
                new AssemblyExpressionEvaluator(configSourceStub).evaluate("assembly.${project.build.finalName}");

        assertEquals("assembly.final-name", result);
    }
}
