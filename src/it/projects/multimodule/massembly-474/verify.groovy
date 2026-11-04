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

// dependency versions are not part of what is verified: "maven-monitor-2.2.0.jar" becomes "maven-monitor.jar"
def normalized( File dir )
{
    return dir.list().collect { it.replaceAll( /-\d[\w.\-]*\.jar$/, ".jar" ) }.sort()
}

// each of the three assemblies (client, server and the combination of both) gets only its own libraries
File target = new File( basedir, "descriptor/target" )

def expected = [
    "all"    : [ "client/lib/maven-monitor.jar", "client/lib/modul-client.jar", "server/lib/maven-default-skin.jar", "server/lib/modul-server.jar" ],
    "client" : [ "client/lib/maven-monitor.jar", "client/lib/modul-client.jar" ],
    "server" : [ "server/lib/maven-default-skin.jar", "server/lib/modul-server.jar" ]
]

for ( String id : expected.keySet() )
{
    File dir = new File( target, "distribution-0.0.1-SNAPSHOT-" + id )
    assert dir.isDirectory() : "Missing assembly directory " + dir
    def files = []
    dir.eachFileRecurse( groovy.io.FileType.FILES ) { f ->
        files << dir.toPath().relativize( f.toPath() ).toString().replace( File.separator, "/" ).replaceAll( /-\d[\w.\-]*\.jar$/, ".jar" )
    }
    assert files.sort() == expected[ id ] : "Assembly " + id + " contains " + files
}
