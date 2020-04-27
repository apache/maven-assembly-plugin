package org.apache.maven.plugins.assembly.utils;

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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@RunWith( MockitoJUnitRunner.class )
public class AssemblyFormatUtilsTest
{
    @Test
    public void testFixRelativePathRefs_ShouldRemoveRelativeRefToCurrentDir()
        throws Exception
    {
        assertEquals( "path/", AssemblyFormatUtils.fixRelativeRefs( "./path/" ) );
    }

    @Test
    public void testFixRelativePathRefs_ShouldRemoveEmbeddedSameDirRef()
        throws Exception
    {
        assertEquals( "some/path/", AssemblyFormatUtils.fixRelativeRefs( "some/./path/" ) );
        assertEquals( "some\\path\\", AssemblyFormatUtils.fixRelativeRefs( "some\\.\\path\\" ) );
    }

    @Test
    public void testFixRelativePathRefs_ShouldRemoveEmbeddedParentDirRef()
        throws Exception
    {
        assertEquals( "path/", AssemblyFormatUtils.fixRelativeRefs( "some/../path/" ) );
    }

    @Test
    public void testFixRelativePathRefs_ShouldTruncateRelativeRefToParentDir()
        throws Exception
    {
        assertEquals( "path/", AssemblyFormatUtils.fixRelativeRefs( "../path/" ) );
    }

    @Test
    public void testGetDistroName_ShouldUseJustFinalNameWithNoAppendAssemblyIdOrClassifier()
    {
        verifyDistroName( "assembly", "finalName", false, "finalName" );
    }

    @Test
    public void testGetDistroName_ShouldUseFinalNamePlusAssemblyIdIsNull()
    {
        verifyDistroName( "assembly", "finalName", true, "finalName-assembly" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseArtifactInfo()
        throws Exception
    {
        verifyOutputDirUsingArtifactProject( "${artifact.groupId}", null, "group", null, null, null, null, "group/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseArtifactInfo()
        throws Exception
    {
        verifyOutputDirUsingArtifactProject( "${artifact.artifactId}", null, null, "artifact", null, null, null,
                                             "artifact/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseArtifactInfo()
        throws Exception
    {
        verifyOutputDirUsingArtifactProject( "${artifact.version}", null, null, null, "version", null, null,
                                             "version/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseArtifactInfo()
        throws Exception
    {
        verifyOutputDirUsingArtifactProject( "${artifact.build.finalName}", null, null, null, null, "finalName", null,
                                             "finalName/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseModuleInfo()
        throws Exception
    {
        verifyOutputDirUsingModuleProject( "${module.groupId}", null, "group", null, null, null, null, "group/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseModuleInfo()
        throws Exception
    {
        verifyOutputDirUsingModuleProject( "${module.artifactId}", null, null, "artifact", null, null, null,
                                           "artifact/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseModuleInfo()
        throws Exception
    {
        verifyOutputDirUsingModuleProject( "${module.version}", null, null, null, "version", null, null, "version/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseModuleInfo()
        throws Exception
    {
        verifyOutputDirUsingModuleProject( "${module.build.finalName}", null, null, null, null, "finalName", null,
                                           "finalName/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseExplicitMainProject()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${pom.groupId}", null, "group", null, null, null, null, "group/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${pom.artifactId}", null, null, "artifact", null, null, null, "artifact/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseExplicitMainProject()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${pom.version}", null, null, null, "version", null, null, "version/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseExplicitMainProject()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${pom.build.finalName}", null, null, null, null, "finalName", null,
                                         "finalName/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveGroupIdInOutDir_UseExplicitMainProject_projectRef()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${project.groupId}", null, "group", null, null, null, null, "group/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject_projectRef()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${project.artifactId}", null, null, "artifact", null, null, null,
                                         "artifact/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveVersionInOutDir_UseExplicitMainProject_projectRef()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${project.version}", null, null, null, "version", null, null, "version/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir_UseExplicitMainProject_projectRef()
        throws Exception
    {
        verifyOutputDir( "${project.build.finalName}", null, "finalName", "finalName/" );
    }

    @Test
    public void testGetOutputDir_ShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions()
        throws Exception
    {
        verifyOutputDir( "dir/", "finalName", null, "dir/" );
    }

    @Test
    public void testGetOutputDir_ShouldNotAlterOutDirWhenIncludeBaseFalseAndNoExpressions_CheckWithBackslash()
        throws Exception
    {
        verifyOutputDir( "dir\\", "finalName", null, "dir\\" );
    }

    @Test
    public void testGetOutputDir_ShouldAppendSlashToOutDirWhenMissingAndIncludeBaseFalseAndNoExpressions()
        throws Exception
    {
        verifyOutputDir( "dir", "finalName", null, "dir/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveGroupIdInOutDir()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${groupId}", "finalName", "group", null, null, null, null, "group/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveArtifactIdInOutDir()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${artifactId}", "finalName", null, "artifact", null, null, null,
                                         "artifact/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveVersionInOutDir()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "${version}", "finalName", null, null, "version", null, null, "version/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveVersionInLargerOutDirExpr()
        throws Exception
    {
        verifyOutputDirUsingMainProject( "my-special-${version}", "finalName", null, null, "99", null, null,
                                         "my-special-99/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveFinalNameInOutDir()
        throws Exception
    {
        verifyOutputDir( "${finalName}", "finalName", null, "finalName/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveBuildFinalNameInOutDir()
        throws Exception
    {
        verifyOutputDir( "${build.finalName}", "finalName", null, "finalName/" );
    }

    @Test
    public void testGetOutputDir_ShouldReturnEmptyPathWhenAllInputIsEmptyAndIncludeBaseFalse()
        throws Exception
    {
        verifyOutputDir( null, null, null, "" );
    }

    @Test
    public void testGetOutputDir_ShouldRemoveRelativeRefToCurrentDir()
        throws Exception
    {
        verifyOutputDir( "./path/", null, null, "path/" );
    }

    @Test
    public void testGetOutputDir_ShouldRemoveEmbeddedSameDirRef()
        throws Exception
    {
        verifyOutputDir( "some/./path/", null, null, "some/path/" );
    }

    @Test
    public void testGetOutputDir_ShouldRemoveEmbeddedParentDirRef()
        throws Exception
    {
        verifyOutputDir( "some/../path/", null, null, "path/" );
    }

    @Test
    public void testGetOutputDir_ShouldTruncateRelativeRefToParentDir()
        throws Exception
    {
        verifyOutputDir( "../path/", null, null, "path/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveProjectProperty()
        throws Exception
    {
        final Properties props = new Properties();
        props.setProperty( "myProperty", "value" );

        verifyOutputDirUsingMainProject( "file.${myProperty}", null, null, null, null, null, props, "file.value/" );
    }

    @Test
    public void testGetOutputDir_ShouldResolveProjectPropertyAltExpr()
        throws Exception
    {
        final Properties props = new Properties();
        props.setProperty( "myProperty", "value" );

        verifyOutputDirUsingMainProject( "file.${pom.properties.myProperty}", null, null, null, null, null, props,
                                         "file.value/" );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveArtifactIdAndBaseVersionInOutDir_UseArtifactInfo_WithValidMainProject()
        throws Exception
    {
        final MavenProject mainProject = createProject( "group", "main", "1", null );

        final String artifactVersion = "2-20070807.112233-1";
        final String artifactBaseVersion = "2-SNAPSHOT";
        final MavenProject artifactProject = createProject( "group", "artifact", artifactVersion, null );

        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "group" );
        when( artifact.getBaseVersion() ).thenReturn( artifactBaseVersion );

        artifactProject.setArtifact( artifact );

        final MavenSession session = mock( MavenSession.class );

        final AssemblerConfigurationSource cs = mock( AssemblerConfigurationSource.class );
        when( cs.getMavenSession() ).thenReturn( session );
        DefaultAssemblyArchiverTest.setupInterpolators( cs, mainProject );

        final String result =
            AssemblyFormatUtils.evaluateFileNameMapping( "${artifact.artifactId}-${artifact.baseVersion}",
                                                         artifact, mainProject, null, cs,
                                                         AssemblyFormatUtils.moduleProjectInterpolator( null ),
                                                         AssemblyFormatUtils.artifactProjectInterpolator( artifactProject ) );

        assertEquals( "artifact-2-SNAPSHOT", result );
        
        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( cs ).getMavenSession();
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseArtifactInfo()
        throws Exception
    {
        verifyEvalFileNameMappingUsingArtifactProject( "${artifact.groupId}", null, "group", null, null, null, "group",
                                                       null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseArtifactInfo()
        throws Exception
    {
        verifyEvalFileNameMappingUsingArtifactProject( "${artifact.artifactId}", null, null, "artifact", null, null,
                                                       "artifact", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseArtifactInfo()
        throws Exception
    {
        verifyEvalFileNameMappingUsingArtifactProject( "${artifact.version}", null, null, null, "version", null,
                                                       "version", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseArtifactInfoAndModulePrefix()
        throws Exception
    {
        verifyEvalFileNameMappingUsingModuleProject( "${module.groupId}", null, "group", null, null, null, "group",
                                                     null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseArtifactInfoAndModulePrefix()
        throws Exception
    {
        verifyEvalFileNameMappingUsingModuleProject( "${module.artifactId}", null, null, "artifact", null, null,
                                                     "artifact", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseArtifactInfoAndModulePrefix()
        throws Exception
    {
        verifyEvalFileNameMappingUsingModuleProject( "${module.version}", null, null, null, "version", null, "version",
                                                     null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseExplicitMainProject()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${pom.groupId}", null, "group", null, null, null, "group", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${pom.artifactId}", null, null, "artifact", null, null, "artifact",
                                                   null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseExplicitMainProject()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${pom.version}", null, null, null, "version", null, "version",
                                                   null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveGroupIdInOutDir_UseExplicitMainProject_projectRef()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${project.groupId}", null, "group", null, null, null, "group",
                                                   null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveArtifactIdInOutDir_UseExplicitMainProject_projectRef()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${project.artifactId}", null, null, "artifact", null, null,
                                                   "artifact", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveVersionInOutDir_UseExplicitMainProject_projectRef()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${project.version}", null, null, null, "version", null, "version",
                                                   null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldRemoveRelativeRefToCurrentDir()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "./path/", null, null, null, null, null, "path/", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldRemoveEmbeddedSameDirRef()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "some/./path/", null, null, null, null, null, "some/path/", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldRemoveEmbeddedParentDirRef()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "some/../path/", null, null, null, null, null, "path/", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldTruncateRelativeRefToParentDir()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "../path/", null, null, null, null, null, "path/", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldPassExpressionThroughUnchanged()
        throws Exception
    {
        verifyEvalFileNameMapping( "filename", null, null, "filename", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldInsertClassifierAheadOfExtension()
        throws Exception
    {
        verifyEvalFileNameMapping( "filename-${artifact.classifier}.ext", "classifier", null, "filename-classifier.ext",
                                   null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldAppendDashClassifierWhenClassifierPresent()
        throws Exception
    {
        verifyEvalFileNameMapping( "filename${dashClassifier?}", "classifier", null, "filename-classifier", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldNotAppendDashClassifierWhenClassifierMissing()
        throws Exception
    {
        verifyEvalFileNameMapping( "filename${dashClassifier?}", null, null, "filename", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldNotAppendDashClassifierWhenClassifierEmpty()
        throws Exception
    {
        verifyEvalFileNameMapping( "filename${dashClassifier?}", "", null, "filename", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveGroupId()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${groupId}", null, "group", null, null, null, "group", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveArtifactId()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${artifactId}", null, null, "artifact", null, null, "artifact",
                                                   null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveVersion()
        throws Exception
    {
        verifyEvalFileNameMappingUsingMainProject( "${version}", null, null, null, "version", null, "version", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveExtension()
        throws Exception
    {
        verifyEvalFileNameMapping( "file.${artifact.extension}", null, "ext", "file.ext", null );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveProjectProperty()
        throws Exception
    {
        final Properties props = new Properties();
        props.setProperty( "myProperty", "value" );

        verifyEvalFileNameMapping( "file.${myProperty}", null, null, "file.value", props );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveProjectPropertyAltExpr()
        throws Exception
    {
        final Properties props = new Properties();
        props.setProperty( "myProperty", "value" );

        verifyEvalFileNameMapping( "file.${pom.properties.myProperty}", null, null, "file.value", props );
    }

    @Test
    public void testEvalFileNameMapping_ShouldResolveSystemPropertyWithoutMainProjectPresent()
        throws Exception
    {
        verifyEvalFileNameMapping( "file.${java.version}", null, null, "file." + System.getProperty( "java.version" ),
                                   null );
    }

    private void verifyEvalFileNameMapping( final String expression, final String classifier, final String extension,
                                            final String checkValue, final Properties projectProperties )
        throws AssemblyFormattingException
    {
        verifyEvalFileNameMappingUsingMainProject( expression, classifier, null, null, null, extension, checkValue,
                                                   projectProperties );
    }

    private void verifyEvalFileNameMappingUsingMainProject( final String expression, final String classifier,
                                                            final String groupId, final String artifactId,
                                                            final String version, final String extension,
                                                            final String checkValue,
                                                            final Properties projectProperties )
        throws AssemblyFormattingException
    {
        final MavenProject mainProject = createProject( groupId, artifactId, version, projectProperties );

        final MavenProject artifactProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject moduleProject = createProject( "unknown", "unknown", "unknown", null );

        verifyEvalFileNameMapping( expression, classifier, extension, mainProject, moduleProject, artifactProject,
                                   checkValue );
    }

    private void verifyEvalFileNameMappingUsingArtifactProject( final String expression, final String classifier,
                                                                final String groupId, final String artifactId,
                                                                final String version, final String extension,
                                                                final String checkValue,
                                                                final Properties projectProperties )
        throws AssemblyFormattingException
    {
        final MavenProject artifactProject = createProject( groupId, artifactId, version, projectProperties );

        final MavenProject mainProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject moduleProject = createProject( "unknown", "unknown", "unknown", null );

        verifyEvalFileNameMapping( expression, classifier, extension, mainProject, moduleProject, artifactProject,
                                   checkValue );
    }

    private void verifyEvalFileNameMappingUsingModuleProject( final String expression, final String classifier,
                                                              final String groupId, final String artifactId,
                                                              final String version, final String extension,
                                                              final String checkValue,
                                                              final Properties projectProperties )
        throws AssemblyFormattingException
    {
        final MavenProject moduleProject = createProject( groupId, artifactId, version, projectProperties );

        final MavenProject mainProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject artifactProject = createProject( "unknown", "unknown", "unknown", null );

        verifyEvalFileNameMapping( expression, classifier, extension, mainProject, moduleProject, artifactProject,
                                   checkValue );
    }

    private MavenProject createProject( String groupId, String artifactId, String version,
                                        final Properties projectProperties )
    {
        if ( artifactId == null )
        {
            artifactId = "artifact";
        }

        if ( groupId == null )
        {
            groupId = "group";
        }

        if ( version == null )
        {
            version = "version";
        }

        final Model model = new Model();
        model.setGroupId( groupId );
        model.setArtifactId( artifactId );
        model.setVersion( version );

        model.setProperties( projectProperties );

        return new MavenProject( model );
    }

    private void verifyEvalFileNameMapping( final String expression, final String classifier, final String extension,
                                            final MavenProject mainProject, final MavenProject moduleProject,
                                            final MavenProject artifactProject, final String checkValue )
        throws AssemblyFormattingException
    {

        Artifact artifactMock = mock( Artifact.class );
        when( artifactMock.getGroupId() ).thenReturn( artifactProject.getGroupId() );
        when( artifactMock.getClassifier() ).thenReturn( classifier );
        ArtifactHandler artifactHandler = mock( ArtifactHandler.class );
        when( artifactHandler.getExtension() ).thenReturn( extension );
        when( artifactMock.getArtifactHandler() ).thenReturn( artifactHandler );

        Artifact moduleArtifactMock = mock( Artifact.class );
        when( moduleArtifactMock.getGroupId() ).thenReturn( moduleProject.getGroupId() );

        final MavenSession session = mock( MavenSession.class );
        when( session.getExecutionProperties() ).thenReturn( System.getProperties() );

        final AssemblerConfigurationSource cs = mock( AssemblerConfigurationSource.class );
        when( cs.getMavenSession() ).thenReturn( session );
        
        DefaultAssemblyArchiverTest.setupInterpolators( cs, mainProject );

        final String result =
            AssemblyFormatUtils.evaluateFileNameMapping( expression, artifactMock, mainProject,
                                                         moduleArtifactMock, cs,
                                                         AssemblyFormatUtils.moduleProjectInterpolator( moduleProject ),
                                                         AssemblyFormatUtils.artifactProjectInterpolator( artifactProject ) );

        assertEquals( checkValue, result );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( cs ).getMavenSession();
    }

    private void verifyOutputDir( final String outDir, final String finalName, final String projectFinalName,
                                  final String checkValue )
        throws AssemblyFormattingException
    {
        verifyOutputDirUsingMainProject( outDir, finalName, null, null, null, projectFinalName, null, checkValue );
    }

    private void verifyOutputDirUsingMainProject( final String outDir, final String finalName, final String groupId,
                                                  final String artifactId, final String version,
                                                  final String projectFinalName, final Properties properties,
                                                  final String checkValue )
        throws AssemblyFormattingException
    {
        final MavenProject project = createProject( groupId, artifactId, version, properties );

        if ( projectFinalName != null )
        {
            final Build build = new Build();
            build.setFinalName( projectFinalName );

            project.getModel().setBuild( build );
        }

        final MavenProject moduleProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject artifactProject = createProject( "unknown", "unknown", "unknown", null );

        verifyOutputDir( outDir, finalName, project, moduleProject, artifactProject, checkValue );
    }

    private void verifyOutputDirUsingModuleProject( final String outDir, final String finalName, final String groupId,
                                                    final String artifactId, final String version,
                                                    final String projectFinalName, final Properties properties,
                                                    final String checkValue )
        throws AssemblyFormattingException
    {
        final MavenProject project = createProject( groupId, artifactId, version, properties );

        if ( projectFinalName != null )
        {
            final Build build = new Build();
            build.setFinalName( projectFinalName );

            project.getModel().setBuild( build );
        }

        final MavenProject mainProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject artifactProject = createProject( "unknown", "unknown", "unknown", null );

        verifyOutputDir( outDir, finalName, mainProject, project, artifactProject, checkValue );
    }

    private void verifyOutputDirUsingArtifactProject( final String outDir, final String finalName, final String groupId,
                                                      final String artifactId, final String version,
                                                      final String projectFinalName, final Properties properties,
                                                      final String checkValue )
        throws AssemblyFormattingException
    {
        final MavenProject project = createProject( groupId, artifactId, version, properties );

        if ( projectFinalName != null )
        {
            final Build build = new Build();
            build.setFinalName( projectFinalName );

            project.getModel().setBuild( build );
        }

        final MavenProject moduleProject = createProject( "unknown", "unknown", "unknown", null );
        final MavenProject mainProject = createProject( "unknown", "unknown", "unknown", null );

        verifyOutputDir( outDir, finalName, mainProject, moduleProject, project, checkValue );
    }

    private void verifyOutputDir( final String outDir, final String finalName, final MavenProject mainProject,
                                  final MavenProject moduleProject, final MavenProject artifactProject,
                                  final String checkValue )
        throws AssemblyFormattingException
    {

        final MavenSession session = mock( MavenSession.class );
        when( session.getExecutionProperties() ).thenReturn( System.getProperties() );

        final AssemblerConfigurationSource cs = mock( AssemblerConfigurationSource.class );
        when( cs.getMavenSession() ).thenReturn( session );
        
        DefaultAssemblyArchiverTest.setupInterpolators( cs, mainProject );

        String result =
            AssemblyFormatUtils.getOutputDirectory( outDir, finalName, cs,
                                                    AssemblyFormatUtils.moduleProjectInterpolator( moduleProject ),
                                                    AssemblyFormatUtils.artifactProjectInterpolator( artifactProject ) );

        assertEquals( checkValue, result );

        // result of easymock migration, should be assert of expected result instead of verifying methodcalls
        verify( cs ).getMavenSession();
    }

    private void verifyDistroName( final String assemblyId, final String finalName, final boolean appendAssemblyId,
                                   final String checkValue )
    {
        final AssemblerConfigurationSource configSource = mock( AssemblerConfigurationSource.class );
        when( configSource.isAssemblyIdAppended() ).thenReturn( appendAssemblyId );
        when( configSource.getFinalName() ).thenReturn( finalName );

        final Assembly assembly = new Assembly();
        assembly.setId( assemblyId );

        final String result = AssemblyFormatUtils.getDistributionName( assembly, configSource );

        assertEquals( checkValue, result );

        verify( configSource, atLeast( 1 ) ).isAssemblyIdAppended();
        verify( configSource, atLeast( 1 ) ).getFinalName();
    }

    @Test
    public void testWindowsPath()
    {
        assertTrue( AssemblyFormatUtils.isWindowsPath( "C:\foobar" ) );
    }

    @Test
    public void testLinuxRootReferencePath()
    {
        assertTrue( AssemblyFormatUtils.isUnixRootReference( "/etc/home" ) );
    }

    @Test
    public void groupIdPath_artifactProjectInterpolator()
    {
        Artifact artifact = mock( Artifact.class );
        when( artifact.getFile() ).thenReturn( new File( "dir", "artifactId.jar" ) );
                        
        MavenProject project = mock( MavenProject.class );
        when( project.getGroupId() ).thenReturn( "a.b.c" );
        when( project.getArtifact() ).thenReturn( artifact );
        
        FixedStringSearchInterpolator interpolator = AssemblyFormatUtils.artifactProjectInterpolator( project );
        assertEquals( "a/b/c", interpolator.interpolate( "${artifact.groupIdPath}" ) );
        assertEquals( "a/b/c/artifactId.jar", interpolator.interpolate( "${artifact.groupIdPath}/${artifact.file.name}" ) );
    }

    @Test
    public void groupIdPath_artifactInterpolator()
    {
        Artifact artifact = mock( Artifact.class );
        when( artifact.getGroupId() ).thenReturn( "a.b.c" );
        when( artifact.getFile() ).thenReturn( new File( "dir", "artifactId.jar" ) );
                        
        FixedStringSearchInterpolator interpolator = AssemblyFormatUtils.artifactInterpolator( artifact );
        assertEquals( "a/b/c", interpolator.interpolate( "${artifact.groupIdPath}" ) );
        assertEquals( "a/b/c/artifactId.jar", interpolator.interpolate( "${artifact.groupIdPath}/${artifact.file.name}" ) );
    }

}
