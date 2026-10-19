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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tinygears.uom.Scale;
import org.tinygears.uom.SystemOfUnits;
import org.tinygears.uom.Unit;
import org.tinygears.uom.UnitFormat;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.quantity.Length;
import org.tinygears.uom.quantity.Temperature;
import org.tinygears.uom.systems.SI;

/** The settings of the builder. */
class NativeOptionsTest {
    private static ObjectMapper mapper(UomModule module) {
        return Mappers.with(module);
    }

    @Test
    void writesObjectsOnRequest() throws Exception {
        ObjectMapper mapper = mapper(UomModule.builder().writeAs(UomModule.Form.OBJECT).build());
        assertEquals("{\"value\":5.0,\"unit\":\"km\"}", mapper.writeValueAsString(Length.of(5, SI.KILOMETRE)));
        assertEquals(
                "{\"value\":1.50,\"unit\":\"km\"}",
                mapper.writeValueAsString(Length.of(new BigDecimal("1.50"), SI.KILOMETRE)));
        assertEquals(
                "{\"value\":5.0,\"unit\":\"°C\",\"scale\":\"RELATIVE\"}",
                mapper.writeValueAsString(Temperature.of(5, SI.DEGREE_CELSIUS, Scale.RELATIVE)));
    }

    @Test
    void writesValuesAsStrings() throws Exception {
        ObjectMapper mapper =
                mapper(UomModule.builder().writeAs(UomModule.Form.OBJECT).valuesAsStrings(true).build());
        assertEquals("{\"value\":\"5.0\",\"unit\":\"km\"}", mapper.writeValueAsString(Length.of(5, SI.KILOMETRE)));
        assertEquals(
                "{\"value\":\"1.50\",\"unit\":\"km\"}",
                mapper.writeValueAsString(Length.of(new BigDecimal("1.50"), SI.KILOMETRE)));
        assertEquals(Length.of(5, SI.KILOMETRE), mapper.readValue("{\"value\":\"5.0\",\"unit\":\"km\"}", Length.class));
    }

    @Test
    void theTextFormIsNotAffectedByValuesAsStrings() throws Exception {
        ObjectMapper mapper = mapper(UomModule.builder().valuesAsStrings(true).build());
        assertEquals("\"5.0 km\"", mapper.writeValueAsString(Length.of(5, SI.KILOMETRE)));
    }

    @Test
    void readsDecimalsOnRequest() throws Exception {
        ObjectMapper mapper = mapper(UomModule.builder().decimals(MathContext.DECIMAL64).build());
        Length read = mapper.readValue("\"0.10 m\"", Length.class);
        assertEquals(new BigDecimal("0.10"), read.value());
        Length sum = read.add(mapper.readValue("{\"value\":0.20,\"unit\":\"m\"}", Length.class));
        assertEquals(new BigDecimal("0.30"), sum.value());
    }

    @Test
    void readsDoublesByDefault() throws Exception {
        Length read = mapper(new UomModule()).readValue("\"0.1 m\"", Length.class);
        assertEquals(0.1, read.value());
    }

    @Test
    void readsAndWritesUnitsOfAUserSystem(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("medieval.units");
        Files.writeString(file, "@system Medieval\nfurlong:furlong 220 yd\n");
        UnitFormat format = UnitFormat.standard().with(SystemOfUnits.load(file));
        ObjectMapper mapper = mapper(UomModule.builder().unitFormat(format).build());
        Unit<?> furlong = format.parse("furlong");
        Untyped twoFurlongs = Untyped.of(2, furlong);
        String json = mapper.writeValueAsString(twoFurlongs);
        assertEquals("\"2.0 furlong\"", json);
        assertEquals(twoFurlongs, mapper.readValue(json, Untyped.class));
        assertEquals("\"furlong\"", mapper.writeValueAsString(furlong));
        assertEquals(furlong, mapper.readValue("\"furlong\"", Unit.class));
    }

    @Test
    void theDefaultModuleDoesNotKnowAUserUnit() {
        assertThrows(DatabindException.class, () -> mapper(new UomModule()).readValue("\"2 furlong\"", Length.class));
    }

    @Test
    void refusesNullSettings() {
        assertThrows(NullPointerException.class, () -> UomModule.builder().writeAs(null));
        assertThrows(NullPointerException.class, () -> UomModule.builder().unitFormat(null));
    }
}
