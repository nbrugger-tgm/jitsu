# Syntax

## Interfaces
```go
interface SomeInterface {
	fn dox(): String
	fn run()
	fn params(param1: String, param2: i64)
}
```

```go
interface SomeInterface {
	dox(): String
	run()
	params(param1: String, param2: i64)
}
```

```go
type SomeInterface = interface {
	fn dox(): String
	fn run()
	fn params(param1: String, param2: i64)
}
```

## Switch/case

### As Expression
#### Using constant value matching
```java
var output = switch(someVariable) {
    case 10 -> "ten";
	case 20 -> "twenty";
	default -> "neither 10 nor 20";
};
```
#### Using block expressions
```java
var output = switch(someVariable) {
    case 10 -> {
        var x = "te";
        var y ="n";
        yield x+y;
    }
	case 20 -> {
        var x = "twe";
        var y ="nty";
        yield x+y;
    }
	default -> "neither 10 nor 20";
};
```

### As statement
```java
switch(someVariable) {
    case 10 -> println("Proccess died from OOM");
	case 20 -> println("Proccess timed out");
};
```
```java
switch(someVariable) {
    case 10 -> {
        println("Proccess died from OOM")
    }
	case 20 -> {
        println("Proccess timed out")
    }
};
```

## Types

### Unitons
```ts
type UnionType = TypeA | TypeB | TypeC;
```

### Vaues
```ts
type StatusCode = "SomeString";
type StatusCode = 12331;

//this is usefull for unions
type HttpCode = 101 | 200 | 404 | 500
```