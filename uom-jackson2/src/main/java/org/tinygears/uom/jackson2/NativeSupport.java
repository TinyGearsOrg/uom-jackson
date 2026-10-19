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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.fasterxml.jackson.databind.ser.Serializers;
import org.tinygears.uom.Quantity;
import org.tinygears.uom.Unit;

/** Registers the serializers and deserializers of the native uom API; loaded only when uom is on the class path. */
final class NativeSupport {
    private NativeSupport() {}

    static void install(Module.SetupContext context, Options options) {
        JsonSerializer<Quantity<?>> quantities = new QuantitySerializer(options);
        JsonSerializer<Unit<?>> units = new UnitSerializer(options);
        context.addSerializers(new Serializers.Base() {
            @Override
            public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription desc) {
                Class<?> raw = type.getRawClass();
                if (Quantity.class.isAssignableFrom(raw)) {
                    return quantities;
                }
                return raw == Unit.class ? units : null;
            }
        });
        context.addDeserializers(new Deserializers.Base() {
            @Override
            public JsonDeserializer<?> findBeanDeserializer(
                    JavaType type, DeserializationConfig config, BeanDescription desc) {
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
