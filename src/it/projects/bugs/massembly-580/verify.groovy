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

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile

// MASSEMBLY-580: fileMode 0644 and directoryMode 0750 of the dependencySet are applied to the zip entries
File zip = new File( basedir, "target/project-deploy.zip" )
assert zip.isFile() : "Missing " + zip

ZipFile zf = new ZipFile( zip )
try
{
    ZipArchiveEntry dir = zf.getEntry( "webapps/" )
    assert dir != null : "Missing directory entry webapps/"
    assert ( dir.unixMode & 0777 ) == 0750 : "Mode of webapps/: " + Integer.toOctalString( dir.unixMode & 0777 )

    def files = zf.entries.toList().findAll { it.name.startsWith( "webapps/" ) && !it.directory }
    assert files.any { it.name == "webapps/commons-fileupload.jar" } : "Entries: " + zf.entries.toList()*.name
    for ( ZipArchiveEntry file : files )
    {
        assert ( file.unixMode & 0777 ) == 0644 : "Mode of " + file.name + ": " + Integer.toOctalString( file.unixMode & 0777 )
    }
}
finally
{
    zf.close()
}
