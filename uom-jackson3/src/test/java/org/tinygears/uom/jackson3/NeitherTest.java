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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Neither uom nor uom-jsr is on the class path (the surefire execution neither). */
@Tag("neither")
class NeitherTest {
    @Test
    void theClassPathHasNeitherApi() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.tinygears.uom.Quantity"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.tinygears.uom.jsr.Jsr"));
    }

    @Test
    void theModuleSaysWhatIsMissing() {
        IllegalStateException e = assertThrows(IllegalStateException.class, UomModule::new);
        assertEquals(
                "uom-jackson3 needs org.tinygears:uom or org.tinygears:uom-jsr on the class path", e.getMessage());
    }
}
