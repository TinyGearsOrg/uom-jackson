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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The module on the module path, which the other tests do not use: each test compiles a small module that requires it
 * and runs that module with {@code --module-path}, for the three module graphs that matter (uom and uom-jsr, uom only,
 * neither). The module under test is the exploded {@code target/classes}.
 */
class ModulePathTest {
    private record Run(int exit, List<String> out, String err) {}

    /** The jar of an artifact on the class path of this test. */
    private static Path jar(String artifact) {
        Pattern name = Pattern.compile(Pattern.quote(artifact) + "-\\d.*\\.jar");
        String classPath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        List<Path> found = new ArrayList<>();
        for (String entry : classPath.split(File.pathSeparator)) {
            Path path = Path.of(entry);
            if (name.matcher(path.getFileName().toString()).matches()) {
                found.add(path.toAbsolutePath());
            }
        }
        assertEquals(1, found.size(), () -> artifact + " on the class path: " + found);
        return found.get(0);
    }

    private static List<Path> jackson() {
        return List.of(
                Path.of("target/classes").toAbsolutePath(),
                jar("jackson-databind"),
                jar("jackson-core"),
                jar("jackson-annotations"));
    }

    private static List<Path> uom() {
        return List.of(jar("uom"), jar("uom-engine"));
    }

    private static List<Path> jsr() {
        return List.of(jar("uom-jsr"), jar("unit-api"));
    }

    private static String join(List<Path> modulePath) {
        return modulePath.stream().map(Path::toString).collect(Collectors.joining(File.pathSeparator));
    }

    @SafeVarargs
    private static List<Path> path(List<Path>... parts) {
        List<Path> all = new ArrayList<>();
        for (List<Path> part : parts) {
            all.addAll(part);
        }
        return all;
    }

    /**
     * Compiles the module {@code consumer} against {@code compilePath} and runs its {@code consumer.Main} on
     * {@code runPath}, which is the same unless the test is about a module that is missing at run time.
     */
    private static Run run(
            Path dir, String moduleInfo, String main, List<Path> compilePath, List<Path> runPath, String... vmArguments)
            throws IOException, InterruptedException {
        Path source = dir.resolve("src");
        Files.createDirectories(source.resolve("consumer"));
        Path info = source.resolve("module-info.java");
        Path mainFile = source.resolve("consumer/Main.java");
        Files.writeString(info, moduleInfo);
        Files.writeString(mainFile, main);
        String path = join(compilePath);
        Path classes = dir.resolve("classes");
        Path compiler = dir.resolve("javac.txt");
        Process javac = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "javac").toString(),
                        "--module-path", path, "-d", classes.toString(), info.toString(), mainFile.toString())
                .redirectErrorStream(true)
                .redirectOutput(compiler.toFile())
                .start();
        assertEquals(true, javac.waitFor(120, TimeUnit.SECONDS), "javac did not finish");
        assertEquals(0, javac.exitValue(), Files.readString(compiler));

        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("--module-path");
        command.add(classes + File.pathSeparator + join(runPath));
        command.addAll(List.of(vmArguments));
        command.add("-m");
        command.add("consumer/consumer.Main");
        Path stdout = dir.resolve("stdout.txt");
        Path stderr = dir.resolve("stderr.txt");
        Process process = new ProcessBuilder(command).redirectOutput(stdout.toFile()).redirectError(stderr.toFile())
                .start();
        assertEquals(true, process.waitFor(120, TimeUnit.SECONDS), "the consumer did not finish");
        return new Run(process.exitValue(), Files.readAllLines(stdout), Files.readString(stderr));
    }

    @Test
    void bothApisAreFoundThroughTheServiceLoader(@TempDir Path dir) throws Exception {
        String moduleInfo = """
                module consumer {
                    requires org.tinygears.uom.jackson3;
                    requires org.tinygears.uom;
                    requires org.tinygears.uom.jsr;
                }
                """;
        String main = """
                package consumer;

                import tools.jackson.databind.ObjectMapper;
                import tools.jackson.databind.json.JsonMapper;
                import org.tinygears.uom.quantity.Length;
                import org.tinygears.uom.systems.SI;

                public final class Main {
                    static String q(String text) {
                        return '"' + text + '"';
                    }

                    public static void main(String[] args) throws Exception {
                        // the module is found by its provides, there is no META-INF/services on the module path
                        ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();
                        System.out.println("native " + mapper.writeValueAsString(Length.of(5, SI.KILOMETRE)));
                        System.out.println("native read " + mapper.readValue(q("5 km"), Length.class));
                        org.tinygears.uom.jsr.quantity.Length jsr =
                                org.tinygears.uom.jsr.quantity.Length.of(5, org.tinygears.uom.jsr.systems.SI.KILOMETRE);
                        System.out.println("jsr " + mapper.writeValueAsString(jsr));
                        System.out.println("jsr read " + mapper.readValue(q("5 km"), javax.measure.quantity.Length.class));
                        System.out.println("version known " + !new org.tinygears.uom.jackson3.UomModule().version().isUnknownVersion());
                    }
                }
                """;
        List<Path> both = path(jackson(), uom(), jsr());
        Run run = run(dir, moduleInfo, main, both, both);
        assertEquals(0, run.exit(), run.err());
        assertEquals(
                List.of("native \"5.0 km\"", "native read 5.0 km", "jsr \"5.0 km\"", "jsr read 5.0 km", "version known true"),
                run.out());
    }

    @Test
    void uomAloneIsEnoughAndTheModuleReadsItsTypes(@TempDir Path dir) throws Exception {
        // the consumer requires only the module: uom is read through "requires static transitive", and is added to the
        // module graph on the command line, since nothing requires it
        String moduleInfo = """
                module consumer {
                    requires org.tinygears.uom.jackson3;
                }
                """;
        String main = """
                package consumer;

                import tools.jackson.databind.ObjectMapper;
                import tools.jackson.databind.json.JsonMapper;
                import org.tinygears.uom.UnitFormat;
                import org.tinygears.uom.jackson3.UomModule;
                import org.tinygears.uom.quantity.Length;
                import org.tinygears.uom.systems.SI;

                public final class Main {
                    static String q(String text) {
                        return '"' + text + '"';
                    }

                    public static void main(String[] args) throws Exception {
                        UomModule module = UomModule.builder()
                                .writeAs(UomModule.Form.OBJECT)
                                .unitFormat(UnitFormat.standard())
                                .build();
                        ObjectMapper mapper = JsonMapper.builder().addModule(module).build();
                        System.out.println("native " + mapper.writeValueAsString(Length.of(5, SI.KILOMETRE)));
                        System.out.println("native read " + mapper.readValue(q("5 km"), Length.class));
                        try {
                            Class.forName("org.tinygears.uom.jsr.Jsr");
                            System.out.println("jsr present");
                        } catch (ClassNotFoundException e) {
                            System.out.println("jsr absent");
                        }
                        System.out.println("version known " + !module.version().isUnknownVersion());
                    }
                }
                """;
        List<Path> uomOnly = path(jackson(), uom());
        Run run = run(dir, moduleInfo, main, uomOnly, uomOnly, "--add-modules", "org.tinygears.uom");
        assertEquals(0, run.exit(), run.err());
        assertEquals(
                List.of("native {\"value\":5.0,\"unit\":\"km\"}", "native read 5.0 km", "jsr absent", "version known true"),
                run.out());
    }

    @Test
    void withoutUomTheModuleSaysWhatIsMissing(@TempDir Path dir) throws Exception {
        String moduleInfo = """
                module consumer {
                    requires org.tinygears.uom.jackson3;
                }
                """;
        String main = """
                package consumer;

                import org.tinygears.uom.jackson3.UomModule;

                public final class Main {
                    public static void main(String[] args) {
                        try {
                            new UomModule();
                            System.out.println("created");
                        } catch (IllegalStateException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
                """;
        // compiling against the module needs uom on the module path (requires static transitive); running does not
        Run run = run(dir, moduleInfo, main, path(jackson(), uom()), jackson());
        assertEquals(0, run.exit(), run.err());
        assertEquals(
                List.of("uom-jackson3 needs org.tinygears:uom or org.tinygears:uom-jsr on the class path"),
                run.out());
    }
}
