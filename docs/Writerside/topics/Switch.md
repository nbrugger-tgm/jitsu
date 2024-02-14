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

### Exhaustive check

Every switch needs to be exhaustive, that means that a switch has to **always** execute exactly one case, not less!
If your cases do not cover all cases you can use the [`else` case](#else-case)

## Cases {id="cases"}

Different kind of cases help with conditions to assert upon the expression under test

### Type Case

Matches the variable against a type and casts it respectively

```Kotlin
var variable: string | int = computeValue();

switch(variable) {
    String -> println("It is a string : "+variable);
    int -> println("$variable * 2 = ${variable * 2}");
}
```

### Constant Case

### Else Case {id="else-case"}