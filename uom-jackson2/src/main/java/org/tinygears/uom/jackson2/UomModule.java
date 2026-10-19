/*
 * Copyright (c) 2026 TinyGears.org
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package org.tinygears.uom.jackson2;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;

/**
 * The Jackson 2 module for uom quantities and units. uom and uom-jsr are optional dependencies: the module serves the
 * API that is on the class path, so a user declares {@code uom}, {@code uom-jsr} or both.
 */
public final class UomModule extends Module {
    private static final String NATIVE_API = "org.tinygears.uom.Quantity";
    private static final String JSR_API = "org.tinygears.uom.jsr.Jsr";

    private final boolean nativeApi = present(NATIVE_API);
    private final boolean jsrApi = present(JSR_API);

    /**
     * Creates the module.
     *
     * @throws IllegalStateException if neither uom nor uom-jsr is on the class path
     */
    public UomModule() {
        if (!nativeApi && !jsrApi) {
            throw new IllegalStateException(
                    "uom-jackson2 needs org.tinygears:uom or org.tinygears:uom-jsr on the class path");
        }
    }

    boolean supportsNative() {
        return nativeApi;
    }

    boolean supportsJsr() {
        return jsrApi;
    }

    @Override
    public String getModuleName() {
        return "uom-jackson2";
    }

    @Override
    public Version version() {
        return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {
        // the serializers and deserializers of the types that are present are added here
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
