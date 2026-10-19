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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Unit;
import org.tinygears.uom.engine.Dimension;
import org.tinygears.uom.quantity.QuantityTypes;

/** Reads a unit from its symbol; a {@code Unit<Length>} takes only units of that type. */
final class UnitDeserializer extends StdDeserializer<Unit<?>> {
    private static final long serialVersionUID = 1L;

    private final Class<?> target;
    private final transient Options options;

    /** @param target the quantity type of a {@code Unit<Length>}, or null for any unit */
    UnitDeserializer(Class<?> target, Options options) {
        super(Unit.class);
        this.target = target;
        this.options = options;
    }

    @Override
    public Unit<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() != JsonToken.VALUE_STRING) {
            return (Unit<?>) ctxt.handleUnexpectedToken(Unit.class, p);
        }
        String text = p.getText();
        String what = "\"" + TypeNames.abbreviate(text) + "\"";
        Unit<?> unit;
        try {
            unit = options.unitFormat().parse(text);
        } catch (RuntimeException e) {
            throw invalid(p, what, e.getMessage());
        }
        if (target == null) {
            return unit;
        }
        Dimension wanted = QuantityTypes.dimension(target);
        if (wanted != null && !wanted.equals(unit.dimension())) {
            throw invalid(p, what, TypeNames.describe(unit.dimension()) + " is not " + TypeNames.describe(target));
        }
        return asType(unit, target);
    }

    private InvalidFormatException invalid(JsonParser p, String what, String reason) {
        String name = target == null ? "a unit" : "Unit<" + target.getSimpleName() + ">";
        return InvalidFormatException.from(p, "cannot read " + what + " as " + name + ": " + reason, what, Unit.class);
    }

    @SuppressWarnings("unchecked") // the type is a quantity type, so it is a type of itself
    private static <T extends Quantity<T>> Unit<?> asType(Unit<?> unit, Class<?> type) {
        return unit.asType((Class<T>) type);
    }
}
