# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
the project follows [semantic versioning](https://semver.org/) from 1.0.0. Nothing is released yet.

## [Unreleased]

### Added

- `uom-jackson2` and `uom-jackson3`: the Jackson 2 and the Jackson 3 module for uom, each with a `UomModule` that
  registers with `ObjectMapper.registerModule` (Jackson 2), `JsonMapper.builder().addModule` (Jackson 3) or the service
  loader.
- Quantities as JSON in the format of [docs/JSON.md](docs/JSON.md): the text form (`"5 km"`) and the object form
  (`{"value": 5, "unit": "km"}`) are read; text is written, and an object for a temperature difference, whose scale the
  text cannot carry.
- The type of the field decides what is read: `Length`, `Untyped`, `Quantity<?>` (the type of the unit) or
  `Quantity<Length>`. A quantity of another dimension is refused with a message that names both types, as in
  `cannot read "5 s" as Length: Time [T] is not Length [L]`, and the path of the field.
- Units as their symbol: `Unit<?>` and `Unit<Length>` fields of the native API.
- Quantities of uom-jsr, in the fields `javax.measure.quantity.Length`, `org.tinygears.uom.jsr.quantity.Length` and
  `javax.measure.Quantity<Length>`, in the same JSON as the native ones. The units of uom-jsr are not handled yet.
- Settings of the module: `writeAs` (text or object), `valuesAsStrings`, `decimals` (exact `BigDecimal` quantities that
  keep every digit) and `unitFormat` (units of user `.units` systems).
- uom and uom-jsr are optional dependencies: the module serves the API that is on the class path, and says what is
  missing when neither is.
- The version of the module is the version of the build.
- Tests on the module path: a consumer module is compiled and run on `--module-path` for three module graphs (uom and
  uom-jsr, uom only, neither), which checks `provides`, the readability of uom through `requires static transitive`, the
  version resource and that no package is opened.
- CI on JDK 17 and 25 against the baselines, a weekly build against the latest Jackson and uom, and Dependabot.

[Unreleased]: https://github.com/TinyGearsOrg/uom-jackson/commits/main
