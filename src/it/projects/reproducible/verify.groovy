
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

//import java.util.zip.*
import org.apache.commons.compress.archivers.zip.*

File deployDir = new File( basedir, 'target/repo/org/apache/maven/its/reproducible/1.0' )

assert deployDir.exists()

ZipFile zip = new ZipFile( new File( deployDir, "reproducible-1.0-src.zip" ) )
StringBuilder sb = new StringBuilder()
for( ZipArchiveEntry entry : zip.getEntries() )
{
    sb.append( String.format("%o %s\n", entry.getUnixMode(), entry.getName() ) )
}

content = new File( basedir, "zip-content.txt" ).text.replace( "\r\n", "\n" )
effective = sb.toString()
assert content == effective

assert new File( deployDir, 'reproducible-1.0-src.zip.sha1' ).text == '50116502c6107740c2a35ef296b5abda08c5dec7'
assert new File( deployDir, 'reproducible-1.0-src.tar.sha1' ).text == '3efc10ec9c3099ba061e58d5b2a935ba643da237'
assert new File( deployDir, 'reproducible-1.0-src.jar.sha1' ).text == 'cc7e3a984179f63d6b37bc86c61e9cc461c62288'
