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
package org.apache.maven.plugins.assembly.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;

/**
 * The URL Location, storing the URL content to a temporary local file.
 *
 */
/*class URLLocation extends FileLocation {

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
/*  URLLocation(
            URL url, String specification, String tempFilePrefix, String tempFileSuffix, boolean tempFileDeleteOnExit) {
        super(specification);

        this.url = url;
        this.tempFilePrefix = tempFilePrefix;
        this.tempFileSuffix = tempFileSuffix;
        this.tempFileDeleteOnExit = tempFileDeleteOnExit;
    }

    @Override
    protected void initFile() throws IOException {
        if (unsafeGetFile() == null) {
            File tempFile = Files.createTempFile(tempFilePrefix, tempFileSuffix).toFile();

            if (tempFileDeleteOnExit) {
                tempFile.deleteOnExit();
            }

            IOUtils.copy(url, tempFile);

            setFile(tempFile);
        }
    }
}*/

// -------Refactored code using Push Down method------

class URLLocation implements Location {

    private final URL url;
    private final String tempFilePrefix;
    private final String tempFileSuffix;
    private final boolean tempFileDeleteOnExit;
    private File file;
    private FileInputStream stream;
    private FileChannel channel;
    private final String specification;

    URLLocation(
            URL url, String specification, String tempFilePrefix, String tempFileSuffix, boolean tempFileDeleteOnExit) {
        this.url = url;
        this.specification = specification;
        this.tempFilePrefix = tempFilePrefix;
        this.tempFileSuffix = tempFileSuffix;
        this.tempFileDeleteOnExit = tempFileDeleteOnExit;
    }

    @Override
    public void close() {
        if ((channel != null) && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException e) {
                // swallow it.
            }
        }

        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // swallow it.
            }
        }
    }

    @Override
    public File getFile() throws IOException {
        initFile();

        return unsafeGetFile();
    }

    @Override
    public String getSpecification() {
        return specification;
    }

    @Override
    public void open() throws IOException {
        if (stream == null) {
            initFile();

            stream = new FileInputStream(file);
            channel = stream.getChannel();
        }
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        open();
        return channel.read(buffer);
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        open();
        return channel.read(ByteBuffer.wrap(buffer));
    }

    @Override
    public InputStream getInputStream() throws IOException {
        open();
        return stream;
    }

    protected void initFile() throws IOException {

        if (file == null) {
            File tempFile = Files.createTempFile(tempFilePrefix, tempFileSuffix).toFile();

            if (tempFileDeleteOnExit) {
                tempFile.deleteOnExit();
            }

            IOUtils.copy(url, tempFile);

            file = tempFile;
        }
    }

    protected void setFile(File file) {
        if (channel != null) {
            throw new IllegalStateException("Location is already open; cannot setFile(..).");
        }

        this.file = file;
    }

    File unsafeGetFile() {
        return file;
    }
}
