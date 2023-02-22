package org.apache.maven.plugins.assembly.io;

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

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.input.XmlStreamReader;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.interpolation.AssemblyExpressionEvaluator;
import org.apache.maven.plugins.assembly.interpolation.AssemblyInterpolator;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.Component;
import org.apache.maven.plugins.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugins.assembly.model.DependencySet;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.assembly.model.ModuleSet;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Reader;
import org.apache.maven.plugins.assembly.model.io.xpp3.AssemblyXpp3Writer;
import org.apache.maven.plugins.assembly.model.io.xpp3.ComponentXpp3Reader;
import org.apache.maven.plugins.assembly.resolved.AssemblyId;
import org.apache.maven.plugins.assembly.utils.InterpolationConstants;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.fixed.FixedStringSearchInterpolator;
import org.codehaus.plexus.interpolation.fixed.InterpolationState;
import org.codehaus.plexus.interpolation.fixed.PrefixedObjectValueSource;
import org.codehaus.plexus.interpolation.fixed.PrefixedPropertiesValueSource;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
@Singleton
@Named
public class DefaultAssemblyReader implements AssemblyReader
{
    private static final Logger LOGGER = LoggerFactory.getLogger( DefaultAssemblyReader.class );

    public static FixedStringSearchInterpolator createProjectInterpolator( MavenProject project )
    {
        // CHECKSTYLE_OFF: LineLength
        return FixedStringSearchInterpolator.create( new PrefixedPropertiesValueSource( InterpolationConstants.PROJECT_PROPERTIES_PREFIXES,
                                                                                        project.getProperties(), true ),
                                                     new PrefixedObjectValueSource( InterpolationConstants.PROJECT_PREFIXES,
                                                                                    project, true ) );
        // CHECKSTYLE_ON: LineLength
    }

    @Override
    public List<Assembly> readAssemblies( final AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        final Locator locator = new Locator();

        final List<LocatorStrategy> strategies = new ArrayList<>();
        strategies.add( new RelativeFileLocatorStrategy( configSource.getBasedir() ) );
        strategies.add( new FileLocatorStrategy() );

        final List<LocatorStrategy> refStrategies = new ArrayList<>();
        refStrategies.add( new PrefixedClasspathLocatorStrategy( "/assemblies/" ) );

        final List<Assembly> assemblies = new ArrayList<>();

        final String[] descriptors = configSource.getDescriptors();
        final String[] descriptorRefs = configSource.getDescriptorReferences();
        final File descriptorSourceDirectory = configSource.getDescriptorSourceDirectory();

        if ( ( descriptors != null ) && ( descriptors.length > 0 ) )
        {
            locator.setStrategies( strategies );
            for ( String descriptor1 : descriptors )
            {
                LOGGER.info( "Reading assembly descriptor: " + descriptor1 );
                addAssemblyFromDescriptor( descriptor1, locator, configSource, assemblies );
            }
        }

        if ( ( descriptorRefs != null ) && ( descriptorRefs.length > 0 ) )
        {
            locator.setStrategies( refStrategies );
            for ( String descriptorRef : descriptorRefs )
            {
                addAssemblyForDescriptorReference( descriptorRef, configSource, assemblies );
            }
        }

        if ( ( descriptorSourceDirectory != null ) && descriptorSourceDirectory.isDirectory() )
        {
            // CHECKSTYLE_OFF: LineLength
            locator.setStrategies( Collections.<LocatorStrategy>singletonList( new RelativeFileLocatorStrategy( descriptorSourceDirectory ) ) );
            // CHECKSTYLE_ON: LineLength

            final DirectoryScanner scanner = new DirectoryScanner();
            scanner.setBasedir( descriptorSourceDirectory );
            scanner.setIncludes( new String[] { "**/*.xml" } );
            scanner.addDefaultExcludes();

            scanner.scan();

            final String[] paths = scanner.getIncludedFiles();

            for ( String path : paths )
            {
                addAssemblyFromDescriptor( path, locator, configSource, assemblies );
            }
        }

        if ( assemblies.isEmpty() )
        {
            if ( configSource.isIgnoreMissingDescriptor() )
            {
                LOGGER.debug( "Ignoring missing assembly descriptors per configuration. "
                    + "See messages above for specifics." );
            }
            else
            {
                throw new AssemblyReadException( "No assembly descriptors found." );
            }
        }

        // check unique IDs
        final Set<String> ids = new HashSet<>();
        for ( final Assembly assembly : assemblies )
        {
            if ( !ids.add( assembly.getId() ) )
            {
                LOGGER.warn( "The assembly id " + assembly.getId() + " is used more than once." );
            }

        }
        return assemblies;
    }

    @Override
    public Assembly getAssemblyForDescriptorReference( final String ref,
                                                       final AssemblerConfigurationSource configSource )
                                                           throws AssemblyReadException,
                                                           InvalidAssemblerConfigurationException
    {
        return addAssemblyForDescriptorReference( ref, configSource, new ArrayList<Assembly>( 1 ) );
    }

    @Override
    public Assembly getAssemblyFromDescriptorFile( final File file, final AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        return addAssemblyFromDescriptorFile( file, configSource, new ArrayList<Assembly>( 1 ) );
    }

    private Assembly addAssemblyForDescriptorReference( final String ref,
                                                        final AssemblerConfigurationSource configSource,
                                                        final List<Assembly> assemblies )
                                                            throws AssemblyReadException,
                                                            InvalidAssemblerConfigurationException
    {
        final InputStream resourceAsStream = getClass().getResourceAsStream( "/assemblies/" + ref + ".xml" );

        if ( resourceAsStream == null )
        {
            if ( configSource.isIgnoreMissingDescriptor() )
            {
                LOGGER.debug( "Ignoring missing assembly descriptor with ID '" + ref + "' per configuration." );
                return null;
            }
            else
            {
                throw new AssemblyReadException( "Descriptor with ID '" + ref + "' not found" );
            }
        }

        try ( Reader reader = new XmlStreamReader( resourceAsStream ) )
        {
            final Assembly assembly = readAssembly( reader, ref, null, configSource );
            assemblies.add( assembly );
            return assembly;
        }
        catch ( final IOException e )
        {
            throw new AssemblyReadException( "Problem with descriptor with ID '" + ref + "'", e );
        }
    }

    private Assembly addAssemblyFromDescriptorFile( final File descriptor,
                                                    final AssemblerConfigurationSource configSource,
                                                    final List<Assembly> assemblies )
                                                        throws AssemblyReadException,
                                                        InvalidAssemblerConfigurationException
    {
        if ( !descriptor.exists() )
        {
            if ( configSource.isIgnoreMissingDescriptor() )
            {
                LOGGER.debug( "Ignoring missing assembly descriptor: '" + descriptor + "' per configuration." );
                return null;
            }
            else
            {
                throw new AssemblyReadException( "Descriptor: '" + descriptor + "' not found" );
            }
        }

        try ( Reader r = new XmlStreamReader( descriptor ) )
        {
            final Assembly assembly =
                readAssembly( r, descriptor.getAbsolutePath(), descriptor.getParentFile(), configSource );

            assemblies.add( assembly );

            return assembly;
        }
        catch ( final IOException e )
        {
            throw new AssemblyReadException( "Error reading assembly descriptor: " + descriptor, e );
        }
    }

    private Assembly addAssemblyFromDescriptor( final String spec, final Locator locator,
                                                final AssemblerConfigurationSource configSource,
                                                final List<Assembly> assemblies )
                                                    throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        final Location location = locator.resolve( spec );

        if ( location == null )
        {
            if ( configSource.isIgnoreMissingDescriptor() )
            {
                LOGGER.debug( "Ignoring missing assembly descriptor with ID '" + spec
                    + "' per configuration.\nLocator output was:\n\n" + locator.getMessageHolder().render() );
                return null;
            }
            else
            {
                throw new AssemblyReadException( "Error locating assembly descriptor: " + spec + "\n\n"
                    + locator.getMessageHolder().render() );
            }
        }

        
        try ( Reader r = new XmlStreamReader( location.getInputStream() ) )
        {
            File dir = null;
            if ( location.getFile() != null )
            {
                dir = location.getFile().getParentFile();
            }

            final Assembly assembly = readAssembly( r, spec, dir, configSource );

            assemblies.add( assembly );

            return assembly;
        }
        catch ( final IOException e )
        {
            throw new AssemblyReadException( "Error reading assembly descriptor: " + spec, e );
        }
    }

    public Assembly readAssembly( Reader reader, final String locationDescription, final File assemblyDir,
                                  final AssemblerConfigurationSource configSource )
        throws AssemblyReadException, InvalidAssemblerConfigurationException
    {
        Assembly assembly;

        final MavenProject project = configSource.getProject();
        try
        {

            InterpolationState is = new InterpolationState();
            final RecursionInterceptor interceptor =
                new PrefixAwareRecursionInterceptor( InterpolationConstants.PROJECT_PREFIXES, true );
            is.setRecursionInterceptor( interceptor );

            FixedStringSearchInterpolator interpolator =
                AssemblyInterpolator.fullInterpolator( project, createProjectInterpolator( project ), configSource );
            AssemblyXpp3Reader.ContentTransformer transformer =
                AssemblyInterpolator.assemblyInterpolator( interpolator, is, LOGGER );

            final AssemblyXpp3Reader r = new AssemblyXpp3Reader( transformer );
            assembly = r.read( reader );

            ComponentXpp3Reader.ContentTransformer ctrans =
                AssemblyInterpolator.componentInterpolator( interpolator, is, LOGGER );
            mergeComponentsWithMainAssembly( assembly, assemblyDir, configSource, ctrans );
            debugPrintAssembly( "After assembly is interpolated:", assembly );

            AssemblyInterpolator.checkErrors( AssemblyId.createAssemblyId( assembly ), is, LOGGER );

            reader.close();
            reader = null;
        }
        catch ( final IOException | XmlPullParserException e )
        {
            throw new AssemblyReadException( "Error reading descriptor: " + locationDescription + ": " + e.getMessage(),
                                             e );
        }
        finally
        {
            IOUtil.close( reader );
        }

        if ( assembly.isIncludeSiteDirectory() )
        {
            includeSiteInAssembly( assembly, configSource );
        }

        return assembly;
    }

    private void debugPrintAssembly( final String message, final Assembly assembly )
    {
        final StringWriter sWriter = new StringWriter();
        try
        {
            new AssemblyXpp3Writer().write( sWriter, assembly );
        }
        catch ( final IOException e )
        {
            LOGGER.debug( "Failed to print debug message with assembly descriptor listing, and message: "
                + message, e );
        }

        LOGGER.debug( message + "\n\n" + sWriter.toString() + "\n\n" );
    }

    /**
     * Add the contents of all included components to main assembly
     *
     * @param assembly the assembly
     * @param assemblyDir the assembly directory
     * @param transformer the component interpolator
     * @throws AssemblyReadException
     */
    protected void mergeComponentsWithMainAssembly( final Assembly assembly, final File assemblyDir,
                                                    final AssemblerConfigurationSource configSource,
                                                    ComponentXpp3Reader.ContentTransformer transformer )
                                                        throws AssemblyReadException
    {
        final Locator locator = new Locator();

        if ( assemblyDir != null && assemblyDir.exists() && assemblyDir.isDirectory() )
        {
            locator.addStrategy( new RelativeFileLocatorStrategy( assemblyDir ) );
        }

        // allow absolute paths in componentDescriptor... MASSEMBLY-486
        locator.addStrategy( new RelativeFileLocatorStrategy( configSource.getBasedir() ) );
        locator.addStrategy( new FileLocatorStrategy() );
        locator.addStrategy( new ClasspathResourceLocatorStrategy() );

        final AssemblyExpressionEvaluator aee = new AssemblyExpressionEvaluator( configSource );

        final List<String> componentLocations = assembly.getComponentDescriptors();

        for ( String location : componentLocations )
        {
            // allow expressions in path to component descriptor... MASSEMBLY-486
            try
            {
                location = aee.evaluate( location ).toString();
            }
            catch ( final Exception eee )
            {
                LOGGER.error( "Error interpolating componentDescriptor: " + location, eee );
            }

            final Location resolvedLocation = locator.resolve( location );

            if ( resolvedLocation == null )
            {
                throw new AssemblyReadException( "Failed to locate component descriptor: " + location );
            }

            Component component = null;
            try ( Reader reader = new InputStreamReader( resolvedLocation.getInputStream() ) )
            {
                component = new ComponentXpp3Reader( transformer ).read( reader );
            }
            catch ( final IOException | XmlPullParserException e )
            {
                throw new AssemblyReadException( "Error reading component descriptor: " + location + " (resolved to: "
                    + resolvedLocation.getSpecification() + ")", e );
            }

            mergeComponentWithAssembly( component, assembly );
        }
    }

    /**
     * Add the content of a single Component to main assembly
     *
     * @param component The component
     * @param assembly The assembly
     */
    protected void mergeComponentWithAssembly( final Component component, final Assembly assembly )
    {
        final List<ContainerDescriptorHandlerConfig> containerHandlerDescriptors =
            component.getContainerDescriptorHandlers();

        for ( final ContainerDescriptorHandlerConfig cfg : containerHandlerDescriptors )
        {
            assembly.addContainerDescriptorHandler( cfg );
        }

        final List<DependencySet> dependencySetList = component.getDependencySets();

        for ( final DependencySet dependencySet : dependencySetList )
        {
            assembly.addDependencySet( dependencySet );
        }

        final List<FileSet> fileSetList = component.getFileSets();

        for ( final FileSet fileSet : fileSetList )
        {
            assembly.addFileSet( fileSet );
        }

        final List<FileItem> fileList = component.getFiles();

        for ( final FileItem fileItem : fileList )
        {
            assembly.addFile( fileItem );
        }

        final List<ModuleSet> moduleSets = component.getModuleSets();
        for ( final ModuleSet moduleSet : moduleSets )
        {
            assembly.addModuleSet( moduleSet );
        }
    }

    @Override
    public void includeSiteInAssembly( final Assembly assembly, final AssemblerConfigurationSource configSource )
        throws InvalidAssemblerConfigurationException
    {
        final File siteDirectory = configSource.getSiteDirectory();

        if ( !siteDirectory.exists() )
        {
            throw new InvalidAssemblerConfigurationException( "site did not exist in the target directory - "
                + "please run site:site before creating the assembly" );
        }

        LOGGER.info( "Adding site directory to assembly : " + siteDirectory );

        final FileSet siteFileSet = new FileSet();

        siteFileSet.setDirectory( siteDirectory.getPath() );

        siteFileSet.setOutputDirectory( "/site" );

        assembly.addFileSet( siteFileSet );
    }
}
