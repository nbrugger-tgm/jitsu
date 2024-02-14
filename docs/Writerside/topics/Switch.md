# Switch

The `switch` statement is used to make decisions based on a value. Can be used as an <tooltip term="expression">
expression</tooltip>.

#### General syntax

**As statement**

```Java
switch (<expression>) {
    <cases>
}
```

**As <tooltip term="expression">expression</tooltip>**

```Java
var result = switch (<expression>) {
    <expression cases>
};
```

* `<expression>` can be any <tooltip term="expression">expression</tooltip>
* `<cases>` There are multiple [types of cases](#cases) explained in the
* `<expression cases>` are `<cases>`

### Case Syntax

The syntax and semantics of `<conditons>` is explained in ["case types"](#cases)

```
<condition> -> <statement | expression>;
<condition> -> { 
    multiple();
    statements();
}
<condition> -> { 
    multiple();
    yield expression();
};
```

Here we can execute a single instruction or statement or use an expression.

The `yield` keyword is used to yield a value out of a scope/multiline lambda, for more info see the [docs](Code-blocks.md#yield)

## Limitations & Rules

### Exhaustive check {id="exhaustiveness"}

Every switch needs to be exhaustive, that means that a switch has to **always** execute exactly one case, not less!
If your cases do not cover all cases you can use the [`else` case](#else-case)

## Cases {id="cases"}

Different kind of cases help with conditions to assert upon the expression under test

### Type Case

Matches the variable against a type and casts it respectively. Denoted by the `is` keyword

```Kotlin
var variable: string | int = computeValue();

switch(variable) {
    is String -> println("It is a string : "+variable);
    is int -> println("$variable * 2 = ${variable * 2}");
}
```

### Constant Case

When matching against compile time values. This most likely requires an [`else`](#else-case) case to satisfy [exhaustiveness](#exhaustiveness).
Denoted by the `equals` keyword
```Java
var x = randomInt();
var xAsText = switch(x) {
    equals 1 -> "first";
    equals 2 -> "second";
    equals 3 -> "third";
    else -> "${x}th";
}
```

It is also possible to match against an arbitrary amount of values.

```Java
var statusCode = randomInt();
switch(statusCode) {
    equals 200, 201, 202 -> println("Operation completed!");
    equals 400 -> printerr("Wrong data sent!");
    else -> "Request errored with ${statusCode}";
}
```


### Else Case {id="else-case"}

The fallthrough case. This happens when no other condition applies. It has to be the last case and cannot be the only case!

For usage examples see [constant case](#constant-case) 