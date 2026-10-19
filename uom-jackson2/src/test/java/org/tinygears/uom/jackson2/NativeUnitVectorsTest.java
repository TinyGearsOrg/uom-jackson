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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinygears.uom.Unit;
import org.tinygears.uom.UnitFormat;

/** The units of vectors/units.json, read and written as the native {@code Unit} of a field. */
class NativeUnitVectorsTest {
    private static final JsonNode VECTORS = load();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new UomModule());

    private static JsonNode load() {
        try {
            return new ObjectMapper().readTree(Path.of("../vectors/units.json").toFile());
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

    /** {@code Unit<Length>} for a type, the raw {@code Unit} for none. */
    private JavaType type(JsonNode vector) throws ClassNotFoundException {
        if (vector.get("type").isNull()) {
            return mapper.constructType(Unit.class);
        }
        Class<?> quantity = Class.forName("org.tinygears.uom.quantity." + vector.get("type").asText());
        return mapper.getTypeFactory().constructParametricType(Unit.class, quantity);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTrips")
    void roundTrip(String name, JsonNode vector) throws Exception {
        Object read = mapper.readValue(vector.get("read").asText(), type(vector));
        assertInstanceOf(Unit.class, read);
        assertEquals(UnitFormat.standard().parse(vector.get("symbol").asText()), read);
        assertEquals(vector.get("write").asText(), mapper.writeValueAsString(read));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errors")
    void error(String name, JsonNode vector) throws Exception {
        JavaType type = type(vector);
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
