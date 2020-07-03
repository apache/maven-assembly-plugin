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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 
 */
final class MessageLevels
{

    /**
     * Debug.
     */
    public static final int LEVEL_DEBUG = 0;
    /**
     * Info
     */
    public static final int LEVEL_INFO = 1;
    /**
     * Warning.
     */
    public static final int LEVEL_WARNING = 2;
    /**
     * Error
     */
    public static final int LEVEL_ERROR = 3;
    /**
     * Severe
     */
    public static final int LEVEL_SEVERE = 4;
    /**
     * Disabled.
     */
    public static final int LEVEL_DISABLED = 5;

    private static final List<String> LEVEL_NAMES;

    static
    {
        List<String> names = new ArrayList<String>();
        names.add( "DEBUG" );
        names.add( "INFO" );
        names.add( "WARN" );
        names.add( "ERROR" );
        names.add( "SEVERE" );

        LEVEL_NAMES = Collections.unmodifiableList( names );
    }

    private MessageLevels()
    {
    }

    /**
     * @param maxMessageLevel for which level
     * @return level states
     */
    static boolean[] getLevelStates( int maxMessageLevel )
    {
        boolean[] states = new boolean[5];

        Arrays.fill( states, false );

        switch ( maxMessageLevel )
        {
            case ( LEVEL_DEBUG ):
                states[LEVEL_DEBUG] = true;
            case ( LEVEL_INFO ):
                states[LEVEL_INFO] = true;
            case ( LEVEL_WARNING ):
                states[LEVEL_WARNING] = true;
            case ( LEVEL_ERROR ):
                states[LEVEL_ERROR] = true;
            case ( LEVEL_SEVERE ):
                states[LEVEL_SEVERE] = true;
            default:
        }

        return states;
    }

    /**
     * @param messageLevel the message leve.
     * @return The label.
     */
    static String getLevelLabel( int messageLevel )
    {
        if ( messageLevel > -1 && LEVEL_NAMES.size() > messageLevel )
        {
            return (String) LEVEL_NAMES.get( messageLevel );
        }

        throw new IllegalArgumentException( "Invalid message level: " + messageLevel );
    }
}
