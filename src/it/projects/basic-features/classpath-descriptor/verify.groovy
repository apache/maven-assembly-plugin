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

import java.util.zip.ZipFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

// the "src" descriptorRef is built in three formats; each archive holds the project sources under test-1.0/
def tarNames( InputStream compressed )
{
    def names = []
    TarArchiveInputStream tar = new TarArchiveInputStream( compressed )
    try
    {
        for ( def entry = tar.nextTarEntry; entry != null; entry = tar.nextTarEntry )
        {
            names << entry.name
        }
    }
    finally
    {
        tar.close()
    }
    return names
}

File zip = new File( basedir, "target/test-1.0-src.zip" )
assert zip.isFile() : "Missing " + zip
def zipNames = []
new ZipFile( zip ).withCloseable { z -> zipNames = Collections.list( z.entries() ).collect { it.name } }
assert zipNames.contains( "test-1.0/pom.xml" ) : "zip entries: " + zipNames

File tgz = new File( basedir, "target/test-1.0-src.tar.gz" )
assert tgz.isFile() : "Missing " + tgz
def tgzNames = tarNames( new GzipCompressorInputStream( new FileInputStream( tgz ) ) )
assert tgzNames.contains( "test-1.0/pom.xml" ) : "tar.gz entries: " + tgzNames

File tbz = new File( basedir, "target/test-1.0-src.tar.bz2" )
assert tbz.isFile() : "Missing " + tbz
def tbzNames = tarNames( new BZip2CompressorInputStream( new FileInputStream( tbz ) ) )
assert tbzNames.contains( "test-1.0/pom.xml" ) : "tar.bz2 entries: " + tbzNames
