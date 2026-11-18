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

import java.nio.file.Files

// Git and the Invoker copy cannot store hard-link relationships, so create real aliases after cloning.
['plain': 'plain payload\n', 'mapped': 'mapped payload\n',
 'transformed': 'value=${hardlinks.value}\r\n', 'filtered': 'value=${hardlinks.value}\r\n'].each { name, contents ->
    def directory = new File(basedir, "input/$name").toPath()
    Files.createDirectories(directory)
    def source = directory.resolve('data.txt')
    Files.writeString(source, contents)
    def aliases = name == 'transformed' ? ['filtered.txt', 'unix.txt'] : ['alias.txt']
    aliases.each { alias ->
        def link = directory.resolve(alias)
        Files.deleteIfExists(link)
        Files.createLink(link, source)
        assert Files.isSameFile(source, link)
    }
}

// Matching bytes alone must not make an unrelated file eligible for preservation.
def copy = new File(basedir, 'input/plain/copy.txt').toPath()
Files.deleteIfExists(copy)
Files.copy(new File(basedir, 'input/plain/data.txt').toPath(), copy)
assert !Files.isSameFile(copy, new File(basedir, 'input/plain/data.txt').toPath())
return true
