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
package org.apache.maven.plugins.assembly.utils;

import java.io.File;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.DefaultAssemblyArchiverTest;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AssemblyFormatUtilsTest {
    @Test
    public void testFixRelativePathRefsShouldRemoveRelativeRefToCurrentDir() {
        assertEquals("path/", AssemblyFormatUtils.fixRelativeRefs("./path/"));
    }

    @Test
    public void testFixRelativePathRefsShouldRemoveEmbeddedSameDirRef() {
        assertEquals("some/path/", AssemblyFormatUtils.fixRelativeRefs("some/./path/"));
        assertEquals("some\\path\\", AssemblyFormatUtils.fixRelativeRefs("some\\.\\path\\"));
    }

    @Test
    public void testFixRelativePathRefsShouldRemoveEmbeddedParentDirRef() {
        assertEquals("path/", AssemblyFormatUtils.fixRelativeRefs("some/../path/"));
    }

    @Test
    public void testFixRelativePathRefsShouldTruncateRelativeRefToParentDir() {
        assertEquals("path/", AssemblyFormatUtils.fixRelativeRefs("../path/"));
    }

    @Test
    public void testGetDistroNameShouldUseJustFinalNameWithNoAppendAssemblyIdOrClassifier() {
        verifyDistroName("assembly", "finalName", false, "finalName");
    }

    @Test
    public void testGetDistroNameShouldUseFinalNamePlusAssemblyIdIsNull() {
        verifyDistroName("assembly", "finalName", true, "finalName-assembly");
    }

    @Test
    public void testGetOutputDirShouldResolveGroupIdInOutDirUseArtifactInfo() throws Exception {
        verifyOutputDirUsingArtifactProject("${artifact.groupId}", null, "group", null, null, null, null, "group/");
    }

    @Test
    public void testGetOutputDirShouldResolveArtifactIdInOutDirUseArtifactInfo() throws Exception {
        verifyOutputDirUsingArtifactProject(
                "${artifact.artifactId}", null, null, "artifact", null, null, null, "artifact/");
    }

    @Test
    public void testGetOutputDirShouldResolveVersionInOutDirUseArtifactInfo() throws Exception {
        verifyOutputDirUsingArtifactProject("${artifact.version}", null, null, null, "version", null, null, "version/");
    }

    @Test
    public void testGetOutputDirShouldResolveBuildFinalNameInOutDirUseArtifactInfo() throws Exception {
        verifyOutputDirUsingArtifactProject(
                "${artifact.build.finalName}", null, null, null, null, "finalName", null, "finalName/");
    }

    @Test
    public void testGetOutputDirShouldResolveGroupIdInOutDirUseModuleInfo() throws Exception {
        verifyOutputDirUsingModuleProject("${module.groupId}", null, "group", null, null, null, null, "group/");
    }

    @Test
    public void testGetOutputDirShouldResolveArtifactIdInOutDirUseModuleInfo() throws Exception {
        verifyOutputDirUsingModuleProject(
                "${module.artifactId}", null, null, "artifact", null, null, null, "artifact/");
    }

    @Test
    public void testGetOutputDirShouldResolveVersionInOutDirUseModuleInfo() throws Exception {
        verifyOutputDirUsingModuleProject("${module.version}", null, null, null, "version", null, null, "version/");
    }

    @Test
    public void testGetOutputDirShouldResolveBuildFinalNameInOutDirUseModuleInfo() throws Exception {
        verifyOutputDirUsingModuleProject(
                "${module.build.finalName}", null, null, null, null, "finalName", null, "finalName/");
    }

    @Test
    public void testGetOutputDirShouldResolveGroupIdInOutDirUseExplicitMainProject() throws Exception {
        verifyOutputDirUsingMainProject("${pom.groupId}", null, "group", null, null, null, null, "group/");
    }

    @Test
    public void testGetOutputDirShouldResolveArtifactIdInOutDirUseExplicitMainProject() throws Exception {
        verifyOutputDirUsingMainProject("${pom.artifactId}", null, null, "artifact", null, null, null, "artifact/");
    }

    @Test
    public void testGetOutputDirShouldResolveVersionInOutDirUseExplicitMainProject() throws Exception {
        verifyOutputDirUsingMainProject("${pom.version}", null, null, null, "version", null, null, "version/");
    }

    @Test
    public void testGetOutputDirShouldResolveBuildFinalNameInOutDirUseExplicitMainProject() throws Exception {
        verifyOutputDirUsingMainProject(
                "${pom.build.finalName}", null, null, null, null, "finalName", null, "finalName/");
    }

    @Test
    public void testGetOutputDirShouldResolveGroupIdInOutDirUseExplicitMainProjectProjectRef() throws Exception {
        verifyOutputDirUsingMainProject("${project.groupId}", null, "group", null, null, null, null, "group/");
    }

    @Test
    public void testGetOutputDirShouldResolveArtifactIdInOutDirUseExplicitMainProjectProjectRef() throws Exception {
        verifyOutputDirUsingMainProject("${project.artifactId}", null, null, "artifact", null, null, null, "artifact/");
    }

    @Test
    public void testGetOutputDirShouldResolveVersionInOutDirUseExplicitMainProjectProjectRef() throws Exception {
        verifyOutputDirUsingMainProject("${project.version}", null, null, null, "version", null, null, "version/");
    }

    @Test
    public void testGetOutputDirShouldResolveBuildFinalNameInOutDirUseExplicitMainProjectProjectRef() throws Exception {
        verifyOutputDir("${project.build.finalName}", null, "finalName", "finalName/");
    }

    @Test
    public void testGetOutputDirShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions() throws Exception {
        verifyOutputDir("dir/", "finalName", null, "dir/");
    }

    @Test
    public void testGetOutputDirShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressionsCheckWithBackslash()
            throws Exception {
        verifyOutputDir("dir\\", "finalName", null, "dir\\");
    }

    @Test
    public void testGetOutputDirShouldAppendSlashToOutDirWhenMissingAndIncludeBaseFalseAndNoExpressions()
            throws Exception {
        verifyOutputDir("dir", "finalName", null, "dir/");
    }

    @Test
    public void testGetOutputDirShouldResolveGroupIdInOutDir() throws Exception {
        verifyOutputDirUsingMainProject("${groupId}", "finalName", "group", null, null, null, null, "group/");
    }

    @Test
    public void testGetOutputDirShouldResolveArtifactIdInOutDir() throws Exception {
        verifyOutputDirUsingMainProject("${artifactId}", "finalName", null, "artifact", null, null, null, "artifact/");
    }

    @Test
    public void testGetOutputDirShouldResolveVersionInOutDir() throws Exception {
        verifyOutputDirUsingMainProject("${version}", "finalName", null, null, "version", null, null, "version/");
    }

    @Test
    public void testGetOutputDirShouldResolveVersionInLargerOutDirExpr() throws Exception {
        verifyOutputDirUsingMainProject(
                "my-special-${version}", "finalName", null, null, "99", null, null, "my-special-99/");
    }

    @Test
    public void testGetOutputDirShouldResolveFinalNameInOutDir() throws Exception {
        verifyOutputDir("${finalName}", "finalName", null, "finalName/");
    }

    @Test
    public void testGetOutputDirShouldResolveBuildFinalNameInOutDir() throws Exception {
        verifyOutputDir("${build.finalName}", "finalName", null, "finalName/");
    }

    @Test
    public void testGetOutputDirShouldReturnEmptyPathWhenAllInputIsEmptyAndIncludeBaseFalse() throws Exception {
        verifyOutputDir(null, null, null, "");
    }

    @Test
    public void testGetOutputDirShouldRemoveRelativeRefToCurrentDir() throws Exception {
        verifyOutputDir("./path/", null, null, "path/");
    }

    @Test
    public void testGetOutputDirShouldRemoveEmbeddedSameDirRef() throws Exception {
        verifyOutputDir("some/./path/", null, null, "some/path/");
    }

    @Test
    public void testGetOutputDirShouldRemoveEmbeddedParentDirRef() throws Exception {
        verifyOutputDir("some/../path/", null, null, "path/");
    }

    @Test
    public void testGetOutputDirShouldTruncateRelativeRefToParentDir() throws Exception {
        verifyOutputDir("../path/", null, null, "path/");
    }

    @Test
    public void testGetOutputDirShouldResolveProjectProperty() throws Exception {
        final Properties props = new Properties();
        props.setProperty("myProperty", "value");

        verifyOutputDirUsingMainProject("file.${myProperty}", null, null, null, null, null, props, "file.value/");
    }

    @Test
    public void testGetOutputDirShouldResolveProjectPropertyAltExpr() throws Exception {
        final Properties props = new Properties();
        props.setProperty("myProperty", "value");

        verifyOutputDirUsingMainProject(
                "file.${pom.properties.myProperty}", null, null, null, null, null, props, "file.value/");
    }

    @Test
    public void
            testEvalFileNameMappingShouldResolveArtifactIdAndBaseVersionInOutDirUseArtifactInfoWithValidMainProject() {
        final MavenProject mainProject = createProject("group", "main", "1", null);

        final String artifactVersion = "2-20070807.112233-1";
        final String artifactBaseVersion = "2-SNAPSHOT";
        final MavenProject artifactProject = createProject("group", "artifact", artifactVersion, null);

        Artifact artifact = mock(Artifact.class);
        when(artifact.getGroupId()).thenReturn("group");
        when(artifact.getBaseVersion()).thenReturn(artifactBaseVersion);

        artifactProject.setArtifact(artifact);

        final MavenSession session = mock(MavenSession.class);

        final AssemblerConfigurationSource cs = mock(AssemblerConfigurationSource.class);
        when(cs.getMavenSession()).thenReturn(session);
        DefaultAssemblyArchiverTest.setupInterpolators(cs, mainProject);

        final String result = AssemblyFormatUtils.evaluateFileNameMapping(
                "${artifact.artifactId}-${artifact.baseVersion}",
                artifact,
                mainProject,
                null,
                cs,
                AssemblyFormatUtils.moduleProjectInterpolator(null),
                AssemblyFormatUtils.artifactProjectInterpolator(artifactProject));

        assertEquals("artifact-2-SNAPSHOT", result);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(cs).getMavenSession();
    }

    @Test
    public void testEvalFileNameMappingShouldResolveGroupIdInOutDirUseArtifactInfo() {
        verifyEvalFileNameMappingUsingArtifactProject(
                "${artifact.groupId}", null, "group", null, null, null, "group", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveArtifactIdInOutDirUseArtifactInfo() {
        verifyEvalFileNameMappingUsingArtifactProject(
                "${artifact.artifactId}", null, null, "artifact", null, null, "artifact", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveVersionInOutDirUseArtifactInfo() {
        verifyEvalFileNameMappingUsingArtifactProject(
                "${artifact.version}", null, null, null, "version", null, "version", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveGroupIdInOutDirUseArtifactInfoAndModulePrefix() {
        verifyEvalFileNameMappingUsingModuleProject(
                "${module.groupId}", null, "group", null, null, null, "group", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveArtifactIdInOutDirUseArtifactInfoAndModulePrefix() {
        verifyEvalFileNameMappingUsingModuleProject(
                "${module.artifactId}", null, null, "artifact", null, null, "artifact", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveVersionInOutDirUseArtifactInfoAndModulePrefix() {
        verifyEvalFileNameMappingUsingModuleProject(
                "${module.version}", null, null, null, "version", null, "version", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveGroupIdInOutDirUseExplicitMainProject() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("${pom.groupId}", null, "group", null, null, null, "group", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveArtifactIdInOutDirUseExplicitMainProject() throws Exception {
        verifyEvalFileNameMappingUsingMainProject(
                "${pom.artifactId}", null, null, "artifact", null, null, "artifact", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveVersionInOutDirUseExplicitMainProject() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("${pom.version}", null, null, null, "version", null, "version", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveGroupIdInOutDirUseExplicitMainProjectProjectRef() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("${project.groupId}", null, "group", null, null, null, "group", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveArtifactIdInOutDirUseExplicitMainProjectProjectRef()
            throws Exception {
        verifyEvalFileNameMappingUsingMainProject(
                "${project.artifactId}", null, null, "artifact", null, null, "artifact", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveVersionInOutDirUseExplicitMainProjectProjectRef() throws Exception {
        verifyEvalFileNameMappingUsingMainProject(
                "${project.version}", null, null, null, "version", null, "version", null);
    }

    @Test
    public void testEvalFileNameMappingShouldRemoveRelativeRefToCurrentDir() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("./path/", null, null, null, null, null, "path/", null);
    }

    @Test
    public void testEvalFileNameMappingShouldRemoveEmbeddedSameDirRef() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("some/./path/", null, null, null, null, null, "some/path/", null);
    }

    @Test
    public void testEvalFileNameMappingShouldRemoveEmbeddedParentDirRef() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("some/../path/", null, null, null, null, null, "path/", null);
    }

    @Test
    public void testEvalFileNameMappingShouldTruncateRelativeRefToParentDir() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("../path/", null, null, null, null, null, "path/", null);
    }

    @Test
    public void testEvalFileNameMappingShouldPassExpressionThroughUnchanged() throws Exception {
        verifyEvalFileNameMapping("filename", null, null, "filename", null);
    }

    @Test
    public void testEvalFileNameMappingShouldInsertClassifierAheadOfExtension() throws Exception {
        verifyEvalFileNameMapping(
                "filename-${artifact.classifier}.ext", "classifier", null, "filename-classifier.ext", null);
    }

    @Test
    public void testEvalFileNameMappingShouldAppendDashClassifierWhenClassifierPresent() throws Exception {
        verifyEvalFileNameMapping("filename${dashClassifier?}", "classifier", null, "filename-classifier", null);
    }

    @Test
    public void testEvalFileNameMappingShouldNotAppendDashClassifierWhenClassifierMissing() throws Exception {
        verifyEvalFileNameMapping("filename${dashClassifier?}", null, null, "filename", null);
    }

    @Test
    public void testEvalFileNameMappingShouldNotAppendDashClassifierWhenClassifierEmpty() throws Exception {
        verifyEvalFileNameMapping("filename${dashClassifier?}", "", null, "filename", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveGroupId() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("${groupId}", null, "group", null, null, null, "group", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveArtifactId() throws Exception {
        verifyEvalFileNameMappingUsingMainProject(
                "${artifactId}", null, null, "artifact", null, null, "artifact", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveVersion() throws Exception {
        verifyEvalFileNameMappingUsingMainProject("${version}", null, null, null, "version", null, "version", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveExtension() throws Exception {
        verifyEvalFileNameMapping("file.${artifact.extension}", null, "ext", "file.ext", null);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveProjectProperty() throws Exception {
        final Properties props = new Properties();
        props.setProperty("myProperty", "value");

        verifyEvalFileNameMapping("file.${myProperty}", null, null, "file.value", props);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveProjectPropertyAltExpr() throws Exception {
        final Properties props = new Properties();
        props.setProperty("myProperty", "value");

        verifyEvalFileNameMapping("file.${pom.properties.myProperty}", null, null, "file.value", props);
    }

    @Test
    public void testEvalFileNameMappingShouldResolveSystemPropertyWithoutMainProjectPresent() throws Exception {
        verifyEvalFileNameMapping(
                "file.${java.version}", null, null, "file." + System.getProperty("java.version"), null);
    }

    private void verifyEvalFileNameMapping(
            final String expression,
            final String classifier,
            final String extension,
            final String checkValue,
            final Properties projectProperties)
            throws AssemblyFormattingException {
        verifyEvalFileNameMappingUsingMainProject(
                expression, classifier, null, null, null, extension, checkValue, projectProperties);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void verifyEvalFileNameMappingUsingMainProject(
            final String expression,
            final String classifier,
            final String groupId,
            final String artifactId,
            final String version,
            final String extension,
            final String checkValue,
            final Properties projectProperties) {
        final MavenProject mainProject = createProject(groupId, artifactId, version, projectProperties);
        final MavenProject artifactProject = createProject("unknown", "unknown", "unknown", null);
        final MavenProject moduleProject = createProject("unknown", "unknown", "unknown", null);

        verifyEvalFileNameMapping(
                expression, classifier, extension, mainProject, moduleProject, artifactProject, checkValue);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void verifyEvalFileNameMappingUsingArtifactProject(
            final String expression,
            final String classifier,
            final String groupId,
            final String artifactId,
            final String version,
            final String extension,
            final String checkValue,
            final Properties projectProperties) {
        final MavenProject artifactProject = createProject(groupId, artifactId, version, projectProperties);

        final MavenProject mainProject = createProject("unknown", "unknown", "unknown", null);
        final MavenProject moduleProject = createProject("unknown", "unknown", "unknown", null);

        verifyEvalFileNameMapping(
                expression, classifier, extension, mainProject, moduleProject, artifactProject, checkValue);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void verifyEvalFileNameMappingUsingModuleProject(
            final String expression,
            final String classifier,
            final String groupId,
            final String artifactId,
            final String version,
            final String extension,
            final String checkValue,
            final Properties projectProperties) {
        final MavenProject moduleProject = createProject(groupId, artifactId, version, projectProperties);

        final MavenProject mainProject = createProject("unknown", "unknown", "unknown", null);
        final MavenProject artifactProject = createProject("unknown", "unknown", "unknown", null);

        verifyEvalFileNameMapping(
                expression, classifier, extension, mainProject, moduleProject, artifactProject, checkValue);
    }

    private MavenProject createProject(
            String groupId, String artifactId, String version, final Properties projectProperties) {
        if (artifactId == null) {
            artifactId = "artifact";
        }

        if (groupId == null) {
            groupId = "group";
        }

        if (version == null) {
            version = "version";
        }

        final Model model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);

        model.setProperties(projectProperties);

        return new MavenProject(model);
    }

    private void verifyEvalFileNameMapping(
            final String expression,
            final String classifier,
            final String extension,
            final MavenProject mainProject,
            final MavenProject moduleProject,
            final MavenProject artifactProject,
            final String checkValue) {

        Artifact artifactMock = mock(Artifact.class);
        when(artifactMock.getGroupId()).thenReturn(artifactProject.getGroupId());
        when(artifactMock.getClassifier()).thenReturn(classifier);
        ArtifactHandler artifactHandler = mock(ArtifactHandler.class);
        when(artifactHandler.getExtension()).thenReturn(extension);
        when(artifactMock.getArtifactHandler()).thenReturn(artifactHandler);

        Artifact moduleArtifactMock = mock(Artifact.class);
        when(moduleArtifactMock.getGroupId()).thenReturn(moduleProject.getGroupId());

        final MavenSession session = mock(MavenSession.class);
        when(session.getUserProperties()).thenReturn(new Properties());
        when(session.getSystemProperties()).thenReturn(System.getProperties());

        final AssemblerConfigurationSource cs = mock(AssemblerConfigurationSource.class);
        when(cs.getMavenSession()).thenReturn(session);

        DefaultAssemblyArchiverTest.setupInterpolators(cs, mainProject);

        final String result = AssemblyFormatUtils.evaluateFileNameMapping(
                expression,
                artifactMock,
                mainProject,
                moduleArtifactMock,
                cs,
                AssemblyFormatUtils.moduleProjectInterpolator(moduleProject),
                AssemblyFormatUtils.artifactProjectInterpolator(artifactProject));

        assertEquals(checkValue, result);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(cs).getMavenSession();
    }

    private void verifyOutputDir(
            final String outDir, final String finalName, final String projectFinalName, final String checkValue)
            throws AssemblyFormattingException {
        verifyOutputDirUsingMainProject(outDir, finalName, null, null, null, projectFinalName, null, checkValue);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void verifyOutputDirUsingMainProject(
            final String outDir,
            final String finalName,
            final String groupId,
            final String artifactId,
            final String version,
            final String projectFinalName,
            final Properties properties,
            final String checkValue)
            throws AssemblyFormattingException {
        final MavenProject project = createProject(groupId, artifactId, version, properties);

        if (projectFinalName != null) {
            final Build build = new Build();
            build.setFinalName(projectFinalName);

            project.getModel().setBuild(build);
        }

        final MavenProject moduleProject = createProject("unknown", "unknown", "unknown", null);
        final MavenProject artifactProject = createProject("unknown", "unknown", "unknown", null);

        verifyOutputDir(outDir, finalName, project, moduleProject, artifactProject, checkValue);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void verifyOutputDirUsingModuleProject(
            final String outDir,
            final String finalName,
            final String groupId,
            final String artifactId,
            final String version,
            final String projectFinalName,
            final Properties properties,
            final String checkValue)
            throws AssemblyFormattingException {
        final MavenProject project = createProject(groupId, artifactId, version, properties);

        if (projectFinalName != null) {
            final Build build = new Build();
            build.setFinalName(projectFinalName);

            project.getModel().setBuild(build);
        }

        final MavenProject mainProject = createProject("unknown", "unknown", "unknown", null);
        final MavenProject artifactProject = createProject("unknown", "unknown", "unknown", null);

        verifyOutputDir(outDir, finalName, mainProject, project, artifactProject, checkValue);
    }

    @SuppressWarnings("checkstyle:parameternumber")
    private void verifyOutputDirUsingArtifactProject(
            final String outDir,
            final String finalName,
            final String groupId,
            final String artifactId,
            final String version,
            final String projectFinalName,
            final Properties properties,
            final String checkValue)
            throws AssemblyFormattingException {
        final MavenProject project = createProject(groupId, artifactId, version, properties);

        if (projectFinalName != null) {
            final Build build = new Build();
            build.setFinalName(projectFinalName);

            project.getModel().setBuild(build);
        }

        final MavenProject moduleProject = createProject("unknown", "unknown", "unknown", null);
        final MavenProject mainProject = createProject("unknown", "unknown", "unknown", null);

        verifyOutputDir(outDir, finalName, mainProject, moduleProject, project, checkValue);
    }

    private void verifyOutputDir(
            final String outDir,
            final String finalName,
            final MavenProject mainProject,
            final MavenProject moduleProject,
            final MavenProject artifactProject,
            final String checkValue)
            throws AssemblyFormattingException {

        final MavenSession session = mock(MavenSession.class);
        when(session.getUserProperties()).thenReturn(new Properties());
        when(session.getSystemProperties()).thenReturn(System.getProperties());

        final AssemblerConfigurationSource cs = mock(AssemblerConfigurationSource.class);
        when(cs.getMavenSession()).thenReturn(session);

        DefaultAssemblyArchiverTest.setupInterpolators(cs, mainProject);

        String result = AssemblyFormatUtils.getOutputDirectory(
                outDir,
                finalName,
                cs,
                AssemblyFormatUtils.moduleProjectInterpolator(moduleProject),
                AssemblyFormatUtils.artifactProjectInterpolator(artifactProject));

        assertEquals(checkValue, result);

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify(cs).getMavenSession();
    }

    private void verifyDistroName(
            final String assemblyId, final String finalName, final boolean appendAssemblyId, final String checkValue) {
        final AssemblerConfigurationSource configSource = mock(AssemblerConfigurationSource.class);
        when(configSource.isAssemblyIdAppended()).thenReturn(appendAssemblyId);
        when(configSource.getFinalName()).thenReturn(finalName);

        final Assembly assembly = new Assembly();
        assembly.setId(assemblyId);

        final String result = AssemblyFormatUtils.getDistributionName(assembly, configSource);

        assertEquals(checkValue, result);

        verify(configSource, atLeast(1)).isAssemblyIdAppended();
        verify(configSource, atLeast(1)).getFinalName();
    }

    @Test
    public void testWindowsPath() {
        assertTrue(AssemblyFormatUtils.isWindowsPath("C:\foobar"));
    }

    @Test
    public void testLinuxRootReferencePath() {
        assertTrue(AssemblyFormatUtils.isUnixRootReference("/etc/home"));
    }

    @Test
    public void groupIdPathArtifactProjectInterpolator() {
        Artifact artifact = mock(Artifact.class);
        when(artifact.getFile()).thenReturn(new File("dir", "artifactId.jar"));

        MavenProject project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("a.b.c");
        when(project.getArtifact()).thenReturn(artifact);

        FixedStringSearchInterpolator interpolator = AssemblyFormatUtils.artifactProjectInterpolator(project);
        assertEquals("a/b/c", interpolator.interpolate("${artifact.groupIdPath}"));
        assertEquals("a/b/c/artifactId.jar", interpolator.interpolate("${artifact.groupIdPath}/${artifact.file.name}"));
    }

    @Test
    public void groupIdPathArtifactInterpolator() {
        Artifact artifact = mock(Artifact.class);
        when(artifact.getGroupId()).thenReturn("a.b.c");
        when(artifact.getFile()).thenReturn(new File("dir", "artifactId.jar"));

        FixedStringSearchInterpolator interpolator = AssemblyFormatUtils.artifactInterpolator(artifact);
        assertEquals("a/b/c", interpolator.interpolate("${artifact.groupIdPath}"));
        assertEquals("a/b/c/artifactId.jar", interpolator.interpolate("${artifact.groupIdPath}/${artifact.file.name}"));
    }
}
