/*
 * Copyright (c) 2026 TinyGears.org
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/** Jackson 3 support for uom quantities and units; uom and uom-jsr are optional. */
// the digit names the Jackson major, not a version of this module
@SuppressWarnings("module")
module org.tinygears.uom.jackson3 {
    requires transitive tools.jackson.databind;
    requires static org.tinygears.uom;
    requires static org.tinygears.uom.jsr;

    exports org.tinygears.uom.jackson3;

    provides tools.jackson.databind.JacksonModule with org.tinygears.uom.jackson3.UomModule;
}
