package org.apache.maven.plugins.assembly.archive.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArtifactLogger {
    private final Logger logger;

    public ArtifactLogger(Class<?> clazz) {
        logger = (Logger) LoggerFactory.getLogger(clazz);
    }

    public Logger getLogger() {
        return logger;
    }
}