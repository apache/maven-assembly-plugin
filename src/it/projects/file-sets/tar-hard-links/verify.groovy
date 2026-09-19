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

import java.nio.file.Files
import java.util.concurrent.TimeUnit

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory

/**
 * Opens a TAR payload, decompressing the selected Assembly format when necessary.
 * @param archive the generated assembly
 * @param compression the Commons Compress name, or null for plain TAR
 * @return the payload stream owned by the caller
 */
InputStream openTar(File archive, String compression) {
    def input = new BufferedInputStream(new FileInputStream(archive))
    try {
        return compression == null ? input : new CompressorStreamFactory().createCompressorInputStream(compression, input)
    } catch (Throwable failure) {
        input.close()
        throw failure
    }
}

/**
 * Runs an extractor with bounded execution time and preserves its diagnostics on failure.
 * @param command the executable and arguments
 * @param log the output file
 * @return the command's combined standard output and error
 */
String runCommand(List<String> command, File log) {
    def process = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log).start()
    if (!process.waitFor(30, TimeUnit.SECONDS)) {
        process.destroyForcibly()
        throw new AssertionError("Timed out: $command")
    }
    assert process.exitValue() == 0 : "$command failed: ${log.text}"
    return log.text
}

def formats = ['tar': null, 'tar.gz': 'gz', 'tar.bz2': 'bzip2', 'tar.xz': 'xz',
               'tar.snappy': 'snappy-framed', 'tar.zst': 'zstd']
def contents = [
    'plain/data.txt': 'plain payload\n',
    'plain/alias.txt': 'plain payload\n',
    'plain/copy.txt': 'plain payload\n',
    'mapped/renamed-data.txt': 'mapped payload\n',
    'mapped/renamed-alias.txt': 'mapped payload\n',
    'transformed/original.txt': 'value=${hardlinks.value}\r\n',
    'transformed/filtered.txt': 'value=filtered value\r\n',
    'transformed/unix.txt': 'value=${hardlinks.value}\n',
    'filtered-fileset/data.txt': 'value=filtered value\r\n',
    'filtered-fileset/alias.txt': 'value=filtered value\r\n'
].collectEntries { name, value -> ["distribution/$name".toString(), value] }
def groups = [
    ['distribution/plain/data.txt', 'distribution/plain/alias.txt'],
    ['distribution/mapped/renamed-data.txt', 'distribution/mapped/renamed-alias.txt']
]

// Header/content verification always runs. External utilities are an additional check when installed.
def extractors = ['tar': 'GNU tar', 'bsdtar': 'bsdtar'].findAll { tool, banner ->
    try {
        def version = runCommand([tool, '--version'], new File(basedir, "target/$tool-version.log"))
        if (!version.contains(banner)) {
            println "Skipping $banner extraction: $tool is a different implementation"
            return false
        }
        return true
    } catch (IOException unavailable) {
        println "Skipping $banner extraction: executable is unavailable"
        return false
    }
}.keySet()

['default', 'disabled', 'enabled'].each { mode ->
    formats.each { format, compression ->
        def archive = new File(basedir, "target/$mode-dist.$format")
        assert archive.isFile() : "Missing $archive"
        def entries = [:]
        new TarArchiveInputStream(openTar(archive, compression)).withCloseable { tar ->
            def entry
            while ((entry = tar.nextTarEntry) != null) {
                if (entry.isDirectory()) {
                    continue
                }
                assert !entries.containsKey(entry.name) : "Duplicate member ${entry.name} in $archive"
                assert contents.containsKey(entry.name) : "Unexpected member ${entry.name} in $archive"
                if (entry.isLink()) {
                    // Links must be empty and point backward to a full payload using its final mapped name.
                    assert mode == 'enabled'
                    assert entry.size == 0
                    assert entries.containsKey(entry.linkName) : "Forward/missing target ${entry.linkName} in $archive"
                    assert !entries[entry.linkName].link
                    assert groups.any { it.contains(entry.name) && it.contains(entry.linkName) }
                    assert entries[entry.linkName].contents == contents[entry.name]
                    entries[entry.name] = [link: true, contents: entries[entry.linkName].contents]
                } else {
                    assert entry.isFile() && !entry.isSymbolicLink()
                    def bytes = tar.readAllBytes()
                    assert bytes.length == entry.size
                    assert new String(bytes, 'UTF-8') == contents[entry.name] : "Wrong bytes for ${entry.name} in $archive"
                    entries[entry.name] = [link: false, contents: new String(bytes, 'UTF-8')]
                }
            }
        }
        assert entries.keySet() == contents.keySet()
        assert entries.values().count { it.link } == (mode == 'enabled' ? 2 : 0)
        groups.each { group ->
            assert group.count { !entries[it].link } == (mode == 'enabled' ? 1 : 2)
        }

        // Supply decompressed TAR to both tools so even Snappy works without optional CLI decompressors.
        // The Commons Compress checks above independently verify every compressed assembly itself.
        def payload = new File(basedir, "target/$mode-$format-payload.tar")
        openTar(archive, compression).withCloseable { input ->
            payload.withOutputStream { output -> input.transferTo(output) }
        }
        extractors.each { tool ->
            def destination = new File(basedir, "target/extracted/$mode-$format-$tool")
            assert destination.mkdirs()
            runCommand([tool, '-xf', payload.absolutePath, '-C', destination.absolutePath],
                       new File(basedir, "target/$mode-$format-${tool}.log"))
            contents.each { name, expected ->
                def file = new File(destination, name).toPath()
                assert Files.isRegularFile(file) && !Files.isSymbolicLink(file)
                assert Files.readString(file) == expected : "Wrong extracted bytes for $name with $tool"
            }
            // Compare every pair, checking both preserved aliases and all required independent inodes.
            def names = contents.keySet().toList()
            names.eachWithIndex { first, index ->
                names.drop(index + 1).each { second ->
                    def linked = mode == 'enabled' && groups.any { it.contains(first) && it.contains(second) }
                    assert Files.isSameFile(new File(destination, first).toPath(),
                                            new File(destination, second).toPath()) == linked :
                            "Unexpected inode relationship: $first / $second in $archive with $tool"
                }
            }
        }
        assert payload.delete()
        println "Verified $archive.name: ${entries.size()} members, ${entries.values().count { it.link }} hard links; extractors=$extractors"
    }
}
return true
