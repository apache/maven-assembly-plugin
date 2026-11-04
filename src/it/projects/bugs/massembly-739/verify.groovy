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

// MASSEMBLY-739: directory entries of the zip got mode 000, which makes the extracted directories unusable
File zip = new File( basedir, "target/assembly-739-bug-1.0.0-SNAPSHOT-conapp.zip" )
assert zip.isFile() : "Missing " + zip

ZipFile zf = new ZipFile( zip )
try
{
    def dirs = zf.entries.toList().findAll { it.directory }
    assert dirs*.name.containsAll( [ "assembly-739-bug-1.0.0-SNAPSHOT/lib/", "assembly-739-bug-1.0.0-SNAPSHOT/bin/" ] ) : "Entries: " + zf.entries.toList()*.name
    for ( ZipArchiveEntry dir : dirs )
    {
        // the exact bits depend on the umask of the build; the owner has to be able to enter and list the directory
        assert ( dir.unixMode & 0700 ) == 0700 : "Mode of " + dir.name + ": " + Integer.toOctalString( dir.unixMode & 0777 )
    }
}
finally
{
    zf.close()
}
