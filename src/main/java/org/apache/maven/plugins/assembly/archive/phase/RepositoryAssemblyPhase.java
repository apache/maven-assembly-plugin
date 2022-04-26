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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.phase.wrappers.RepoBuilderConfigSourceWrapper;
import org.apache.maven.plugins.assembly.archive.phase.wrappers.RepoInfoWrapper;
import org.apache.maven.plugins.assembly.archive.task.AddDirectoryTask;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Repository;
import org.apache.maven.plugins.assembly.repository.RepositoryAssembler;
import org.apache.maven.plugins.assembly.repository.RepositoryAssemblyException;
import org.apache.maven.plugins.assembly.repository.RepositoryBuilderConfigSource;
import org.apache.maven.plugins.assembly.repository.model.RepositoryInfo;
import org.apache.maven.plugins.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 *
 */
@Singleton
@Named( "repositories" )
public class RepositoryAssemblyPhase implements AssemblyArchiverPhase, PhaseOrder
{
    private static final Logger LOGGER = LoggerFactory.getLogger( RepositoryAssemblyPhase.class );

    private final RepositoryAssembler repositoryAssembler;


    @Inject
    public RepositoryAssemblyPhase( final RepositoryAssembler repositoryAssembler )
    {
        this.repositoryAssembler = requireNonNull( repositoryAssembler );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings( "ResultOfMethodCallIgnored" )
    public void execute( final Assembly assembly, final Archiver archiver,
                         final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final List<Repository> repositoriesList = assembly.getRepositories();

        final File tempRoot = configSource.getTemporaryRootDirectory();

        for ( final Repository repository : repositoriesList )
        {
            final String outputDirectory =
                AssemblyFormatUtils.getOutputDirectory( repository.getOutputDirectory(), configSource.getFinalName(),
                                                        configSource, AssemblyFormatUtils.moduleProjectInterpolator(
                        configSource.getProject() ), AssemblyFormatUtils.artifactProjectInterpolator( null ) );

            final File repositoryDirectory = new File( tempRoot, outputDirectory );

            if ( !repositoryDirectory.exists() )
            {
                repositoryDirectory.mkdirs();
            }

            try
            {
                LOGGER.debug( "Assembling repository to: " + repositoryDirectory );
                repositoryAssembler.buildRemoteRepository( repositoryDirectory, wrap( repository ),
                                                           wrap( configSource ) );
                LOGGER.debug( "Finished assembling repository to: " + repositoryDirectory );
            }
            catch ( final RepositoryAssemblyException e )
            {
                throw new ArchiveCreationException( "Failed to assemble repository: " + e.getMessage(), e );
            }

            final AddDirectoryTask task = new AddDirectoryTask( repositoryDirectory );

            final int dirMode = TypeConversionUtils.modeToInt( repository.getDirectoryMode(), LOGGER );
            if ( dirMode != -1 )
            {
                task.setDirectoryMode( dirMode );
            }

            final int fileMode = TypeConversionUtils.modeToInt( repository.getFileMode(), LOGGER );
            if ( fileMode != -1 )
            {
                task.setFileMode( fileMode );
            }

            task.setOutputDirectory( outputDirectory );

            task.execute( archiver );
        }
    }

    private RepositoryBuilderConfigSource wrap( final AssemblerConfigurationSource configSource )
    {
        return new RepoBuilderConfigSourceWrapper( configSource );
    }

    private RepositoryInfo wrap( final Repository repository )
    {
        return new RepoInfoWrapper( repository );
    }

    @Override
    public int order()
    {
        // CHECKSTYLE_OFF: MagicNumber
        return 50;
        // CHECKSTYLE_ON: MagicNumber
    }
}

