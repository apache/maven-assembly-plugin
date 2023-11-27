package org.apache.maven.plugins.assembly.format;
import org.apache.maven.plugins.assembly.AssemblerConfigurationSource;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Properties;

public class FilterableReader {
    public static Reader createReaderFilter(
            Reader source,
            String escapeString,
            List<String> delimiters,
            AssemblerConfigurationSource configSource,
            boolean isPropertiesFile,
            Properties additionalProperties)
            throws IOException{
    return source;}
}