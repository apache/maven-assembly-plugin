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

import java.util.zip.*


String basename = "multimodule-sources-copied-1.0-SNAPSHOT"

File zipFile = new File(basedir, "target/" + basename + "-project.zip")

System.out.println("Checking for existence and file-ishness of: " + zipFile)

assert zipFile.exists() && !zipFile.isDirectory() : "zip file is missing or a directory."

System.out.println( "Creating zipFile instance." )

ZipFile zf = new ZipFile( zipFile )

String childPath = basename + "/child1"

System.out.println( "Checking child1 project files..." )

System.out.println( "Looking for project source file." )

ZipEntry je = zf.getEntry( childPath + "/src/main/java/test/App.java" )

assert je != null : childPath + " source file missing."

System.out.println( "Looking for project test-source file." )

je = null
je = zf.getEntry( childPath + "/src/test/java/test/AppTest.java" )

assert je != null : childPath + " test-source file missing."

System.out.println( "Looking for pom.xml file." )

je = null
je = zf.getEntry( childPath + "/pom.xml" )

assert je != null : childPath + " POM missing."

je = null
je = zf.getEntry( childPath + "/target/child1-1.0-SNAPSHOT.jar" )

assert je == null : childPath + " target directory should not be included."

childPath = basename + "/child2"

System.out.println( "Checking child2 project files..." )

System.out.println( "Looking for project source file." )

je = null
je = zf.getEntry( childPath + "/src/main/java/test/App.java" )

assert je != null : childPath + " source file missing."

System.out.println( "Looking for project test-source file." )

je = null
je = zf.getEntry( childPath + "/src/test/java/test/AppTest.java" )

assert je != null : childPath + " test-source file missing."

System.out.println( "Looking for pom.xml file." )

je = null
je = zf.getEntry( childPath + "/pom.xml" )

assert je != null : childPath + " POM missing."

je = null
je = zf.getEntry( childPath + "/target/child2-1.0-SNAPSHOT.jar" )

assert je == null : childPath + " target directory should not be included."

System.out.println( "Looking for main pom.xml file." )

je = null
je = zf.getEntry( basename + "/pom.xml" )

assert je != null : "project POM missing."
