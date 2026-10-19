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

import java.math.MathContext;

import org.tinygears.uom.QuantityFormat;
import org.tinygears.uom.UnitFormat;

/** The settings of a module; {@code decimals} is null to read {@code double}. */
record Options(UomModule.Form writeAs, boolean valuesAsStrings, MathContext decimals, UnitFormat unitFormat) {
    static Options defaults() {
        return new Options(UomModule.Form.TEXT, false, null, UnitFormat.standard());
    }

    QuantityFormat writeFormat() {
        return new QuantityFormat(unitFormat);
    }

    QuantityFormat readFormat() {
        QuantityFormat format = new QuantityFormat(unitFormat);
        return decimals == null ? format : format.withDecimals(decimals);
    }
}
