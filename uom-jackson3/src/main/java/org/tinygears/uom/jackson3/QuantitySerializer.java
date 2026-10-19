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

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.QuantityFormat;
import org.tinygears.uom.Scale;

/** Writes a quantity as text, or as an object when the options say so or when it is a difference. */
final class QuantitySerializer extends ValueSerializer<Quantity<?>> {
    private final Options options;
    private final QuantityFormat format;

    QuantitySerializer(Options options) {
        this.options = options;
        this.format = options.writeFormat();
    }

    @Override
    public void serialize(Quantity<?> quantity, JsonGenerator gen, SerializationContext provider) {
        Number value = quantity.value();
        if (value instanceof Double d && !Double.isFinite(d)) {
            throw DatabindException.from(gen, "cannot write the non-finite value " + d + " of a quantity");
        }
        boolean difference = quantity.scale() == Scale.RELATIVE;
        String text = null;
        String unit = null;
        try {
            if (options.writeAs() == UomModule.Form.TEXT && !difference) {
                text = format.format(quantity);
            } else {
                unit = options.unitFormat().format(quantity.unit());
            }
        } catch (IllegalArgumentException e) {
            throw DatabindException.from(gen, "cannot write a quantity: " + e.getMessage(), e);
        }
        if (text != null) {
            gen.writeString(text);
            return;
        }
        gen.writeStartObject();
        if (options.valuesAsStrings()) {
            gen.writeStringProperty("value", value.toString());
        } else if (value instanceof BigDecimal decimal) {
            gen.writeName("value");
            gen.writeNumber(decimal);
        } else {
            gen.writeNumberProperty("value", value.doubleValue());
        }
        gen.writeStringProperty("unit", unit);
        if (difference) {
            gen.writeStringProperty("scale", "RELATIVE");
        }
        gen.writeEndObject();
    }
}
