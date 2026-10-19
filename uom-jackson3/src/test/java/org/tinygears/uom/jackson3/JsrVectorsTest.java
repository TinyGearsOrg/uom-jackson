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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.MathContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Scale;
import org.tinygears.uom.UnitFormat;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.jsr.Jsr;

/** The vectors of vectors/quantities.json, read and written through the JSR-385 types of uom-jsr. */
class JsrVectorsTest {
    private static final JsonNode VECTORS = load();

    private static JsonNode load() {
        return JsonMapper.builder().build().readTree(Path.of("../vectors/quantities.json").toFile());
    }

    static Stream<Arguments> roundTrips() {
        return arguments("roundTrips");
    }

    static Stream<Arguments> errors() {
        return arguments("errors");
    }

    private static Stream<Arguments> arguments(String section) {
        List<Arguments> list = new ArrayList<>();
        for (JsonNode vector : VECTORS.get(section)) {
            list.add(Arguments.of(vector.get("name").asString(), vector));
        }
        return list.stream();
    }

    private static Class<?> jsrType(JsonNode vector) throws ClassNotFoundException {
        return Class.forName("org.tinygears.uom.jsr.quantity." + vector.get("type").asString());
    }

    private static ObjectMapper mapper(boolean decimals) {
        UomModule module = decimals ? UomModule.builder().decimals(MathContext.DECIMAL64).build() : new UomModule();
        return Mappers.with(module);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTrips")
    void roundTrip(String name, JsonNode vector) throws Exception {
        ObjectMapper mapper = mapper(vector.get("decimals").asBoolean());
        Class<?> type = jsrType(vector);
        Object read = mapper.readValue(vector.get("read").asString(), type);
        assertTrue(type.isInstance(read), () -> read.getClass() + " is not a " + type);
        Quantity<?> quantity = Jsr.fromJsr((javax.measure.Quantity<?>) read, Untyped.class);
        assertEquals(vector.get("value").asString(), quantity.value().toString());
        assertEquals(vector.get("unit").asString(), UnitFormat.standard().format(quantity.unit()));
        assertEquals(Scale.valueOf(vector.get("scale").asString()), quantity.scale());
        assertEquals(vector.get("write").asString(), mapper.writeValueAsString(read));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTrips")
    void readsAsTheStandardInterface(String name, JsonNode vector) throws Exception {
        String type = vector.get("type").asString();
        if (type.equals("Untyped")) {
            return;
        }
        Class<?> standard = Class.forName("javax.measure.quantity." + type);
        ObjectMapper mapper = mapper(vector.get("decimals").asBoolean());
        Object read = mapper.readValue(vector.get("read").asString(), standard);
        assertInstanceOf(standard, read);
        assertEquals(vector.get("write").asString(), mapper.writeValueAsString(read));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errors")
    void error(String name, JsonNode vector) throws Exception {
        ObjectMapper mapper = mapper(false);
        Class<?> type = jsrType(vector);
        String text = vector.get("read").asString();
        InvalidFormatException e = assertThrows(InvalidFormatException.class, () -> mapper.readValue(text, type));
        if (vector.has("message")) {
            assertEquals(vector.get("message").asString(), e.getOriginalMessage());
        } else {
            String prefix = vector.get("messagePrefix").asString();
            assertTrue(e.getOriginalMessage().startsWith(prefix), e.getOriginalMessage());
        }
    }
}
