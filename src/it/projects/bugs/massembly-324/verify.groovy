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

// MASSEMBLY-324: runtime dependencies end up in WEB-INF/lib below the base directory spring-<version>
File zip = new File( basedir, "target/project2-0.0.1-SNAPSHOT-bin.zip" )
assert zip.isFile() : "Missing " + zip
def names = []
new ZipFile( zip ).withCloseable { z -> names = Collections.list( z.entries() ).collect { it.name } }

String lib = "spring-0.0.1-SNAPSHOT/WEB-INF/lib/"
def libFiles = names.findAll { it.startsWith( lib ) && !it.endsWith( "/" ) }.collect { it.substring( lib.length() ) }
assert libFiles.any { it ==~ /maven-shared-io-.*\.jar/ } : "maven-shared-io is missing from WEB-INF/lib: " + names
assert libFiles.contains( "project2-0.0.1-SNAPSHOT.jar" ) : "The project artifact is missing from WEB-INF/lib: " + names
assert !libFiles.any { it.contains( "/" ) } : "Unexpected nested entries in WEB-INF/lib: " + libFiles
assert names.every { it.startsWith( "spring-0.0.1-SNAPSHOT/" ) } : "Entries outside of the base directory: " + names
