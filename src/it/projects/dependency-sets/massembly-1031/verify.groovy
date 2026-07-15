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

// commons-lang3 is excluded: the direct provided declaration wins mediation over the transitive
// compile one pulled in by commons-text, so only commons-text remains on the runtime classpath.
def expectedFilenames = [
        "commons-text-1.12.0.jar"
]

File assemblyBasedir = new File( basedir, "target/massembly-1031-1-bin/" )

assert assemblyBasedir.listFiles().length == expectedFilenames.size()

for ( fileName in expectedFilenames )
{
  File file = new File( assemblyBasedir, fileName )
  assert file.isFile() // exists and is file
}

// commons-lang3 is provided and must be excluded
assert !new File( assemblyBasedir, "commons-lang3-3.14.0.jar" ).exists()

return true
