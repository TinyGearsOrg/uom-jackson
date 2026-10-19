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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.tinygears.uom.Unit;

/** Writes a unit as its symbol. */
final class UnitSerializer extends JsonSerializer<Unit<?>> {
    private final Options options;

    UnitSerializer(Options options) {
        this.options = options;
    }

    @Override
    public void serialize(Unit<?> unit, JsonGenerator gen, SerializerProvider provider) throws IOException {
        try {
            gen.writeString(options.unitFormat().format(unit));
        } catch (IllegalArgumentException e) {
            throw JsonMappingException.from(gen, "cannot write a unit: " + e.getMessage(), e);
        }
    }
}
