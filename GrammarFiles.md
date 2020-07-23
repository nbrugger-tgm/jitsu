# Grammar Files
Grammar Files are used to define a Grammar in a File reather than just cerate them with code.

## Syntax
### File Over All
```
[Token Definitions]
[Grammars]
```
### Token Definitions
```
<token_name> = '<regex>'
```

 - Spaces do not matter
 - only one token definition per line
 - The `regex` Delimitter is `'` to use it within a regex use `\'`
 - `token_name` may contain letters and underscores
 - Example
	```
	LINE_END='\r\n'
	IDENTIFYER='[A-Za-z_]+'
	```
### Grammars
```
<grammar_name> :
	rule
	rule2
	...
```
 - Spaces and Tabs do not matter at any point between items
 - So the intendation is not neccessary
 - A grammar might have as many rules as you like
 - `grammar_name` may contain letters and underscores

### Rule
```
<specification><item><multi><name_assign>
```

 - Optional:
	 - specification
	 - name_assign
	 - multi
	 
 - #### specifications
	 - `?` Optional: The `item` is optional, it is used if it exists but ignored if not
	 - `!` AnyExcept: Anything except `item` is used (multiple Times)
		 - Example
			 - `!END_OF_LINE` 
			 - will collect all Tokens bevore the `END_OF_LINE` Token
	 - `~` Ignore: Ignores `item` completely it it exists
	 - An empty specification indicates to simply match the `item` (in case of an *array* this means it matches the first matchable item)
 - #### item
	 - is a refence to a Token ,grammar or an array
	 - Syntax: `[#]reference` or `<array>`
	    - The ``reference`` is used for grammars and tokens
	 - The `#` is used when reference is a token
	 - If there is no `#` it is a grammar
	 - #### array
     	 - `{item1 item2, item3...}`
     	 - Indicates that any of the items in the array has to match -> `OR`
     	 - you can ***not*** mix grammars and tokens
 - #### multi
     - Symbol: `*`
     - Indicates that the specification will be used more or less than (or exactly) one time (0..N)
     - note that this does not makes sense when used with `!` (Any Except)
 - #### name_assign
	 - Assigns a name to the rule
	 - `> <name_to_assign>`
	 - `name_to_assign` needs to be a valid identifier
	 - Whitespace between the arrow and the name is allowed also before the arrow

## Example
A full example (building a JSON parser) can be found here, a short one is bellow.
Some of the tokens used are pseudocode

```java
Grammar
	.build("String")
	.token(Tokens.STRING_DELIMITER).match()
	.token(Tokens.STRING_DELIMITER).anyExcept().name("content")
	.token(Tokens.STRING_DELIMITER).match();

Grammar
	.build("VariableAssignment")
	.token(Tokens.IDENTIFYER).match().name("name")
	.token(Tokens.WHITESPACE).ignore()
	.token(Tokens.EQUAL).match()// EQUAL is =
	.grammar("String").match().name("value")
	.token(Tokens.SEMICOLON).match();
```
The same as GRM file

    STRING_DELMITTER = '"'
    IDENTIFYER = '[A-Za-z_]+'
    WHITESPACE = '[ \t]+'
    EQUAL = '='
    SEMICOLON = ';'
    
    String:
        #STRING_DELMITTER
        !#STRING_DELMITTER >content
        #STRING_DELMITTER
    
    VariableAssignment:
        #IDENTIFYER > name
        ~#WHITESPACE
        #EQUAL
        String > value
        #SEMICOLON
