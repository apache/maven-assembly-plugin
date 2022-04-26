package org.apache.maven.plugins.assembly.utils;

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

import org.codehaus.plexus.components.io.fileselectors.FileInfo;

import java.io.File;
import java.util.Locale;

/**
 *
 */
public final class AssemblyFileUtils
{

    private AssemblyFileUtils()
    {
        // no op
    }

    public static String makePathRelativeTo( String path, final File basedir )
    {
        if ( basedir == null )
        {
            return path;
        }

        if ( path == null )
        {
            return null;
        }

        path = path.trim();

        String base = basedir.getAbsolutePath();
        if ( path.startsWith( base ) )
        {
            path = path.substring( base.length() );
            if ( path.length() > 0 )
            {
                if ( path.startsWith( "/" ) || path.startsWith( "\\" ) )
                {
                    path = path.substring( 1 );
                }
            }

            if ( path.length() == 0 )
            {
                path = ".";
            }
        }

        if ( !new File( path ).isAbsolute() )
        {
            path = path.replace( '\\', '/' );
        }

        return path;
    }

    @SuppressWarnings( "ResultOfMethodCallIgnored" )
    public static void verifyTempDirectoryAvailability( final File tempDir )
    {
        if ( !tempDir.exists() )
        {
            tempDir.mkdirs();
        }
    }

    private static String normalizePath( String path )
    {
        return path.replace( '\\', '/' );
    }

    public static String normalizeFileInfo( FileInfo fileInfo )
    {
        String name = fileInfo.getName();
        name = normalizePath( name );
        return name.replace( File.separatorChar, '/' ); // How can this be anything but a no-op
    }

    public static boolean isPropertyFile( String sourceName )
    {
        return sourceName.toLowerCase( Locale.ENGLISH ).endsWith( ".properties" );
    }

    public static boolean isPropertyFile( File file )
    {
        return isPropertyFile( file.getName() );
    }

    /**
     * This method exists because {@link File#isAbsolute()} is not OS independent.
     *
     * In addition to the check in {@link File#isAbsolute()} we will also test for a leading '/'.
     *
     * @return {@code true} if {@code File#isAbsolute()} or starts with a '/'
     */
    public static boolean isAbsolutePath( File dir )
    {
        return dir != null
                && ( dir.isAbsolute()
                   || dir.getPath().startsWith( "\\" ) ); // on Win* platforms
    }
}
