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

import java.math.BigDecimal;
import java.util.regex.Pattern;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.InvalidFormatException;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.QuantityFormat;
import org.tinygears.uom.Scale;
import org.tinygears.uom.Unit;
import org.tinygears.uom.Untyped;
import org.tinygears.uom.engine.Dimension;
import org.tinygears.uom.quantity.QuantityTypes;

/**
 * Reads the text or the object form of a quantity. The target is a typed interface ({@code Length}), {@code Untyped},
 * or {@code Quantity} itself, which takes the type of the unit.
 */
final class QuantityDeserializer extends StdDeserializer<Quantity<?>> {
    private static final long serialVersionUID = 1L;
    private static final Pattern NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?");

    private final Class<?> target;
    private final transient Options options;
    private final transient QuantityFormat format;

    QuantityDeserializer(Class<?> target, Options options) {
        super(target);
        this.target = target;
        this.options = options;
        this.format = options.readFormat();
    }

    @Override
    public Quantity<?> deserialize(JsonParser p, DeserializationContext ctxt) {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            return fromText(p, p.getString());
        }
        if (token == JsonToken.START_OBJECT) {
            return fromObject(p, ctxt);
        }
        return (Quantity<?>) ctxt.handleUnexpectedToken(target, p);
    }

    private Quantity<?> fromText(JsonParser p, String text) {
        String what = "\"" + TypeNames.abbreviate(text) + "\"";
        Quantity<?> parsed;
        try {
            parsed = format.parse(text);
        } catch (RuntimeException e) {
            throw invalid(p, what, e.getMessage());
        }
        return narrow(p, what, parsed);
    }

    private Quantity<?> fromObject(JsonParser p, DeserializationContext ctxt) {
        String what = "an object";
        String value = null;
        String unit = null;
        Scale scale = Scale.ABSOLUTE;
        boolean scaled = false;
        while (p.nextToken() == JsonToken.PROPERTY_NAME) {
            String name = p.currentName();
            JsonToken token = p.nextToken();
            switch (name) {
                case "value" -> {
                    once(p, what, value != null, name);
                    if (token != JsonToken.VALUE_STRING && !token.isNumeric()) {
                        throw invalid(p, what, "value must be a number or a string");
                    }
                    value = p.getString();
                }
                case "unit" -> {
                    once(p, what, unit != null, name);
                    if (token != JsonToken.VALUE_STRING) {
                        throw invalid(p, what, "unit must be a string");
                    }
                    unit = p.getString();
                }
                case "scale" -> {
                    once(p, what, scaled, name);
                    scaled = true;
                    scale = scale(p, what, token);
                }
                default -> ctxt.handleUnknownProperty(p, this, Quantity.class, name);
            }
        }
        if (value == null) {
            throw invalid(p, what, "it has no \"value\"");
        }
        if (unit == null) {
            throw invalid(p, what, "it has no \"unit\"");
        }
        return narrow(p, what, build(p, what, value, unit, scale));
    }

    private Quantity<?> build(JsonParser p, String what, String value, String unitText, Scale scale)
            {
        if (!NUMBER.matcher(value).matches()) {
            throw invalid(p, what, "value \"" + TypeNames.abbreviate(value) + "\" is not a number");
        }
        Unit<?> unit;
        try {
            unit = options.unitFormat().parse(unitText);
        } catch (RuntimeException e) {
            throw invalid(p, what, e.getMessage());
        }
        Untyped quantity;
        try {
            quantity = options.decimals() == null
                    ? Untyped.of(Double.parseDouble(value), unit, scale)
                    : Untyped.of(new BigDecimal(value), unit, scale, options.decimals());
        } catch (RuntimeException e) {
            throw invalid(p, what, e.getMessage());
        }
        Class<?> natural = target == Quantity.class ? QuantityTypes.typeOf(unit) : null;
        return natural == null ? quantity : as(quantity, natural);
    }

    private Quantity<?> narrow(JsonParser p, String what, Quantity<?> quantity) {
        if (target == Quantity.class) {
            return quantity;
        }
        Dimension wanted = QuantityTypes.dimension(target);
        Dimension actual = quantity.unit().dimension();
        if (wanted != null && !wanted.equals(actual)) {
            throw invalid(p, what, TypeNames.describe(actual) + " is not " + TypeNames.describe(target));
        }
        try {
            return as(quantity, target);
        } catch (RuntimeException e) {
            throw invalid(p, what, e.getMessage());
        }
    }

    private Scale scale(JsonParser p, String what, JsonToken token) {
        if (token != JsonToken.VALUE_STRING) {
            throw invalid(p, what, "scale must be a string");
        }
        String text = p.getString();
        return switch (text) {
            case "ABSOLUTE" -> Scale.ABSOLUTE;
            case "RELATIVE" -> Scale.RELATIVE;
            default -> throw invalid(
                    p, what, "scale must be \"ABSOLUTE\" or \"RELATIVE\", not \"" + TypeNames.abbreviate(text) + "\"");
        };
    }

    private void once(JsonParser p, String what, boolean seen, String name) {
        if (seen) {
            throw invalid(p, what, "\"" + name + "\" appears twice");
        }
    }

    private InvalidFormatException invalid(JsonParser p, String what, String reason) {
        String name = target == Quantity.class ? "a quantity" : target.getSimpleName();
        return InvalidFormatException.from(p, "cannot read " + what + " as " + name + ": " + reason, what, target);
    }

    @SuppressWarnings("unchecked") // the type is a quantity type, so it is a type of itself
    private static <T extends Quantity<T>> Quantity<?> as(Quantity<?> quantity, Class<?> type) {
        return quantity.as((Class<T>) type);
    }
}
