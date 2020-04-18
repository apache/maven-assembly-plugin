
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

import java.util.jar.*

def log4jApi = new JarFile(new File(localRepositoryPath, 'org/apache/logging/log4j/log4j-api/2.9.1/log4j-api-2.9.1.jar'))
def jarWithDeps = new JarFile(new File( basedir, 'target/massembly891-0.0.1-SNAPSHOT-jar-with-dependencies.jar'))

cls = 'org/apache/logging/log4j/util/StackLocator.class'
assert log4jApi.getEntry(cls) != null
assert jarWithDeps.getEntry(cls) != null
assert log4jApi.getEntry(cls).size == jarWithDeps.getEntry(cls).size 
assert log4jApi.getEntry(cls).time == jarWithDeps.getEntry(cls).time 

cls = 'META-INF/versions/9/org/apache/logging/log4j/util/StackLocator.class'
assert log4jApi.getEntry(cls) != null
assert jarWithDeps.getEntry(cls) != null
assert log4jApi.getEntry(cls).size == jarWithDeps.getEntry(cls).size 
assert log4jApi.getEntry(cls).time == jarWithDeps.getEntry(cls).time 
