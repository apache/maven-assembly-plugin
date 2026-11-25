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

import java.util.zip.*;


File zipFile = new File( basedir, "child2/target/project2-0.0.1-SNAPSHOT-deployable.zip" );

assert zipFile.exists() && !zipFile.isDirectory() : "zip-file is missing or a directory.";

ZipFile zf = new ZipFile( zipFile );

assert zf.getEntry( "project2-0.0.1-SNAPSHOT/empty.properties" ) != null : "empty.properties entry is missing.";

assert zf.getEntry( "project2-0.0.1-SNAPSHOT/bin/start.sh" ) != null : "start.sh entry is missing.";
