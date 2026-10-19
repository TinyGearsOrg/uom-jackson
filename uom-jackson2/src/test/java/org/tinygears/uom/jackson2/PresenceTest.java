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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Both uom and uom-jsr are on the class path. */
class PresenceTest {
    @Test
    void servesBothApisWhenBothArePresent() {
        UomModule module = new UomModule();
        assertTrue(module.supportsNative());
        assertTrue(module.supportsJsr());
    }

    @Test
    void registersWithAMapper() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new UomModule());
        assertTrue(mapper.getRegisteredModuleIds().contains(new UomModule().getTypeId()));
    }

    @Test
    void theServiceLoaderFindsTheModule() {
        assertTrue(ServiceLoader.load(Module.class).stream().anyMatch(p -> p.type() == UomModule.class));
    }

    @Test
    void aMapperDiscoversTheModule() {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        assertTrue(mapper.getRegisteredModuleIds().contains(new UomModule().getTypeId()));
    }
}
