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
package org.apache.maven.plugins.assembly.mojos;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

import org.apache.maven.plugins.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugins.assembly.io.AssemblyReader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.filtering.MavenReaderFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class AbstractAssemblyMojoTest {

    private SingleAssemblyMojo newMojo() {
        return new SingleAssemblyMojo(
                mock(MavenProjectHelper.class),
                mock(MavenReaderFilter.class),
                mock(AssemblyArchiver.class),
                mock(AssemblyReader.class));
    }

    private static void setField(final Object target, final String fieldName, final Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void setInheritedField(final Object target, final String fieldName, final Object value)
            throws Exception {
        Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void additionalPropertiesIncludeMavenBuildTimestamp() throws Exception {
        SingleAssemblyMojo mojo = newMojo();
        setField(mojo, "project", new MavenProject());

        Properties properties = mojo.getAdditionalProperties();

        String timestamp = properties.getProperty("maven.build.timestamp");
        assertNotNull(timestamp);
        assertFalse(timestamp.isEmpty());
    }

    @Test
    void additionalPropertiesIncludeProjectBaseUri() throws Exception {
        SingleAssemblyMojo mojo = newMojo();
        MavenProject project = new MavenProject();
        File basedir = new File(".").getAbsoluteFile();
        project.setFile(new File(basedir, "pom.xml"));
        setField(mojo, "project", project);

        Properties properties = mojo.getAdditionalProperties();

        assertEquals(
                project.getBasedir().getAbsoluteFile().toURI().toString(), properties.getProperty("project.baseUri"));
    }

    @Test
    void userConfiguredAdditionalPropertyOverridesComputedOne() throws Exception {
        SingleAssemblyMojo mojo = newMojo();
        setField(mojo, "project", new MavenProject());
        Properties userProperties = new Properties();
        userProperties.put("maven.build.timestamp", "frozen-for-reproducible-builds");
        setInheritedField(mojo, "additionalProperties", userProperties);

        Properties properties = mojo.getAdditionalProperties();

        assertEquals("frozen-for-reproducible-builds", properties.getProperty("maven.build.timestamp"));
    }
}
