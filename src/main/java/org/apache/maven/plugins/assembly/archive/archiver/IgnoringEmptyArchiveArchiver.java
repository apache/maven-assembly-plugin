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
package org.apache.maven.plugins.assembly.archive.archiver;

import java.io.IOException;

import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.diags.DelgatingArchiver;
import org.codehaus.plexus.archiver.exceptions.EmptyArchiveException;
import org.slf4j.Logger;

/**
 * Delegating archiver implementation that swallows {@link EmptyArchiveException}, so an assembly that ends up
 * with no content completes without producing an archive instead of failing the build.
 *
 * @since 3.8.1
 */
public class IgnoringEmptyArchiveArchiver extends DelgatingArchiver {

    private final Logger logger;

    public IgnoringEmptyArchiveArchiver(final Archiver target, final Logger logger) {
        super(target);
        this.logger = logger;
    }

    @Override
    public void createArchive() throws ArchiverException, IOException {
        try {
            super.createArchive();
        } catch (final EmptyArchiveException e) {
            logger.info("Ignoring empty assembly, no archive created.");
        }
    }
}
