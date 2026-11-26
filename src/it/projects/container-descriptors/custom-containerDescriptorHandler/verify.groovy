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

File a = new File(basedir, "assembly/target/assembly-1-bin/file.txt")
File b = new File(basedir, "assembly/target/assembly-1-bin/b/file.txt")

assert a.exists() : "File: " + a + " should have been generated, but was not."
List<String> lines = a.readLines('UTF-8')
assert lines.contains("file A") && lines.contains("file B") : "File: " + a + " should contain lines:\nfile A\nfile B\n\n ...but contained:\n" + lines
assert !b.exists() : "File: " + b + " should not exist, but does."
return true
