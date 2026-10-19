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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Only uom is on the class path (the surefire execution native-only), as for a user who declares uom alone. */
@Tag("native-only")
class NativeOnlyTest {
    @Test
    void theClassPathHasUomButNotUomJsr() throws ClassNotFoundException {
        assertNotNull(Class.forName("org.tinygears.uom.Quantity"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.tinygears.uom.jsr.Jsr"));
    }

    @Test
    void servesTheNativeApiOnly() {
        UomModule module = new UomModule();
        assertTrue(module.supportsNative());
        assertFalse(module.supportsJsr());
    }
}
