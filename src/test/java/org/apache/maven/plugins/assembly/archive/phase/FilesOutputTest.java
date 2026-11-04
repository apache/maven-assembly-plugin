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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugins.assembly.testutils.AssemblyRunner;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import static org.apache.maven.plugins.assembly.testutils.AssemblyRunner.descriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs {@code files} descriptors through the real archivers and checks what lands in the archive. These cases
 * used to be maven-invoker-plugin ITs ({@code basic-features}, {@code file-modes}, {@code files},
 * {@code file-sets/precedence-check}, {@code filtering-feature}, {@code bugs/massembly-743}).
 */
class FilesOutputTest {

    @TempDir
    private File projectDir;

    private static String file(String... children) {
        StringBuilder sb = new StringBuilder("<files><file>");
        for (String c : children) {
            sb.append(c);
        }
        return sb.append("</file></files>").toString();
    }

    private static List<String> lines(File f, java.nio.charset.Charset charset) throws Exception {
        return Files.readAllLines(f.toPath(), charset);
    }

    @Test
    void emptyOutputDirectoryCopiesFileIntoArchiveRoot() throws Exception {
        AssemblyRunner runner = new AssemblyRunner(projectDir);
        runner.write("TODO.txt", "todo");

        File out = runner.build(
                descriptor(
                        "bug-example", false, file("<source>TODO.txt</source>", "<outputDirectory></outputDirectory>")),
                "dir");

        assertTrue(new File(out, "TODO.txt").isFile());
    }

    @Test
    void sameNamedFilesFromDifferentDirectoriesKeepTheirOwnContent() throws Exception {
        AssemblyRunner runner = new AssemblyRunner(projectDir).withFiltering();
        runner.write("a.properties", "testProp=boo\n");
        runner.write("src/config/a/file.txt", "file A\n${testProp}\n");
        runner.write("src/config/b/file.txt", "file B\n${testProp}\n");
        runner.configSource().setFilters(Collections.singletonList(new File(projectDir, "a.properties").getPath()));

        File out = runner.build(
                descriptor(
                        "massembly-148-example",
                        false,
                        "<files>",
                        "<file><source>src/config/a/file.txt</source><outputDirectory>a</outputDirectory>"
                                + "<filtered>true</filtered></file>",
                        "<file><source>src/config/b/file.txt</source><outputDirectory>b</outputDirectory>"
                                + "<destName>file.txt</destName><filtered>true</filtered></file>",
                        "</files>"),
                "dir");

        assertEquals(
                java.util.Arrays.asList("file A", "boo"), lines(new File(out, "a/file.txt"), StandardCharsets.UTF_8));
        assertEquals(
                java.util.Arrays.asList("file B", "boo"), lines(new File(out, "b/file.txt"), StandardCharsets.UTF_8));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void fileModeMakesFileReadOnly() throws Exception {
        AssemblyRunner runner = new AssemblyRunner(projectDir);
        runner.write("src/main/java/test/App.java", "class App {}");

        File out = runner.build(
                descriptor(
                        "src",
                        false,
                        file("<source>src/main/java/test/App.java</source>", "<fileMode>0400</fileMode>")),
                "dir");

        File app = new File(out, "App.java");
        assertTrue(app.isFile());
        assertEquals(
                Collections.singleton(PosixFilePermission.OWNER_READ), Files.getPosixFilePermissions(app.toPath()));
    }

    @Test
    void firstFileWinsWhenTwoFilesShareADestName() throws Exception {
        AssemblyRunner runner = new AssemblyRunner(projectDir);
        runner.write("emptyfile.txt", "");
        runner.write("src.xml", "<assembly/>");

        File out = runner.build(
                descriptor(
                        "src",
                        false,
                        "<files>",
                        "<file><source>emptyfile.txt</source><destName>fud.txt</destName></file>",
                        "<file><source>src.xml</source><destName>fud.txt</destName></file>",
                        "</files>"),
                "dir");

        assertEquals(0L, new File(out, "fud.txt").length());
    }

    @Test
    void destNameWithSubdirectoryIsPlacedBelowBaseDirectory() throws Exception {
        AssemblyRunner runner = new AssemblyRunner(projectDir);
        runner.project().getProperties().setProperty("mine.includeBaseDirectory", "true");
        runner.write("structure/README.txt", "BLA BLI\n");

        File out = runner.build(
                "<assembly><id>dist</id><formats><format>dir</format></formats>"
                        + "<includeBaseDirectory>${mine.includeBaseDirectory}</includeBaseDirectory>"
                        + file("<source>structure/README.txt</source>", "<destName>folder/README.txt</destName>")
                        + "</assembly>",
                "dir");

        assertEquals(
                Collections.singletonList("BLA BLI"),
                lines(new File(out, "it-1.0-SNAPSHOT/folder/README.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void filteredFileReceivesSystemProperty() throws Exception {
        AssemblyRunner runner = new AssemblyRunner(projectDir).withFiltering();
        runner.write("src/config/a/filtered.txt", "OS Version: ${os.version}");

        File out = runner.build(
                descriptor(
                        "bin",
                        false,
                        file(
                                "<source>src/config/a/filtered.txt</source>",
                                "<outputDirectory>a</outputDirectory>",
                                "<filtered>true</filtered>")),
                "dir");

        List<String> lines = lines(new File(out, "a/filtered.txt"), StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertEquals("OS Version: " + System.getProperty("os.version"), lines.get(0));
    }

    @Test
    void componentDescriptorPathIsInterpolatedBeforeResolution() throws Exception {
        AssemblyRunner runner = new AssemblyRunner(projectDir);
        runner.write("pom.xml", "<project/>");
        runner.write("component.xml", "<component><files><file><source>pom.xml</source></file></files></component>");

        // MASSEMBLY-486: the expression has to be resolved before the descriptor is looked up
        File out = runner.build(
                "<assembly><id>filtered-assembly</id><formats><format>dir</format></formats>"
                        + "<includeBaseDirectory>false</includeBaseDirectory>"
                        + "<componentDescriptors><componentDescriptor>${project.basedir}/component.xml"
                        + "</componentDescriptor></componentDescriptors></assembly>",
                "dir");

        assertTrue(new File(out, "pom.xml").isFile());
    }

    @Test
    void zstdTarFormatsProduceArchives() throws Exception {
        // zstd-jni needs a native library, which is not available on every platform
        Assumptions.assumeTrue(zstdNativeLibraryLoads(), "zstd native library not available");
        AssemblyRunner runner = new AssemblyRunner(projectDir);
        runner.write("TODO.txt", "todo");
        String xml = "<assembly><id>format-test</id><formats><format>tar.zst</format><format>tzst</format></formats>"
                + "<includeBaseDirectory>false</includeBaseDirectory>"
                + file("<source>TODO.txt</source>", "<outputDirectory></outputDirectory>")
                + "</assembly>";

        File tarZst = runner.build(xml, "tar.zst");
        File tzst = runner.build(xml, "tzst");

        assertTrue(tarZst.isFile());
        assertTrue(tarZst.getName().endsWith(".tar.zst"));
        assertTrue(tzst.isFile());
        assertTrue(tzst.getName().endsWith(".tzst"));
    }

    private static boolean zstdNativeLibraryLoads() {
        try {
            com.github.luben.zstd.util.Native.load();
            return true;
        } catch (UnsatisfiedLinkError | NoClassDefFoundError e) {
            return false;
        }
    }
}
