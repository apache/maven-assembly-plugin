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
package org.apache.maven.plugins.assembly.testutils;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.MavenReaderFilter;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;

public class PojoConfigSource implements AssemblerConfigurationSource {
    String descriptor;

    private File basedir;

    private MavenProject mavenProject;

    private boolean isSitencluded;

    private File siteDirectory;

    private String decriptorId;

    private String finalName;

    private List<String> delimiters;

    private String escapeString;

    private String encoding;

    private boolean isUpdateOnly;

    private boolean isUseJvmChmod;

    private boolean isIgnorePermissions;

    private String archiverConfig;

    private boolean isAssemblyIdAppended;

    private String classifier;

    private String tarLongFileMode;

    private File workingDirectory;

    private MavenArchiveConfiguration jarArchiveConfiguration;

    private MavenReaderFilter mavenReaderFilter;

    private File outputDirectory;

    private String[] descriptors;

    private String[] descriptorReferences;

    private File descriptorSourceReference;

    private ArtifactRepository localRepository;

    private File temporaryRootDirectory;

    private File archiveBaseDirectory;

    private List<String> filters;

    private Properties additionalProperties;

    private boolean isIncludeProjectBuildFilter;

    private List<MavenProject> reactorProjects;

    private List<ArtifactRepository> remoteRepository;

    private boolean isDryRun;

    private boolean isIgnoreDirFormatExtensions;

    private boolean isIgnoreMissingDescriptor;

    private MavenSession mavenSession;

    private FixedStringSearchInterpolator rootInterpolator = FixedStringSearchInterpolator.empty();

    private FixedStringSearchInterpolator environmentInterpolator = FixedStringSearchInterpolator.empty();

    private FixedStringSearchInterpolator envInterpolator = FixedStringSearchInterpolator.empty();

    private FixedStringSearchInterpolator mainProjectInterpolator;

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getDescriptorId() {
        return decriptorId;
    }

    public String[] getDescriptors() {
        return descriptors;
    }

    public void setDescriptors(String[] descriptors) {
        this.descriptors = descriptors;
    }

    public String[] getDescriptorReferences() {
        return descriptorReferences;
    }

    public void setDescriptorReferences(String[] descriptorReferences) {
        this.descriptorReferences = descriptorReferences;
    }

    public File getDescriptorSourceDirectory() {
        return descriptorSourceReference;
    }

    public File getBasedir() {
        return basedir;
    }

    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    public MavenProject getProject() {
        return mavenProject;
    }

    public boolean isSiteIncluded() {
        return isSitencluded;
    }

    public File getSiteDirectory() {
        return siteDirectory;
    }

    public void setSiteDirectory(File siteDirectory) {
        this.siteDirectory = siteDirectory;
    }

    public String getFinalName() {
        return finalName;
    }

    public void setFinalName(String finalName) {
        this.finalName = finalName;
    }

    public boolean isAssemblyIdAppended() {
        return isAssemblyIdAppended;
    }

    public void setAssemblyIdAppended(boolean isAssemblyIdAppended) {
        this.isAssemblyIdAppended = isAssemblyIdAppended;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getTarLongFileMode() {
        return tarLongFileMode;
    }

    public void setTarLongFileMode(String tarLongFileMode) {
        this.tarLongFileMode = tarLongFileMode;
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public MavenArchiveConfiguration getJarArchiveConfiguration() {
        return jarArchiveConfiguration;
    }

    public void setJarArchiveConfiguration(MavenArchiveConfiguration jarArchiveConfiguration) {
        this.jarArchiveConfiguration = jarArchiveConfiguration;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    public File getTemporaryRootDirectory() {
        return temporaryRootDirectory;
    }

    public void setTemporaryRootDirectory(File temporaryRootDirectory) {
        this.temporaryRootDirectory = temporaryRootDirectory;
    }

    public File getArchiveBaseDirectory() {
        return archiveBaseDirectory;
    }

    public void setArchiveBaseDirectory(File archiveBaseDirectory) {
        this.archiveBaseDirectory = archiveBaseDirectory;
    }

    public List<String> getFilters() {
        return filters;
    }

    public void setFilters(List<String> filters) {
        this.filters = filters;
    }

    @Override
    public Properties getAdditionalProperties() {
        return additionalProperties;
    }

    public void setAdditionalProperties(Properties additionalProperties) {
        this.additionalProperties = additionalProperties;
    }

    public boolean isIncludeProjectBuildFilters() {
        return isIncludeProjectBuildFilter;
    }

    public List<MavenProject> getReactorProjects() {
        return reactorProjects;
    }

    public void setReactorProjects(List<MavenProject> reactorProjects) {
        this.reactorProjects = reactorProjects;
    }

    public List<ArtifactRepository> getRemoteRepositories() {
        return remoteRepository;
    }

    public boolean isDryRun() {
        return isDryRun;
    }

    public void setDryRun(boolean isDryRun) {
        this.isDryRun = isDryRun;
    }

    public boolean isIgnoreDirFormatExtensions() {
        return isIgnoreDirFormatExtensions;
    }

    public void setIgnoreDirFormatExtensions(boolean isIgnoreDirFormatExtensions) {
        this.isIgnoreDirFormatExtensions = isIgnoreDirFormatExtensions;
    }

    public boolean isIgnoreMissingDescriptor() {
        return isIgnoreMissingDescriptor;
    }

    public void setIgnoreMissingDescriptor(boolean isIgnoreMissingDescriptor) {
        this.isIgnoreMissingDescriptor = isIgnoreMissingDescriptor;
    }

    public MavenSession getMavenSession() {
        return mavenSession;
    }

    public void setMavenSession(MavenSession mavenSession) {
        this.mavenSession = mavenSession;
    }

    public String getArchiverConfig() {
        return archiverConfig;
    }

    public void setArchiverConfig(String archiverConfig) {
        this.archiverConfig = archiverConfig;
    }

    public MavenReaderFilter getMavenReaderFilter() {
        return mavenReaderFilter;
    }

    public void setMavenReaderFilter(MavenReaderFilter mavenReaderFilter) {
        this.mavenReaderFilter = mavenReaderFilter;
    }

    public boolean isUpdateOnly() {
        return isUpdateOnly;
    }

    public void setUpdateOnly(boolean isUpdateOnly) {
        this.isUpdateOnly = isUpdateOnly;
    }

    public boolean isUseJvmChmod() {
        return isUseJvmChmod;
    }

    public void setUseJvmChmod(boolean isUseJvmChmod) {
        this.isUseJvmChmod = isUseJvmChmod;
    }

    public boolean isIgnorePermissions() {
        return isIgnorePermissions;
    }

    public void setIgnorePermissions(boolean isIgnorePermissions) {
        this.isIgnorePermissions = isIgnorePermissions;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEscapeString() {
        return escapeString;
    }

    public void setEscapeString(String escapeString) {
        this.escapeString = escapeString;
    }

    public List<String> getDelimiters() {
        return delimiters;
    }

    public void setDelimiters(List<String> delimiters) {
        this.delimiters = delimiters;
    }

    public void setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    public void setSitencluded(boolean isSitencluded) {
        this.isSitencluded = isSitencluded;
    }

    public void setDecriptorId(String decriptorId) {
        this.decriptorId = decriptorId;
    }

    public void setDescriptorSourceReference(File descriptorSourceReference) {
        this.descriptorSourceReference = descriptorSourceReference;
    }

    public void setIncludeProjectBuildFilter(boolean isIncludeProjectBuildFilter) {
        this.isIncludeProjectBuildFilter = isIncludeProjectBuildFilter;
    }

    public void setRemoteRepository(List<ArtifactRepository> remoteRepository) {
        this.remoteRepository = remoteRepository;
    }

    public FixedStringSearchInterpolator getRepositoryInterpolator() {
        return rootInterpolator;
    }

    public FixedStringSearchInterpolator getCommandLinePropsInterpolator() {
        return environmentInterpolator;
    }

    public void setRootInterpolator(FixedStringSearchInterpolator rootInterpolator) {
        this.rootInterpolator = rootInterpolator;
    }

    public FixedStringSearchInterpolator getEnvInterpolator() {
        return envInterpolator;
    }

    public void setEnvInterpolator(FixedStringSearchInterpolator envInterpolator) {
        this.envInterpolator = envInterpolator;
    }

    public FixedStringSearchInterpolator getMainProjectInterpolator() {
        return mainProjectInterpolator;
    }

    public void setMainProjectInterpolator(FixedStringSearchInterpolator mainProjectInterpolator) {
        this.mainProjectInterpolator = mainProjectInterpolator;
    }

    public void setEnvironmentInterpolator(FixedStringSearchInterpolator environmentInterpolator) {
        this.environmentInterpolator = environmentInterpolator;
    }

    @Override
    public Integer getOverrideUid() {
        return 0;
    }

    @Override
    public String getOverrideUserName() {
        return "root";
    }

    @Override
    public Integer getOverrideGid() {
        return 0;
    }

    @Override
    public String getOverrideGroupName() {
        return "root";
    }
}
