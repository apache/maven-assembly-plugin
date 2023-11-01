// Test cases written by Kaushik Dhola for AssemblyFileUtils.java

package org.apache.maven.plugins.assembly.utils;

import org.junit.Assert;
import org.junit.Test;
import java.io.File;


public class TestAssemblyFileUtils {

    @Test
    public void testMakePathRelativeTo() {

        // Test when base directory is null
        String path = "/path/to/file";
        String result = AssemblyFileUtils.makePathRelativeTo(path, null);
        Assert.assertEquals("basedir is null", path, result);

        // Test when the given path is null
        String nullPath = null;
        result = AssemblyFileUtils.makePathRelativeTo(nullPath, new File("//base/dir"));
        Assert.assertNull("path is null", result);

        // Test when the given path is relative
        String basePath = "/base/dir/";
        String relativePath = "/file.txt";
        result = AssemblyFileUtils.makePathRelativeTo(basePath + relativePath, new File(basePath));
        Assert.assertEquals("path is already relative", relativePath, result);

        // Test when path is absolute but starts with the base directory
        String absolutePath = "/base/dir/other/directory/file.txt";
        result = AssemblyFileUtils.makePathRelativeTo(absolutePath, new File(basePath));
        Assert.assertEquals("path is absolute but starts with the base directory", "other/directory/file.txt", result);

        // Test when path absolute but not within the base directory
        String differentBasePath = "/different/base/dir/";
        String absolutePathNotInBase = "/base/dir/other/file.txt";
        result = AssemblyFileUtils.makePathRelativeTo(absolutePathNotInBase, new File(differentBasePath));
        Assert.assertEquals("path is absolute but not within the base directory", absolutePathNotInBase, result);

        // Test when path is empty
        String emptyPath = "";
        result = AssemblyFileUtils.makePathRelativeTo(emptyPath, new File("//base/dir"));
        Assert.assertEquals("path is empty", "", result);

        // Test when path is root
        String rootPath = "/";
        result = AssemblyFileUtils.makePathRelativeTo(rootPath, new File("/"));
        Assert.assertEquals("path is root", ".", result);
    }

}

