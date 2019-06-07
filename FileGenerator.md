# JainParse Code Generator
This generator generates the Source files to easy read a parsed Grammar.


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
//removed imports as they are not neccesary here
public class StringGrammar /*The name of the grammar*/ {
	private SubGrammarObject obj;
	public StringGrammar(SubGrammarObject obj){
		this.obj = obj;
	}
	public String getValue(){
		//some code you do not need to know
	}
}
```
So you can simply parse and use like this
```java
//Building grammar
Grammar string = Grammar.build("StringGrammar")
.matchToken(Tokens.STRING_DELIMITTER)
.anyExcept(Tokens.STRING_DELIMITTER,"value")
.matchToken(Tokens.STRING_DELIMITTER);

//Parse into object
Parser p = new Parser(string);
SubGrammarObject obj = p.parse("\"Ich bin ein String\"");

//Read with generated Code (obviusly you need to generate it first)
StringGrammar parsed = new StringGrammar(obj);
System.out.println(parsed.getValue()); //output: Ich bin ein String
```
> Written with [StackEdit](https://stackedit.io/).
<!--stackedit_data:
eyJoaXN0b3J5IjpbLTEzNjE0ODU1NjVdfQ==
-->