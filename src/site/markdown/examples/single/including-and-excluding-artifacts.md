---
title: Including and Excluding Artifacts
author: 
  - Barrie Treloar
date: 2006-07-31
---

<!--
Copyright 2006 The Apache Software Foundation.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Including and Excluding Artifacts

The include/exclude format is based on the dependency conflict id. The form is: `groupId:artifactId:type:classifier`. A shortened form `groupId:artifactId` can also be used.

The check for inclusion/exclusion is done based on either the dependency conflict id or the shortened form as a `String.equals()` match. It must be an identical match for the artifact to be included or excluded. There is no support for regular expressions.

This example excludes the log4j-1.2-api and commons-lang3 jar files from the assembly. This is useful when you are building a super distribution assembly. The assembly contains sub distributions (other already assembled zips or tars). Your pom depends on those distributions. Because the distributions transitively depend on the project's dependencies, the assembly also includes the jar files. These files are already in the assemblies and do not need to be duplicated.

The pom might include something like:

```xml
    <dependencies>
        <dependency>
            <groupId>YOUR GROUP</groupId>
            <artifactId>YOUR ARTIFACT</artifactId>
            <version>YOUR VERSION</version>
            <classifier>bin</classifier>
            <type>zip</type>
        </dependency>
```

Then, in the assembly, exclude all the jar dependencies pulled in from the binary assembly. In this example the commons-lang3 and log4j-1.2-api jars are included unnecessarily (as they are in the bin.zip file already).

```xml
  <dependencySets>
    <dependencySet>
      ....
      <excludes>
        <exclude>org.apache.commons:commons-lang3</exclude>
        <exclude>org.apache.logging.log4j:log4j-1.2-api</exclude>
      </excludes>
    </dependencySet>
    ....
  </dependencySets>
```

## What about your Project's Artifacts?

[MASSEMBLY-197](https://issues.apache.org/jira/browse/MASSEMBLY-197) added `useProjectArtifact` and `useProjectAttachments` to the `dependencySet` configuration.

See [Assembly Descriptor Format](../../assembly.html#class_dependencySet) for the default values and how to configure them.
