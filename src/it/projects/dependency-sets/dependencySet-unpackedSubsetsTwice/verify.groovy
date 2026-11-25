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

File jarFile = new File( basedir, "child2/target/child2-1.0-SNAPSHOT-bin.jar" );

System.out.println( "Checking for existence and file-ishness of: " + jarFile );

assert jarFile.exists() && !jarFile.isDirectory() : "jar file is missing or a directory.";

System.out.println( "Creating JarFile instance." );

JarFile jf = new JarFile( jarFile );

System.out.println( "Looking for 'classes1/test/App.class' jar entry." );

assert jf.getEntry( "classes1/test/App.class" ) != null : "classes1/test/App.class entry is missing.";

System.out.println( "Looking for absence of 'classes1/test/App2.class' jar entry." );

assert jf.getEntry( "classes1/test/App2.class" ) == null : "classes1/test/App2.class should not be present.";

System.out.println( "Looking for 'classes2/test/App2.class' jar entry." );

assert jf.getEntry( "classes2/test/App2.class" ) != null : "classes2/test/App2.class entry is missing.";

System.out.println( "Looking for absence of 'classes2/test/App.class' jar entry." );

assert jf.getEntry( "classes2/test/App.class" ) == null : "classes2/test/App.class should not be present.";
