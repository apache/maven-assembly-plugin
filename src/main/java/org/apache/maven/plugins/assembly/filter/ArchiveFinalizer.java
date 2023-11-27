package org.apache.maven.plugins.assembly.filter;

import org.apache.commons.compress.archivers.examples.Archiver;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileInfo;

import java.io.IOException;

public interface ArchiveFinalizer {
    void finalizeArchiveCreation(Archiver archiver);
    void finalizeArchiveExtraction(UnArchiver unArchiver);
}
 interface FileSelector {
    boolean isSelected(FileInfo fileInfo) throws IOException;
}
