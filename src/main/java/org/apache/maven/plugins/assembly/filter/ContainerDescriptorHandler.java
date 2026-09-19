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

import org.codehaus.plexus.archiver.ArchiveFinalizer;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

/**
 * Customizes archive contents through selection and finalization callbacks.
 * <p>Handlers that call APIs removed by Plexus Archiver 5 must migrate and be recompiled against the
 * version used by this plugin. In particular, use {@code Archiver.getResources()} instead of
 * {@code Archiver.getFiles()}, and the {@code FileTime} timestamp methods instead of the removed
 * {@code Date} methods. Assembly's proxy follows the Archiver 5 API and does not provide compatibility
 * shims for removed methods.</p>
 */
public interface ContainerDescriptorHandler extends ArchiveFinalizer, FileSelector {}
