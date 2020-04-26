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

import static org.codehaus.plexus.components.io.resources.ResourceFactory.createResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.format.ReaderFormatter;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugins.assembly.utils.AssemblyFormatUtils;
import org.apache.maven.plugins.assembly.utils.TypeConversionUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.components.io.functions.ContentSupplier;
import org.codehaus.plexus.components.io.functions.InputStreamTransformer;
import org.codehaus.plexus.components.io.resources.PlexusIoFileResource;
import org.codehaus.plexus.components.io.resources.PlexusIoResource;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Handles the top-level &lt;files/&gt; section of the assembly descriptor.
 *
 *
 */
@Component( role = AssemblyArchiverPhase.class, hint = "file-items" )
public class FileItemAssemblyPhase
    extends AbstractLogEnabled
    implements AssemblyArchiverPhase, PhaseOrder
{

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute( final Assembly assembly, final Archiver archiver,
                         final AssemblerConfigurationSource configSource )
        throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException
    {
        final List<FileItem> fileList = assembly.getFiles();
        final File basedir = configSource.getBasedir();

        for ( final FileItem fileItem : fileList )
        {
            if ( fileItem.getSource() != null ^ fileItem.getSources().isEmpty() )
            {
                throw new InvalidAssemblerConfigurationException( 
                                                      "Misconfigured file: one of source or sources must be set" );
            }
            
            String destName = fileItem.getDestName();
            
            final String sourcePath;
            if ( fileItem.getSource() != null )
            {
                sourcePath = fileItem.getSource();
            }
            else if ( destName != null )
            {
                // because createResource() requires a file
                sourcePath = fileItem.getSources().get( 0 );
            }
            else
            {
                throw new InvalidAssemblerConfigurationException( 
                                "Misconfigured file: specify destName when using sources" );
            }            

            // ensure source file is in absolute path for reactor build to work
            File source = new File( sourcePath );

            // save the original sourcefile's name, because filtration may
            // create a temp file with a different name.
            final String sourceName = source.getName();

            if ( !AssemblyFileUtils.isAbsolutePath( source ) )
            {
                source = new File( basedir, sourcePath );
            }
            if ( destName == null )
            {
                destName = sourceName;
            }

            final String outputDirectory1 = fileItem.getOutputDirectory();

            final String outputDirectory =
                AssemblyFormatUtils.getOutputDirectory( outputDirectory1, configSource.getFinalName(), configSource,
                                                        AssemblyFormatUtils.moduleProjectInterpolator(
                                                            configSource.getProject() ),
                                                        AssemblyFormatUtils.artifactProjectInterpolator( null ) );

            String target;

            // omit the last char if ends with / or \\
            if ( outputDirectory.endsWith( "/" ) || outputDirectory.endsWith( "\\" ) )
            {
                target = outputDirectory + destName;
            }
            else if ( outputDirectory.length() < 1 )
            {
                target = destName;
            }
            else
            {
                target = outputDirectory + "/" + destName;
            }

            try
            {
                final InputStreamTransformer fileSetTransformers =
                    ReaderFormatter.getFileSetTransformers( configSource, fileItem.isFiltered(),
                                                            Collections.<String>emptySet(),
                                                            fileItem.getLineEnding() );
                
                final PlexusIoResource restoUse;
                if ( !fileItem.getSources().isEmpty() )
                {
                    List<InputStream> content = new ArrayList<>( fileItem.getSources().size() );
                    for ( String contentSourcePath : fileItem.getSources() )
                    {
                        File contentSource = new File( contentSourcePath );
                        if ( !AssemblyFileUtils.isAbsolutePath( contentSource ) )
                        {
                            contentSource = new File( basedir, contentSourcePath );
                        }
                        content.add( new FileInputStream( contentSource ) );
                    }
                    
                    String name = PlexusIoFileResource.getName( source );
                    restoUse = createResource( source, name, getContentSupplier( content ), fileSetTransformers );
                }
                else
                {
                    restoUse = createResource( source, fileSetTransformers );
                }

                int mode = TypeConversionUtils.modeToInt( fileItem.getFileMode(), getLogger() );
                archiver.addResource( restoUse, target, mode );
            }
            catch ( final ArchiverException | IOException e )
            {
                throw new ArchiveCreationException( "Error adding file to archive: " + e.getMessage(), e );
            }
        }
    }

    @Override
    public int order()
    {
        return 10;
    }
    
    private ContentSupplier getContentSupplier( final Collection<InputStream> contentStreams ) 
    {
        return new ContentSupplier()
        {
            @Override
            public InputStream getContents()
                throws IOException
            {
                return new SequenceInputStream( Collections.enumeration( contentStreams ) );
            }
        };
    }
}
