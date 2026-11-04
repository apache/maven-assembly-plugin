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

import java.util.zip.ZipFile

// archiverConfig <appxml> is the application.xml of the ear
File ear = new File( basedir, "target/massembly-345-1-bin.ear" )
assert ear.exists() : "Missing " + ear

new ZipFile( ear ).withCloseable { z ->
    def entry = z.getEntry( "META-INF/application.xml" )
    assert entry != null : "META-INF/application.xml is missing from the ear"
    String actual = z.getInputStream( entry ).getText( "UTF-8" )
    String configured = new File( basedir, "src/main/resources/application.xml" ).getText( "UTF-8" )
    assert actual.replace( "\r\n", "\n" ) == configured.replace( "\r\n", "\n" ) : "application.xml differs from the configured appxml"
}
