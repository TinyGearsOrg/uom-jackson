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

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.jsr.Jsr;

/** Reads a unit as the native deserializer does, which checks its type, and converts it to a unit of uom-jsr. */
final class JsrUnitDeserializer extends StdDeserializer<javax.measure.Unit<?>> {
    private static final long serialVersionUID = 1L;

    private final transient UnitDeserializer nativeDeserializer;

    JsrUnitDeserializer(UnitDeserializer nativeDeserializer) {
        super(javax.measure.Unit.class);
        this.nativeDeserializer = nativeDeserializer;
    }

    @Override
    public javax.measure.Unit<?> deserialize(JsonParser p, DeserializationContext ctxt) {
        // a quantity of one carries a unit of any dimension from the native API to uom-jsr
        return Jsr.toJsr(Untyped.of(1, nativeDeserializer.deserialize(p, ctxt))).getUnit();
    }
}
