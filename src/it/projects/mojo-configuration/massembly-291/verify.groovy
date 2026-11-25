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
String base = "./deploy/org/test/massembly-291/1/massembly-291-1";

File pom = new File( basedir, base + ".pom" );
File zip = new File( basedir, base + ".zip" );
File tgz = new File( basedir, base + ".tar.gz" );

assert pom.exists() : "Project POM is missing in deployment repository. File was: " + pom;

assert zip.exists() : "Project sources zip is missing in deployment repository. File was: " + zip;

assert tgz.exists() : "Project sources tar.gz is missing in deployment repository. File was: " + tgz;
