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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.Version;

/** The version of the module is the one of the build. */
class VersionTest {
    private static InputStream stream(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String pomVersion() throws IOException {
        Matcher m = Pattern.compile("<artifactId>uom-jackson-parent</artifactId>\\s*<version>([^<]+)</version>")
                .matcher(Files.readString(Path.of("../pom.xml")));
        assertTrue(m.find(), "the version is not in the parent pom.xml");
        return m.group(1);
    }

    @Test
    void isTheVersionOfThePom() throws IOException {
        Version version = new UomModule().version();
        assertEquals(pomVersion(), version.toString());
        assertEquals("org.tinygears", version.getGroupId());
        assertEquals("uom-jackson2", version.getArtifactId());
        assertEquals(pomVersion().endsWith("-SNAPSHOT"), version.isSnapshot());
        assertFalse(version.isUnknownVersion());
    }

    @Test
    void readsAFilledInResource() {
        Version version = UomModule.version(stream("version=1.2.3\ngroupId=g\nartifactId=a\n"));
        assertEquals("1.2.3", version.toString());
        assertEquals("g/a/1.2.3", version.toFullString());
        assertFalse(version.isSnapshot());
    }

    @Test
    void readsASnapshot() {
        Version version = UomModule.version(stream("version=0.2.0-SNAPSHOT\ngroupId=g\nartifactId=a\n"));
        assertEquals(0, version.getMajorVersion());
        assertEquals(2, version.getMinorVersion());
        assertEquals(0, version.getPatchLevel());
        assertTrue(version.isSnapshot());
    }

    @Test
    void aMissingResourceIsAnUnknownVersion() {
        assertTrue(UomModule.version(null).isUnknownVersion());
    }

    @Test
    void aResourceThatWasNotFilledInIsAnUnknownVersion() {
        String sources = "version=${project.version}\ngroupId=${project.groupId}\nartifactId=${project.artifactId}\n";
        assertTrue(UomModule.version(stream(sources)).isUnknownVersion());
        assertTrue(UomModule.version(stream("groupId=g\n")).isUnknownVersion());
    }

    @Test
    void aResourceThatCannotBeReadIsAnUnknownVersion() {
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken");
            }
        };
        assertTrue(UomModule.version(broken).isUnknownVersion());
    }
}
