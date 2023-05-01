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

File f = new File( basedir, "target/symbolic-links-1-src/a-symbolic-link" );

if ( !f.exists() ) {
    println( "Copied link " + f + " is missing." )
    return false
}

if ( !java.nio.file.Files.isSymbolicLink( f.toPath() ) ) {
	println( "Not a symbolic link: " + f );
	return false;
}

def linkTarget = java.nio.file.Files.readSymbolicLink( f.toPath() ).toString()
assert linkTarget == 'test.txt' : "Wrong link target for $f"

return true