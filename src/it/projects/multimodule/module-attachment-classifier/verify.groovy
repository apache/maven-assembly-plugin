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

import java.util.jar.JarFile

def modulesDirectory = new File(basedir, 'assembly/target/assembly-1.0-bin/modules')
def attachment = new File(modulesDirectory, 'producer-tests.jar')

assert attachment.isFile() : "The attached test JAR is missing: ${attachment}"

def moduleArtifacts = modulesDirectory.listFiles()*.name.sort()
assert moduleArtifacts == ['producer-tests.jar'] :
        "Expected only the attached test JAR, but found: ${moduleArtifacts}"

def jar = new JarFile(attachment)
try {
    assert jar.getEntry('attachment-marker.txt') != null :
            'The selected artifact does not contain the attachment marker.'
} finally {
    jar.close()
}
