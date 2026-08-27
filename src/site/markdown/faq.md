---
title: Frequently Asked Questions
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

<a id="top"></a>

# Frequently Asked Questions

1. [If the Assembly Plugin is run during the package phase, do my assemblies get deployed during the deploy phase?](#deploy)
2. [Can I use an artifact created by the assembly plugin as a dependency?](#classifier)
3. [How do I use the Assembly Plugin to package my project's javadoc files?](#javadoc)
4. [Starting with version 2.2 configuration/executions defined in a parent POM are no longer inherited, why?](#inherit)
5. [In previous versions (before 2.2 final), I followed the example in the documentation for sharing assembly descriptors, which recommended using the `<descriptors/>` configuration section to refer to the shared assembly descriptor. As of version 2.2, this no longer works. Why not?](#shared-descriptor-bug)
6. [The Assembly Plugin is saying it cannot find files for the module binaries included by my assembly descriptor. What gives?](#module-binaries)
7. [In previous versions (before 2.2 final), leaving off the assembly id and leaving the classifier unconfigured resulted in the assembly used as the project's main artifact. With the 2.2 release, this configuration results in a validation error. My project depended on the previous behavior! Why has this changed, and how can I make this work in my project?](#required-classifiers)
8. [I have a dependencySet that includes some artifacts with classifiers, and others without classifiers. How can I setup the file mappings to handle both cases appropriately?](#dashClassifier)
9. [Which properties can be used in the outputFileNameMapping parameter?](#outputFileNameMapping)
10. [Tar complains about groupid value too big](#tarFileModes)

<a id="deploy"></a>

### If the Assembly Plugin is run during the package phase, do my assemblies get deployed during the deploy phase?

Yes. The assemblies created by the Assembly Plugin are attached to your project so they are deployed too.

<a id="classifier"></a>

### Can I use an artifact created by the assembly plugin as a dependency?

Yes. You can refer to it using the id of the assembly as the dependency classifier.

<a id="javadoc"></a>

### How do I use the Assembly Plugin to package my project's javadoc files?

The Javadoc Plugin can generate the javadoc files of your projects. The Javadoc Plugin can also package them.

See the [Javadoc Plugin Documentation](https://maven.apache.org/plugins/maven-javadoc-plugin/).

<a id="inherit"></a>

### Starting with version 2.2 configuration/executions defined in a parent POM are no longer inherited, why?

The goals of the Assembly Plugin and their configuration/executions are no longer inherited by default. This change is part of the deprecation of all goals except the `single` goal.

If you need the configuration/executions to be inherited, add a line to the plugin declaration in the parent POM:

```xml
<plugin>
  <artifactId>maven-assembly-plugin</artifactId>
  <inherited>true</inherited>
  ...
```

<a id="shared-descriptor-bug"></a>

### In previous versions (before 2.2 final), I followed the example in the documentation for [sharing assembly descriptors](/examples/sharing-descriptors.html), which recommended using the `<descriptors/>` configuration section to refer to the shared assembly descriptor. As of version 2.2, *this no longer works*. Why not?

The use of `<descriptors/>` was incorrect. It was counter to the design of this configuration parameter. Some code was introduced in version 2.2-beta-2. This code allowed the parameter to reference descriptors on the classpath. In version 2.2, this bug was fixed.

The correct form `<descriptorRefs/>` has always worked and continues to work. The documentation now reflects the correct configuration.

<a id="module-binaries"></a>

### The Assembly Plugin is saying it cannot find files for the module binaries included by my assembly descriptor. What gives?

**If your assembly includes module binaries, those binaries are not available to the assembly plugin except in special cases.** This problem usually occurs when the Assembly Plugin is bound to a phase of the standard build lifecycle in the **parent POM** of a multimodule build. It is a result of the way Maven sorts and executes the build process for a multimodule project layout.

In a multimodule hierarchy, when a child module declares the parent POM in its `<parent/>` section, Maven interprets this to mean that the parent project's build must be completed before the child build can start. This ensures that the parent project is in its final form by the time the child needs access to its POM information. If the Assembly Plugin is included as part of that parent project's build process, it will execute as part of the parent build **before the child build can start**. If the assembly descriptor used in that parent build references module binaries, it expects the child build to be completed **before the assembly is processed**. This leads to a recursive dependency situation. The child build depends on the parent build to complete before it can start. The parent build depends on the presence of child-module artifacts to complete successfully. Since these artifacts are missing, the Assembly Plugin will report missing artifacts and the build will fail.

You can avoid this problem by adding a new child module. This module's sole purpose is to produce your assembly. In the POM for this new project, add dependency definitions for any of the module binaries you referenced. This will ensure the new assembly child is built last. Then, move your assembly descriptor into this new child module. You can change all moduleSet/binaries references to dependencySet references. You can also keep the moduleSets and set the useAllReactorProjects flag to true for each moduleSet.

Any fileSet or file references in this descriptor may need to be adjusted. The files they reference may need to be moved into the new child module alongside the descriptor.

If you must use module-binaries references, create an assembly-child POM as mentioned above. Then, add `<useAllReactorProjects>true</useAllReactorProjects>` to each of your `moduleSet` sections. Bind the assembly in your assembly-child POM to the `package` phase using the `single` goal. When you execute the build from the top-level POM, Maven will generate your assembly in the new child project.

**NOTE:** The useAllReactorProjects flag is only available in version 2.2 and higher.

<a id="required-classifiers"></a>

### In previous versions (before 2.2 final), leaving off the assembly id and leaving the classifier unconfigured resulted in the assembly used as the project's main artifact. With the 2.2 release, this configuration results in a validation error. My project depended on the previous behavior! Why has this changed, and how can I make this work in my project?

The assembly id is used for reporting and calculating descriptor/component merges. They are also required to avoid collisions with the main output of the project's build process. This id must be in place to prevent overriding the project's main artifact inadvertently. It also helps error-reporting make sense to the user. *Leaving off the assembly id was always an error*. However, previous releases contained a bug in the model that allowed empty or missing assembly id's. This bug was fixed in version 2.2.

In some cases, it makes sense to use the assembly output as the main project artifact. This use case requires deliberate configuration so your intention to depart from the normal behavior is clear. To configure the use of the assembly output as the main project artifact, follow these steps:

1. Make sure your assembly uses only **one format**. More than one format could mean the artifact used for the project's main artifact is non-deterministic.
2. Add the configuration `<appendAssemblyId>false</appendAssemblyId>` to your assembly-plugin execution. This prevents the assembly artifact from attachment to the project.

<a id="dashClassifier"></a>

### I have a dependencySet that includes some artifacts with classifiers, and others without classifiers. How can I setup the file mappings to handle both cases appropriately?

The the **${dashClassifier?}** expression is the best way to handle a mixed bag of dependencies with and without classifiers.
This expression was added in version 2.2-beta-2 of the assembly plugin. It determines whether each artifact has a classifier. If it does, it substitutes the artifact's classifier (prepended by a dash) in place of the expression.

For example, suppose you want to include two artifacts. One is commons-logging-1.0.4.jar. The other is yourserver-1.0-client.jar (where 'client' is the classifier). Add the following to your dependencySet:

```xml
<outputFileNameMapping>${artifact.artifactId}-${artifact.version}${dashClassifier?}.${artifact.extension}</outputFileNameMapping>
```

<a id="outputFileNameMapping"></a>

### Which properties can be used in the outputFileNameMapping parameter?

You can use :

- All system or maven properties available in your build with the syntax `${myProperty}`.
- All environment variables with `${env.XXX}` where `XXX` is the environment variable.
- The special `${dashClassifier?}` property (see above).
- All artifacts attributes ([from the Artifact class](https://maven.apache.org/ref/3.0.4/maven-artifact/apidocs/org/apache/maven/artifact/Artifact.html)) like :
    - `${artifact.groupId}` : The artifact groupId.
    - `${artifact.artifactId}` : The artifact artifactId.
    - `${artifact.version}` : The artifact classifier.
    - `${artifact.baseVersion}` : The artifact base version (For a SNAPSHOT it will be always -SNAPSHOT and not its timestamp even if you didn't built it yourself).
    - `${artifact.classifier}` : The artifact classifier.
    - `${artifact.scope}` : The artifact scope.
    - `${artifact.groupIdPath}` : The groupId of the artifact as path
    - `${artifact.file.name}` : The name of the file attached to this artifact
    - ...

    You can use `${module.XXXXX}` when using it for your project modules artifacts.

<a id="tarFileModes"></a>

### Tar complains about groupid value too big

GNU tar has restrictions on max value of UID/GUID in tar files. Use the POSIX tar format instead. The POSIX format supports larger values. Set tarLongFileMode=posix in the assembly:single goal.
