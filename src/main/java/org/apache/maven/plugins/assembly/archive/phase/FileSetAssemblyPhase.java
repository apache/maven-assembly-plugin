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

import javax.inject.Named;
import javax.inject.Singleton;

import java.util.List;

import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;
import org.apache.maven.plugins.assembly.archive.ArchiveCreationException;
import org.apache.maven.plugins.assembly.archive.task.AddFileSetsTask;
import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.codehaus.plexus.archiver.Archiver;

/**
 * Handles the &lt;fileSets/&gt; top-level section of the assembly descriptor.
 *
 *
 */
@Singleton
@Named("file-sets")
public class FileSetAssemblyPhase implements AssemblyArchiverPhase, PhaseOrder {

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(
            final Assembly assembly, final Archiver archiver, final AssemblerConfigurationSource configSource)
            throws ArchiveCreationException, AssemblyFormattingException {
        final List<FileSet> fileSets = assembly.getFileSets();

        if ((fileSets != null) && !fileSets.isEmpty()) {
            final AddFileSetsTask task = new AddFileSetsTask(fileSets);
            task.execute(archiver, configSource);
        }
    }

    @Override
    public int order() {
        // CHECKSTYLE_OFF: MagicNumber
        return 20;
        // CHECKSTYLE_ON: MagicNumber
    }
}
