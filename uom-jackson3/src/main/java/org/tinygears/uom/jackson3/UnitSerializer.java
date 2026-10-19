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


import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import org.tinygears.uom.Unit;

/** Writes a unit as its symbol. */
final class UnitSerializer extends ValueSerializer<Unit<?>> {
    private final Options options;

    UnitSerializer(Options options) {
        this.options = options;
    }

    @Override
    public void serialize(Unit<?> unit, JsonGenerator gen, SerializationContext provider) {
        try {
            gen.writeString(options.unitFormat().format(unit));
        } catch (IllegalArgumentException e) {
            throw DatabindException.from(gen, "cannot write a unit: " + e.getMessage(), e);
        }
    }
}
