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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.phase.ModuleSetAssemblyPhase;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.DependencyFilterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * @author jdcasey
 */
@Singleton
@Named
public class DefaultDependencyResolver implements DependencyResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDependencyResolver.class);

    private final ArtifactHandlerManager artifactHandlerManager;

    private final RepositorySystem repositorySystem;

    @Inject
    public DefaultDependencyResolver(ArtifactHandlerManager artifactHandlerManager, RepositorySystem repositorySystem) {
        this.artifactHandlerManager = requireNonNull(artifactHandlerManager);
        this.repositorySystem = requireNonNull(repositorySystem);
    }

    @Override
    public Map<DependencySet, Set<Artifact>> resolveDependencySets(
            final Assembly assembly,
            ModuleSet moduleSet,
            final AssemblerConfigurationSource configSource,
            List<DependencySet> dependencySets)
            throws DependencyResolutionException {
        Map<DependencySet, Set<Artifact>> result = new LinkedHashMap<>();

        for (DependencySet dependencySet : dependencySets) {

            final MavenProject currentProject = configSource.getProject();

            final ResolutionManagementInfo info = new ResolutionManagementInfo();
            updateDependencySetResolutionRequirements(
                    configSource.getMavenSession().getRepositorySession(), dependencySet, info, currentProject);
            updateModuleSetResolutionRequirements(moduleSet, dependencySet, info, configSource);

            result.put(dependencySet, info.getArtifacts());
        }
        return result;
    }

    @Override
    public Map<DependencySet, Set<Artifact>> resolveDependencySets(
            final Assembly assembly,
            final AssemblerConfigurationSource configSource,
            List<DependencySet> dependencySets)
            throws DependencyResolutionException {
        Map<DependencySet, Set<Artifact>> result = new LinkedHashMap<>();

        for (DependencySet dependencySet : dependencySets) {

            final MavenProject currentProject = configSource.getProject();

            final ResolutionManagementInfo info = new ResolutionManagementInfo();
            updateDependencySetResolutionRequirements(
                    configSource.getMavenSession().getRepositorySession(), dependencySet, info, currentProject);

            result.put(dependencySet, info.getArtifacts());
        }
        return result;
    }

    void updateModuleSetResolutionRequirements(
            ModuleSet set,
            DependencySet dependencySet,
            final ResolutionManagementInfo requirements,
            final AssemblerConfigurationSource configSource)
            throws DependencyResolutionException {
        final ModuleBinaries binaries = set.getBinaries();
        if (binaries != null) {
            Set<MavenProject> projects;
            try {
                projects = ModuleSetAssemblyPhase.getModuleProjects(set, configSource, LOGGER);
            } catch (final ArchiveCreationException e) {
                throw new DependencyResolutionException(
                        "Error determining project-set for moduleSet with binaries.", e);
            }

            for (final MavenProject p : projects) {
                if (p.getArtifact() == null) {
                    p.setArtifact(createArtifact(p.getGroupId(), p.getArtifactId(), p.getVersion(), p.getPackaging()));
                }
            }

            if (binaries.isIncludeDependencies()) {
                updateDependencySetResolutionRequirements(
                        configSource.getMavenSession().getRepositorySession(),
                        dependencySet,
                        requirements,
                        projects.toArray(new MavenProject[0]));
            }
        }
    }

    private Artifact createArtifact(String groupId, String artifactId, String version, String type) {
        VersionRange versionRange = null;
        if (version != null) {
            versionRange = VersionRange.createFromVersion(version);
        }
        return new DefaultArtifact(
                groupId,
                artifactId,
                versionRange,
                null,
                type,
                null,
                artifactHandlerManager.getArtifactHandler(type),
                false);
    }

    void updateDependencySetResolutionRequirements(
            RepositorySystemSession systemSession,
            final DependencySet set,
            final ResolutionManagementInfo requirements,
            final MavenProject... projects)
            throws DependencyResolutionException {
        for (final MavenProject project : projects) {
            if (project == null) {
                continue;
            }

            Set<Artifact> dependencyArtifacts = null;
            if (set.isUseTransitiveDependencies()) {
                try {
                    // we need resolve project again according to requested scope
                    dependencyArtifacts = resolveTransitive(systemSession, set.getScope(), project);
                } catch (org.eclipse.aether.resolution.DependencyResolutionException e) {
                    throw new DependencyResolutionException(e.getMessage(), e);
                }
            } else {
                // FIXME remove using deprecated method
                dependencyArtifacts = project.getDependencyArtifacts();
            }

            requirements.addArtifacts(dependencyArtifacts);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "Dependencies for project: {} are:\n{}",
                        project.getId(),
                        StringUtils.join(dependencyArtifacts.iterator(), "\n"));
            }
        }
    }

    private Set<Artifact> resolveTransitive(
            RepositorySystemSession repositorySession, String scope, MavenProject project)
            throws org.eclipse.aether.resolution.DependencyResolutionException {

        // scope dependency filter
        DependencyFilter scoopeDependencyFilter = DependencyFilterUtils.classpathFilter(scope);

        // get project dependencies filtered by requested scope
        List<Dependency> dependencies = project.getDependencies().stream()
                .map(d -> RepositoryUtils.toDependency(d, repositorySession.getArtifactTypeRegistry()))
                .filter(d -> scoopeDependencyFilter.accept(new DefaultDependencyNode(d), null))
                .collect(Collectors.toList());

        List<Dependency> managedDependencies = Optional.ofNullable(project.getDependencyManagement())
                .map(DependencyManagement::getDependencies)
                .map(list -> list.stream()
                        .map(d -> RepositoryUtils.toDependency(d, repositorySession.getArtifactTypeRegistry()))
                        .collect(Collectors.toList()))
                .orElse(null);

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setManagedDependencies(managedDependencies);
        collectRequest.setRepositories(project.getRemoteProjectRepositories());
        collectRequest.setDependencies(dependencies);
        collectRequest.setRootArtifact(RepositoryUtils.toArtifact(project.getArtifact()));

        DependencyRequest request = new DependencyRequest(collectRequest, scoopeDependencyFilter);

        DependencyResult dependencyResult = repositorySystem.resolveDependencies(repositorySession, request);

        // cache for artifact mapping
        Map<org.eclipse.aether.artifact.Artifact, Artifact> aetherToMavenArtifacts = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(project.getArtifact().getId());

        Set<Artifact> artifacts = new HashSet<>();

        // we need rebuild artifact dependencyTrail - it is used by useTransitiveFiltering
        dependencyResult.getRoot().accept(new DependencyVisitor() {
            @Override
            public boolean visitEnter(DependencyNode node) {
                if (node.getDependency() != null) {
                    stack.push(aetherToMavenArtifacts
                            .computeIfAbsent(node.getDependency().getArtifact(), RepositoryUtils::toArtifact)
                            .getId());
                }
                return true;
            }

            @Override
            public boolean visitLeave(DependencyNode node) {
                Dependency dependency = node.getDependency();
                if (dependency != null) {
                    Artifact artifact = aetherToMavenArtifacts.computeIfAbsent(
                            dependency.getArtifact(), RepositoryUtils::toArtifact);
                    if (artifact.isResolved() && artifact.getFile() != null) {
                        List<String> depTrail = new ArrayList<>();
                        stack.descendingIterator().forEachRemaining(depTrail::add);
                        artifact.setDependencyTrail(depTrail);
                        artifact.setOptional(dependency.isOptional());
                        artifact.setScope(dependency.getScope());
                        artifacts.add(artifact);
                    }
                    stack.pop();
                }
                return true;
            }
        });

        return artifacts;
    }
}
