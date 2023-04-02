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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.phase.ModuleSetAssemblyPhase;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * @author jdcasey
 *
 */
@Singleton
@Named
public class DefaultDependencyResolver implements DependencyResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDependencyResolver.class);

    private final ArtifactHandlerManager artifactHandlerManager;

    @Inject
    public DefaultDependencyResolver(ArtifactHandlerManager artifactHandlerManager) {
        this.artifactHandlerManager = requireNonNull(artifactHandlerManager);
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
            updateDependencySetResolutionRequirements(dependencySet, info, currentProject);
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
            updateDependencySetResolutionRequirements(dependencySet, info, currentProject);

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
                        dependencySet, requirements, projects.toArray(new MavenProject[0]));
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
            final DependencySet set, final ResolutionManagementInfo requirements, final MavenProject... projects)
            throws DependencyResolutionException {
        for (final MavenProject project : projects) {
            if (project == null) {
                continue;
            }

            Set<Artifact> dependencyArtifacts = null;
            if (set.isUseTransitiveDependencies()) {
                dependencyArtifacts = project.getArtifacts();
            } else {
                dependencyArtifacts = project.getDependencyArtifacts();
            }

            requirements.addArtifacts(dependencyArtifacts);
            LOGGER.debug("Dependencies for project: " + project.getId() + " are:\n"
                    + StringUtils.join(dependencyArtifacts.iterator(), "\n"));
        }
    }
}
