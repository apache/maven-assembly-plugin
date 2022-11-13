
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

//import java.util.zip.*
import org.apache.commons.compress.archivers.zip.*

File deployDir = new File( basedir, 'target/repo/org/apache/maven/its/reproducible/1.0' )

assert deployDir.exists()

ZipFile zip = new ZipFile( new File( deployDir, "reproducible-1.0-src.zip" ) )
StringBuilder sb = new StringBuilder()
for( ZipArchiveEntry entry : zip.getEntries() )
{
    sb.append( String.format("%o %s\n", entry.getUnixMode(), entry.getName() ) )
}
for( String type : [ "zip", "jar", "tar" ] )
{
    String name = "reproducible-1.0-src." + type + ".sha1"
    sb.append( String.format("%s %s\n", new File( deployDir, name ).text, name ) )
}

effective = sb.toString()

// 3 different reference results:
// 1. Windows does not support executable flag
// 2. on *nix, based on system configuration, group flag differs
reference = "zip-content-" + ( effective.contains( "644 executable" ) ? "win" : effective.contains( "0775" ) ? "775" : "755" ) + ".txt"
content = new File( basedir, reference ).text.replace( "\r\n", "\n" )

assert content == effective
