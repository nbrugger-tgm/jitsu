# Structs

Structs are the same as known from languages like C or Rust, they are data constructs that hold different fields.
The difference to [Classes](Classes.md) is that they are meant for simple use,
not many functions attached to them - just a mean to pass data. 
Besides that they don't offer encapsulation, all fields are always public.


## Definition
```Typescript
struct DatabaseConfig {
    url: URL,
    username: string,
    password: string
}
```

## Access

> As with all things in **jitsu** mutability is explicit. To make field mutable prefix them with `mut`

```Typescript
var postgres = DatabaseConfig {
    url: URL.parse("postgres://db.example.com/"),
    username: "root",
    mut password: "pa55word
};
var url = postgres.url;
postgres.password = "s3cret";
```

Since structs are supposed to be pure data carriers all fields are public and cannot be made private!

## Anonymous Structs

Anonymous structs are structs without name that are not  explicitly defined.

```Typescript
var oicdResult = {
    accessToken: base64(...),
    idToken: JWT.encode(...)
}
```

In this case an anonymous struct with the defined fields and inferred types is created at compile time.
The code above is equivalent to

```Typescript
struct $anonymous$struct$1 {
    accessToken: string,
    idToken: i8[]
}
var oicdResult = $anonymous$struct$1 {
    accessToken: base64(...),
    idToken: JWT.encode(...)
}
```
The declared/infered type of the variable `oicdResult` would be a [structural interface](Interfaces.md#structural). So
the infered type would be `{accessToken: string, idToken: string}` and **not** `$anonymous$struct$1`
