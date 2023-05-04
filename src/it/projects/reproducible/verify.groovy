
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

import java.util.jar.*;
import org.apache.commons.compress.archivers.zip.*

File deployDir = new File( basedir, 'target/repo/org/apache/maven/its/reproducible/1.0' )

assert deployDir.exists()

// Minimal Manifest was created
JarFile jarFile = new JarFile( new File( deployDir, "reproducible-1.0-src.jar" ) )
Manifest mf = jarFile.getManifest()
Attributes attrs = mf.getMainAttributes()
assert attrs.size() == 1
assert attrs.containsKey(Attributes.Name.MANIFEST_VERSION)

ZipFile zip = new ZipFile( new File( deployDir, "reproducible-1.0-src.zip" ) )
StringBuilder sb = new StringBuilder()
StringBuilder sb2 = new StringBuilder()
int i = 0
for( ZipArchiveEntry entry : zip.getEntries() )
{
    sb.append( String.format("%o %s\n", entry.getUnixMode(), entry.getName() ) )
    switch (++i)
    {
        case 1:
            sb2.append( String.format(" directory: %o\n", entry.getUnixMode() ) )
            break
        case 9:
            sb2.append( String.format("      file: %o\n", entry.getUnixMode() ) )
            break
        case 21:
            sb2.append( String.format("executable: %o\n", entry.getUnixMode() ) )
            break
        default:
            break
    }
}
sb.append( '\n' )
sb.append( 'unix modes summary:\n' )
sb.append( sb2.toString() )
sb.append( '\n' )
sb.append( 'resulting sha1:\n' )
for( String type : [ "zip", "jar", "tar" ] )
{
    String name = "reproducible-1.0-src." + type + ".sha1"
    sb.append( String.format("%s %s\n", new File( deployDir, name ).text, name ) )
}

effective = sb.toString()

// 3 different reference results:
// 1. Windows does not support executable flag:
//    => reference result is zip-content-win.txt: directory=40755, file=100644, executable=100644
// 2. on *nix, based on umask system configuration, group write mode differs:
//    - umask == 002: many Linux distro and MacOS create group writable files/directories:
//      => reference result is zip-content-775.txt: directory=40775, file=100664, executable=100775
//    - umask == 022: some Linux distros like recent Fedora create group read-only files/directories:
//      => reference result is zip-content-755.txt: directory=40755, file=100644, executable=100755
// with MASSEMBLY-989, umask 022 is forced: 775 is not happening any more, even if the IT check could detect it...
reference = "zip-content-" + ( effective.contains( "644 executable" ) ? "win" : effective.contains( "0775" ) ? "775" : "755" ) + ".txt"
content = new File( basedir, reference ).text.replace( "\r\n", "\n" )

println( 'effective content:' )
println( effective )
println( 'comparing against reference ' + reference )
if ( reference.contains( "775" ) )
{
    println( '775 reference is not supposed to happen since MASSEMBLY-989' )
    return -1
}

index = content.indexOf( 'resulting sha1:' )
contentMode = content.substring( 0, index )
contentSha1 = content.substring( index )
index = content.indexOf( 'resulting sha1:' )
effectiveMode = effective.substring( 0, index )
effectiveSha1 = effective.substring( index )

assert contentMode == effectiveMode
println( 'unix mode ok')
assert contentSha1 == effectiveSha1
println( 'sha1 ok')
