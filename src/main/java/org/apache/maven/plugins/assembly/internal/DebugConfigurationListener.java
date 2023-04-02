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
package org.apache.maven.plugins.assembly.internal;

import java.lang.reflect.Array;

import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.slf4j.Logger;

/**
 * Copy of deprecated {@link org.apache.maven.plugin.DebugConfigurationListener} updated to Slf4j.
 */
public class DebugConfigurationListener implements ConfigurationListener {
    private final Logger logger;

    public DebugConfigurationListener(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void notifyFieldChangeUsingSetter(String fieldName, Object value, Object target) {
        if (logger.isDebugEnabled()) {
            logger.debug("  (s) " + fieldName + " = " + toString(value));
        }
    }

    @Override
    public void notifyFieldChangeUsingReflection(String fieldName, Object value, Object target) {
        if (logger.isDebugEnabled()) {
            logger.debug("  (f) " + fieldName + " = " + toString(value));
        }
    }

    /**
     * Creates a human-friendly string representation of the specified object.
     *
     * @param obj The object to create a string representation for, may be <code>null</code>.
     * @return The string representation, never <code>null</code>.
     */
    private String toString(Object obj) {
        String str;
        if (obj != null && obj.getClass().isArray()) {
            int n = Array.getLength(obj);
            StringBuilder buf = new StringBuilder(256);
            buf.append('[');
            for (int i = 0; i < n; i++) {
                if (i > 0) {
                    buf.append(", ");
                }
                buf.append(Array.get(obj, i));
            }
            buf.append(']');
            str = buf.toString();
        } else {
            str = String.valueOf(obj);
        }
        return str;
    }
}
