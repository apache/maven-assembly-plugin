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

import java.nio.charset.StandardCharsets
import java.nio.file.Files

String expected = "1\r\nchild"
for ( String name : [ "test.txt", "mac2win.txt" ] )
{
    File file = new File( basedir, "child/target/child-1-src/" + name )
    assert file.exists() : "Filtered file from file-set: " + file + " is missing."

    String actual = new String( Files.readAllBytes( file.toPath() ), StandardCharsets.UTF_8 )
    assert actual == expected :
            "Contents of " + name + ": '" + actual + "' should contain exactly one windows newline: '\\r\\n'."
}

return true;
