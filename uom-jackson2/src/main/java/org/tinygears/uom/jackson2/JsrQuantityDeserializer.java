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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.jsr.Jsr;
import org.tinygears.uom.jsr.quantity.UomQuantity;

/** Reads a quantity as the native deserializer does, and converts it to the quantity of uom-jsr that was asked for. */
final class JsrQuantityDeserializer extends StdDeserializer<javax.measure.Quantity<?>> {
    private static final long serialVersionUID = 1L;

    private final transient QuantityDeserializer nativeDeserializer;
    private final Class<?> jsrType;

    /**
     * @param declared the type of the field, which is what the deserializer handles
     * @param nativeDeserializer the deserializer of the native type with the same name
     * @param jsrType the typed interface of uom-jsr to convert to, or null for the type of the unit
     */
    JsrQuantityDeserializer(Class<?> declared, QuantityDeserializer nativeDeserializer, Class<?> jsrType) {
        super(declared);
        this.nativeDeserializer = nativeDeserializer;
        this.jsrType = jsrType;
    }

    @Override
    public javax.measure.Quantity<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        Quantity<?> read = nativeDeserializer.deserialize(p, ctxt);
        return jsrType == null ? Jsr.toJsr(read) : toJsr(read, jsrType);
    }

    @SuppressWarnings("unchecked") // the type is a typed interface of uom-jsr, so it is a type of itself
    private static <T extends UomQuantity<?, T>> javax.measure.Quantity<?> toJsr(Quantity<?> quantity, Class<?> type) {
        return Jsr.toJsr(quantity, (Class<T>) type);
    }
}
