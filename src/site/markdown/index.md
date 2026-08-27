---
title: Introduction
author: 
  - John Casey
  - Edwin Punzalan
date: 2013-07-22
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

# Apache Maven Assembly Plugin
## Introduction

The Assembly Plugin for Maven combines project output into a single distributable archive. The archive also contains dependencies, modules, site documentation, and other files.

Your project can build distribution "assemblies" by using one of the [prefabricated assembly descriptors](./descriptor-refs.html). These descriptors handle common operations. For example, they package a project's artifact along with generated documentation into a [single zip archive](./descriptor-refs.html#bin). Your project can also provide its own [descriptor](./assembly.html). This gives you more control over how dependencies, modules, file-sets, and individual files are packaged.

The plugin can create distributions in these formats:

- zip
- tar
- tar.gz (or tgz)
- tar.bz2 (or tbz2)
- tar.snappy
- tar.xz (or txz)
- tar.zst (or tzst)
- jar
- dir
- war
- Any other format that the ArchiveManager supports

If your project must package artifacts in an uber-jar, the assembly plugin provides only basic support. For more control, use the [Maven Shade Plugin](/plugins/maven-shade-plugin/).

To use the Assembly Plugin in Maven, you must:

- Choose or write the assembly descriptor.
- Configure the Assembly Plugin in your project's `pom.xml`.
- Run `mvn assembly:single` on your project.

To write a custom assembly, refer to the [Assembly Descriptor Format](./assembly.html) reference.

## What is an Assembly?

An "assembly" is a group of files, directories, and dependencies that are assembled into an archive format and distributed. For example, consider a Maven project that defines a single JAR artifact. The artifact contains both a console application and a Swing application. This project could define two "assemblies". One assembly bundles the console application with its supporting scripts and dependencies. The other assembly bundles the Swing application with a different set of dependencies.

The Assembly Plugin provides a descriptor format. This format allows you to define an arbitrary assembly of files and directories from a project. For example, if your Maven project contains the directory `src/main/bin`, you can instruct the plugin to copy this directory to the `bin` directory of an assembly. You can also change the file permissions to UNIX mode 755. The parameters for this configuration are supplied through the [assembly descriptor](./assembly.html).

## Goals

The main goal in the assembly plugin is the [single](./single-mojo.html) goal. This goal creates all assemblies.

For more information about the goals in the Assembly Plugin, see [the plugin documentation page](./plugin-info.html).

## Assembly and Component Descriptor Schemas (XSD)

- [https://maven.apache.org/xsd/assembly-2.2.0.xsd](/xsd/assembly-2.2.0.xsd), [https://maven.apache.org/xsd/assembly-component-2.2.0.xsd](/xsd/assembly-component-2.2.0.xsd) (for version 3.6.0 and higher)
- [https://maven.apache.org/xsd/assembly-2.1.1.xsd](/xsd/assembly-2.1.1.xsd), [https://maven.apache.org/xsd/assembly-component-2.1.1.xsd](/xsd/assembly-component-2.1.1.xsd) (for version 3.4.0 and higher)
- [https://maven.apache.org/xsd/assembly-2.1.0.xsd](/xsd/assembly-2.1.0.xsd), [https://maven.apache.org/xsd/assembly-component-2.1.0.xsd](/xsd/assembly-component-2.1.0.xsd) (for version 3.2 and higher)
- [https://maven.apache.org/xsd/assembly-2.0.0.xsd](/xsd/assembly-2.0.0.xsd), [https://maven.apache.org/xsd/assembly-component-2.0.0.xsd](/xsd/assembly-component-2.0.0.xsd) (for version 3.0 and higher)
- [https://maven.apache.org/xsd/assembly-1.1.3.xsd](/xsd/assembly-1.1.3.xsd), [https://maven.apache.org/xsd/component-1.1.3.xsd](/xsd/component-1.1.3.xsd) (for version 2.5.4 and higher)
- [https://maven.apache.org/xsd/assembly-1.1.2.xsd](/xsd/assembly-1.1.2.xsd), [https://maven.apache.org/xsd/component-1.1.2.xsd](/xsd/component-1.1.2.xsd) (for version 2.2 and higher)
- [https://maven.apache.org/xsd/assembly-1.1.1.xsd](/xsd/assembly-1.1.1.xsd), [https://maven.apache.org/xsd/component-1.1.1.xsd](/xsd/component-1.1.1.xsd) (for version 2.2-beta-4 - 2.2-beta-5)
- [https://maven.apache.org/xsd/assembly-1.1.0.xsd](/xsd/assembly-1.1.0.xsd), [https://maven.apache.org/xsd/component-1.1.0.xsd](/xsd/component-1.1.0.xsd) (for version 2.2-beta-1 - 2.2-beta-3)
- [https://maven.apache.org/xsd/assembly-1.0.0.xsd](/xsd/assembly-1.0.0.xsd), [https://maven.apache.org/xsd/component-1.0.0.xsd](/xsd/component-1.0.0.xsd) (for version 2.1 and lower)

## Usage

General instructions on how to use the Assembly Plugin are on the [usage page](./usage.html). Some more specific use cases are described in the examples given below.

If you have questions about the plugin's usage, look at the [FAQ](./faq.html) and contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and may already contain the answer to your question. You can browse the [mail archive](./mailing-lists.html).

If the plugin is missing a feature or has a defect, file a feature request or bug report in our [issue tracker](./issue-management.html). When you create a new issue, provide a clear description of your concern. For bug fixes, developers must be able to reproduce your problem. Attach debug logs, POMs, or small demo projects to the issue. Contributors can check out the project from our [source repository](./scm.html) and find information in the [guide to helping with Maven](/guides/development/guide-helping.html).

## Examples

For a deeper understanding of the Assembly Plugin, see the [examples](./examples/index.html).
