
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

//import java.util.zip.*;
import org.apache.commons.compress.archivers.zip.*;

File deployDir = new File( basedir, 'target/repo/org/apache/maven/its/reproducible/1.0' )

assert deployDir.exists()

assert new File( deployDir, 'reproducible-1.0-src.zip.sha1' ).text == '5ce34fc133d47cbc9c81195877dbe10b9ec7d864'
assert new File( deployDir, 'reproducible-1.0-src.tar.sha1' ).text == '0b9dc1da069705a93b4954a198c18bd248822bf8'
assert new File( deployDir, 'reproducible-1.0-src.jar.sha1' ).text == '9b41cf2a80bc91c1f56b769bbef61c5dfd57974c'
