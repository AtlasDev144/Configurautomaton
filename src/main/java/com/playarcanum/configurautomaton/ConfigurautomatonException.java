package com.playarcanum.configurautomaton;

import lombok.NonNull;

public abstract class ConfigurautomatonException extends Throwable{

    protected ConfigurautomatonException(final String message) { super(message); }

    public static final class AnnotationMissingException extends ConfigurautomatonException {
        public AnnotationMissingException(final Class<?> clazz) {
            super(clazz.getSimpleName() + " seems to be missing the @Configuration annotation.");
        }
    }

    public static final class PathException extends ConfigurautomatonException {
        public PathException(final @NonNull String pathName) {
            super("A path couldn't be found with a corresponding name of: " + pathName + ". Did you register it?");
        }
    }

    public static final class ConfigurationException extends ConfigurautomatonException {
        public ConfigurationException(final @NonNull String fileName) {
            super("A configuration object relating to the file: " + fileName + " wasn't found. Did you register it?");
        }
    }

    public static final class LoadException extends ConfigurautomatonException {
        public LoadException(final @NonNull String fileName) {
            super("File couldn't be loaded: " + fileName);
        }
    }

    public static class FormatterException extends ConfigurautomatonException {
        public FormatterException(final @NonNull String file) {
            super("A format for the file: " + file + " couldn't be found. Looked for extensions: .toml, .yaml and .json.");
        }
    }
}
