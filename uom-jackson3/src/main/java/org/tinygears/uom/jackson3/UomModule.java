/*
 * Copyright (c) 2026 TinyGears.org
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.tinygears.uom.jackson3;

import java.io.IOException;
import java.io.InputStream;
import java.math.MathContext;
import java.util.Objects;
import java.util.Properties;

import tools.jackson.core.Version;
import tools.jackson.core.util.VersionUtil;
import tools.jackson.databind.JacksonModule;
import org.tinygears.uom.UnitFormat;

/**
 * The Jackson 3 module for uom quantities and units, in the format of docs/JSON.md. uom and uom-jsr are optional
 * dependencies: the module serves the API that is on the class path, so a user declares {@code uom}, {@code uom-jsr} or
 * both. Nothing of uom is loaded before that is known, so this class works without it.
 */
public final class UomModule extends JacksonModule {
    /** How a quantity is written. */
    public enum Form {
        /** The text of uom's {@code QuantityFormat}, such as {@code "5.0 km"}; a difference is still an object. */
        TEXT,
        /** An object such as {@code {"value": 5.0, "unit": "km"}}. */
        OBJECT
    }

    private static final String NATIVE_API = "org.tinygears.uom.Quantity";
    private static final String JSR_API = "org.tinygears.uom.jsr.Jsr";

    private static final Version VERSION = version(UomModule.class.getResourceAsStream("version.properties"));
    private static final boolean NATIVE = present(NATIVE_API);
    private static final boolean JSR = present(JSR_API);

    private final Options options;

    /**
     * Creates the module with the default settings: quantities are written as text, read as {@code double}, and units
     * are those of {@code UnitFormat.standard()}.
     *
     * @throws IllegalStateException if neither uom nor uom-jsr is on the class path
     */
    public UomModule() {
        requireApi();
        this.options = Options.defaults();
    }

    private UomModule(Options options) {
        requireApi();
        this.options = options;
    }

    /**
     * A builder for a module with other settings.
     *
     * @return the builder
     * @throws IllegalStateException if neither uom nor uom-jsr is on the class path
     */
    public static Builder builder() {
        requireApi();
        return new Builder();
    }

    /** The settings of a module. */
    public static final class Builder {
        private Form writeAs = Form.TEXT;
        private boolean valuesAsStrings;
        private MathContext decimals;
        private UnitFormat unitFormat = UnitFormat.standard();

        private Builder() {}

        /**
         * How a quantity is written; the default is {@link Form#TEXT}. A temperature difference is always written as an
         * object, because the text cannot carry the scale.
         *
         * @param form the form
         * @return this builder
         */
        public Builder writeAs(Form form) {
            this.writeAs = Objects.requireNonNull(form, "form");
            return this;
        }

        /**
         * Whether the object form writes the value as a string, for readers that take a JSON number for a
         * {@code double}. The default is a number. The text form is not affected.
         *
         * @param strings true to write a string
         * @return this builder
         */
        public Builder valuesAsStrings(boolean strings) {
            this.valuesAsStrings = strings;
            return this;
        }

        /**
         * Reads quantities as exact decimals that keep every digit, rounded to {@code mc} in arithmetic. The default
         * (null) reads {@code double}. A decimal quantity is always written with its exact value.
         *
         * @param mc the context, or null for {@code double}
         * @return this builder
         */
        public Builder decimals(MathContext mc) {
            this.decimals = mc;
            return this;
        }

        /**
         * The format of unit symbols, to read and write units of user {@code .units} systems.
         *
         * @param format the format
         * @return this builder
         */
        public Builder unitFormat(UnitFormat format) {
            this.unitFormat = Objects.requireNonNull(format, "format");
            return this;
        }

        /**
         * Creates the module.
         *
         * @return the module
         */
        public UomModule build() {
            return new UomModule(new Options(writeAs, valuesAsStrings, decimals, unitFormat));
        }
    }

    boolean supportsNative() {
        return NATIVE;
    }

    boolean supportsJsr() {
        return JSR;
    }

    @Override
    public String getModuleName() {
        return "uom-jackson3";
    }

    @Override
    public Version version() {
        return VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
        if (NATIVE) {
            NativeSupport.install(context, options);
        }
        if (JSR) {
            JsrSupport.install(context, options);
        }
    }

    /**
     * The version in the properties that Maven filled in at build time; unknown if there is none, or if it was not
     * filled in (the file as it is in the sources).
     */
    static Version version(InputStream resource) {
        if (resource == null) {
            return Version.unknownVersion();
        }
        try (resource) {
            Properties properties = new Properties();
            properties.load(resource);
            String version = properties.getProperty("version", "");
            if (version.isEmpty() || version.startsWith("${")) {
                return Version.unknownVersion();
            }
            return VersionUtil.parseVersion(
                    version, properties.getProperty("groupId"), properties.getProperty("artifactId"));
        } catch (IOException e) {
            return Version.unknownVersion();
        }
    }

    private static void requireApi() {
        if (!NATIVE && !JSR) {
            throw new IllegalStateException(
                    "uom-jackson3 needs org.tinygears:uom or org.tinygears:uom-jsr on the class path");
        }
    }

    /** Nothing of uom is loaded here, so this class works without it. */
    private static boolean present(String className) {
        try {
            Class.forName(className, false, UomModule.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
