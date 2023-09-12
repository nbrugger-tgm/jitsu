# Jitsu

Jitsu is a experimental programming language that is designed to learn about the creation programming lanugages
and explore interesting ideas in compiler development. So be warned - don't use it for anything serious.

## Idea and philisophy
The main objective of this language is "everything that can be done at compile time will be done at compile time". It will offer many ways of automatic as well as manual compile time Features such as [compile time constant computation], [compile time literals], [compile time identifiers], [attribute processors], [compile time Code builders] and [custom syntax plugins]

The other Main points of the language are memory safety, explicit mutability and static typing

While this might seem like feature creep (might be) a high priority is that the language is easy to read, write and understand 

## Features

- [ ] Static types / Typesafe
- [ ] Null safety / no null
- [ ] Sum types
- [ ] explicit mutability
- [ ] compiles to native (LLVM)
- [ ] Default values for function parameters

## Blazingly fast?

Probably not. The goal is to learn and explore interesting ideas in compiler development not how to write the fastest
compiler (like v) or optimizer (like zig/rust/c).

## Syntax

> This is the vision how it should look like in the end!

```
fn main() {
    const x = 1;
    const y = 2;
    var z = x;
    z += y;
    println(z);
}
```

Type declaration
```
var y : File;
```

## Features

### compile time constant computation

When variables can be preprocessed at compile time they will be. So the following code
```rust
var allowedChars = "aeiou".charArray()
```
Will compile to
```rust
var allowedChars = ['a', 'e', 'I', 'o', 'u']
```
But also more complex expressions will be pre computed
```rust
var magicMike = "${Math.pow(8, 2)}\nYes yes".split("\n")[2]
```
This would compile to a string "72"

You can even use your own functions! 
```rust
var compileTimeNumber = compute_me(12);
var anotherConstant = 2*2;
fn compute_me(num: int) : {
    return "${num/another Constant}".lenght;
}
```
this would lead to the constant 12/4 = "3" = length = 1 and the compute me function not being included in the production binary since it's only needed in compiling 

For functions to be precomputed they need to be *pure*(no side effects) and deterministic 

This works for any type - custom struts, strings, enums, unions - just anything 
### compile time literals

Compile time literals are the manual way of compile time constant computation. Here you can (explicitly) compute an arbitrary value at compile time and use it as a literal

Examples
```vlang
var compiled_at = $( unix_timestamp() ) 
var run_at = unix_timestamp()
```
So the `$()` forces compile time execution. Again any complex expression can be used in the brackets, your own functions too,even if they are non-deterministic (they sill can not have side-effects) 

You also can use controll flows

```vlang
var included_image = $(
     var images = file("./images/").find(img -> img.filename.ends_with(".png"));
    if(images is IOError) yield ["no files found"];
    yield images.map(it -> it.filename).to_array();
) 
```
 This would result in included images being an array with the names of the images found in "images" at compile time
     
### compile time identifiers

Compile time identifier look similar to compile time literals but work differently. 

Here the goal is to enable dynamic identifiers like given in this example (this is a example you should never EVER do!) 
```rust
fn fast_file_save(f: File) {
    f.$(random_boolean()? "delete" : "create_file")() 
}
```
This will compile to either `f.delete()` or `f.create_file()` depending on what the random boolean is at compile time. 

While it should be painfully obvious why you do not want to do that this feature will come in handy with the other examples

But even standalone this can be a useful tool

```
fn name_of(object: NamedStruct) : i64 {
    return object.$(is_test_context? "debug_symbol" : "name");
} 
```
In this case on compile depending on test or production build `object.debug_symbol` or `object.name` is the compiled result. 

### attribute processors

Attributes look like this
```
[Attribute] 
[Attribute2(format="json")] 
fn do_something() {...} 
```

Attribute processors allow you to scan, create new (and modify code  under restricted circumstances) 

This allows for powerful systems like compile time dependency injection, ORMs, documentation generators and so on

> the API for attribute processors is not yet decided on but it will be similar to Java annotation processors but way simpler to use


### compile time Code builders

This is the most unique type of compile time processing and is stolen from the V language so shoutout to them

Here is an example how this could be used to implement a compile time, typesafe json writer

```
fn to_json<T>(object: T) : String {
    $if(T.name == "string") {
        return "\"${object}\""
    } $else if(T.name == "int") {
        return object.to_string()
    } $else if(T.kind == STRUCT) {
        var objString = "{";
        $for(var field : T.fields){
            objString += "$(field.name): ${to_json(object.$(field.name))}," //yes the last comma is not stripped for simplicity
       }
       objString +="}"
       return objString
   }  $else if(T.kind == ARRAY) {
        var objString = "[";
        for(var item : object){
            objString += to_json(item)+", "
       }
       objString +="]"
   } 
} 
```

Now assuming we call the function like this:
```
struct SomeDTO {
    id: int
    name: String
}
var me : SomeDTO = {
    id: 12,
    name: "Max" 
} 
to_json(me)
```

The code will compile to the following:
```
fn to_json(object: String) : String {
    return "\"${object}\"";
} 
fn to_json(object: int) : String {
    return object.to_string();
}

fn to_json(object: SomeDTO) : String {
    var objString = "{";
    objString += "id: ${to_json(object.id)}," //yes the last comma is not stripped for simplicity
    objString += "name: ${to_json(object.name)}," //yes the last comma is not stripped for simplicity
    objString +="}"
    return objString
}
 ```

You see only the types it is called with are generated and the fields of the struct are inlined and accessed using **compile time  identifiers** 
### custom syntax plugins
 This is for extension syntax like JSX

```
fn my_web_component() : HtmlElement {
   return !jsx {
      <h1>Hallo Welt</h1>
   } 
} 
```
The code this compiles to is not yet clear but probably something like this
```
var h1_1 : Html Element = HTML.h1();
return h1_1;
```

API details are to be determined 