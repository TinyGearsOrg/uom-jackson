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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Scale;
import org.tinygears.uom.UnitFormat;
import org.tinygears.uom.Untyped;

/** The vectors in vectors/quantities.json, which every module and API reads and writes alike. */
class NativeVectorsTest {
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

    private static Class<?> type(JsonNode vector) throws ClassNotFoundException {
        String name = vector.get("type").asString();
        return name.equals("Untyped") ? Untyped.class : Class.forName("org.tinygears.uom.quantity." + name);
    }

    private static ObjectMapper mapper(boolean decimals) {
        UomModule module = decimals ? UomModule.builder().decimals(MathContext.DECIMAL64).build() : new UomModule();
        return Mappers.with(module);
    }

    @Test
    void thereAreVectors() {
        assertTrue(VECTORS.get("roundTrips").size() > 10);
        assertTrue(VECTORS.get("errors").size() > 10);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTrips")
    void roundTrip(String name, JsonNode vector) throws Exception {
        ObjectMapper mapper = mapper(vector.get("decimals").asBoolean());
        Class<?> type = type(vector);
        Object read = mapper.readValue(vector.get("read").asString(), type);
        assertTrue(type.isInstance(read), () -> read.getClass() + " is not a " + type);
        Quantity<?> quantity = (Quantity<?>) read;
        assertEquals(vector.get("value").asString(), quantity.value().toString());
        assertEquals(vector.get("unit").asString(), UnitFormat.standard().format(quantity.unit()));
        assertEquals(Scale.valueOf(vector.get("scale").asString()), quantity.scale());
        assertEquals(vector.get("write").asString(), mapper.writeValueAsString(quantity));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errors")
    void error(String name, JsonNode vector) throws Exception {
        ObjectMapper mapper = mapper(false);
        Class<?> type = type(vector);
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
