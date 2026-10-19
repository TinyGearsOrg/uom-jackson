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
import org.tinygears.uom.jsr.quantity.UomQuantity;

/**
 * Registers the serializers and the deserializers of the quantities and the units of uom-jsr; loaded only when uom-jsr
 * is on the class path. A field of type {@code javax.measure.quantity.Length}, {@code org.tinygears.uom.jsr.quantity.Length}
 * or {@code javax.measure.Quantity<Length>} reads like the native {@code Length}, and gets a quantity of uom-jsr; a field
 * of type {@code javax.measure.Unit<Length>} reads like the native {@code Unit<Length>}, and gets a unit of uom-jsr.
 */
final class JsrSupport {
    private static final String STANDARD_PACKAGE = "javax.measure.quantity";
    private static final String JSR_PACKAGE = "org.tinygears.uom.jsr.quantity";
    private static final String NATIVE_PACKAGE = "org.tinygears.uom.quantity";

    private JsrSupport() {}

    static void install(JacksonModule.SetupContext context, Options options) {
        ValueSerializer<javax.measure.Quantity<?>> quantities =
                new JsrQuantitySerializer(new QuantitySerializer(options));
        ValueSerializer<javax.measure.Unit<?>> units = new JsrUnitSerializer(new UnitSerializer(options));
        context.addSerializers(new Serializers() {
            @Override
            public ValueSerializer<?> findSerializer(
                    SerializationConfig config, JavaType type, BeanDescription.Supplier desc, JsonFormat.Value format) {
                Class<?> raw = type.getRawClass();
                if (javax.measure.Quantity.class.isAssignableFrom(raw)) {
                    return quantities;
                }
                return javax.measure.Unit.class.isAssignableFrom(raw) ? units : null;
            }
        });
        context.addDeserializers(new Deserializers() {
            @Override
            public ValueDeserializer<?> findBeanDeserializer(
                    JavaType type, DeserializationConfig config, BeanDescription.Supplier desc) {
                return deserializer(type, options);
            }

            @Override
            public boolean hasDeserializerFor(DeserializationConfig config, Class<?> valueType) {
                return valueType == javax.measure.Quantity.class
                        || valueType == javax.measure.Unit.class
                        || isQuantityType(valueType);
            }
        });
    }

    private static ValueDeserializer<?> deserializer(JavaType type, Options options) {
        Class<?> raw = type.getRawClass();
        if (raw == javax.measure.Quantity.class) {
            Class<?> argument = argument(type);
            return argument == null ? natural(raw, options) : typed(raw, argument, options);
        }
        if (raw == javax.measure.Unit.class) {
            return unit(type, options);
        }
        return isQuantityType(raw) ? typed(raw, raw, options) : null;
    }

    /** A unit field: the native deserializer reads it and checks its type, and the result is a unit of uom-jsr. */
    private static ValueDeserializer<?> unit(JavaType type, Options options) {
        Class<?> argument = argument(type);
        Class<?> nativeType = argument != null && argument.getPackageName().equals(STANDARD_PACKAGE)
                ? load(NATIVE_PACKAGE, argument.getSimpleName())
                : null;
        return new JsrUnitDeserializer(new UnitDeserializer(nativeType, options));
    }

    /** A field of the type of its unit: the native deserializer reads any quantity, and the result is converted. */
    private static ValueDeserializer<?> natural(Class<?> declared, Options options) {
        return new JsrQuantityDeserializer(
                declared, new QuantityDeserializer(org.tinygears.uom.Quantity.class, options), null);
    }

    /** A field of a quantity type, which has the same name in the standard API, in uom-jsr and in uom. */
    private static ValueDeserializer<?> typed(Class<?> declared, Class<?> quantityType, Options options) {
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

    /** The quantity type of {@code Quantity<Length>} or {@code Unit<Length>}; null for a wildcard or a raw type. */
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
