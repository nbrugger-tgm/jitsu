# Attributes

Attributes allow to attach meta information to all _definitions_ (function declaration, constants, types).

```kotlin
[Native(
    target = "malloc",
    deterministic = false
)]
native fn libc_malloc()
```

## Provided Attributes
Jitsu provides a few attributes out of the box
- `Native`

## Create attributes

```
attribute AttributeName {
    property: type;
    property2: type2;
}
```

