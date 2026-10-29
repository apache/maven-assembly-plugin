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

File file = new File(basedir, "target/massembly-603-1.0-dist/file.txt")
assert file.exists()

List<String> lines = file.readLines()
assert lines.contains("version=1.0")

String timestampLine = lines.find { it.startsWith("timestamp=") }
assert timestampLine != null
assert !timestampLine.contains('${maven.build.timestamp}')
assert timestampLine ==~ /timestamp=\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z/

return true
