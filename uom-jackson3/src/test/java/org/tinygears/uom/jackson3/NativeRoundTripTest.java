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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Scale;
import org.tinygears.uom.Unit;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.quantity.Length;
import org.tinygears.uom.quantity.QuantityTypes;
import org.tinygears.uom.systems.SI;

/** Every quantity type, as {@code double} and decimal, as a point and a difference, reads back equal. */
class NativeRoundTripTest {
    @Test
    void thereAreThirtyThreeTypes() {
        assertEquals(33, QuantityTypes.types().size());
    }

    @TestFactory
    Stream<DynamicTest> everyTypeRoundTrips() {
        List<DynamicTest> tests = new ArrayList<>();
        for (Class<?> type : QuantityTypes.types()) {
            for (boolean decimal : new boolean[] {false, true}) {
                for (Scale scale : Scale.values()) {
                    String name = type.getSimpleName() + (decimal ? " decimal " : " double ") + scale;
                    tests.add(DynamicTest.dynamicTest(name, () -> roundTrip(type, decimal, scale)));
                }
            }
        }
        return tests.stream();
    }

    private static void roundTrip(Class<?> type, boolean decimal, Scale scale) throws Exception {
        Unit<?> unit = QuantityTypes.systemUnit(type);
        Untyped base = decimal
                ? Untyped.of(new BigDecimal("1.25"), unit, scale, MathContext.DECIMAL64)
                : Untyped.of(1.25, unit, scale);
        Quantity<?> original = as(base, type);
        UomModule module = decimal ? UomModule.builder().decimals(MathContext.DECIMAL64).build() : new UomModule();
        ObjectMapper mapper = Mappers.with(module);
        String json = mapper.writeValueAsString(original);
        Object back = mapper.readValue(json, type);
        assertEquals(original, back, json);
    }

    @Test
    void aTypedQuantityKeepsItsUnit() throws Exception {
        ObjectMapper mapper = Mappers.with(new UomModule());
        Length original = Length.of(5, SI.KILOMETRE);
        Length back = mapper.readValue(mapper.writeValueAsString(original), Length.class);
        assertEquals(original, back);
        assertEquals("km", back.unit().symbol());
    }

    @SuppressWarnings("unchecked") // the type is a quantity type, so it is a type of itself
    private static <T extends Quantity<T>> Quantity<?> as(Quantity<?> quantity, Class<?> type) {
        return quantity.as((Class<T>) type);
    }
}
