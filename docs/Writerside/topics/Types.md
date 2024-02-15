# Types

The types-system in jitsu is quite rich and comparable to Typescript, with inspirations from Kotlin and other languages.

One important concept is the difference between raw and boundary types.

## Raw types
Raw types are types that have an actual runtime implication and a memory layout.
- [Classes](Classes.md)
- [Structs](Structs.md)
- [Enums](Enums.md)
- [Native](Natives.md)

These types are always inferred and have initializers to create instances of them. They are a runtime concept.

**Every variable has a raw type since it needs a memory layout!**

One can get information about a variables raw type using: ``rawType<T>(variable: T): RawType``
with `RawType` being defined as `type RawType = Class | Struct | Enum | Native`

## Boundary types
Boundary types are primarily a compile time concept to provide a rich type system and good DX.

- [Unions](Unions.md)
- [Interfaces](Interfaces.md)
- [Value Types](#value-types)

_This types cannot be instantiated_ but be used as boundaries for input types and type checking.

For example in the following example `x` is not of type `IntOrStr` but `int` or va:
```Typescript
type IntOrStr = int | String;
var x = 123;
```
While `x` is not [_explicitly_](#explicit-implicit) of type `IntToStr` its raw type `int` is. 
This means that this is valid
```Typescript
type IntOrStr = int | String;
var x = 123;
var y: IntOrStr = x;
```
or more direct
```Typescript
type IntOrStr = int | String;
var x: IntOrStr = 123;
println(explicitType(x)) //prints 'IntOrStr'
println(rawType(x)) //prints 'int'
```

In general boundary types are mostly meant for inputs/[parameters](Functions.md#parameters)

### Explicit vs implicit {id="explicit-implicit" collapsible="true" default-state="collapsed"}
A variable can have a boundary type _implicitly_ or _explicitly_.

Implicitly means that the variable is not defined
as a type of this boundary type.
```Typescript
type IntOrStr = int | String;
var x = 123;
```
In this example `x` is only implicitly a `IntOrStr` since it is not defined as such but can be cast (runtime cost-free)
to the respective type and be used at places where an `IntOrStr` is required.

But as shown above boundary types can also be _explicitly_ declared
```Typescript
type IntOrStr = int | String;
var x: IntOrStr = 123;
```
This is important when dealing with [extension functions](Functions.md#extension-functions).

## Value Types

In jitsu values can be used as type. This is only applicable with compile time constant values.
```type BitSize = 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128;```

It can also be used with values of different [raw types](#raw-types).

``fn File.isDelete(): true | IOError``

Whenever possible the value type is inferred!

```Typescript
type IntBitSize = 1 | 2 | 4 | 8 | 16 | 32 | 64 | 128;
var x = 1;
println(explicitType(x)) //prints 'null'
println(rawType(x)) //prints 'int'
println(valueType(x)) //prints '1' since it can be infered

var y : 2 = 2;
println(explicitType(y)) //prints '2'
println(rawType(y)) //prints 'int'
println(valueType(y)) //prints '2'
```