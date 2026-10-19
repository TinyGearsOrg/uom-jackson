<h1 align="center">uom-jackson</h1>

<p align="center"><b>Jackson modules for uom</b><br>
quantities and units as JSON, for Jackson 2 and Jackson 3, on the native and the JSR-385 API</p>

<p align="center">
  <a href="https://github.com/TinyGearsOrg/uom-jackson/actions/workflows/ci.yml"><img src="https://github.com/TinyGearsOrg/uom-jackson/actions/workflows/ci.yml/badge.svg?branch=main" alt="CI"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache--2.0-blue" alt="License: Apache-2.0"></a>
  <img src="https://img.shields.io/badge/Java-17%2B-orange" alt="Java 17+">
  <a href="https://central.sonatype.com/artifact/org.tinygears/uom"><img src="https://img.shields.io/maven-central/v/org.tinygears/uom?label=uom" alt="uom on Maven Central"></a>
</p>

Jackson modules for [uom](https://github.com/TinyGearsOrg/uom): quantities and units as JSON, with exact decimals, typed
fields and units that Jackson checks while it reads.

**Status:** under construction, nothing is released. `uom-jackson2` reads and writes quantities and units on the native
API, and quantities on the JSR-385 API of uom-jsr, in the format of [docs/JSON.md](docs/JSON.md). `uom-jackson3` detects
the uom API but has no serializers yet.

```java
ObjectMapper mapper = new ObjectMapper().registerModule(new UomModule());
mapper.writeValueAsString(Length.of(5, SI.KILOMETRE));              // "5.0 km"
mapper.readValue("{\"value\": 5, \"unit\": \"km\"}", Length.class);  // a Length of 5 km
mapper.readValue("\"5 s\"", Length.class);   // error: cannot read "5 s" as Length: Time [T] is not Length [L]
```

## Modules

| Artifact | For |
|---|---|
| `org.tinygears:uom-jackson2` | Jackson 2.x (`com.fasterxml.jackson`) |
| `org.tinygears:uom-jackson3` | Jackson 3.x (`tools.jackson`) |

Both use the same JSON format and the same version number, so one release covers both.

## Dependencies

uom and uom-jsr are **optional** dependencies of the modules. Declare the one you use, or both, and the module serves
what is on the class path:

| You declare | The module serves |
|---|---|
| `org.tinygears:uom` | the native API |
| `org.tinygears:uom-jsr` (it brings `uom`) | both APIs |
| neither | nothing: creating the module throws an `IllegalStateException` that names what is missing |

Jackson itself is an ordinary dependency, and your own Jackson version (from your BOM or your framework) wins over the
one the module is built against.

## Versions

The modules are built against a baseline and are meant to work with later versions of the same major:

| | Baseline |
|---|---|
| Jackson 2 | 2.18.12 |
| Jackson 3 | 3.0.4 |
| uom, uom-jsr | 0.1.0 |

Until uom 1.0 any `0.x` release may change its API, so only the tested combinations are supported. CI builds on JDK 17
and 25 against the baselines, and builds weekly against the latest Jackson 2, Jackson 3 and uom releases.

## Build

Java 17 or later and Maven 3.9:

```
mvn -B verify
```

The tests run on three class paths, to prove that the dependencies are optional: uom and uom-jsr, uom only, and neither.

## License

Apache License 2.0, see [LICENSE](LICENSE).
