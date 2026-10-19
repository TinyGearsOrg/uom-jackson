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

import java.util.List;

import org.tinygears.uom.engine.Dimension;
import org.tinygears.uom.quantity.QuantityTypes;

/** Names for error messages: a dimension with the uom types that have it, as in {@code Length [L]}. */
final class TypeNames {
    private TypeNames() {}

    /** The dimension with the types that have it ({@code Frequency/Radioactivity [T^-1]}), or just {@code [L·T]}. */
    static String describe(Dimension dimension) {
        List<String> names = QuantityTypes.types().stream()
                .filter(type -> dimension.equals(QuantityTypes.dimension(type)))
                .map(Class::getSimpleName)
                .sorted()
                .toList();
        String text = "[" + text(dimension) + "]";
        return names.isEmpty() ? text : String.join("/", names) + " " + text;
    }

    /** A type with its dimension, as in {@code Length [L]}; {@code Untyped} has none. */
    static String describe(Class<?> type) {
        Dimension dimension = QuantityTypes.dimension(type);
        return dimension == null ? type.getSimpleName() : type.getSimpleName() + " [" + text(dimension) + "]";
    }

    /** The text cut to a length that fits in a message. */
    static String abbreviate(String text) {
        return text.length() <= 60 ? text : text.substring(0, 57) + "...";
    }

    private static String text(Dimension dimension) {
        return dimension.equals(Dimension.NONE) ? "1" : dimension.toString();
    }
}
