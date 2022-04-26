package org.apache.maven.plugins.assembly.archive.phase;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.task.AddArtifactTask;
import org.apache.maven.plugins.assembly.archive.task.AddDependencySetsTask;
import org.apache.maven.plugins.assembly.archive.task.AddFileSetsTask;
import org.apache.maven.plugins.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugins.assembly.artifact.DependencyResolver;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.functions.MavenProjects;
import org.apache.maven.plugins.assembly.functions.ModuleSetConsumer;
import org.apache.maven.plugins.assembly.model.Assemblies;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.ModuleBinaries;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.ModuleSources;
import org.apache.maven.plugins.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugins.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.utils.FilterUtils;
import org.apache.maven.plugins.assembly.utils.ProjectUtils;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.apache.maven.plugins.assembly.functions.MavenProjects.addTo;
import static org.apache.maven.plugins.assembly.functions.MavenProjects.log;

/**
 * Handles the &lt;moduleSets/&gt; top-level section of the assembly descriptor.
 *
 *
 */
@Singleton
@Named( "module-sets" )
public class ModuleSetAssemblyPhase implements AssemblyArchiverPhase, PhaseOrder
{
    private static final Logger LOGGER = LoggerFactory.getLogger( ModuleSetAssemblyPhase.class );

    // TODO: Remove if using something like commons-lang instead.

    /**
     * The line separator.
     */
    private static final String LINE_SEPARATOR = System.getProperty( "line.separator" );

    private final ProjectBuilder projectBuilder;

    private final DependencyResolver dependencyResolver;

    /**
     * Injected ctor.
     */
    @Inject
    public ModuleSetAssemblyPhase( final ProjectBuilder projectBuilder,
                                   final DependencyResolver dependencyResolver )
    {
        this.projectBuilder = requireNonNull( projectBuilder );
        this.dependencyResolver = requireNonNull( dependencyResolver );
    }

    public static List<DependencySet> getDependencySets( final ModuleBinaries binaries )
    {
        List<DependencySet> depSets = binaries.getDependencySets();

        if ( ( ( depSets == null ) || depSets.isEmpty() ) && binaries.isIncludeDependencies() )
        {
            final DependencySet impliedDependencySet = new DependencySet();

            impliedDependencySet.setOutputDirectory( binaries.getOutputDirectory() );
            //impliedDependencySet.setOutputFileNameMapping( binaries.getOutputFileNameMapping() );
            impliedDependencySet.setFileMode( binaries.getFileMode() );
            impliedDependencySet.setDirectoryMode( binaries.getDirectoryMode() );
            impliedDependencySet.setExcludes( binaries.getExcludes() );
            impliedDependencySet.setIncludes( binaries.getIncludes() );
            impliedDependencySet.setUnpack( binaries.isUnpack() );
            // unpackOptions is handled in the first stage of dependency-set handling, below.

            depSets = Collections.singletonList( impliedDependencySet );
        }

        return depSets;
    }

    public static Set<MavenProject> getModuleProjects( final ModuleSet moduleSet,
                                                       final AssemblerConfigurationSource configSource,
                                                       final Logger logger )
        throws ArchiveCreationException
    {
        MavenProject project = configSource.getProject();
        Set<MavenProject> moduleProjects = null;

        if ( moduleSet.isUseAllReactorProjects() )
        {
            if ( !moduleSet.isIncludeSubModules() )
            {
                moduleProjects = new LinkedHashSet<>( configSource.getReactorProjects() );
            }

            project = configSource.getReactorProjects().get( 0 );
        }

        if ( moduleProjects == null )
        {
            try
            {
                moduleProjects = ProjectUtils.getProjectModules( project, configSource.getReactorProjects(),
                                                                 moduleSet.isIncludeSubModules(), logger );
            }
            catch ( final IOException e )
            {
                throw new ArchiveCreationException(
                    "Error retrieving module-set for project: " + project.getId() + ": " + e.getMessage(), e );
            }
        }

        return FilterUtils.filterProjects( moduleProjects, moduleSet.getIncludes(), moduleSet.getExcludes(), true,
                                           logger );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute( final Assembly assembly, final Archiver archiver,
                         final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
        DependencyResolutionException
    {
        Assemblies.forEachModuleSet( assembly, new ModuleSetConsumer()
        {
            @Override
            public void accept( ModuleSet resolvedModule )
                throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
                DependencyResolutionException
            {
                validate( resolvedModule, configSource );

                final Set<MavenProject> moduleProjects = getModuleProjects( resolvedModule, configSource, LOGGER );

                final ModuleSources sources = resolvedModule.getSources();
                addModuleSourceFileSets( sources, moduleProjects, archiver, configSource );

                final ModuleBinaries binaries = resolvedModule.getBinaries();
                addModuleBinaries( assembly, resolvedModule, binaries, moduleProjects, archiver, configSource );
            }
        } );
    }

    private void validate( final ModuleSet moduleSet, final AssemblerConfigurationSource configSource )
    {
        if ( ( moduleSet.getSources() == null ) && ( moduleSet.getBinaries() == null ) )
        {
            LOGGER.warn( "Encountered ModuleSet with no sources or binaries specified. Skipping." );
        }

        if ( moduleSet.isUseAllReactorProjects() && !moduleSet.isIncludeSubModules() )
        {
            LOGGER.warn( "includeSubModules == false is incompatible with useAllReactorProjects. Ignoring."
                                  + "\n\nTo refactor, remove the <includeSubModules/> flag, and use the <includes/> "
                                  + "and <excludes/> sections to fine-tune the modules included." );
        }

        final List<MavenProject> projects = configSource.getReactorProjects();
        if ( projects != null && projects.size() > 1 && projects.indexOf( configSource.getProject() ) == 0
            && moduleSet.getBinaries() != null )
        {
            LOGGER.warn( "[DEPRECATION] moduleSet/binaries section detected in root-project assembly."
                                  + "\n\nMODULE BINARIES MAY NOT BE AVAILABLE FOR THIS ASSEMBLY!"
                                  + "\n\n To refactor, move this assembly into a child project and use the flag "
                                  + "<useAllReactorProjects>true</useAllReactorProjects> in each moduleSet." );
        }

        if ( moduleSet.getSources() != null )
        {
            final ModuleSources sources = moduleSet.getSources();
            if ( isDeprecatedModuleSourcesConfigPresent( sources ) )
            {
                LOGGER.warn( "[DEPRECATION] Use of <moduleSources/> as a file-set is deprecated. "
                                      + "Please use the <fileSets/> sub-element of <moduleSources/> instead." );
            }
            else if ( !sources.isUseDefaultExcludes() )
            {
                LOGGER.warn( "[DEPRECATION] Use of directoryMode, fileMode, or useDefaultExcludes "
                                      + "elements directly within <moduleSources/> are all deprecated. "
                                      + "Please use the <fileSets/> sub-element of <moduleSources/> instead." );
            }
        }
    }

    void addModuleBinaries( final Assembly assembly, ModuleSet moduleSet, final ModuleBinaries binaries,
                            final Set<MavenProject> projects, final Archiver archiver,
                            final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
        DependencyResolutionException
    {
        if ( binaries == null )
        {
            return;
        }

        final Set<MavenProject> moduleProjects = new LinkedHashSet<>();

        MavenProjects.select( projects, "pom", log( LOGGER ), addTo( moduleProjects ) );

        final String classifier = binaries.getAttachmentClassifier();

        final Map<MavenProject, Artifact> chosenModuleArtifacts = new HashMap<>();

        for ( final MavenProject project : moduleProjects )
        {
            Artifact artifact;

            if ( classifier == null )
            {
                LOGGER.debug( "Processing binary artifact for module project: " + project.getId() );

                artifact = project.getArtifact();
            }
            else
            {
                LOGGER.debug(
                    "Processing binary attachment: " + classifier + " for module project: " + project.getId() );

                artifact = MavenProjects.findArtifactByClassifier( project, classifier );

                if ( artifact == null )
                {
                    throw new InvalidAssemblerConfigurationException(
                        "Cannot find attachment with classifier: " + classifier + " in module project: "
                            + project.getId() + ". Please exclude this module from the module-set." );
                }
            }

            chosenModuleArtifacts.put( project, artifact );
            addModuleArtifact( artifact, project, archiver, configSource, binaries );
        }

        final List<DependencySet> depSets = getDependencySets( binaries );

        if ( depSets != null )
        {
            Map<DependencySet, Set<Artifact>> dependencySetSetMap =
                dependencyResolver.resolveDependencySets( assembly, moduleSet, configSource, depSets );

            for ( final DependencySet ds : depSets )
            {
                // NOTE: Disabling useProjectArtifact flag, since module artifact has already been handled!
                ds.setUseProjectArtifact( false );
            }

            // TODO: The following should be moved into a shared component, cause this
            // test is the same as in maven-enforce rules ReactorModuleConvergence.
            List<MavenProject> validateModuleVersions = validateModuleVersions( moduleProjects );
            if ( !validateModuleVersions.isEmpty() )
            {

                StringBuilder sb =
                    new StringBuilder().append( "The current modules seemed to be having different versions." );
                sb.append( LINE_SEPARATOR );
                for ( MavenProject mavenProject : validateModuleVersions )
                {
                    sb.append( " --> " );
                    sb.append( mavenProject.getId() );
                    sb.append( LINE_SEPARATOR );
                }
                LOGGER.warn( sb.toString() );
            }

            for ( final MavenProject moduleProject : moduleProjects )
            {
                LOGGER.debug( "Processing binary dependencies for module project: " + moduleProject.getId() );

                for ( Map.Entry<DependencySet, Set<Artifact>> dependencySetSetEntry : dependencySetSetMap.entrySet() )
                {
                    final AddDependencySetsTask task =
                        new AddDependencySetsTask( Collections.singletonList( dependencySetSetEntry.getKey() ),
                                                   dependencySetSetEntry.getValue(), moduleProject, projectBuilder );

                    task.setModuleProject( moduleProject );
                    task.setModuleArtifact( chosenModuleArtifacts.get( moduleProject ) );
                    task.setDefaultOutputDirectory( binaries.getOutputDirectory() );
                    task.setDefaultOutputFileNameMapping( binaries.getOutputFileNameMapping() );

                    task.execute( archiver, configSource );

                }
            }
        }
    }

    private List<MavenProject> validateModuleVersions( Set<MavenProject> moduleProjects )
    {
        List<MavenProject> result = new ArrayList<>();

        if ( moduleProjects != null && !moduleProjects.isEmpty() )
        {
            String version = moduleProjects.iterator().next().getVersion();
            LOGGER.debug( "First version:" + version );
            for ( MavenProject mavenProject : moduleProjects )
            {
                LOGGER.debug( " -> checking " + mavenProject.getId() );
                if ( !version.equals( mavenProject.getVersion() ) )
                {
                    result.add( mavenProject );
                }
            }
        }
        return result;
    }

    void addModuleArtifact( final Artifact artifact, final MavenProject project, final Archiver archiver,
                            final AssemblerConfigurationSource configSource, final ModuleBinaries binaries )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( artifact.getFile() == null )
        {
            throw new ArchiveCreationException(
                "Artifact: " + artifact.getId() + " (included by module) does not have an artifact with a file. "
                    + "Please ensure the package phase is run before the assembly is generated." );
        }

        final AddArtifactTask task = new AddArtifactTask( artifact, null );

        task.setFileNameMapping( binaries.getOutputFileNameMapping() );
        task.setOutputDirectory( binaries.getOutputDirectory() );
        task.setProject( project );
        task.setModuleProject( project );
        task.setModuleArtifact( artifact );

        final int dirMode = TypeConversionUtils.modeToInt( binaries.getDirectoryMode(), LOGGER );
        if ( dirMode != -1 )
        {
            task.setDirectoryMode( dirMode );
        }

        final int fileMode = TypeConversionUtils.modeToInt( binaries.getFileMode(), LOGGER );
        if ( fileMode != -1 )
        {
            task.setFileMode( fileMode );
        }

        task.setUnpack( binaries.isUnpack() );

        if ( binaries.isUnpack() && binaries.getUnpackOptions() != null )
        {
            task.setIncludes( binaries.getUnpackOptions().getIncludes() );
            task.setExcludes( binaries.getUnpackOptions().getExcludes() );
        }

        task.execute( archiver, configSource );
    }

    void addModuleSourceFileSets( final ModuleSources sources, final Set<MavenProject> moduleProjects,
                                  final Archiver archiver, final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException
    {
        if ( sources == null )
        {
            return;
        }

        final List<FileSet> fileSets = new ArrayList<>();

        if ( isDeprecatedModuleSourcesConfigPresent( sources ) )
        {
            final FileSet fs = new FileSet();
            fs.setOutputDirectory( sources.getOutputDirectory() );
            fs.setIncludes( sources.getIncludes() );
            fs.setExcludes( sources.getExcludes() );
            fs.setUseDefaultExcludes( sources.isUseDefaultExcludes() );

            fileSets.add( fs );
        }

        List<FileSet> subFileSets = sources.getFileSets();

        if ( ( subFileSets == null ) || subFileSets.isEmpty() )
        {
            final FileSet fs = new FileSet();
            fs.setDirectory( "src" );

            subFileSets = Collections.singletonList( fs );
        }

        fileSets.addAll( subFileSets );

        for ( final MavenProject moduleProject : moduleProjects )
        {
            LOGGER.info( "Processing sources for module project: " + moduleProject.getId() );

            final List<FileSet> moduleFileSets = new ArrayList<>();

            for ( final FileSet fileSet : fileSets )
            {
                moduleFileSets.add( createFileSet( fileSet, sources, moduleProject, configSource ) );
            }

            final AddFileSetsTask task = new AddFileSetsTask( moduleFileSets );

            task.setProject( moduleProject );
            task.setModuleProject( moduleProject );

            task.execute( archiver, configSource );
        }
    }

    /**
     * Determine whether the deprecated file-set configuration directly within the ModuleSources object is present.
     */
    boolean isDeprecatedModuleSourcesConfigPresent( final ModuleSources sources )
    {
        boolean result = false;

        if ( sources.getOutputDirectory() != null )
        {
            result = true;
        }
        else if ( ( sources.getIncludes() != null ) && !sources.getIncludes().isEmpty() )
        {
            result = true;
        }
        else if ( ( sources.getExcludes() != null ) && !sources.getExcludes().isEmpty() )
        {
            result = true;
        }

        return result;
    }

    FileSet createFileSet( final FileSet fileSet, final ModuleSources sources,
                           final MavenProject moduleProject,
                           final AssemblerConfigurationSource configSource )
        throws AssemblyFormattingException
    {
        final FileSet fs = new FileSet();

        String sourcePath = fileSet.getDirectory();

        final File moduleBasedir = moduleProject.getBasedir();

        if ( sourcePath != null )
        {
            final File sourceDir = new File( sourcePath );

            if ( !AssemblyFileUtils.isAbsolutePath( sourceDir ) )
            {
                sourcePath = new File( moduleBasedir, sourcePath ).getAbsolutePath();
            }
        }
        else
        {
            sourcePath = moduleBasedir.getAbsolutePath();
        }

        fs.setDirectory( sourcePath );
        fs.setDirectoryMode( fileSet.getDirectoryMode() );

        final List<String> excludes = new ArrayList<>();

        final List<String> originalExcludes = fileSet.getExcludes();
        if ( ( originalExcludes != null ) && !originalExcludes.isEmpty() )
        {
            excludes.addAll( originalExcludes );
        }

        if ( sources.isExcludeSubModuleDirectories() )
        {
            final List<String> modules = moduleProject.getModules();
            for ( final String moduleSubPath : modules )
            {
                excludes.add( moduleSubPath + "/**" );
            }
        }

        fs.setExcludes( excludes );
        fs.setFiltered( fileSet.isFiltered() );
        fs.setFileMode( fileSet.getFileMode() );
        fs.setIncludes( fileSet.getIncludes() );
        fs.setLineEnding( fileSet.getLineEnding() );

        FixedStringSearchInterpolator moduleProjectInterpolator =
            AssemblyFormatUtils.moduleProjectInterpolator( moduleProject );
        FixedStringSearchInterpolator artifactProjectInterpolator =
            AssemblyFormatUtils.artifactProjectInterpolator( moduleProject );
        String destPathPrefix = "";
        if ( sources.isIncludeModuleDirectory() )
        {
            destPathPrefix = AssemblyFormatUtils.evaluateFileNameMapping( sources.getOutputDirectoryMapping(),
                                                                          moduleProject.getArtifact(),
                                                                          configSource.getProject(),
                                                                          moduleProject.getArtifact(), configSource,
                                                                          moduleProjectInterpolator,
                                                                          artifactProjectInterpolator );

            if ( !destPathPrefix.endsWith( "/" ) )
            {
                destPathPrefix += "/";
            }
        }

        String destPath = fileSet.getOutputDirectory();

        if ( destPath == null )
        {
            destPath = destPathPrefix;
        }
        else
        {
            destPath = destPathPrefix + destPath;
        }

        destPath = AssemblyFormatUtils.getOutputDirectory( destPath, configSource.getFinalName(), configSource,
                                                           moduleProjectInterpolator, artifactProjectInterpolator );

        fs.setOutputDirectory( destPath );

        LOGGER.debug( "module source directory is: " + sourcePath );
        LOGGER.debug( "module dest directory is: " + destPath + " (assembly basedir may be prepended)" );

        return fs;
    }

    @Override
    public int order()
    {
        // CHECKSTYLE_OFF: MagicNumber
        return 30;
        // CHECKSTYLE_ON: MagicNumber
    }
}
