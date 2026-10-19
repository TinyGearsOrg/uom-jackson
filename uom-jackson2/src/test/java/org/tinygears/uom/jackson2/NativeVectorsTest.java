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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.MathContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
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
        try {
            return new ObjectMapper().readTree(Path.of("../vectors/quantities.json").toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
            list.add(Arguments.of(vector.get("name").asText(), vector));
        }
        return list.stream();
    }

    private static Class<?> type(JsonNode vector) throws ClassNotFoundException {
        String name = vector.get("type").asText();
        return name.equals("Untyped") ? Untyped.class : Class.forName("org.tinygears.uom.quantity." + name);
    }

    private static ObjectMapper mapper(boolean decimals) {
        UomModule module = decimals ? UomModule.builder().decimals(MathContext.DECIMAL64).build() : new UomModule();
        return new ObjectMapper().registerModule(module);
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
        Object read = mapper.readValue(vector.get("read").asText(), type);
        assertTrue(type.isInstance(read), () -> read.getClass() + " is not a " + type);
        Quantity<?> quantity = (Quantity<?>) read;
        assertEquals(vector.get("value").asText(), quantity.value().toString());
        assertEquals(vector.get("unit").asText(), UnitFormat.standard().format(quantity.unit()));
        assertEquals(Scale.valueOf(vector.get("scale").asText()), quantity.scale());
        assertEquals(vector.get("write").asText(), mapper.writeValueAsString(quantity));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errors")
    void error(String name, JsonNode vector) throws Exception {
        ObjectMapper mapper = mapper(false);
        Class<?> type = type(vector);
        String text = vector.get("read").asText();
        InvalidFormatException e = assertThrows(InvalidFormatException.class, () -> mapper.readValue(text, type));
        if (vector.has("message")) {
            assertEquals(vector.get("message").asText(), e.getOriginalMessage());
        } else {
            String prefix = vector.get("messagePrefix").asText();
            assertTrue(e.getOriginalMessage().startsWith(prefix), e.getOriginalMessage());
        }
    }
}
