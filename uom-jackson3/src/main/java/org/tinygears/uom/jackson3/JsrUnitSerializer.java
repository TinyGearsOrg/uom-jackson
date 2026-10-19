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
import org.tinygears.uom.Untyped;
import org.tinygears.uom.jsr.Jsr;
import org.tinygears.uom.jsr.UomUnit;
import org.tinygears.uom.jsr.quantity.UntypedDoubleQuantity;

/** Writes a unit of uom-jsr as the symbol of the native unit, which is what the native serializer writes. */
final class JsrUnitSerializer extends ValueSerializer<javax.measure.Unit<?>> {
    private final UnitSerializer nativeSerializer;

    JsrUnitSerializer(UnitSerializer nativeSerializer) {
        this.nativeSerializer = nativeSerializer;
    }

    @Override
    public void serialize(javax.measure.Unit<?> unit, JsonGenerator gen, SerializationContext provider)
            {
        org.tinygears.uom.Unit<?> converted;
        try {
            // a quantity of one carries a unit of any dimension from uom-jsr to the native API
            converted = Jsr.fromJsr(UntypedDoubleQuantity.of(1, (UomUnit<?>) unit), Untyped.class).unit();
        } catch (RuntimeException e) {
            throw DatabindException.from(
                    gen,
                    "cannot write a unit of " + unit.getClass().getName() + ": only units of uom-jsr can be written",
                    e);
        }
        nativeSerializer.serialize(converted, gen, provider);
    }
}
