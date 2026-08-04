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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Collections;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.diags.NoOpArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractLineAggregatingHandlerTest {

    @Test
    void addToArchiveShouldPropagateIOExceptionWhenAggregationWriteFails() {
        final AbstractLineAggregatingHandler handler = new AbstractLineAggregatingHandler() {
            @Override
            protected String getOutputPathPrefix(final FileInfo fileInfo) {
                return "";
            }

            @Override
            protected boolean fileMatches(final FileInfo fileInfo) {
                return false;
            }

            @Override
            protected OutputStream newAggregationOutputStream(final Path path) {
                return new OutputStream() {
                    @Override
                    public void write(final int b) throws IOException {
                        throw new IOException("simulated write failure");
                    }

                    @Override
                    public void write(final byte[] b, final int off, final int len) throws IOException {
                        throw new IOException("simulated write failure");
                    }
                };
            }
        };

        handler.setCatalog(Collections.singletonMap(
                "META-INF/services/example.Service", Collections.singletonList("com.example.Service")));

        assertThrows(ArchiverException.class, () -> handler.addToArchive(new NoOpArchiver()));
    }
}
