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
package org.apache.maven.plugins.assembly.archive.phase;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.maven.plugins.assembly.testutils.AssemblyRunner;
import org.apache.maven.plugins.assembly.testutils.ZipEntryModes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.plugins.assembly.testutils.AssemblyRunner.descriptor;
import static org.apache.maven.plugins.assembly.testutils.AssemblyRunner.listing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@code fileSets} descriptors through the real archivers and checks what lands in the archive. These cases
 * used to be maven-invoker-plugin ITs under {@code src/it/projects/file-sets}.
 */
class FileSetsOutputTest {

    @TempDir
    private File projectDir;

    private static String fileSet(String... children) {
        StringBuilder sb = new StringBuilder("<fileSets><fileSet>");
        for (String c : children) {
            sb.append(c);
        }
        return sb.append("</fileSet></fileSets>").toString();
    }

    private AssemblyRunner runner() throws Exception {
        return new AssemblyRunner(projectDir);
    }

    private AssemblyRunner runnerWithSvnDirectory() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("src/main/assembly/src.xml", "<assembly/>");
        // Subversion metadata, which default excludes are meant to keep out of an archive
        File entries = runner.write("src/main/assembly/.svn/entries", "Test, Test");
        assertTrue(entries.isFile());
        return runner;
    }

    @Test
    void useDefaultExcludesTrueDropsSvnDirectory() throws Exception {
        AssemblyRunner runner = runnerWithSvnDirectory();

        File out = runner.build(
                descriptor(
                        "src",
                        false,
                        fileSet("<useDefaultExcludes>true</useDefaultExcludes><directory>src</directory>")),
                "dir");

        assertTrue(new File(out, "src/main/assembly/src.xml").isFile());
        assertFalse(new File(out, "src/main/assembly/.svn").exists());
    }

    @Test
    void defaultExcludesApplyWhenUseDefaultExcludesIsUnspecified() throws Exception {
        AssemblyRunner runner = runnerWithSvnDirectory();

        File out = runner.build(descriptor("src", false, fileSet("<directory>src</directory>")), "dir");

        assertTrue(new File(out, "src/main/assembly/src.xml").isFile());
        assertFalse(new File(out, "src/main/assembly/.svn").exists());
    }

    @Test
    void useDefaultExcludesFalseKeepsSvnDirectory() throws Exception {
        AssemblyRunner runner = runnerWithSvnDirectory();

        File out = runner.build(
                descriptor(
                        "src",
                        false,
                        fileSet("<useDefaultExcludes>false</useDefaultExcludes><directory>src</directory>")),
                "dir");

        assertTrue(new File(out, "src/main/assembly/src.xml").isFile());
        assertTrue(new File(out, "src/main/assembly/.svn").isDirectory());
        assertTrue(new File(out, "src/main/assembly/.svn/entries").isFile());
    }

    @Test
    void explicitExcludeDropsSvnDirectory() throws Exception {
        AssemblyRunner runner = runnerWithSvnDirectory();

        // default excludes are off, so only the explicit pattern can keep .svn out
        File out = runner.build(
                descriptor(
                        "src",
                        false,
                        fileSet(
                                "<useDefaultExcludes>false</useDefaultExcludes>",
                                "<directory>src</directory>",
                                "<excludes><exclude>**/.svn/**</exclude></excludes>")),
                "dir");

        assertTrue(new File(out, "src/main/assembly/src.xml").isFile());
        assertFalse(new File(out, "src/main/assembly/.svn/entries").exists());
    }

    @Test
    void directoryDefaultsToProjectBaseDirectory() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("build.log", "log");
        runner.write(".svn/entries", "Test, Test");
        // matches *.* and is a default exclude
        runner.write("notes.txt~", "x");

        File out =
                runner.build(descriptor("src", false, fileSet("<includes><include>*.*</include></includes>")), "dir");

        assertTrue(new File(out, "build.log").isFile());
        assertFalse(new File(out, ".svn").exists());
        assertFalse(new File(out, "notes.txt~").exists());
    }

    @Test
    void includeBaseDirectoryPrefixesEntriesWithFinalName() throws Exception {
        AssemblyRunner runner = runnerWithSvnDirectory();

        File out = runner.build(
                descriptor(
                        "src",
                        true,
                        fileSet("<directory>src</directory><excludes><exclude>**/.svn/**</exclude></excludes>")),
                "dir");

        assertTrue(new File(out, "it-1.0-SNAPSHOT/src/main/assembly/src.xml").isFile());
        assertFalse(new File(out, "src").exists());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void symbolicLinkIsCopiedAsRelativeSymbolicLink() throws Exception {
        AssemblyRunner runner = runner();
        File resources = new File(projectDir, "src/main/assembly-resources");
        runner.write("src/main/assembly-resources/test.txt", "${project.version}\n${project.artifactId}");
        Files.createSymbolicLink(new File(resources, "a-symbolic-link").toPath(), new File("test.txt").toPath());

        File out = runner.build(
                descriptor(
                        "src",
                        false,
                        fileSet(
                                "<outputDirectory></outputDirectory><directory>src/main/assembly-resources</directory>")),
                "dir");

        File copied = new File(out, "a-symbolic-link");
        assertTrue(Files.isSymbolicLink(copied.toPath()), "Not a symbolic link: " + copied);
        assertEquals("test.txt", Files.readSymbolicLink(copied.toPath()).toString());
    }

    @Test
    void emptyOutputDirectoryPutsJarEntryAtRootWithoutLeadingSlash() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("src/main/assembly/test.txt", "content");

        String xml = "<assembly><id>test</id><formats><format>jar</format></formats>"
                + "<includeBaseDirectory>false</includeBaseDirectory>"
                + "<fileSets><fileSet><directory>src/main/assembly</directory>"
                + "<outputDirectory></outputDirectory>"
                + "<includes><include>test.txt</include></includes></fileSet></fileSets></assembly>";
        File jar = runner.build(xml, "jar");

        try (JarFile jf = new JarFile(jar)) {
            JarEntry leadingSlash = jf.getJarEntry("/test.txt");
            assertNull(leadingSlash, "entry /test.txt must not exist");
            assertNotNull(jf.getJarEntry("test.txt"), "entry test.txt must exist");
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void symbolicLinksAreStoredAsUnixSymlinkEntriesInZip() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("symlinks/src/targetDir/targetFile.txt", "a regular File payload");
        runner.write("symlinks/src/aRegularDir/aRegularFile.txt", "a regular File payload");
        runner.write("symlinks/dirOnTheOutside/FileInDirOnTheOutside.txt", "a file in dir on the outside");
        runner.write("symlinks/onTheOutside.txt", "A file on the outside");
        runner.write("symlinks/src/fileR.txt", "FileR payload");
        File src = new File(projectDir, "symlinks/src");
        Files.createSymbolicLink(new File(src, "symDir").toPath(), new File("targetDir").toPath());
        Files.createSymbolicLink(
                new File(src, "symLinkToDirOnTheOutside").toPath(), new File("../dirOnTheOutside").toPath());
        Files.createSymbolicLink(
                new File(src, "symLinkToFileOnTheOutside").toPath(), new File("../onTheOutside.txt").toPath());
        Files.createSymbolicLink(new File(src, "symR").toPath(), new File("fileR.txt").toPath());

        String xml = "<assembly><id>deployable</id><formats><format>zip</format></formats>"
                + "<fileSets><fileSet><includes><include>symlinks/src/**</include></includes>"
                + "<directoryMode>700</directoryMode><fileMode>600</fileMode></fileSet></fileSets></assembly>";
        File zip = runner.build(xml, "zip");

        assertTrue(ZipEntryModes.isSymlink(ZipEntryModes.modeOf(zip, "it-1.0-SNAPSHOT/symlinks/src/symDir")));
        assertTrue(ZipEntryModes.isSymlink(ZipEntryModes.modeOf(zip, "it-1.0-SNAPSHOT/symlinks/src/symR")));
        assertFalse(ZipEntryModes.isSymlink(ZipEntryModes.modeOf(zip, "it-1.0-SNAPSHOT/symlinks/src/fileR.txt")));
    }

    @Test
    void outputDirectoryIsInterpolatedWithProjectVersion() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("src/main/etc/hello.txt", "hello");

        File out = runner.build(
                descriptor(
                        "dist",
                        false,
                        fileSet(
                                "<directory>src/main/etc</directory>",
                                "<outputDirectory>massembly-232-${version}/etc</outputDirectory>")),
                "dir");

        assertTrue(new File(out, "massembly-232-1.0-SNAPSHOT/etc/hello.txt").isFile());
    }

    @Test
    void outputDirectoryAndIncludesUseProjectProperties() throws Exception {
        AssemblyRunner runner = runner();
        runner.project().getProperties().setProperty("fileLocation", "file/path");
        runner.project().getProperties().setProperty("fileLocation2", "file2/path");
        runner.project().getProperties().setProperty("fileLocation3", "file3/path");
        runner.write("src/main/files/some.file", "x");

        File out = runner.build(
                descriptor(
                        "myassembly",
                        false,
                        "<fileSets>",
                        "<fileSet><directory>src/main/files</directory><outputDirectory>${fileLocation}</outputDirectory></fileSet>",
                        "<fileSet><directory>src/main/files</directory>"
                                + "<outputDirectory>${project.properties.fileLocation2}</outputDirectory></fileSet>",
                        "<fileSet><directory>src/main/files</directory>"
                                + "<outputDirectory>${project.properties.fileLocation3}</outputDirectory></fileSet>",
                        "</fileSets>"),
                "dir");

        assertTrue(new File(out, "file/path/some.file").isFile());
        assertTrue(new File(out, "file2/path/some.file").isFile());
        assertTrue(new File(out, "file3/path/some.file").isFile());
    }

    @Test
    void projectPrefixedAndBareExpressionsInIncludesAreInterpolated() throws Exception {
        AssemblyRunner runner = runner();
        // artifactId of the test project is "it"
        runner.write("src/main/files/it1.file", "1");
        runner.write("src/main/files/it2.file", "2");
        runner.write("src/main/files/it3.file", "3");
        runner.write("src/main/files/other.file", "4");

        File out = runner.build(
                descriptor(
                        "myassembly",
                        false,
                        "<fileSets>",
                        "<fileSet><directory>src/main/files</directory><outputDirectory>files</outputDirectory>"
                                + "<includes><include>${artifactId}1.file</include></includes></fileSet>",
                        "<fileSet><directory>src/main/files</directory><outputDirectory>files</outputDirectory>"
                                + "<includes><include>${project.artifactId}2.file</include></includes></fileSet>",
                        "<fileSet><directory>src/main/files</directory><outputDirectory>files</outputDirectory>"
                                + "<includes><include>${project.artifactId}3.file</include></includes></fileSet>",
                        "</fileSets>"),
                "dir");

        Set<String> expected =
                new TreeSet<>(Arrays.asList("files/", "files/it1.file", "files/it2.file", "files/it3.file"));
        assertEquals(expected, listing(out));
    }

    @Test
    void projectBaseDirectoryWithSpaceAndSpecialCharacters() throws Exception {
        File weird = new File(projectDir, "a b&c");
        assertTrue(weird.mkdirs());
        AssemblyRunner runner = new AssemblyRunner(weird);
        runner.write("src/main/assembly/src.xml", "<assembly/>");
        runner.write("target/ignored.txt", "x");

        File out = runner.build(
                descriptor(
                        "src",
                        false,
                        fileSet(
                                "<directory>${project.basedir}</directory>",
                                "<excludes><exclude>target/**</exclude></excludes>")),
                "dir");

        assertTrue(new File(out, "src/main/assembly/src.xml").isFile());
        assertFalse(new File(out, "target").exists());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void fileSetFileModeMakesOutputReadOnly() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("src/main/java/test/App.java", "class App {}");

        File out = runner.build(
                descriptor("src", false, fileSet("<directory>src/main/java</directory><fileMode>0400</fileMode>")),
                "dir");

        File app = new File(out, "src/main/java/test/App.java");
        assertTrue(app.isFile());
        assertEquals(
                Collections.singleton(PosixFilePermission.OWNER_READ), Files.getPosixFilePermissions(app.toPath()));
        assertEquals("class App {}", new String(Files.readAllBytes(app.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    void configuredFileAggregatorReplacesMatchingFilesWithOneOutputFile() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("src/config/a/file.txt", "file A\n");
        runner.write("src/config/b/file.txt", "file B\n");

        File out = runner.build(
                descriptor(
                        "bin",
                        false,
                        fileSet("<directory>src/config</directory><outputDirectory></outputDirectory>"),
                        "<containerDescriptorHandlers><containerDescriptorHandler>"
                                + "<handlerName>file-aggregator</handlerName>"
                                + "<configuration><filePattern>.*/file.txt</filePattern>"
                                + "<outputPath>file.txt</outputPath></configuration>"
                                + "</containerDescriptorHandler></containerDescriptorHandlers>"),
                "dir");

        // the matching sources are replaced by one output file; its content is not checked here, see
        // aggregatedFileContainsTheContentOfAllMatchingFiles
        assertTrue(new File(out, "file.txt").isFile());
        assertFalse(new File(out, "b/file.txt").exists());
        assertFalse(new File(out, "a/file.txt").exists());
    }

    @Test
    @Disabled("https://github.com/apache/maven-assembly-plugin/issues/1024 (MASSEMBLY-815): the aggregate is written"
            + " before the lazily selected fileSet entries are added, so file.txt holds only the header")
    void aggregatedFileContainsTheContentOfAllMatchingFiles() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("src/config/a/file.txt", "file A\n");
        runner.write("src/config/b/file.txt", "file B\n");

        File out = runner.build(
                descriptor(
                        "bin",
                        false,
                        fileSet("<directory>src/config</directory><outputDirectory></outputDirectory>"),
                        "<containerDescriptorHandlers><containerDescriptorHandler>"
                                + "<handlerName>file-aggregator</handlerName>"
                                + "<configuration><filePattern>.*/file.txt</filePattern>"
                                + "<outputPath>file.txt</outputPath></configuration>"
                                + "</containerDescriptorHandler></containerDescriptorHandlers>"),
                "dir");

        String aggregate = new String(Files.readAllBytes(new File(out, "file.txt").toPath()), StandardCharsets.UTF_8);
        assertTrue(aggregate.contains("file A"), aggregate);
        assertTrue(aggregate.contains("file B"), aggregate);
    }

    @Test
    void platformSpecificDirectoriesStillCopyTheFiles() throws Exception {
        AssemblyRunner runner = runner();
        runner.write("adir/emptyfile.txt", "");

        File out = runner.build(
                "<assembly><id>src</id><formats><format>dir</format></formats>"
                        + "<includeBaseDirectory>false</includeBaseDirectory><fileSets>"
                        + "<fileSet><directory>adir</directory></fileSet>"
                        + "<fileSet><directory>/adir</directory></fileSet>"
                        + "<fileSet><directory>c:\\adir</directory></fileSet>"
                        + "</fileSets></assembly>",
                "dir");

        assertTrue(new File(out, "adir/emptyfile.txt").isFile());
    }

    @Test
    void propertiesFileWithLatin1KeyIsFilteredWithNonAsciiProjectProperty() throws Exception {
        // MASSEMBLY-736: the project encoding is UTF-8, the properties file is ISO-8859-1
        AssemblyRunner runner = new AssemblyRunner(projectDir).withFiltering();
        runner.project().getProperties().setProperty("anExpr", "\u00fcber");
        File input = runner.write("src/main/config/file-1.properties", "");
        Files.write(input.toPath(), "b\u00f8s=${anExpr}\n\n".getBytes(StandardCharsets.ISO_8859_1));

        File out = runner.build(
                descriptor(
                        "assembly",
                        false,
                        fileSet(
                                "<directory>${basedir}/src/main/config</directory>",
                                "<outputDirectory></outputDirectory>",
                                "<lineEnding>unix</lineEnding>",
                                "<filtered>true</filtered>")),
                "dir");

        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(
                Files.newInputStream(new File(out, "file-1.properties").toPath()), StandardCharsets.ISO_8859_1)) {
            properties.load(reader);
        }
        assertEquals("\u00fcber", properties.getProperty("b\u00f8s"));
    }

    @Test
    void buildFiltersAreAppliedEscapedExpressionsKeptAndLatin1PreservedInPropertiesFiles() throws Exception {
        AssemblyRunner runner = new AssemblyRunner(projectDir).withFiltering();
        runner.write(
                "src/main/filters/filter-build.properties",
                "filter.build=Filter defined in the projects' build section.\n");
        runner.configSource().setEscapeString("\\");
        runner.configSource()
                .setFilters(Collections.singletonList(
                        new File(projectDir, "src/main/filters/filter-build.properties").getPath()));
        // properties files are read as ISO-8859-1 whatever the project encoding is
        File file1 = runner.write("src/main/config/file-1.properties", "");
        Files.write(
                file1.toPath(),
                ("test.build.filter.props.1=${filter.build}\n"
                                + "escapedString=\\${project.artifactId}\n"
                                + "non.ascii.characters=some characters \u00e5\u00e4\u00f6\n")
                        .getBytes(StandardCharsets.ISO_8859_1));
        runner.write("src/main/config/file-2.properties", "test.build.filter.props.2=${filter.build}\n");

        File out = runner.build(
                descriptor(
                        "assembly",
                        false,
                        fileSet(
                                "<directory>${basedir}/src/main/config</directory>",
                                "<outputDirectory></outputDirectory>",
                                "<lineEnding>unix</lineEnding>",
                                "<filtered>true</filtered>")),
                "dir");

        assertEquals(
                Arrays.asList(
                        "test.build.filter.props.1=Filter defined in the projects' build section.",
                        "escapedString=${project.artifactId}",
                        "non.ascii.characters=some characters \u00e5\u00e4\u00f6"),
                Files.readAllLines(new File(out, "file-1.properties").toPath(), StandardCharsets.ISO_8859_1));
        assertEquals(
                Collections.singletonList("test.build.filter.props.2=Filter defined in the projects' build section."),
                Files.readAllLines(new File(out, "file-2.properties").toPath(), StandardCharsets.ISO_8859_1));
    }
}
