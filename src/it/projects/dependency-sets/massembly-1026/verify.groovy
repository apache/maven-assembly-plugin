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

// guava 16.0 (from the test-scoped jackson-datatype-guava) wins version mediation over guice's
// guava 31.0.1-jre and stays on the runtime classpath via guice.
def expectedFilenames = [
        "aopalliance-1.0.jar",
        "guava-16.0.jar",
        "guice-6.0.0.jar",
        "jakarta.inject-api-2.0.1.jar",
        "javax.inject-1.jar"
]

File assemblyBasedir = new File( basedir, "target/massembly-1026-1-bin/" )

assert assemblyBasedir.listFiles().length == expectedFilenames.size()

for ( fileName in expectedFilenames )
{
  File file = new File( assemblyBasedir, fileName )
  assert file.isFile() // exists and is file
}

// the losing version must not be assembled
assert !new File( assemblyBasedir, "guava-31.0.1-jre.jar" ).exists()

return true
