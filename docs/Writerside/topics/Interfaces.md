# Interfaces

Interfaces are contracts that types and values can fulfill in order to allow more abstract access to resources.

There are three types of interfaces: [explicit](#explicit), [implicit](#implicit) and [structural](#structural)


## Explicit interfaces {id="explicit"}

Explicit interfaces are well known from languages such as Rust, Java or C#.
This interface have to be _explicitly_ implemented by [classes](Classes.md) or [enums](Enums.md).

```Java
interface DbDriver {
    fn connect(parameters: Map<String,String> = emptyMap()): Socket;
}

class PostgreSQLDriver implements DbDriver {
    fn connect(parameters: Map<String,String> = emptyMap()): Socket {
        ... some postgres specifiv implementation ...
    }
}
```

## Implicit interfaces {id="implicit"}

Other than explicit interfaces you do not have to _explicitly_ define that a type is implementing this interfaces.
The interface is "automatically" implemented by all types having a method with the matching functions/methods.

```Kotlin
implicit interface Cloneable {
    fn clone(): this;
}
fn sendToProcessingQueue(element: Cloneable) {
    processingQueue.enque(element.clone());
}
class Person {
    name: string;
    public fn clone(): Person {
        return new Person(name);
    }
}

sendToProcessingQueue(new Person("Bert"))
```

#### When to use implicit or explicit interfaces?

In general explicit interfaces should be more specific in what they do, 
so that you cannot "accidentally" implement the interface. 
On the other hand implicit interfaces are aimed at "common functionality".

If you have trouble finding out if you want implicit or explicit interfaces ask the following questions:
1. Would it make sense if every class implemented this interface?
   * **Yes**: implicit
   * **No** : explicit
2. Can you take any random class and implement the methods for this class?
   * **Yes**: implicit
   * **No** : explicit
3. Would one create a class for the sole purpose of having an implementation of the interface?
   * **Yes**: explicit
   * **No** : implicit

**Examples (implicit)**
 - Serializable
 - Cloneable
 - Closeable

**Examples (explicit)**
 - DatabaseDriver
 - Collection
 - Stream

## Structural interfaces {id="structural"}

Structural interfaces are contracts about the _structure_ of an object rather than the exposed methods.
This means that you define the [fields](Classes.md#fields) of classes and structs that need to be publicly accessible.

<chapter title="Named">
   <pre>
       <code-block lang="typescript">
         type TypedSQLValue = {value: String, type: String};
         fn encodeSqlValue(value: TypedSQLValue): String {
             return "$\{value.value}::$\{value.type}";
         }
       </code-block>
   </pre>
</chapter>
<chapter title="Anonymous">
   <pre>
       <code-block lang="typescript">
       fn encodeSqlValue(value: {value: String, type: String}): String {
           return "$\{value.value}::$\{value.type}";
       }
       </code-block>
   </pre>
</chapter>

This works great in conjunction with [anonymous structs](Structs.md#anonymous-structs)!
```Typescript
encodeSqlValue({value: "f50a96b2-f13e-465b-9901-62a87618b787", type: "uuid"});
//or alternatively
var myUUID = {value: "f50a96b2-f13e-465b-9901-62a87618b787", type: "uuid"};
encodeSqlValue(myUUID);
```

A structural interface can also dictate mutability
```Typescript
type Indexed = { mut index: int }
```
