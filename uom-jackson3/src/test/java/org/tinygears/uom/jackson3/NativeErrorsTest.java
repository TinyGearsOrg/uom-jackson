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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.MathContext;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import org.junit.jupiter.api.Test;
import org.tinygears.uom.Unit;
import org.tinygears.uom.quantity.Length;
import org.tinygears.uom.systems.SI;

/** What goes wrong, in the terms of Jackson. The messages of each case are in the vectors. */
class NativeErrorsTest {
    private final ObjectMapper mapper = Mappers.with(new UomModule());

    record Trip(Length distance) {}

    record Walk(Unit<Length> unit) {}

    @Test
    void aNumberIsNotAQuantity() {
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("5", Length.class));
    }

    @Test
    void anArrayIsNotAQuantity() {
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("[\"5 km\"]", Length.class));
    }

    @Test
    void aNumberIsNotAUnit() {
        assertThrows(MismatchedInputException.class, () -> mapper.readValue("5", Unit.class));
    }

    @Test
    void nullIsNull() throws Exception {
        assertNull(mapper.readValue("null", Length.class));
        assertNull(mapper.readValue("null", Unit.class));
    }

    @Test
    void anUnknownPropertyIsRefusedWhenTheMapperFailsOnUnknownProperties() {
        ObjectMapper strict = mapper.rebuild().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        UnrecognizedPropertyException e = assertThrows(
                UnrecognizedPropertyException.class,
                () -> strict.readValue("{\"value\":5,\"unit\":\"km\",\"colour\":\"red\"}", Length.class));
        assertEquals("colour", e.getPropertyName());
    }

    /** Jackson 3 ignores unknown properties by default, and the deserializer follows the setting of the mapper. */
    @Test
    void anUnknownPropertyIsIgnoredByDefault() {
        Length read = mapper.readValue("{\"value\":5,\"unit\":\"km\",\"colour\":\"red\"}", Length.class);
        assertEquals(Length.of(5, SI.KILOMETRE), read);
    }

    @Test
    void anUnknownPropertyIsIgnoredWhenTheMapperIgnoresThem() throws Exception {
        ObjectMapper lenient = mapper.rebuild().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();
        Length read = lenient.readValue("{\"colour\":{\"a\":[1]},\"value\":5,\"unit\":\"km\"}", Length.class);
        assertEquals(Length.of(5, SI.KILOMETRE), read);
    }

    @Test
    void nonFiniteValuesAreNotWritten() {
        DatabindException nan = assertThrows(
                DatabindException.class, () -> mapper.writeValueAsString(Length.of(Double.NaN, SI.METRE)));
        assertEquals("cannot write the non-finite value NaN of a quantity", nan.getOriginalMessage());
        DatabindException infinite = assertThrows(
                DatabindException.class,
                () -> mapper.writeValueAsString(Length.of(Double.NEGATIVE_INFINITY, SI.METRE)));
        assertEquals("cannot write the non-finite value -Infinity of a quantity", infinite.getOriginalMessage());
    }

    @Test
    void aHugeTextIsCutInTheMessage() {
        ObjectMapper decimals = Mappers.with(UomModule.builder().decimals(MathContext.DECIMAL64).build());
        String text = "\"" + "1".repeat(60000) + " m\"";
        InvalidFormatException e = assertThrows(InvalidFormatException.class, () -> decimals.readValue(text, Length.class));
        assertTrue(e.getOriginalMessage().length() < 200, () -> "" + e.getOriginalMessage().length());
        assertTrue(e.getOriginalMessage().startsWith("cannot read \"1111111111"), e.getOriginalMessage());
    }

    @Test
    void theErrorNamesTheFieldOfARecord() {
        DatabindException e = assertThrows(
                DatabindException.class, () -> mapper.readValue("{\"distance\":\"5 s\"}", Trip.class));
        assertEquals("distance", e.getPath().get(0).getPropertyName());
        assertEquals("cannot read \"5 s\" as Length: Time [T] is not Length [L]", e.getOriginalMessage());
    }

    @Test
    void aUnitOfAnotherTypeIsRefused() {
        DatabindException e = assertThrows(
                DatabindException.class, () -> mapper.readValue("{\"unit\":\"s\"}", Walk.class));
        assertEquals("unit", e.getPath().get(0).getPropertyName());
        assertEquals("cannot read \"s\" as Unit<Length>: Time [T] is not Length [L]", e.getOriginalMessage());
    }

    @Test
    void anUnknownUnitIsRefused() {
        InvalidFormatException e = assertThrows(
                InvalidFormatException.class, () -> mapper.readValue("\"foo\"", Unit.class));
        assertTrue(e.getOriginalMessage().startsWith("cannot read \"foo\" as a unit: "), e.getOriginalMessage());
    }
}
