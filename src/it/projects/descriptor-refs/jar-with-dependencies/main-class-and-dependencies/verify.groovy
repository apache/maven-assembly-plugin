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

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.Attributes

def expectedJarEntries = [
        "org/apache/commons/io/IOUtils.class",
        "test/App.class"
]

File file = new File(basedir, "target/main-class-and-dependencies-1-jar-with-dependencies.jar")
assert file.isFile(): "jar file is missing or a directory."

JarFile jarFile = new JarFile(file)
expectedJarEntries.each {entryName ->
    JarEntry jarEntry = jarFile.getJarEntry(entryName)
    assert jarEntry != null: "missing jar entry: " + entryName
}

assert "test.App" == jarFile.manifest.mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
