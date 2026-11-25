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

JarFile file = new JarFile( new File( basedir, "child2/target/child2-1.0-SNAPSHOT-bin.jar" ) );
JarEntry handlerEntry = file.getEntry( "META-INF/spring.handlers" );
assert handlerEntry != null : "Entry META-INF/spring.handlers is missing";

List handlers = file.getInputStream( handlerEntry ).withCloseable { it.readLines( "UTF-8" ) };

assert handlers.any { it.endsWith( "AopNamespaceHandler1" ) } : "Cannot find entry 'AopNamespaceHandler1' in: " + handlerEntry.getName();

assert handlers.any { it.endsWith( "ContextNamespaceHandler1" ) } : "Cannot find entry 'ContextNamespaceHandler1' in: " + handlerEntry.getName();

JarEntry schemaEntry = file.getEntry( "META-INF/spring.schemas" );
assert schemaEntry != null : "Entry META-INF/spring.schemas is missing";

List schemas = file.getInputStream( schemaEntry ).withCloseable { it.readLines( "UTF-8" ) };

assert schemas.any { it.endsWith( "spring-aop-3.0.xsd" ) } : "Cannot find entry 'spring-aop-3.0.xsd' in: " + schemaEntry.getName();

assert schemas.any { it.endsWith( "spring-aop-4.0.xsd" ) } : "Cannot find entry 'spring-aop-4.0.xsd' in: " + schemaEntry.getName();
