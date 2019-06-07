# JainParse Code Generator
This generator generates the Source files to easy read a parsed Grammar.
## Installation
Very Easy! Use `Maven` and add this as dependency
```xml
<dependency>
    <groupId>com.niton</groupId>
    <artifactId>jainparse</artifactId>
    <version>1.0.4</version>
</dependency>
```
## Usage
As you have a valid Grammar; for example:
```java
Grammar string = Grammar.build("StringGrammar")
.matchToken(Tokens.STRING_DELIMITTER) // No name as we dont care about the delmitters
.anyExcept(Tokens.STRING_DELIMITTER,"value") //Named property to get it later on
.matchToken(Tokens.STRING_DELIMITTER); // No name as we dont care here to
```
Then you simply pass it into the Generator
```java
JPGenerator gen = new JPGenerator();
gen.setOutputDirecory("C:\\Users\\Example\\Workspace\\Project\\src");
gen.setPackage("com.niton.generated");
gen.generate(string);
```
### Result
```java
public class StringGrammar /*The name of the grammar*/ {
	private Sub
}
```
> Written with [StackEdit](https://stackedit.io/).
<!--stackedit_data:
eyJoaXN0b3J5IjpbLTE2NDY1OTcxNjBdfQ==
-->