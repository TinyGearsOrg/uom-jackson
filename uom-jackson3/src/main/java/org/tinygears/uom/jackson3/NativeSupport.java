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

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.deser.Deserializers;
import tools.jackson.databind.ser.Serializers;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Unit;

/** Registers the serializers and deserializers of the native uom API; loaded only when uom is on the class path. */
final class NativeSupport {
    private NativeSupport() {}

    static void install(JacksonModule.SetupContext context, Options options) {
        ValueSerializer<Quantity<?>> quantities = new QuantitySerializer(options);
        ValueSerializer<Unit<?>> units = new UnitSerializer(options);
        context.addSerializers(new Serializers() {
            @Override
            public ValueSerializer<?> findSerializer(
                    SerializationConfig config, JavaType type, BeanDescription.Supplier desc, JsonFormat.Value format) {
                Class<?> raw = type.getRawClass();
                if (Quantity.class.isAssignableFrom(raw)) {
                    return quantities;
                }
                return raw == Unit.class ? units : null;
            }
        });
        context.addDeserializers(new Deserializers() {
            @Override
            public ValueDeserializer<?> findBeanDeserializer(
                    JavaType type, DeserializationConfig config, BeanDescription.Supplier desc) {
                Class<?> raw = type.getRawClass();
                if (raw == Quantity.class) {
                    Class<?> argument = argument(type);
                    return new QuantityDeserializer(argument == null ? Quantity.class : argument, options);
                }
                if (raw.isInterface() && Quantity.class.isAssignableFrom(raw)) {
                    return new QuantityDeserializer(raw, options);
                }
                return raw == Unit.class ? new UnitDeserializer(argument(type), options) : null;
            }

            @Override
            public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
                return valueType == Quantity.class || valueType == Unit.class
                        || (valueType.isInterface() && Quantity.class.isAssignableFrom(valueType));
            }
        });
    }

    /** The quantity type of {@code Quantity<Length>} or {@code Unit<Length>}; null for a wildcard or a raw type. */
    private static Class<?> argument(JavaType type) {
        if (type.containedTypeCount() == 0) {
            return null;
        }
        Class<?> raw = type.containedType(0).getRawClass();
        return raw != Quantity.class && Quantity.class.isAssignableFrom(raw) ? raw : null;
    }
}
