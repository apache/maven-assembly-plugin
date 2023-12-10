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
package org.apache.maven.plugins.assembly;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenReaderFilter;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;

/**
 * Assembly configuration
 */
public interface AssemblerConfigurationSource {
    /**
     * @return The descriptors.
     */
    String[] getDescriptors();

    /**
     * @return The descriptor references.
     */
    String[] getDescriptorReferences();

    /**
     * @return The descriptor source directory.
     */
    File getDescriptorSourceDirectory();

    /**
     * @return The base directory.
     */
    File getBasedir();

    /**
     * @return The Maven Project.
     */
    MavenProject getProject();

    /**
     * @return The site directory.
     */
    File getSiteDirectory();

    /**
     * @return The final name.
     */
    String getFinalName();

    /**
     * @return append the assembly id.
     */
    boolean isAssemblyIdAppended();

    /**
     * @return Tar long file mode.
     */
    String getTarLongFileMode();

    /**
     * @return The output directory.
     */
    File getOutputDirectory();

    /**
     * @return The working directory.
     */
    File getWorkingDirectory();

    /**
     * @return the jar archive configuration.
     */
    MavenArchiveConfiguration getJarArchiveConfiguration();

    /**
     * @return The temporary root directory.
     */
    File getTemporaryRootDirectory();

    /**
     * @return The archive base directory.
     */
    File getArchiveBaseDirectory();

    /**
     * @return The filters.
     */
    List<String> getFilters();

    /**
     * @return the additional properties
     */
    Properties getAdditionalProperties();

    /**
     * @return include the project build filters or not.
     */
    boolean isIncludeProjectBuildFilters();

    /**
     * @return The list of reactor projects.
     */
    List<MavenProject> getReactorProjects();

    /**
     * @return Is this a test run.
     */
    boolean isDryRun();

    /**
     * @return Ignore directory format extensions.
     */
    boolean isIgnoreDirFormatExtensions();

    /**
     * @return Ignore missing descriptor.
     */
    boolean isIgnoreMissingDescriptor();

    /**
     * @return The maven session.
     */
    MavenSession getMavenSession();

    /**
     * @return The archiver configuration.
     */
    String getArchiverConfig();

    /**
     * Maven shared filtering utility.
     *
     * @return the maven reader filter
     */
    MavenReaderFilter getMavenReaderFilter();

    /**
     * @return Update only yes/no.
     */
    boolean isUpdateOnly();

    /**
     * @return Ignore permissions yes/no.
     */
    boolean isIgnorePermissions();

    /**
     * @return The current encoding.
     */
    String getEncoding();

    /**
     * @return The escape string.
     */
    String getEscapeString();

    /**
     * @return The list of delimiters.
     */
    List<String> getDelimiters();

    FixedStringSearchInterpolator getRepositoryInterpolator();

    /**
     * Gets an interpolator from environment variables and stuff
     */
    FixedStringSearchInterpolator getCommandLinePropsInterpolator();

    /**
     * Gets an interpolator from environment variables and stuff
     */
    FixedStringSearchInterpolator getEnvInterpolator();

    FixedStringSearchInterpolator getMainProjectInterpolator();

    /**
     * @return Override UID.
     */
    Integer getOverrideUid();

    /**
     * @return Override user name.
     */
    String getOverrideUserName();

    /**
     * @return Override GID.
     */
    Integer getOverrideGid();

    /**
     * @return Override group name.
     */
    String getOverrideGroupName();

    /**
     * @return Indicates if zip archives being added to the assembly should be compressed again.
     */
    boolean isRecompressZippedFiles();

    /**
     *
     * @return the merge manifest mode in the JarArchiver
     */
    String getMergeManifestMode();
}
