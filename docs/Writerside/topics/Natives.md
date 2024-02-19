# Natives

Native types are the simplest fixed size (mostly numeric) types provided by the platform:

- `u8`,`u16`,`u32`,`u64`,`u128`: Unsigned 8,16,32,64,128 bit integers
- `i8`,`i16`,`i32`,`i64`,`i128`: Signed 8,16,32,64,128 bit integers
- `f32`,`f64`: Signed 32 and 64 bit floating values
- `int`: Alias for `i64`
- `byte`: Alias for `u8`
- `char`: Alias for `u16`
- `boolean`: 1 bit truth/false value

<warning>String is not a native type but a <a href="Classes.md">class</a> from the standard library</warning>