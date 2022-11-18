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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;

/**
 * The URL Location, storing the URL content to a temporary local file.
 *
 */
class URLLocation
    extends FileLocation
{

    private final URL url;

    private final String tempFilePrefix;

    private final String tempFileSuffix;

    private final boolean tempFileDeleteOnExit;

    /**
     * @param url the URL
     * @param specification the spec
     * @param tempFilePrefix the prefix
     * @param tempFileSuffix the suffix
     * @param tempFileDeleteOnExit delete on exit
     */
    URLLocation( URL url, String specification, String tempFilePrefix, String tempFileSuffix,
                        boolean tempFileDeleteOnExit )
    {
        super( specification );

        this.url = url;
        this.tempFilePrefix = tempFilePrefix;
        this.tempFileSuffix = tempFileSuffix;
        this.tempFileDeleteOnExit = tempFileDeleteOnExit;
    }

    @Override
    protected void initFile()
        throws IOException
    {
        if ( unsafeGetFile() == null )
        {
            File tempFile = Files.createTempFile( tempFilePrefix, tempFileSuffix ).toFile();

            if ( tempFileDeleteOnExit )
            {
                tempFile.deleteOnExit();
            }

            IOUtils.copy( url, tempFile );

            setFile( tempFile );
        }
    }

}
