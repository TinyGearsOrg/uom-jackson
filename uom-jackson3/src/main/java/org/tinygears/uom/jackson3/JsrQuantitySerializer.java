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
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.jsr.Jsr;

/** Writes a quantity of uom-jsr as the native quantity of the same value, so both APIs give the same JSON. */
final class JsrQuantitySerializer extends ValueSerializer<javax.measure.Quantity<?>> {
    private final QuantitySerializer nativeSerializer;

    JsrQuantitySerializer(QuantitySerializer nativeSerializer) {
        this.nativeSerializer = nativeSerializer;
    }

    @Override
    public void serialize(javax.measure.Quantity<?> quantity, JsonGenerator gen, SerializationContext provider)
            {
        Quantity<?> converted;
        try {
            converted = Jsr.fromJsr(quantity, Untyped.class);
        } catch (RuntimeException e) {
            throw DatabindException.from(
                    gen,
                    "cannot write a quantity of " + quantity.getClass().getName()
                            + ": only quantities of uom-jsr can be written",
                    e);
        }
        nativeSerializer.serialize(converted, gen, provider);
    }
}
