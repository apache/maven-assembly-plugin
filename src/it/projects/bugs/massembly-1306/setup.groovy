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

import java.security.MessageDigest
import java.util.jar.JarOutputStream

File artifactDirectory =
        new File(basedir, "remote-repository/org/apache/maven/its/massembly-1306-dependency/1")
assert artifactDirectory.mkdirs() || artifactDirectory.isDirectory()

File pom = new File(artifactDirectory, "massembly-1306-dependency-1.pom")
pom.text = """\
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>org.apache.maven.its</groupId>
  <artifactId>massembly-1306-dependency</artifactId>
  <version>1</version>
</project>
"""

File jar = new File(artifactDirectory, "massembly-1306-dependency-1.jar")
new JarOutputStream(new FileOutputStream(jar)).close()

[pom, jar].each { file ->
    String sha1 = MessageDigest.getInstance("SHA-1").digest(file.bytes).encodeHex()
    new File(artifactDirectory, "${file.name}.sha1").text = sha1
}

return true
