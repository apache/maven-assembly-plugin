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

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream

// a filtered fileSet written to a tar used to crash the build (MASSEMBLY-722)
File tarFile = new File( basedir, "target/maven-assembly-plugin-2.5-crash-0.0.1-SNAPSHOT-prod.tar" )
assert tarFile.isFile() : "Missing " + tarFile

String content = null
TarArchiveInputStream tar = new TarArchiveInputStream( new FileInputStream( tarFile ) )
try
{
    for ( def entry = tar.nextTarEntry; entry != null; entry = tar.nextTarEntry )
    {
        if ( entry.name.endsWith( "/config/my.properties" ) )
        {
            content = new String( org.apache.commons.compress.utils.IOUtils.toByteArray( tar ), "ISO-8859-1" )
        }
    }
}
finally
{
    tar.close()
}
assert content != null : "config/my.properties is missing from the tar"

def props = new Properties()
props.load( new StringReader( content ) )
// the filter from project.build.filters is applied; the other filter file is not part of this build
assert props.getProperty( "environment" ) == "test-environment" : content
assert props.getProperty( "value.test" ) == "test.value" : content
assert props.getProperty( "value.prod" ) == '${filtered.prod}' : content
