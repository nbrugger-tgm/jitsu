# Unions

A Union (formal "sum type") is a type that can be one of many (2..N) types

``type JsonValue = int | string | JsonObject | JsonArray;``

This means that variables with this type can be either `int`, `string`, `JsonObject` or `JsonArray`

in order to properly use units you probably require [switch](Switch.md).

> For a comparison to enums see [Enum vs Union](Enums.md#enum-vs-union)
