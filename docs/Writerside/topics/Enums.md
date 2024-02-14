# Enums

Enums are a type that offer a limited choice of predefined options.
For differences to enums in other languages see ["but why not like in Rust"](#enums-in-other-langs)

For example

```Java
enum ProcessState {
    RUNNING,
    PAUSED,
    WAITING,
    IDLE,
    STOPPED;
}
var myProcessState = ProcessState.RUNNING;
```

## Default methods

Each enum value by default offers methods:

* ``name()``: returns the name of the regarding constant `ProcessState.IDLE.name() == "IDLE"`
* ``static byName(name: String)``: The inverse operation to `name`, `ProcessState.byName("IDLE") == ProcessState.IDLE`

## Implementing interfaces

Enums can implement interfaces, implicit and explicit. When an enum does this the methods are exposed to its constants:

```Java
enum ProcessState implements JSONSerializable {
    RUNNING,
    PAUSED,
    WAITING,
    IDLE,
    STOPPED;
    
    public fn toJson(): JSONValue {
        return new JSONString(name());
    }
}

var someState: JSONSerializeable = ProcessState.PAUSED; 
var pausedJsonValue = someState.toJson();
```

## Methods

Enums can define methods that are accessible on their constants

```Java
enum Signal {
    TERM,
    KILL,
    ...,
    IOT;
    
    public fn sendTo(ps: Process): int {
        return ps.signal(ps.name());
    }
}

var exitValue = Signal.KILL.sendTo(subProcess);
```

## Fields

An enum can also hav fields that have to be defined for each constant.

```Java
enum TicketState {
    OPEN {
        next: [CLOSED, IN_PROGRESS]
    },
    IN_PROGRESS {
        next: [DONE, REOPENED, CLOSED]
    },
    DONE {
        next: [REOPENED]
    },
    CLOSED {
        next: [REOPENED]
    },
    REOPENED {
        next: [IN_PROGRESS, CLOSED]
    };
    
    public next: TicketState[];
    
    public fn allowSwitchTo(state: TicketState): boolean {
        return next.contains(state);
    }
}
```
<tip>
    Fields in enums can <b>not</b> be mutable (<code>mut</code>)
</tip>

## Enum vs Unions {id="enum-vs-union"}

At first glance Enums do not provide anything over [union types](Unions.md). In general you should use Enums over value
unions (for example `type ProcessState = "RUNNING" | "PAUSED" | "WAITING" | "IDLE" | "STOPPED";`) since it is a lot less
explicit.

Also, a string value union is nothing more than a string in the end of the day. 
Which is not just worse for memory (an enum takes the smallest power of 2 bits to fit the number of constants). 
For example `ProcessStateEnum[8]` requires 8x3 bits = 3 bytes while `ProcessStateUnion[8]` takes 8x64bit = 64 (uint, because its 8 pointers),
but also it is not entirely explicit about intent. For example in this snippet `var x = "The user is WAITING".split(" ")[3];`
is `x` supposed to be `ProcessState` or just a string and an inconvenient match?



## Comparison to other languages {id="enums-in-other-langs"}

Enums as describes above are nearly a 1:1 copy from how java & C# enums work.

### Rust
In rust enums are not actually enums but _sum types_ (union types). If you want rust like enums see [unions](Unions.md).

### Typescript
In typescript enums are similar to jitsu enums, but have a few strong drawbacks due to the javascript restrictions.

* `number`s are assignable to enums (`let status: TicketState = 3`)
* Whatever this is
```Typescript
enum MoreConfusion {
  A,
  B = 2,
  C = "C"
}
```