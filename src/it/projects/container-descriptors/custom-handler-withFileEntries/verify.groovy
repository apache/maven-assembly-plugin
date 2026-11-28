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

// "bin" lists both files as <file> entries, "bin-mixed" lists one as <file> and the other as <fileSet>
for (String assemblyId : ["bin", "bin-mixed"]) {
    File a = new File(basedir, "assembly/target/assembly-1-" + assemblyId + "/file.txt")
    File b = new File(basedir, "assembly/target/assembly-1-" + assemblyId + "/b/file.txt")
    assert a.exists() : "File: " + a + " should have been generated, but was not."
    List<String> lines = a.readLines('UTF-8')
    assert lines.count("file A") == 1 && lines.count("file B") == 1 : "File: " + a + " should contain lines file A and file B once each, but contained:\n" + lines
    assert !b.exists() : "File: " + b + " should not exist, but does."
}

// "bin-filtered" lists a filtered <file> entry, so the aggregate has to get its filtered content
File filtered = new File(basedir, "assembly/target/assembly-1-bin-filtered/file.txt")
File c = new File(basedir, "assembly/target/assembly-1-bin-filtered/c/file.txt")
assert filtered.exists() : "File: " + filtered + " should have been generated, but was not."
List<String> filteredLines = filtered.readLines('UTF-8')
assert filteredLines.count("file A") == 1 && filteredLines.count("file C 1") == 1 : "File: " + filtered + " should contain lines file A and file C 1 once each, but contained:\n" + filteredLines
assert !c.exists() : "File: " + c + " should not exist, but does."
return true
