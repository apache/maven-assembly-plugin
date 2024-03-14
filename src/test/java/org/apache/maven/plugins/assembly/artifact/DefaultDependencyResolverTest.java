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
package org.apache.maven.plugins.assembly.artifact;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDependencyResolverTest {

    @Mock
    private ArtifactHandlerManager artifactHandlerManager;

    @Mock
    private RepositorySystem repositorySystem;

    @Mock
    private RepositorySystemSession systemSession;

    @InjectMocks
    private DefaultDependencyResolver resolver;

    @Test
    public void test_getDependencySetResolutionRequirements_transitive() throws Exception {
        final DependencySet ds = new DependencySet();
        ds.setScope(Artifact.SCOPE_SYSTEM);
        ds.setUseTransitiveDependencies(true);

        final MavenProject project = createMavenProject("main-group", "main-artifact", "1", null);

        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(newArtifact("g.id", "a-id", "1"));
        artifacts.add(newArtifact("g.id", "a-id-2", "2"));

        DefaultDependencyNode node1 = new DefaultDependencyNode(new Dependency(
                new org.eclipse.aether.artifact.DefaultArtifact("g.id:a-id:1").setFile(new File(".")), "runtime"));
        DefaultDependencyNode node2 = new DefaultDependencyNode(new Dependency(
                new org.eclipse.aether.artifact.DefaultArtifact("g.id:a-id-2:2").setFile(new File(".")), "runtime"));

        DependencyResult dependencyResult = new DependencyResult(new DependencyRequest());
        DefaultDependencyNode rootDependencyNode = new DefaultDependencyNode((Dependency) null);
        rootDependencyNode.setChildren(Arrays.asList(node1, node2));
        dependencyResult.setRoot(rootDependencyNode);

        when(repositorySystem.resolveDependencies(eq(systemSession), any())).thenReturn(dependencyResult);

        final ResolutionManagementInfo info = new ResolutionManagementInfo();
        resolver.updateDependencySetResolutionRequirements(systemSession, ds, info, project);
        assertEquals(artifacts, info.getArtifacts());
        // dependencyTrail is set
        info.getArtifacts().forEach(artifact -> {
            assertEquals(2, artifact.getDependencyTrail().size());
            assertEquals(
                    project.getArtifact().getId(), artifact.getDependencyTrail().get(0));
            assertEquals(artifact.getId(), artifact.getDependencyTrail().get(1));
        });
    }

    @Test
    public void test_getDependencySetResolutionRequirements_nonTransitive() throws DependencyResolutionException {
        final DependencySet ds = new DependencySet();
        ds.setScope(Artifact.SCOPE_SYSTEM);
        ds.setUseTransitiveDependencies(false);

        final MavenProject project = createMavenProject("main-group", "main-artifact", "1", null);

        Set<Artifact> dependencyArtifacts = new HashSet<>();
        dependencyArtifacts.add(newArtifact("g.id", "a-id", "1"));
        Set<Artifact> artifacts = new HashSet<>(dependencyArtifacts);
        artifacts.add(newArtifact("g.id", "a-id-2", "2"));
        project.setArtifacts(artifacts);
        project.setDependencyArtifacts(dependencyArtifacts);

        final ResolutionManagementInfo info = new ResolutionManagementInfo();
        resolver.updateDependencySetResolutionRequirements(systemSession, ds, info, project);
        assertEquals(dependencyArtifacts, info.getArtifacts());
    }

    @Test
    public void test_getModuleSetResolutionRequirements_withoutBinaries() throws DependencyResolutionException {
        final File rootDir = new File("root");
        final MavenProject project = createMavenProject("main-group", "main-artifact", "1", rootDir);
        final MavenProject module1 = createMavenProject("main-group", "module-1", "1", new File(rootDir, "module-1"));
        final MavenProject module2 = createMavenProject("main-group", "module-2", "1", new File(rootDir, "module-2"));

        project.getModel().addModule(module1.getArtifactId());
        project.getModel().addModule(module2.getArtifactId());

        final ResolutionManagementInfo info = new ResolutionManagementInfo();

        final ModuleSet ms = new ModuleSet();
        ms.setBinaries(null);

        resolver.updateModuleSetResolutionRequirements(ms, new DependencySet(), info, null);
        assertTrue(info.getArtifacts().isEmpty());
    }

    @Test
    public void test_getModuleSetResolutionRequirements_includeDeps() throws Exception {
        final File rootDir = new File("root");
        final MavenProject project = createMavenProject("main-group", "main-artifact", "1", rootDir);
        final MavenProject module1 = createMavenProject("main-group", "module-1", "1", new File(rootDir, "module-1"));
        final MavenProject module2 = createMavenProject("main-group", "module-2", "1", new File(rootDir, "module-2"));

        Set<Artifact> module1Artifacts = Collections.singleton(newArtifact("group.id", "module-1-dep", "1"));
        Set<Artifact> module2Artifacts = Collections.singleton(newArtifact("group.id", "module-2-dep", "1"));
        module1.setArtifacts(module1Artifacts);
        module2.setArtifacts(module2Artifacts);

        project.getModel().addModule(module1.getArtifactId());
        project.getModel().addModule(module2.getArtifactId());

        final AssemblerConfigurationSource cs = mock(AssemblerConfigurationSource.class);
        when(cs.getReactorProjects()).thenReturn(Arrays.asList(project, module1, module2));
        when(cs.getProject()).thenReturn(project);
        MavenSession mavenSession = mock(MavenSession.class);
        when(cs.getMavenSession()).thenReturn(mavenSession);
        when(mavenSession.getRepositorySession()).thenReturn(systemSession);

        final ResolutionManagementInfo info = new ResolutionManagementInfo();

        final ModuleSet ms = new ModuleSet();
        final ModuleBinaries mb = new ModuleBinaries();
        mb.setIncludeDependencies(true);
        ms.setBinaries(mb);
        // FIXME - this is not checked - because ms.UseAllReactorProjects is false
        ms.addInclude("*:module-1");

        DefaultDependencyNode node1 = new DefaultDependencyNode(new Dependency(
                new org.eclipse.aether.artifact.DefaultArtifact("group.id:module-1-dep:1").setFile(new File(".")),
                "runtime"));

        DependencyResult dependencyResult = new DependencyResult(new DependencyRequest());
        DefaultDependencyNode rootDependencyNode = new DefaultDependencyNode((Dependency) null);
        rootDependencyNode.setChildren(Collections.singletonList(node1));
        dependencyResult.setRoot(rootDependencyNode);

        when(repositorySystem.resolveDependencies(eq(systemSession), any())).thenReturn(dependencyResult);

        resolver.updateModuleSetResolutionRequirements(ms, new DependencySet(), info, cs);
        assertEquals(module1Artifacts, info.getArtifacts());

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(cs).getReactorProjects();
        verify(cs).getProject();
    }

    private MavenProject createMavenProject(
            final String groupId, final String artifactId, final String version, final File basedir) {
        final Model model = new Model();

        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        model.setPackaging("pom");

        final MavenProject project = new MavenProject(model);

        final Artifact pomArtifact = newArtifact(groupId, artifactId, version);
        project.setArtifact(pomArtifact);
        project.setArtifacts(new HashSet<>());
        project.setDependencyArtifacts(new HashSet<>());

        project.setFile(new File(basedir, "pom.xml"));

        return project;
    }

    private Artifact newArtifact(final String groupId, final String artifactId, final String version) {
        return new DefaultArtifact(
                groupId,
                artifactId,
                VersionRange.createFromVersion(version),
                "compile",
                "jar",
                null,
                new DefaultArtifactHandler());
    }
}
