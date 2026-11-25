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

import java.util.jar.*;


System.out.println( "Creating JarFile java.io.File." );

File jarFile = new File( basedir, "target/jar-with-dependencies-no-target-dir-1.0-SNAPSHOT-jar-with-dependencies.jar" );

System.out.println( "Checking for existence and file-ishness of: " + jarFile );

assert jarFile.exists() && !jarFile.isDirectory() : "jar file is missing or a directory.";

System.out.println( "Creating JarFile instance." );

JarFile jf = new JarFile( jarFile );

assert jf.getEntry( "test/App.class" ) != null : "App class is missing.";

System.out.println( "Looking for absence of 'target/classes/test/App.class' jar entry." );

assert jf.getEntry( "target/classes/test/App.class" ) == null : "target directory should not be present.";
