# JSON format

How uom quantities and units appear in JSON. Both modules (`uom-jackson2` and `uom-jackson3`) and both uom APIs (native
and JSR-385) use this one format.

**Status:** both modules implement this format.

## Quantities

A quantity has two forms.

| Form | Example |
|---|---|
| Text, the format of uom's `QuantityFormat` (a number, a space, a unit symbol; the number never depends on the locale) | `"5 km"` |
| Object | `{"value": 5, "unit": "km"}` |

The object may carry `"scale": "RELATIVE"` for a difference (a rise of 5 °C); the default is `"ABSOLUTE"`, a point.

**Writing.** A quantity with the scale `ABSOLUTE` is written as text. One with the scale `RELATIVE` is written as an
object, because the text form cannot carry the scale: a parse always gives `ABSOLUTE`, so a temperature difference
written as text would come back as a point. A setting writes every quantity as an object. A quantity whose value is NaN
or infinite is not written, because JSON has no such number: the writer fails with an error.

**Reading.** Both forms are read. In an object, `value` is a JSON number or a string (an optional minus sign, digits, an
optional fraction and an optional exponent; no NaN and no hexadecimal), `unit` is a string, and `scale` is `"ABSOLUTE"` or
`"RELATIVE"`. Each property may appear once. An unknown property follows the setting of the mapper,
`FAIL_ON_UNKNOWN_PROPERTIES`: it is an error where that is on (the default of Jackson 2) and is ignored where it is off
(the default of Jackson 3).

**Numbers.** A quantity reads as `double` by default, as `QuantityFormat` does. With a `MathContext` set on the
module, it reads as an exact `BigDecimal` that keeps every digit. A decimal quantity is written with its exact value
(`1.50`, `1E+3`).

## Settings

The builder of `UomModule` sets:

| Setting | Effect |
|---|---|
| `writeAs(Form)` | `TEXT` (the default) or `OBJECT` |
| `valuesAsStrings(boolean)` | the object form writes `value` as a string, for readers that take a JSON number for a `double`; the text form is not affected |
| `decimals(MathContext)` | read quantities as exact decimals (the default reads `double`) |
| `unitFormat(UnitFormat)` | the format of unit symbols, with the units of any user `.units` systems; it serves both uom APIs |

## Types

The type of the field decides what is read. A field of type `Length` reads `"5 km"` and rejects `"5 s"` with an error
that names both types and the path of the field: `cannot read "5 s" as Length: Time [T] is not Length [L]`. A field of
type `Untyped` accepts any dimension. A field of type `Quantity<?>` gets the type of the unit, as `QuantityFormat.parse`
gives it (`"90 km/h"` is a `Speed`); a field of type `Quantity<Length>` is a `Length`.

The same holds on the JSR-385 side. A field of type `javax.measure.quantity.Length`,
`org.tinygears.uom.jsr.quantity.Length` or `javax.measure.Quantity<Length>` reads like the native `Length` and gets a
quantity of uom-jsr, and a quantity of uom-jsr is written as the native quantity of the same value, so both APIs give the
same JSON. Only quantities of uom-jsr are written: a quantity of another JSR-385 implementation is an error.

## Units

A native unit is its symbol as a string (`"kg·m/s"`), in the format of uom's `UnitFormat`. A field of type
`Unit<Length>` rejects a unit of another dimension. User `.units` systems work when the module is given a `UnitFormat`
that has them. The units of uom-jsr (`javax.measure.Unit` fields) are not handled yet.

## Errors and limits

A value that cannot be read is a Jackson mapping error with the path of the field, and a message that quotes at most 60
characters of the input. The limits of uom apply (a number of at most 50000 digits, a text of at most 10000 characters),
and so do the limits that Jackson sets on the input.
