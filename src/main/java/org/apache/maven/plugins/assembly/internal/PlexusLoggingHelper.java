package org.apache.maven.plugins.assembly.internal;

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

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.slf4j.Slf4jLogger;

/**
 * Support for plexus logging, as downstream component APIs still expect Plexus logger..
 */
public final class PlexusLoggingHelper
{
    private PlexusLoggingHelper()
    {
        // nop
    }

    public static Logger wrap( final org.slf4j.Logger logger )
    {
        return new Slf4jLogger( 0, logger ); // set it lowest level, let SLF4J filter
    }
}
