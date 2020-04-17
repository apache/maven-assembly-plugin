
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

def buildDirectory = new File(basedir, 'target')

def dirFile = new File(buildDirectory, 'massembly934-1.0')

assert new File( dirFile, "ab.txt").readLines() == ['file A','boo','file B','boo']

def zipFile = new java.util.zip.ZipFile(new File(buildDirectory, 'massembly934-1.0.zip'))

assert zipFile.getInputStream(zipFile.entries().next()).readLines() == ['file A','boo','file B','boo']