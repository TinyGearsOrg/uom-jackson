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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Scale;
import org.tinygears.uom.Unit;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.jsr.Jsr;
import org.tinygears.uom.quantity.QuantityTypes;

/** Every quantity type of uom-jsr, as {@code double} and decimal, as a point and a difference, reads back equal. */
class JsrRoundTripTest {
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
        javax.measure.Quantity<?> original = Jsr.toJsr(as(base, type));
        Class<?> jsrType = Class.forName("org.tinygears.uom.jsr.quantity." + type.getSimpleName());
        UomModule module = decimal ? UomModule.builder().decimals(MathContext.DECIMAL64).build() : new UomModule();
        ObjectMapper mapper = new ObjectMapper().registerModule(module);
        String json = mapper.writeValueAsString(original);
        Object back = mapper.readValue(json, jsrType);
        assertEquals(original, back, json);
    }

    @SuppressWarnings("unchecked") // the type is a quantity type, so it is a type of itself
    private static <T extends Quantity<T>> Quantity<?> as(Quantity<?> quantity, Class<?> type) {
        return quantity.as((Class<T>) type);
    }
}
