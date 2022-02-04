package com.playarcanum.configurautomaton.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that this class is a configuration object that corresponds to the given file.
 * The {@code file} value shouldn't be the entire path; it should be the file name + extension.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configuration {
    /**
     * The file this configuration object corresponds to
     */
    String file();
}
