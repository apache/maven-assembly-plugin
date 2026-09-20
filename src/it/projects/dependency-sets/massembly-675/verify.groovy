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

import java.io.*;

boolean result = true;

result = result && new File( basedir, "target/massembly-675-1-bin/massembly-675-1.jar" ).exists();
result = result && new File( basedir, "target/massembly-675-1-bin/closure-compiler-v20131014.jar" ).exists();

// assert that all the transitive dependencies of closure-compiler were excluded
result = result && !new File( basedir, "target/massembly-675-1-bin/args4j-2.0.16.jar" ).exists();
result = result && !new File( basedir, "target/massembly-675-1-bin/json-20090211.jar" ).exists();
result = result && !new File( basedir, "target/massembly-675-1-bin/protobuf-java-2.4.1.jar" ).exists();
result = result && !new File( basedir, "target/massembly-675-1-bin/jsr305-1.3.9.jar" ).exists();
result = result && !new File( basedir, "target/massembly-675-1-bin/guava-15.0.jar" ).exists();


return result;
