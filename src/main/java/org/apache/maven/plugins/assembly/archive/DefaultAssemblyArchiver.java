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
package org.apache.maven.plugins.assembly.archive;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.archiver.AssemblyProxyArchiver;
import org.apache.maven.plugins.assembly.archive.phase.AssemblyArchiverPhase;
import org.apache.maven.plugins.assembly.archive.phase.AssemblyArchiverPhaseComparator;
import org.apache.maven.plugins.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugins.assembly.filter.ComponentsXmlArchiverFileFilter;
import org.apache.maven.plugins.assembly.filter.ContainerDescriptorHandler;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.internal.DebugConfigurationListener;
import org.apache.maven.plugins.assembly.interpolation.AssemblyExpressionEvaluator;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.ContainerDescriptorHandlerConfig;
import org.apache.maven.plugins.assembly.utils.AssemblyFileUtils;
import org.apache.maven.plugins.assembly.utils.AssemblyFormatUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.diags.DryRunArchiver;
import org.codehaus.plexus.archiver.filters.JarSecurityFileSelector;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.archiver.tar.TarArchiver;
import org.codehaus.plexus.archiver.tar.TarLongFileMode;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.AbstractZipArchiver;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Objects.requireNonNull;

/**
 * Controller component designed to organize the many activities involved in creating an assembly archive. This includes
 * locating and configuring {@link Archiver} instances, executing multiple {@link org.apache.maven.plugins.assembly
 * .archive.phase.AssemblyArchiverPhase} instances to
 * interpret the various sections of the assembly descriptor and determine which files to add, and other associated
 * activities.
 *
 *
 */
@Named
public class DefaultAssemblyArchiver implements AssemblyArchiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAssemblyArchiver.class);

    private final ArchiverManager archiverManager;

    private final List<AssemblyArchiverPhase> assemblyPhases;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<String, ContainerDescriptorHandler> containerDescriptorHandlers;

    private final PlexusContainer container;

    @Inject
    public DefaultAssemblyArchiver(
            ArchiverManager archiverManager,
            List<AssemblyArchiverPhase> assemblyPhases,
            Map<String, ContainerDescriptorHandler> containerDescriptorHandlers,
            PlexusContainer container) {
        this.archiverManager = requireNonNull(archiverManager);
        this.assemblyPhases = requireNonNull(assemblyPhases);
        this.containerDescriptorHandlers = requireNonNull(containerDescriptorHandlers);
        this.container = requireNonNull(container);
    }

    private List<AssemblyArchiverPhase> sortedPhases() {
        List<AssemblyArchiverPhase> sorted = new ArrayList<>(assemblyPhases);
        sorted.sort(new AssemblyArchiverPhaseComparator());
        return sorted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File createArchive(
            final Assembly assembly,
            final String fullName,
            final String format,
            final AssemblerConfigurationSource configSource,
            FileTime outputTimestamp)
            throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException {
        validate(assembly);

        String filename = fullName;
        if (!configSource.isIgnoreDirFormatExtensions() || !format.startsWith("dir")) {
            filename += "." + format;
        }

        AssemblyFileUtils.verifyTempDirectoryAvailability(configSource.getTemporaryRootDirectory());

        final File outputDirectory = configSource.getOutputDirectory();

        final File destFile = new File(outputDirectory, filename);

        if (!shouldRecreateArchive(assembly, configSource, destFile)) {
            LOGGER.info("Skipping archive creation - no source files have been modified since last archive creation");
            return destFile;
        }

        try {
            final String finalName = configSource.getFinalName();
            final String specifiedBasedir = assembly.getBaseDirectory();

            String basedir = finalName;

            if (specifiedBasedir != null) {
                basedir = AssemblyFormatUtils.getOutputDirectory(
                        specifiedBasedir,
                        finalName,
                        configSource,
                        AssemblyFormatUtils.moduleProjectInterpolator(configSource.getProject()),
                        AssemblyFormatUtils.artifactProjectInterpolator(null));
            }

            final List<ContainerDescriptorHandler> containerHandlers =
                    selectContainerDescriptorHandlers(assembly.getContainerDescriptorHandlers(), configSource);

            final Archiver archiver = createArchiver(
                    format,
                    assembly.isIncludeBaseDirectory(),
                    basedir,
                    configSource,
                    containerHandlers,
                    outputTimestamp);

            archiver.setDestFile(destFile);

            for (AssemblyArchiverPhase phase : sortedPhases()) {
                phase.execute(assembly, archiver, configSource);
            }

            archiver.createArchive();
        } catch (final ArchiverException | IOException e) {
            throw new ArchiveCreationException(
                    "Error creating assembly archive " + assembly.getId() + ": " + e.getMessage(), e);
        } catch (final NoSuchArchiverException e) {
            throw new ArchiveCreationException(
                    "Unable to obtain archiver for extension '" + format + "', for assembly: '" + assembly.getId()
                            + "'",
                    e);
        } catch (final DependencyResolutionException e) {
            throw new ArchiveCreationException(
                    "Unable to resolve dependencies for assembly '" + assembly.getId() + "'", e);
        }

        return destFile;
    }

    private boolean shouldRecreateArchive(Assembly assembly, AssemblerConfigurationSource configSource, File destFile) {
        if (!destFile.exists()) {
            return true;
        }

        long lastModified = destFile.lastModified();
        File workingDir = configSource.getWorkingDirectory();

        return checkDirectoryModified(workingDir, lastModified);
    }

    private boolean checkDirectoryModified(File directory, long lastModified) {
        if (!directory.exists() || !directory.isDirectory()) {
            return false;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return false;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                if (checkDirectoryModified(file, lastModified)) {
                    return true;
                }
            } else if (file.lastModified() > lastModified) {
                return true;
            }
        }

        return false;
    }

    private void validate(final Assembly assembly) throws InvalidAssemblerConfigurationException {
        if (assembly.getId() == null || assembly.getId().trim().length() < 1) {
            throw new InvalidAssemblerConfigurationException("Assembly ID must be present and non-empty.");
        }
    }

    // CHECKSTYLE_OFF: LineLength
    private List<ContainerDescriptorHandler> selectContainerDescriptorHandlers(
            List<ContainerDescriptorHandlerConfig> requestedContainerDescriptorHandlers,
            final AssemblerConfigurationSource configSource)
            throws InvalidAssemblerConfigurationException
                // CHECKSTYLE_ON: LineLength
            {
        LOGGER.debug("All known ContainerDescriptorHandler components: "
                + (containerDescriptorHandlers == null
                        ? "none; map is null."
                        : "" + containerDescriptorHandlers.keySet()));

        if (requestedContainerDescriptorHandlers == null) {
            requestedContainerDescriptorHandlers = new ArrayList<>();
        }

        final List<ContainerDescriptorHandler> handlers = new ArrayList<>();
        final List<String> hints = new ArrayList<>();

        if (!requestedContainerDescriptorHandlers.isEmpty()) {
            for (final ContainerDescriptorHandlerConfig config : requestedContainerDescriptorHandlers) {
                final String hint = config.getHandlerName();
                final ContainerDescriptorHandler handler = containerDescriptorHandlers.get(hint);

                if (handler == null) {
                    throw new InvalidAssemblerConfigurationException(
                            "Cannot find ContainerDescriptorHandler with hint: " + hint);
                }

                LOGGER.debug("Found container descriptor handler with hint: " + hint + " (component: " + handler + ")");

                if (config.getConfiguration() != null) {
                    LOGGER.debug("Configuring handler with:\n\n" + config.getConfiguration() + "\n\n");

                    configureContainerDescriptorHandler(handler, (Xpp3Dom) config.getConfiguration(), configSource);
                }

                handlers.add(handler);
                hints.add(hint);
            }
        }

        if (!hints.contains("plexus")) {
            handlers.add(new ComponentsXmlArchiverFileFilter());
        }

        return handlers;
    }

    /**
     * Creates the necessary archiver to build the distribution file.
     *
     * @param format                Archive format
     * @param includeBaseDir        the base directory for include.
     * @param finalName             The final name.
     * @param configSource          {@link AssemblerConfigurationSource}
     * @param containerHandlers     The list of {@link ContainerDescriptorHandler}
     * @return archiver Archiver generated
     * @throws org.codehaus.plexus.archiver.ArchiverException
     * @throws org.codehaus.plexus.archiver.manager.NoSuchArchiverException
     */
    protected Archiver createArchiver(
            final String format,
            final boolean includeBaseDir,
            final String finalName,
            final AssemblerConfigurationSource configSource,
            final List<ContainerDescriptorHandler> containerHandlers,
            FileTime outputTimestamp)
            throws NoSuchArchiverException {

        Archiver archiver = archiverManager.getArchiver(format);

        if (archiver instanceof TarArchiver) {
            ((TarArchiver) archiver).setLongfile(TarLongFileMode.valueOf(configSource.getTarLongFileMode()));
        }

        if (archiver instanceof WarArchiver) {
            ((WarArchiver) archiver).setExpectWebXml(false);
        }

        if (archiver instanceof AbstractZipArchiver) {
            ((AbstractZipArchiver) archiver).setRecompressAddedZips(configSource.isRecompressZippedFiles());
        }

        final List<FileSelector> extraSelectors = new ArrayList<>();
        final List<ArchiveFinalizer> extraFinalizers = new ArrayList<>();
        if (archiver instanceof JarArchiver) {
            configureJarArchiver((JarArchiver) archiver, configSource.getMergeManifestMode());

            extraSelectors.add(new JarSecurityFileSelector());

            extraFinalizers.add(new ManifestCreationFinalizer(
                    configSource.getMavenSession(),
                    configSource.getProject(),
                    configSource.getJarArchiveConfiguration()));
        }

        if (configSource.getArchiverConfig() != null) {
            configureArchiver(archiver, configSource);
        }

        String prefix = "";
        if (includeBaseDir) {
            prefix = finalName;
        }

        archiver = new AssemblyProxyArchiver(
                prefix,
                archiver,
                containerHandlers,
                extraSelectors,
                extraFinalizers,
                configSource.getWorkingDirectory());
        if (configSource.isDryRun()) {
            archiver = new DryRunArchiver(archiver, LOGGER);
        }

        archiver.setIgnorePermissions(configSource.isIgnorePermissions());
        archiver.setForced(!configSource.isUpdateOnly());

        // configure for Reproducible Builds based on outputTimestamp value
        if (outputTimestamp != null) {
            archiver.configureReproducibleBuild(outputTimestamp);
        }

        if (configSource.getOverrideUid() != null) {
            archiver.setOverrideUid(configSource.getOverrideUid());
        }
        if (StringUtils.isNotBlank(configSource.getOverrideUserName())) {
            archiver.setOverrideUserName(StringUtils.trim(configSource.getOverrideUserName()));
        }
        if (configSource.getOverrideGid() != null) {
            archiver.setOverrideGid(configSource.getOverrideGid());
        }
        if (StringUtils.isNotBlank(configSource.getOverrideGroupName())) {
            archiver.setOverrideGroupName(StringUtils.trim(configSource.getOverrideGroupName()));
        }

        return archiver;
    }

    private void configureJarArchiver(JarArchiver archiver, String mergeManifestMode) {

        if (mergeManifestMode != null) {
            archiver.setFilesetmanifest(JarArchiver.FilesetManifestConfig.valueOf(mergeManifestMode));
        }

        archiver.setMinimalDefaultManifest(true);
    }

    private void configureContainerDescriptorHandler(
            final ContainerDescriptorHandler handler,
            final Xpp3Dom config,
            final AssemblerConfigurationSource configSource)
            throws InvalidAssemblerConfigurationException {
        LOGGER.debug("Configuring handler: '" + handler.getClass().getName() + "' -->");

        try {
            configureComponent(handler, config, configSource);
        } catch (final ComponentConfigurationException e) {
            throw new InvalidAssemblerConfigurationException(
                    "Failed to configure handler: " + handler.getClass().getName(), e);
        } catch (final ComponentLookupException e) {
            throw new InvalidAssemblerConfigurationException(
                    "Failed to lookup configurator for setup of handler: "
                            + handler.getClass().getName(),
                    e);
        }

        LOGGER.debug("-- end configuration --");
    }

    private void configureArchiver(final Archiver archiver, final AssemblerConfigurationSource configSource) {
        Xpp3Dom config;
        try {
            config = Xpp3DomBuilder.build(new StringReader(configSource.getArchiverConfig()));
        } catch (final XmlPullParserException | IOException e) {
            throw new ArchiverException(
                    "Failed to parse archiver configuration for: "
                            + archiver.getClass().getName(),
                    e);
        }

        LOGGER.debug("Configuring archiver: '" + archiver.getClass().getName() + "' -->");

        try {
            configureComponent(archiver, config, configSource);
        } catch (final ComponentConfigurationException e) {
            throw new ArchiverException(
                    "Failed to configure archiver: " + archiver.getClass().getName(), e);
        } catch (final ComponentLookupException e) {
            throw new ArchiverException(
                    "Failed to lookup configurator for setup of archiver: "
                            + archiver.getClass().getName(),
                    e);
        }

        LOGGER.debug("-- end configuration --");
    }

    private void configureComponent(
            final Object component, final Xpp3Dom config, final AssemblerConfigurationSource configSource)
            throws ComponentLookupException, ComponentConfigurationException {
        final ComponentConfigurator configurator = container.lookup(ComponentConfigurator.class, "basic");

        final ConfigurationListener listener = new DebugConfigurationListener(LOGGER);

        final ExpressionEvaluator expressionEvaluator = new AssemblyExpressionEvaluator(configSource);

        final XmlPlexusConfiguration configuration = new XmlPlexusConfiguration(config);

        final Object[] containerRealm = getContainerRealm();

        /*
         * NOTE: The signature of configureComponent() has changed in Maven 3.x, the reflection prevents a linkage error
         * and makes the code work with both Maven 2 and 3.
         */
        try {
            final Method configureComponent = ComponentConfigurator.class.getMethod(
                    "configureComponent",
                    Object.class,
                    PlexusConfiguration.class,
                    ExpressionEvaluator.class,
                    (Class<?>) containerRealm[1],
                    ConfigurationListener.class);

            configureComponent.invoke(
                    configurator, component, configuration, expressionEvaluator, containerRealm[0], listener);
        } catch (final NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (final InvocationTargetException e) {
            if (e.getCause() instanceof ComponentConfigurationException) {
                throw (ComponentConfigurationException) e.getCause();
            }
            throw new RuntimeException(e.getCause());
        }
    }

    private Object[] getContainerRealm() {
        /*
         * NOTE: The return type of getContainerRealm() has changed in Maven 3.x, the reflection prevents a linkage
         * error and makes the code work with both Maven 2 and 3.
         */
        try {
            final Method getContainerRealm = container.getClass().getMethod("getContainerRealm");
            return new Object[] {getContainerRealm.invoke(container), getContainerRealm.getReturnType()};
        } catch (final NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (final InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
