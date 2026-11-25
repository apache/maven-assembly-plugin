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

File dir = new File(basedir, "target/include-project-attachments-1.0-SNAPSHOT-bin")

System.out.println("Checking for existence and file-ishness of: " + dir)

assert dir.exists() && dir.isDirectory() : "assembled directory is missing or not a directory."

File projectArtifact = new File( dir, "lib/include-project-attachments-1.0-SNAPSHOT.jar" )

System.out.println( "Looking for 'lib/include-project-attachments-1.0-SNAPSHOT.jar file in assembly." )

assert projectArtifact.exists() : "project-attachments entry is missing."

File attachment = new File( dir, "lib/include-project-attachments-1.0-SNAPSHOT-src.zip" )

System.out.println( "Looking for 'lib/include-project-attachments-1.0-SNAPSHOT-src.zip file in assembly." )

assert attachment.exists() : "project attachment entry is missing."

System.out.println( "Looking for absence of 'lib/junit-jupiter-api.jar' jar entry." )

assert !new File( dir, "lib/junit-jupiter-api-6.0.0.jar" ).exists() : "junit jar should not be present."
