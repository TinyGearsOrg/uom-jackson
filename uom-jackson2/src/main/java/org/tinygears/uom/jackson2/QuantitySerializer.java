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
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.QuantityFormat;
import org.tinygears.uom.Scale;

/** Writes a quantity as text, or as an object when the options say so or when it is a difference. */
final class QuantitySerializer extends JsonSerializer<Quantity<?>> {
    private final Options options;
    private final QuantityFormat format;

    QuantitySerializer(Options options) {
        this.options = options;
        this.format = options.writeFormat();
    }

    @Override
    public void serialize(Quantity<?> quantity, JsonGenerator gen, SerializerProvider provider) throws IOException {
        Number value = quantity.value();
        if (value instanceof Double d && !Double.isFinite(d)) {
            throw JsonMappingException.from(gen, "cannot write the non-finite value " + d + " of a quantity");
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
            throw JsonMappingException.from(gen, "cannot write a quantity: " + e.getMessage(), e);
        }
        if (text != null) {
            gen.writeString(text);
            return;
        }
        gen.writeStartObject();
        if (options.valuesAsStrings()) {
            gen.writeStringField("value", value.toString());
        } else if (value instanceof BigDecimal decimal) {
            gen.writeFieldName("value");
            gen.writeNumber(decimal);
        } else {
            gen.writeNumberField("value", value.doubleValue());
        }
        gen.writeStringField("unit", unit);
        if (difference) {
            gen.writeStringField("scale", "RELATIVE");
        }
        gen.writeEndObject();
    }
}
