# Functions

Functions (called _methods_ when attached to types) are reusable pices of code as known from almost any language.

The most simple function would look like this
```
fn main() {
    println("Hello World");
}
```

## Parameters

As in all languages functions can have parameters to alter their behaviour

```
fn greet(name: String) {
    println(name);
}
```
In this example `name` is the name of the parameter by which it can be addressed and `String` the type.
Types are mandatory for all parameters!
### Mutability
Parameters are immutable by default, and need to be made mutable explicitly using `mut`.
> Note that using `mut` on an inherit immutable structure such as a string doesn't make it mutable<br/>
> Therefore `mut name: String` is the same as `name: String`

An example of such a method would be
```
fn anonymizeName(mut employee: Person) {
    employee.name = "GDPR deleted";
}
```

Note that mutable parameters are discouraged in general and mutation should more happen in [methods](#methods)

## Return Values

Methods often need to report values back to their caller. This values are commonly called "return values", jitsu is
no exception. For a method to be able to return a value, use `: TYPE` at the end of the signature so that one can
return any value of type `TYPE`. The `return` keyword is used to return the actual value.

```Kotlin
fn sum(a: int, b: int): int {
    return a + b;
}
```

## Methods

When a function is attached to a type to act upon it is called a `method`. Methods other than functions can access the
object they are called upon using the `this` keyword. Normally only [classes](Classes.md) can have methods but using
[extension functions](#extension-functions) also other types can have attached methods.

```Typescript
class Person {
    public name: String;
    
    fn catch(otherPerson: Person) {
        println("${this.name} caught ${otherPerson.name}");
    }
}
```

Since we are writing in the class itself and there is no ambiguous names we can access `name` without the `this`
```Typescript
class Person {
    public name: String;
    
    fn catch(otherPerson: Person) {
        println("${name} caught ${otherPerson.name}");
    }
}
```

In a scenario where another candidate for `name` exists `this` can be used to qualify the field.
```Typescript
fn shout(name: String) {
    // here `this` is needed because it would pick the parameter instead!
    println("${this.name} shouts: ${name}");
}
```

### Extension functions {id=extension-functions}
Extension functions are functions that are attached to non-class types or classes from outside your own code.

> Be aware that extension functions can only be called upon variables with [explicit types](Types.md#explicit-implicit)

Here are a few examples of how this can be used
```Kotlin
type SqlValue = int | String | boolean;

fn SqlValue.format(): String {
    return switch(this) {
        is int -> Numbers.toString(this);
        is String -> "'${this}'";//There should be escaping here
        is boolean -> if(this) "1" else "0";
    };
}

var someValue: SqlValue = 123;
var fomatted = someValue.format();
```

```
type Named = { name: String };

fn Named.logName() {
    println(this.name);
}

var markus: Named = {
    name: "Markus
};
markus.logName();
```

Extension functions cannot access class internals, they can only use public methods/fields.