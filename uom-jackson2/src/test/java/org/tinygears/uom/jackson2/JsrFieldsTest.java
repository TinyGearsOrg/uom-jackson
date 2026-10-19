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

import java.lang.reflect.Proxy;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tinygears.uom.UnitFormat;
import org.tinygears.uom.jsr.Jsr;
import org.tinygears.uom.jsr.quantity.Length;
import org.tinygears.uom.jsr.quantity.Mass;
import org.tinygears.uom.jsr.quantity.Speed;
import org.tinygears.uom.jsr.quantity.Untyped;
import org.tinygears.uom.jsr.systems.SI;

/** Quantities of uom-jsr as the fields of a class, and how they compare with the native API. */
class JsrFieldsTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new UomModule());

    record Trip(
            javax.measure.quantity.Length distance,
            Speed pace,
            javax.measure.Quantity<?> any,
            javax.measure.Quantity<javax.measure.quantity.Mass> mass,
            Untyped raw) {}

    record NativeTrip(
            org.tinygears.uom.quantity.Length distance,
            org.tinygears.uom.quantity.Speed pace,
            org.tinygears.uom.Quantity<?> any,
            org.tinygears.uom.Quantity<org.tinygears.uom.quantity.Mass> mass,
            org.tinygears.uom.Untyped raw) {}

    private static Trip trip() {
        return new Trip(
                Length.of(5, SI.KILOMETRE),
                Speed.of(12, SI.METRE_PER_SECOND),
                Length.of(3, SI.METRE),
                Mass.of(2, SI.KILOGRAM),
                (Untyped) Jsr.toJsr(org.tinygears.uom.Untyped.of(6, UnitFormat.standard().parse("kg·m/s"))));
    }

    @Test
    void writesEveryField() throws Exception {
        assertEquals(
                "{\"distance\":\"5.0 km\",\"pace\":\"12.0 m/s\",\"any\":\"3.0 m\",\"mass\":\"2.0 kg\","
                        + "\"raw\":\"6.0 kg·m/s\"}",
                mapper.writeValueAsString(trip()));
    }

    @Test
    void readsWhatItWrote() throws Exception {
        Trip trip = trip();
        assertEquals(trip, mapper.readValue(mapper.writeValueAsString(trip), Trip.class));
    }

    @Test
    void bothApisReadTheSameJson() throws Exception {
        String json = mapper.writeValueAsString(trip());
        NativeTrip nativeTrip = mapper.readValue(json, NativeTrip.class);
        assertEquals(json, mapper.writeValueAsString(nativeTrip));
        assertEquals("km", UnitFormat.standard().format(nativeTrip.distance().unit()));
        assertEquals(5.0, nativeTrip.distance().value());
    }

    @Test
    void aQuantityOfAnyTypeTakesTheTypeOfItsUnit() throws Exception {
        Trip read = mapper.readValue(
                "{\"distance\":\"1 km\",\"pace\":\"1 m/s\",\"any\":\"90 km/h\",\"mass\":\"1 kg\","
                        + "\"raw\":\"1 m\"}",
                Trip.class);
        assertInstanceOf(Speed.class, read.any());
        assertInstanceOf(Mass.class, read.mass());
        assertInstanceOf(javax.measure.quantity.Length.class, read.distance());
    }

    @Test
    void theErrorNamesTheFieldAndTheTypes() {
        JsonMappingException e = assertThrows(
                JsonMappingException.class,
                () -> mapper.readValue(
                        "{\"distance\":\"5 s\",\"pace\":\"1 m/s\",\"any\":\"1 m\",\"mass\":\"1 kg\",\"raw\":\"1 m\"}",
                        Trip.class));
        assertEquals("distance", e.getPath().get(0).getFieldName());
        assertEquals("cannot read \"5 s\" as Length: Time [T] is not Length [L]", e.getOriginalMessage());
    }

    @Test
    void aQuantityOfAnotherImplementationIsNotWritten() {
        javax.measure.Quantity<?> foreign = (javax.measure.Quantity<?>) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[] {javax.measure.Quantity.class}, (proxy, method, args) -> null);
        JsonMappingException e = assertThrows(JsonMappingException.class, () -> mapper.writeValueAsString(foreign));
        assertTrue(
                e.getOriginalMessage().endsWith(": only quantities of uom-jsr can be written"),
                e.getOriginalMessage());
    }
}
