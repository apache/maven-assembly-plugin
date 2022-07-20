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

def expectedFilenames = [ "javafx-base-18.0.1.jar",
  "javafx-controls-18.0.1-linux.jar",
  "javafx-media-18.0.1-linux.jar",
  "javafx-web-18.0.1.jar",
  "javafx-base-18.0.1-linux.jar",
  "javafx-graphics-18.0.1-linux.jar",
  "javafx-swing-18.0.1.jar",
  "javafx-web-18.0.1-linux.jar",
  "javafx-controls-18.0.1.jar",
  "javafx-media-18.0.1.jar",
  "javafx-swing-18.0.1-linux.jar"
]

File assemblyBasedir = new File( basedir, "target/massembly-969-1-bin/" )

assert assemblyBasedir.listFiles().length == expectedFilenames.size()

for ( fileName in expectedFilenames )
{
  File file = new File( assemblyBasedir, fileName )
  assert file.isFile() // exists and is file
}

// defined set vs listed set: same cardinality and all present: OK

return true
