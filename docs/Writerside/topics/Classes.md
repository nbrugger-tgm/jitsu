# Classes

Classes are a more feature rich version of [Structs](Structs.md). 

In contrast to structs they support:
- [Encapsulation](#encapsulation "The ability to hide fields or methods from outside of a class")
- Attaching methods
## Syntax

```Kotlin
class DbConnection {
    user: string;
    password: string;
    url: URL;
    connection: Socket;
    
    fn connect() {
        connection = PostgresDriver.connect(url, user, password);
    }
}
```

## Fields 

> Classes need to at least 1 field! If you feel the need for a class without field use a [Namespace](Namespace.md) instead!

As with everything in **jitsu** mutability is explicit, prefix a field with `mut` to make it mutable.

```Kotlin
class Person {
    mut firstName: string;
    mut lastName: string;
}
```

By default, fields are <tooltip term="private">private</tooltip> and have to be exposed explicitly
{id="encapsulation"}
```Kotlin 
class Person {
    public firstName: string;
    public lastName: string;
}
```

## Methods

Methods are functions that are executed in the context of an instance of that class accessible using `this`.
To learn more about the specific of functions and methods read the [regarding documentation on functions](Functions.md)
and [methods](Functions.md#methods)
## Inheritance

A class can inherit from a [Struct](Structs.md), class or [Interface](Interfaces.md)

### Embedding (Structs)

A [Struct](Structs.md) can be embedded. That means that the class has all fields that the struct has.
Other than with _extending_ the struct, instances of the class **are not** instances of the field

<compare style="left-right">
    <code-block lang="asp.net (c#)">
        struct Person {
            name: string;
        }
        class Employee {
            embed Person;
        }
    </code-block>
    <code-block>
        class Employee {
            name: string;
        }
    </code-block>
</compare>

Therefore, `var myEmployee: Person = new Employee()` would **not** work!

By default, the fields included via `embed` are <tooltip term="private">private</tooltip> and immutable!
To make them accessible from outside the class use `public embed <struct name>`, same goes for mutability.
To make the fields mutable _within this class_ use `mut embed <struct name>`, both modifiers can of course be combined
`public mut embed <struct name>`

Keep in mind that you can only make all fields public or private. With `mut` this works a little different!
When the struct is included without `mut` all fields are immutable, when included using `mut embed` only fields
that are marked as mutable in the struct itself are imported as mutable!

#### Example

<compare type="left-right" second-title="After compilation">
    <code-block lang="asp.net (c#)">
        struct Person {
            mut firstName: string;
            lastName: string;
        }
        class Employee {
            embed Person;
        }
    </code-block>
    <code-block>
        class Employee {
            firstName: string;
            lastName: string;
        }
    </code-block>
</compare>



<compare type="left-right" second-title="After compilation">
    <code-block lang="asp.net (c#)">
        struct Person {
            mut firstName: string;
            lastName: string;
        }
        class Employee {
            public mut embed Person;
        }
    </code-block>
    <code-block>
        class Employee {
            public mut firstName: string;
            public lastName: string;
        }
    </code-block>
</compare>

### Implementing (Interfaces)

Implementing an (explicit) interface has 2 effects:
 - You are forced to implement all non-default methods defined in the interface
 - Each instance of the class has the interface as assignable type

```Java
interface Connectable {
    fn connect(query: Map<String, String>)
}
class DbConnection implements Connectable {
    public fn connect(query: Map<String, String>){
        ....
    }
}
var connectable: Connectable = new DbConnection(...);
```

### Extending (Classes)

<tip>It has yet to be decided if class inheritance is a thing</tip>

## Constructors

Constructors are code blocks that initialize an instance of the given class.

By default, classes provide a public constructor that sets all fields without a default value and optional parameters for all default valued parameters.

```Typescript
class Person {
    name: string;
    gender: Gender = UNDEFINED;
}
```
The default constructor for this class looks like this
```Kotlin
public constructor(name: String, gender: Gender = UNDEFINED)
```

Adding a custom constructor removes the default constructor   

```Kotlin
class LinkedList<E> {
    start: Node<E>;
    end: Node<E>;
    
    public constructor(data: E[]) {
        val nodes: Node[] = linkUp(data);
        start = nodes[0];
        end = nodes[nodes.size - 1];    
    }
}
```

<warning>Constructors <b>must</b> be <a href="Pure-functions.md">pure</a></warning>

In general constructors should be very simple and contain no to very little logic.
If you need complex logic use factory methods instead.

```Kotlin
class LinkedList<E> {
    mut start: Node<E>;
    mut end: Node<E>;
    
    public static fn fromArray<E>(data: E[]): LinkedList<E> | Error {
        var nodes: Node<E>[] = linkUp(data);
        var start = nodes[0];
        var end = nodes[nodes.size - 1];
        return new LinkedList<>(start, end);
    }
}
```
The reason to consider using factory methods over constructors are:
- The names of methods can be more descriptive and add ease of use
- Methods can utilize more diverse return types, to return error types for example


In the example above you most likely want to make the constructor of `LinkedList` private in order to do this use
```
class LinkedList<E> {
    mut start: Node<E>;
    mut end: Node<E>;
    private constructor;   
    public fn fromArray<E>(data: E[]): LinkedList<E> | Error {
        ...
    }
}
```
this way restricts the user to create linked lists using your factory