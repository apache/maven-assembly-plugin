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

// MASSEMBLY-737: a war assembled from the unpacked project artifact (which is a war itself) keeps WEB-INF/web.xml
File war = new File( basedir, "target/massembly-737-0.0.1-SNAPSHOT-deployable.war" )
assert war.isFile() : "Missing " + war
def names = []
new ZipFile( war ).withCloseable { z -> names = Collections.list( z.entries() ).collect { it.name } }

assert names.contains( "WEB-INF/web.xml" ) : "Entries: " + names
assert names.contains( "META-INF/MANIFEST.MF" )
