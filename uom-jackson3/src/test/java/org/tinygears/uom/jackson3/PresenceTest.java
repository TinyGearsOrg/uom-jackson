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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
        ObjectMapper mapper = JsonMapper.builder().addModule(new UomModule()).build();
        assertTrue(mapper.registeredModules().stream().anyMatch(m -> m instanceof UomModule));
    }

    @Test
    void theServiceLoaderFindsTheModule() {
        assertTrue(ServiceLoader.load(JacksonModule.class).stream().anyMatch(p -> p.type() == UomModule.class));
    }

    @Test
    void aMapperDiscoversTheModule() {
        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
        assertTrue(mapper.registeredModules().stream().anyMatch(m -> m instanceof UomModule));
    }
}
