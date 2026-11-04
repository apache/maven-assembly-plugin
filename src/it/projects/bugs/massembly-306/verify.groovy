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

// MASSEMBLY-306: log4j-api is declared as a version range with scope provided, so jar-with-dependencies leaves it out
File jar = new File( basedir, "target/massembly-306-1-jar-with-dependencies.jar" )
assert jar.isFile() : "Missing " + jar
def names = []
new ZipFile( jar ).withCloseable { z -> names = Collections.list( z.entries() ).collect { it.name } }

assert names.contains( "org/junit/jupiter/api/Test.class" ) : "The compile scope dependency is missing"
assert !names.any { it.startsWith( "org/apache/logging/log4j/" ) } : "The provided dependency log4j-api was packed into the jar"
