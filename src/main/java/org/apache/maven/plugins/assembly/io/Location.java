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
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * The location interface.
 *
 */
public interface Location
{

    /**
     * @return {@link File}.
     * @throws IOException in case of an error
     */
    File getFile() throws IOException;

    /**
     * open the location.
     * @throws IOException in case of an error
     */
    void open() throws IOException;

    /**
     * Close the location.
     */
    void close();

    /**
     * @param buffer The buffer.
     * @return number of read bytes.
     * @throws IOException in case of an error.
     */
    int read( ByteBuffer buffer ) throws IOException;

    /**
     * @param buffer the buffer
     * @return number of read bytes
     * @throws IOException in case of an error
     */
    int read( byte[] buffer ) throws IOException;

    /**
     * @return the resulting input stream.
     * @throws IOException in case of an error
     */
    InputStream getInputStream() throws IOException;

    /**
     * @return spec.
     */
    String getSpecification();

}
