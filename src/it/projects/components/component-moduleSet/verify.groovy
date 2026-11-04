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

// the moduleSet comes from a componentDescriptor and includes only test:child1, with its test dependencies
File dir = new File( basedir, "child2/target/child2-1-bin" )
assert dir.isDirectory() : "Missing assembly directory " + dir
def names = normalized( dir )
assert names.contains( "child1.jar" ) : "Entries: " + dir.list()
assert names.contains( "junit-jupiter-api.jar" ) : "Entries: " + dir.list()
assert !names.contains( "child2.jar" ) : "child2 is not part of the moduleSet: " + dir.list()
