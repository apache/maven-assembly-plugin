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
package org.apache.maven.plugins.assembly.io;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Benjamin Bentmann
 */
public class PrefixedClasspathLocatorStrategyTest {

    private MessageHolder mh = new DefaultMessageHolder();

    @Test
    public void testResolvePrefixWithLeadingSlashAndWithTrailingSlash() {
        LocatorStrategy ls = new PrefixedClasspathLocatorStrategy("/assemblies/");
        Location location = ls.resolve("empty.xml", mh);

        assertNotNull(location);
        assertEquals(0, mh.size());
    }

    @Test
    public void testResolvePrefixWithLeadingSlashAndWithoutTrailingSlash() {
        LocatorStrategy ls = new PrefixedClasspathLocatorStrategy("/assemblies");
        Location location = ls.resolve("empty.xml", mh);

        assertNotNull(location);
        assertEquals(0, mh.size());
    }

    @Test
    public void testResolvePrefixWithoutLeadingSlashAndWithTrailingSlash() {
        LocatorStrategy ls = new PrefixedClasspathLocatorStrategy("assemblies/");
        Location location = ls.resolve("empty.xml", mh);

        assertNotNull(location);
        assertEquals(0, mh.size());
    }

    @Test
    public void testResolvePrefixWithoutLeadingSlashAndWithoutTrailingSlash() {
        LocatorStrategy ls = new PrefixedClasspathLocatorStrategy("assemblies");
        Location location = ls.resolve("empty.xml", mh);

        assertNotNull(location);
        assertEquals(0, mh.size());
    }
}
