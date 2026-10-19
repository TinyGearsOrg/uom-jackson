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

import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/** Jackson 3 mappers are immutable, so a test builds one with its module. */
final class Mappers {
    private Mappers() {}

    static ObjectMapper with(JacksonModule module) {
        return JsonMapper.builder().addModule(module).build();
    }
}
