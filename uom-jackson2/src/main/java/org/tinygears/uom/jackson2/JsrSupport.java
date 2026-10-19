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
import org.tinygears.uom.jsr.quantity.UomQuantity;

/**
 * Registers the serializer and the deserializers of the quantities of uom-jsr; loaded only when uom-jsr is on the
 * class path. A field of type {@code javax.measure.quantity.Length}, {@code org.tinygears.uom.jsr.quantity.Length} or
 * {@code javax.measure.Quantity<Length>} reads like the native {@code Length}, and gets a quantity of uom-jsr.
 */
final class JsrSupport {
    private static final String STANDARD_PACKAGE = "javax.measure.quantity";
    private static final String JSR_PACKAGE = "org.tinygears.uom.jsr.quantity";
    private static final String NATIVE_PACKAGE = "org.tinygears.uom.quantity";

    private JsrSupport() {}

    static void install(Module.SetupContext context, Options options) {
        JsonSerializer<javax.measure.Quantity<?>> quantities =
                new JsrQuantitySerializer(new QuantitySerializer(options));
        context.addSerializers(new Serializers.Base() {
            @Override
            public JsonSerializer<?> findSerializer(SerializationConfig config, JavaType type, BeanDescription desc) {
                return javax.measure.Quantity.class.isAssignableFrom(type.getRawClass()) ? quantities : null;
            }
        });
        context.addDeserializers(new Deserializers.Base() {
            @Override
            public JsonDeserializer<?> findBeanDeserializer(
                    JavaType type, DeserializationConfig config, BeanDescription desc) {
                return deserializer(type, options);
            }
        });
    }

    private static JsonDeserializer<?> deserializer(JavaType type, Options options) {
        Class<?> raw = type.getRawClass();
        if (raw == javax.measure.Quantity.class) {
            Class<?> argument = argument(type);
            return argument == null ? natural(raw, options) : typed(raw, argument, options);
        }
        return isQuantityType(raw) ? typed(raw, raw, options) : null;
    }

    /** A field of the type of its unit: the native deserializer reads any quantity, and the result is converted. */
    private static JsonDeserializer<?> natural(Class<?> declared, Options options) {
        return new JsrQuantityDeserializer(
                declared, new QuantityDeserializer(org.tinygears.uom.Quantity.class, options), null);
    }

    /** A field of a quantity type, which has the same name in the standard API, in uom-jsr and in uom. */
    private static JsonDeserializer<?> typed(Class<?> declared, Class<?> quantityType, Options options) {
        String name = quantityType.getSimpleName();
        Class<?> nativeType = load(name.equals("Untyped") ? "org.tinygears.uom" : NATIVE_PACKAGE, name);
        Class<?> jsrType = load(JSR_PACKAGE, name);
        if (nativeType == null || jsrType == null) {
            return null;
        }
        return new JsrQuantityDeserializer(declared, new QuantityDeserializer(nativeType, options), jsrType);
    }

    private static boolean isQuantityType(Class<?> type) {
        if (!type.isInterface()) {
            return false;
        }
        String pkg = type.getPackageName();
        return pkg.equals(STANDARD_PACKAGE)
                || (pkg.equals(JSR_PACKAGE) && type != UomQuantity.class
                        && javax.measure.Quantity.class.isAssignableFrom(type));
    }

    /** The quantity type of {@code Quantity<Length>}; null for a wildcard or a raw type. */
    private static Class<?> argument(JavaType type) {
        if (type.containedTypeCount() == 0) {
            return null;
        }
        Class<?> raw = type.containedType(0).getRawClass();
        return isQuantityType(raw) ? raw : null;
    }

    private static Class<?> load(String pkg, String name) {
        try {
            return Class.forName(pkg + "." + name, false, JsrSupport.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
