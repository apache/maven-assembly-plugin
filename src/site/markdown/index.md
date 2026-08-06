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

The Assembly Plugin for Maven enables developers to combine project output into a single distributable archive that also contains dependencies, modules, site documentation, and other files.

Your project can easily build distribution "assemblies" using one of the [prefabricated assembly descriptors](./descriptor-refs.html). These descriptors handle many common operations, such as packaging a project's artifact along with generated documentation into a [single zip archive](./descriptor-refs.html#bin). Alternatively, your project can provide its own [descriptor](./assembly.html) and assume a much higher level of control over how dependencies, modules, file-sets, and individual files are packaged in the assembly.

Currently it can create distributions in the following formats:

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
- and any other format that the ArchiveManager has been configured for

If your project wants to package your artifact in an uber-jar, the assembly plugin provides only basic support. For more control, use the [Maven Shade Plugin](/plugins/maven-shade-plugin/).

To use the Assembly Plugin in Maven, you simply need to:

- choose or write the assembly descriptor to use,
- configure the Assembly Plugin in your project's `pom.xml`, and
- run "mvn assembly:single" on your project.

To write your own custom assembly, you will need to refer to the [Assembly Descriptor Format](./assembly.html) reference.

## What is an Assembly?

An "assembly" is a group of files, directories, and dependencies that are assembled into an archive format and distributed. For example, assume that a Maven project defines a single JAR artifact that contains both a console application and a Swing application. Such a project could define two "assemblies" that bundle the application with a different set of supporting scripts and dependency sets. One assembly would be the assembly for the console application, and the other assembly could be a Swing application bundled with a slightly different set of dependencies.

The Assembly Plugin provides a descriptor format which allows you to define an arbitrary assembly of files and directories from a project. For example, if your Maven project contains the directory "src/main/bin", you can instruct the Assembly Plugin to copy the contents of this directory to the "bin" directory of an assembly and to change the permissions of the files in the "bin" directory to UNIX mode 755. The parameters for configuring this behavior are supplied to the Assembly Plugin by way of the [assembly descriptor](./assembly.html).

## Goals

The main goal in the assembly plugin is the [single](./single-mojo.html) goal. It is used to create all assemblies.

For more information about the goals that are available in the Assembly Plugin, see [the plugin documentation page](./plugin-info.html).

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

General instructions on how to use the Assembly Plugin can be found on the [usage page](./usage.html). Some more specific use cases are described in the examples given below.

In case you still have questions regarding the plugin's usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel the plugin is missing a feature or has a defect, you can file a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](/guides/development/guide-helping.html).

## Examples

To provide you with better understanding on some usages of the Assembly Plugin, you can take a look into the examples which can be found [here](./examples/index.html).
