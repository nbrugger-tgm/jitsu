# Jitsu

Jitsu is a experimental programming language that is designed to learn to create programming lanugages
and explore interesting ideas in compiler development. So be warned - don't use it for anything serious.

## Features

- [ ] Static types / Typesafe
- [ ] Null safety / no null
- [ ] Sum types
- [ ] explicit mutability
- [ ] compiles to native (LLVM)
- [ ] Default values for function parameters

## Blazingly fast?

Probably not. The goal is to learn and explore interesting ideas in compiler development not how to write the fastest
compiler (like v) or optimizer (like zig/rust/c).

## Syntax

> This is the vision how it should look like in the end!

```
fn main() {
    const x = 1;
    const y = 2;
    var z = x;
    z += y;
    println(z);
}
```

Type declaration
```
var y : File;
```
