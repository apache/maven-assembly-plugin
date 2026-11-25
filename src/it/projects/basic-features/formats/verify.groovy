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

File assembly = new File(basedir, "target/assemblies/format-test-1.0")
assert assembly.exists()
assert assembly.isDirectory()
assert new File(assembly, "TODO.txt").exists()

assembly = new File(basedir, "target/assemblies/format-test-1.0.zip")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.tar")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.tar.gz")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.tgz")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.tar.bz2")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.tbz2")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.jar")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.war")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.tar.snappy")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.tar.xz")
assert assembly.exists()
assert assembly.isFile()

assembly = new File(basedir, "target/assemblies/format-test-1.0.txz")
assert assembly.exists()
assert assembly.isFile()
