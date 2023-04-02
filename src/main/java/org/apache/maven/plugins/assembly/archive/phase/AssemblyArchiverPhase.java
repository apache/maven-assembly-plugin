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
package org.apache.maven.plugins.assembly.archive.phase;

import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.artifact.DependencyResolutionException;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.codehaus.plexus.archiver.Archiver;

/**
 * Handles one top-level section of the assembly descriptor, to determine which files to include in the assembly archive
 * for that section.
 *
 *
 */
public interface AssemblyArchiverPhase {
    /**
     * Handle the associated section of the assembly descriptor.
     *
     * @param assembly     The assembly descriptor to use
     * @param archiver     The archiver used to create the assembly archive, to which files/directories/artifacts are
     *                     added
     * @param configSource The configuration for this assembly build, normally derived from the plugin that launched
     *                     the assembly process.
     * @throws org.apache.maven.plugins.assembly.archive.ArchiveCreationException       in case of an archive
     *                                                                                  creation error.
     * @throws org.apache.maven.plugins.assembly.format.AssemblyFormattingException     in case of a assembly
     *                                                                                  formatting exception.
     * @throws org.apache.maven.plugins.assembly.InvalidAssemblerConfigurationException in case of an invalid
     *                                                                                  assembler configuration.
     */
    void execute(Assembly assembly, Archiver archiver, AssemblerConfigurationSource configSource)
            throws ArchiveCreationException, AssemblyFormattingException, InvalidAssemblerConfigurationException,
                    DependencyResolutionException;
}
