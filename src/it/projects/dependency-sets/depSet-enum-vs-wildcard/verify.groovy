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

File f1 = new File( basedir, "enum/target/enum-1-bin.jar" )
File f2 = new File( basedir, "wildcard/target/wildcard-1-bin.jar" )

JarFile jf1 = new JarFile( f1 )
JarFile jf2 = new JarFile( f2 )

for( Enumeration e = jf1.entries(); e.hasMoreElements(); )
{
    JarEntry entry1 = (JarEntry) e.nextElement()
    JarEntry entry2 = (JarEntry) jf2.getEntry( entry1.getName() )
    
    assert entry2 != null : "Missing entry: ${entry1.name} in ${f2}"
}

for( Enumeration e = jf2.entries(); e.hasMoreElements(); )
{
    JarEntry entry2 = (JarEntry) e.nextElement()
    JarEntry entry1 = (JarEntry) jf2.getEntry( entry2.getName() )
    
    assert entry1 != null : "Missing entry: ${entry2.name} in ${f1}"
    
    if ( !entry1.isDirectory() )
    {
        assert !entry2.isDirectory() : "One file is directory, the other a file! Entry: ${entry2.name}"

        ByteArrayOutputStream b1 = new ByteArrayOutputStream()

        InputStream is = jf1.getInputStream( entry1 )
        byte[] buf = new byte[1024]
        int read = -1

        while( ( read = is.read( buf ) ) > -1 )
        {
            b1.write( buf, 0, read )
        }

        ByteArrayOutputStream b2 = new ByteArrayOutputStream()

        is = jf2.getInputStream( entry2 )
        read = -1

        while( ( read = is.read( buf ) ) > -1 )
        {
            b2.write( buf, 0, read )
        }

        assert  Arrays.equals( b1.toByteArray(), b2.toByteArray() ) : "Entries are not equal! Entry name: ${entry2.name}"
    }
}