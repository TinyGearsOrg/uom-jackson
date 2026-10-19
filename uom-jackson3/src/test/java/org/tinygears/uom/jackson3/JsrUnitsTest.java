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

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinygears.uom.UnitFormat;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.jsr.Jsr;
import org.tinygears.uom.jsr.UomUnit;
import org.tinygears.uom.jsr.quantity.UntypedDoubleQuantity;
import org.tinygears.uom.jsr.systems.SI;

/** The units of vectors/units.json, read and written as the {@code javax.measure.Unit} of uom-jsr. */
class JsrUnitsTest {
    private static final JsonNode VECTORS = load();
    private final ObjectMapper mapper = Mappers.with(new UomModule());

    record Tools(
            javax.measure.Unit<javax.measure.quantity.Length> length,
            javax.measure.Unit<?> any,
            org.tinygears.uom.Unit<org.tinygears.uom.quantity.Length> nativeLength) {}

    private static JsonNode load() {
        return JsonMapper.builder().build().readTree(Path.of("../vectors/units.json").toFile());
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

    /** {@code javax.measure.Unit<Length>} for a type, the raw {@code Unit} for none. */
    private JavaType type(JsonNode vector) throws ClassNotFoundException {
        if (vector.get("type").isNull()) {
            return mapper.constructType(javax.measure.Unit.class);
        }
        Class<?> quantity = Class.forName("javax.measure.quantity." + vector.get("type").asString());
        return mapper.getTypeFactory().constructParametricType(javax.measure.Unit.class, quantity);
    }

    /** The native unit of a unit of uom-jsr, through a quantity of one. */
    private static org.tinygears.uom.Unit<?> nativeUnit(Object unit) {
        return Jsr.fromJsr(UntypedDoubleQuantity.of(1, (UomUnit<?>) unit), Untyped.class).unit();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("roundTrips")
    void roundTrip(String name, JsonNode vector) throws Exception {
        Object read = mapper.readValue(vector.get("read").asString(), type(vector));
        assertInstanceOf(UomUnit.class, read);
        assertEquals(UnitFormat.standard().parse(vector.get("symbol").asString()), nativeUnit(read));
        assertEquals(vector.get("write").asString(), mapper.writeValueAsString(read));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errors")
    void error(String name, JsonNode vector) throws Exception {
        JavaType type = type(vector);
        String text = vector.get("read").asString();
        InvalidFormatException e = assertThrows(InvalidFormatException.class, () -> mapper.readValue(text, type));
        if (vector.has("message")) {
            assertEquals(vector.get("message").asString(), e.getOriginalMessage());
        } else {
            String prefix = vector.get("messagePrefix").asString();
            assertTrue(e.getOriginalMessage().startsWith(prefix), e.getOriginalMessage());
        }
    }

    @Test
    void bothApisWriteTheSameJson() throws Exception {
        assertEquals(
                mapper.writeValueAsString(org.tinygears.uom.systems.SI.KILOMETRE),
                mapper.writeValueAsString(SI.KILOMETRE));
        assertEquals("\"km\"", mapper.writeValueAsString(SI.KILOMETRE));
    }

    @Test
    void writesAndReadsTheFieldsOfARecord() throws Exception {
        Tools tools = new Tools(SI.KILOMETRE, SI.KILOGRAM.multiply(SI.METRE), org.tinygears.uom.systems.SI.KILOMETRE);
        String json = mapper.writeValueAsString(tools);
        assertEquals("{\"length\":\"km\",\"any\":\"kg·m\",\"nativeLength\":\"km\"}", json);
        assertEquals(tools, mapper.readValue(json, Tools.class));
    }

    @Test
    void aUnitOfAnotherTypeIsRefusedInAField() {
        DatabindException e = assertThrows(
                DatabindException.class,
                () -> mapper.readValue("{\"length\":\"s\",\"any\":\"s\",\"nativeLength\":\"km\"}", Tools.class));
        assertEquals("length", e.getPath().get(0).getPropertyName());
        assertEquals("cannot read \"s\" as Unit<Length>: Time [T] is not Length [L]", e.getOriginalMessage());
    }

    @Test
    void aUnitOfAnotherImplementationIsNotWritten() {
        javax.measure.Unit<?> foreign = (javax.measure.Unit<?>) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {javax.measure.Unit.class}, (proxy, method, args) -> null);
        DatabindException e = assertThrows(DatabindException.class, () -> mapper.writeValueAsString(foreign));
        assertTrue(
                e.getOriginalMessage().endsWith(": only units of uom-jsr can be written"), e.getOriginalMessage());
    }

    @Test
    void aUnitOfAUserSystemUsesTheUnitFormatOfTheModule(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        java.nio.file.Files.writeString(dir.resolve("medieval.units"), "@system Medieval\nfurlong:furlong 220 yd\n");
        UnitFormat format = UnitFormat.standard().with(org.tinygears.uom.SystemOfUnits.load(dir.resolve("medieval.units")));
        ObjectMapper medieval = Mappers.with(UomModule.builder().unitFormat(format).build());
        javax.measure.Unit<?> furlong = medieval.readValue("\"furlong\"", javax.measure.Unit.class);
        assertEquals("\"furlong\"", medieval.writeValueAsString(furlong));
        assertEquals(format.parse("furlong"), nativeUnit(furlong));
    }
}
