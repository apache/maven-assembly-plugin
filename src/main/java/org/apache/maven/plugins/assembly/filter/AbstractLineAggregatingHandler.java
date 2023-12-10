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
package org.apache.maven.plugins.assembly.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.assembly.utils.AssemblyFileUtils;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.ResourceIterator;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

abstract class AbstractLineAggregatingHandler implements ContainerDescriptorHandler {

    private Map<String, List<String>> catalog = new HashMap<>();

    private boolean excludeOverride = false;

    protected abstract String getOutputPathPrefix(FileInfo fileInfo);

    protected abstract boolean fileMatches(FileInfo fileInfo);

    String getEncoding() {
        return "UTF-8";
    }

    @Override
    public void finalizeArchiveCreation(final Archiver archiver) {
        // this will prompt the isSelected() call, below, for all resources added to the archive.
        // FIXME: This needs to be corrected in the AbstractArchiver, where
        // runArchiveFinalizers() is called before regular resources are added...
        // which is done because the manifest needs to be added first, and the
        // manifest-creation component is a finalizer in the assembly plugin...
        for (final ResourceIterator it = archiver.getResources(); it.hasNext(); ) {
            it.next();
        }

        addToArchive(archiver);
    }

    void addToArchive(final Archiver archiver) {
        for (final Map.Entry<String, List<String>> entry : catalog.entrySet()) {
            final String name = entry.getKey();
            final String fname = new File(name).getName();

            File f;
            try {
                f = Files.createTempFile("assembly-" + fname, ".tmp").toFile();
                f.deleteOnExit();

                try (PrintWriter writer =
                        new PrintWriter(new OutputStreamWriter(Files.newOutputStream(f.toPath()), getEncoding()))) {
                    for (final String line : entry.getValue()) {
                        writer.println(line);
                    }
                }
            } catch (final IOException e) {
                throw new ArchiverException(
                        "Error adding aggregated content for: " + fname + " to finalize archive creation. Reason: "
                                + e.getMessage(),
                        e);
            }

            excludeOverride = true;
            archiver.addFile(f, name);
            excludeOverride = false;
        }
    }

    @Override
    public void finalizeArchiveExtraction(final UnArchiver unArchiver) {}

    @Override
    public List<String> getVirtualFiles() {
        return new ArrayList<>(catalog.keySet());
    }

    @Override
    public boolean isSelected(final FileInfo fileInfo) throws IOException {
        if (excludeOverride) {
            return true;
        }

        String name = AssemblyFileUtils.normalizeFileInfo(fileInfo);

        if (fileInfo.isFile() && fileMatches(fileInfo)) {
            name = getOutputPathPrefix(fileInfo) + new File(name).getName();
            List<String> lines = catalog.computeIfAbsent(name, k -> new ArrayList<>());
            readLines(fileInfo, lines);
            return false;
        }

        return true;
    }

    void readLines(final FileInfo fileInfo, final List<String> lines) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileInfo.getContents(), getEncoding()))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (!lines.contains(line)) {
                    lines.add(line);
                }
            }
        }
    }

    protected final Map<String, List<String>> getCatalog() {
        return catalog;
    }

    protected final void setCatalog(final Map<String, List<String>> catalog) {
        this.catalog = catalog;
    }
}
