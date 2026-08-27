---
title: Predefined Assembly Descriptors
author: 
  - Johnny R. Ruiz III <jruiz@exist.com>
  - Edwin Punzalan
  - John Casey
date: 2011-02-07
---

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Pre-defined Descriptor Files

There are four predefined descriptor formats available for reuse. They are packaged within the Assembly Plugin. Their descriptorIds are:

<!-- MACRO{toc|fromDepth=2|toDepth=2} -->

## bin

Use `bin` as the `descriptorRef` of your assembly-plugin configuration. This creates a binary distribution archive of your project. The built-in descriptor produces an assembly with the classifier `bin` in three archive formats: tar.gz, tar.bz2, and zip.

The assembled archive contains the binary JAR produced by running `mvn package`. It also contains any README, LICENSE, and NOTICE files available in the project root directory.

Below is the `bin` descriptor format:

<!-- MACRO{snippet|id=bin|file=target/classes/assemblies/bin.xml} -->

## jar-with-dependencies

Use `jar-with-dependencies` as the `descriptorRef` of your assembly-plugin configuration. This creates a JAR which contains the binary output of your project along with its unpacked dependencies. The built-in descriptor produces an assembly with the classifier `jar-with-dependencies` using the JAR archive format.

The `jar-with-dependencies` descriptor provides only basic support for uber-jars. For more control, use the [Maven Shade Plugin](https://maven.apache.org/plugins/maven-shade-plugin/).

Below is the `jar-with-dependencies` descriptor format:

<!-- MACRO{snippet|id=jar-with-dependencies|file=target/classes/assemblies/jar-with-dependencies.xml} -->

## src

Use `src` as the `descriptorRef` in your assembly-plugin configuration. This creates source archives for your project. The archive will contain the contents of your project's `/src` directory structure for reference by your users. The `src` descriptorId produces an assembly archive with the classifier `src`. It supports three formats: tar.gz, tar.bz2, and zip.

Below is the `src` descriptor format:

<!-- MACRO{snippet|id=src|file=target/classes/assemblies/src.xml} -->

## project

Using the `project` `<descriptorRef>` in your Assembly Plugin configuration produces an assembly. The assembly contains your entire project, minus any build output that lands in the `/target` directory. The resulting assembly allows your users to build your project using Maven, Ant, or whatever build system you configured in your project's normal SCM working directory. It produces assemblies with the classifier `project` in three formats: tar.gz, tar.bz2, and zip.

The following is the assembly descriptor for the `project` descriptorRef:

<!-- MACRO{snippet|id=project|file=target/classes/assemblies/project.xml} -->