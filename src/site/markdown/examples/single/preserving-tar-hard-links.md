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

# Preserving TAR Hard Links

Hard-link preservation is **disabled by default**. Without configuration, each
selected regular file is stored with its own contents, even when source names
refer to the same inode.

To enable preservation, use the existing `archiverConfig` parameter in an
execution that produces only TAR formats:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-assembly-plugin</artifactId>
  <version>3.8.1-SNAPSHOT</version>
  <configuration>
    <descriptors>
      <descriptor>src/assembly/distribution.xml</descriptor>
    </descriptors>
    <archiverConfig>
      <preserveHardLinks>true</preserveHardLinks>
    </archiverConfig>
  </configuration>
  <executions>
    <execution>
      <id>distribution</id>
      <phase>package</phase>
      <goals>
        <goal>single</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

For example, `src/assembly/distribution.xml` can contain:

```xml
<assembly xmlns="http://maven.apache.org/ASSEMBLY/2.2.0">
  <id>distribution</id>
  <formats>
    <format>tar</format>
    <format>tar.gz</format>
  </formats>
  <fileSets>
    <fileSet>
      <directory>src/distribution</directory>
      <outputDirectory>payload</outputDirectory>
    </fileSet>
  </fileSets>
</assembly>
```

The option applies to plain TAR and all supported compressed TAR formats:
`tar.gz`/`tgz`, `tar.bz2`/`tbz2`, `tar.xz`/`txz`, `tar.snappy`, and `tar.zst`.
Set `preserveHardLinks` to `false`, or omit it, to store each file independently.
For an assembly that also produces ZIP, JAR, or other formats, use a separate
execution for TAR: the reflective option belongs to the TAR archiver.

## What is preserved

When selected source names are hard links to the same regular file and have
compatible output metadata, the first eligible entry stores the contents.
Subsequent aliases contain zero-length hard-link headers referring to that earlier
entry. GNU tar and bsdtar extract these entries as files sharing an inode.
Targets use the final archive names, including the assembly base directory,
`outputDirectory`, and `destName` mappings.

Preservation requires a known identity for untransformed resource contents.
Unavailable filesystem file keys, custom content suppliers, filtering, line-ending
conversion, and incompatible output metadata cause entries to be stored with
independent payloads. Filtering remains independent even if the resulting bytes
happen to match. Unrelated files with identical bytes are not combined. Symbolic
links retain their usual representation. `tarLongFileMode=truncate` disables
hard-link preservation because truncated target names may be ambiguous.

Hard-link headers refer to paths. As with GNU tar and bsdtar, a later write through
a directory symlink can replace the contents at a target path before an alias is
extracted. Avoid overlapping output paths when the original contents must be
retained for every alias. See the
[Plexus Archiver hard-link documentation](https://codehaus-plexus.github.io/plexus-archiver/hard-links.html)
for writer and extraction semantics.

## Development dependencies and Java requirement

This development version uses `plexus-archiver:5.0.0-SNAPSHOT` and
`plexus-io:3.7.1-SNAPSHOT`, containing
[Plexus Archiver PR #493](https://github.com/codehaus-plexus/plexus-archiver/pull/493)
and [Plexus IO PR #191](https://github.com/codehaus-plexus/plexus-io/pull/191).
Until releases containing both changes are available, install the companion IO
branch first with `mvn install`, then the Archiver branch, before building Assembly.
Both dependency versions must be replaced with released versions before an
Assembly release; an ordinary build cannot fetch these locally built PR snapshots
from Maven Central.

Plexus Archiver 5 requires Java 17. Consequently, this Assembly development version
requires Maven to run on Java 17 or newer, including when preservation is disabled.
Maven 3.9.6 or newer is also required: older Maven versions bundle a Sisu injector
that cannot discover these Java 17 components. Adopting this dependency therefore
raises the requirements from Assembly 3.8.0's Java 8 and Maven 3.6.3 baseline.

The Archiver 5 upgrade also removes deprecated methods from the `Archiver`
interface. Existing custom handlers that call those methods must be migrated and
recompiled. See
[Migrating custom handlers to Plexus Archiver 5](./using-container-descriptor-handlers.html#migrating-custom-handlers-to-plexus-archiver-5)
for the supported replacement APIs.
