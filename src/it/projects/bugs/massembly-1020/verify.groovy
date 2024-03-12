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

import java.util.zip.ZipFile

def f = new File(basedir, "target/massembly-1020-0.0.1-SNAPSHOT-component.zip")

def expectedEntries = [
        "test1.txt",
        "test2.txt",
]

new ZipFile(f).withCloseable {zipFile ->
    expectedEntries.each {entryName ->
        def entry = zipFile.getEntry(entryName)
        assert entry != null: "missing zip entry: " + entryName
    }
}

return true