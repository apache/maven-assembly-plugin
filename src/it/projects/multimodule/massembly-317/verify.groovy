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

// binaries of every module plus the junit-jupiter-api dependency, selected by useStrictFiltering
File dir = new File( basedir, "target/my-app-1.0-SNAPSHOT-bin/my-app-1.0-SNAPSHOT/server/default" )
assert dir.isDirectory() : "Missing assembly directory " + dir
assert normalized( dir ) == [ "junit-jupiter-api.jar", "my-app-module1.jar", "my-app-module2.jar" ] : "Entries: " + dir.list()
