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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Unit;
import org.tinygears.uom.UnitFormat;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.quantity.Length;
import org.tinygears.uom.quantity.Mass;
import org.tinygears.uom.quantity.Speed;
import org.tinygears.uom.systems.SI;

/** Quantities and units as the fields of a class: the type of the field decides what is read. */
class NativeFieldsTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new UomModule());

    record Trip(
            Length distance,
            Speed pace,
            Quantity<?> any,
            Quantity<Mass> mass,
            Untyped raw,
            Unit<Length> unit,
            Unit<?> other) {}

    private static Trip trip() {
        return new Trip(
                Length.of(5, SI.KILOMETRE),
                Speed.of(12, SI.METRE_PER_SECOND),
                Length.of(3, SI.METRE),
                Mass.of(2, SI.KILOGRAM),
                Untyped.of(6, UnitFormat.standard().parse("kg·m/s")),
                SI.KILOMETRE,
                SI.SECOND);
    }

    @Test
    void writesEveryField() throws Exception {
        assertEquals(
                "{\"distance\":\"5.0 km\",\"pace\":\"12.0 m/s\",\"any\":\"3.0 m\",\"mass\":\"2.0 kg\","
                        + "\"raw\":\"6.0 kg·m/s\",\"unit\":\"km\",\"other\":\"s\"}",
                mapper.writeValueAsString(trip()));
    }

    @Test
    void readsWhatItWrote() throws Exception {
        Trip trip = trip();
        assertEquals(trip, mapper.readValue(mapper.writeValueAsString(trip), Trip.class));
    }

    @Test
    void aQuantityOfAnyTypeTakesTheTypeOfItsUnit() throws Exception {
        Trip read = mapper.readValue(
                "{\"distance\":\"1 km\",\"pace\":\"1 m/s\",\"any\":\"90 km/h\",\"mass\":\"1 kg\","
                        + "\"raw\":\"1 m\",\"unit\":\"m\",\"other\":\"kg·m/s\"}",
                Trip.class);
        assertInstanceOf(Speed.class, read.any());
        assertInstanceOf(Mass.class, read.mass());
        assertEquals("m", UnitFormat.standard().format(read.raw().unit()));
        assertEquals("kg·m/s", UnitFormat.standard().format(read.other()));
    }

    @Test
    void aTypedQuantityOfTheWrongTypeIsRefusedInAGenericField() {
        org.junit.jupiter.api.Assertions.assertThrows(
                com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue(
                        "{\"distance\":\"1 km\",\"pace\":\"1 m/s\",\"any\":\"1 m\",\"mass\":\"1 s\","
                                + "\"raw\":\"1 m\",\"unit\":\"m\",\"other\":\"s\"}",
                        Trip.class));
    }
}
