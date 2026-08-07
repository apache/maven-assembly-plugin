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

String unavailableRepositoryMessage =
        "cached from a remote repository ID that is unavailable in current build context"
assert !new File(basedir, "build.log").text.contains(unavailableRepositoryMessage)

new ZipFile(new File(basedir, "target/massembly-1306-1-dist.zip")).withCloseable { zip ->
    assert zip.getEntry("lib/massembly-1306-dependency-1.jar") != null
}
