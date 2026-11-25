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

import java.util.Arrays;

assert new File( basedir, "child4/target/child4-1.0-bin/lib/child1.jar" ).exists() : "Child-1 jar is NOT in output!";

assert new File( basedir, "child4/target/child4-1.0-bin/lib/child3.jar" ).exists() : "Child-3 jar is NOT in output!";

assert !new File( basedir, "child4/target/child4-1.0-bin/lib/child2.jar" ).exists() : "Child-2 jar SHOULD NOT BE in output!";

File libDirectory = new File( basedir, "child4/target/child4-1.0-bin/lib" );
File[] includedArtifacts = libDirectory.listFiles();

assert includedArtifacts != null && includedArtifacts.length == 2 : "Only the two selected module artifacts should be present when includeDependencies=false.\nFiles found: " + Arrays.toString( includedArtifacts );
