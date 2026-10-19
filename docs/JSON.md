# JSON format

How uom quantities and units appear in JSON. Both modules (`uom-jackson2` and `uom-jackson3`) and both uom APIs (native
and JSR-385) use this one format.

**Status:** this is the specification for the first release. The modules do not implement it yet.

## Quantities

A quantity has two forms.

| Form | Example |
|---|---|
| Text, the format of uom's `QuantityFormat` (a number, a space, a unit symbol; the number never depends on the locale) | `"5 km"` |
| Object | `{"value": 5, "unit": "km"}` |

The object may carry `"scale": "RELATIVE"` for a difference (a rise of 5 °C); the default is `"ABSOLUTE"`, a point.

**Writing.** A quantity with the scale `ABSOLUTE` is written as text. One with the scale `RELATIVE` is written as an
object, because the text form cannot carry the scale: a parse always gives `ABSOLUTE`, so a temperature difference
written as text would come back as a point. A setting writes every quantity as an object.

**Reading.** Both forms are read. In an object, `value` is a JSON number or a string.

**Numbers.** A quantity reads as `double` by default, as `QuantityFormat` does. With a `MathContext` set on the
module, it reads as an exact `BigDecimal` that keeps every digit. A decimal quantity is written with its exact value; a
setting writes the value as a string, for consumers that read JSON numbers as `double`.

## Types

The type of the field decides what is read. A field of type `Length` reads `"5 km"` and rejects `"5 s"` with an error
that names both types and the path of the field. A field of type `Untyped` accepts any dimension. A field of type
`Quantity<?>` gets the type of the unit, as `QuantityFormat.parse` gives it (`"90 km/h"` is a `Speed`).

## Units

A unit is its symbol as a string (`"kg·m/s"`), in the format of uom's `UnitFormat`. A field of type `Unit<Length>`
rejects a unit of another dimension. User `.units` systems work when the module is given a `UnitFormat` that has them.

## Errors and limits

A value that cannot be read is a Jackson mapping error with the path of the field. The limits of uom apply (a number of
at most 50000 digits, a text of at most 10000 characters), and so do the limits that Jackson sets on the input.
