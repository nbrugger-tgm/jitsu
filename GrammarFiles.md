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

OR

<any_except><array><name_assign>
```

 - Optionals:
	 - specification
	 - multi
	 - name_assign
 - #### specification
	  - `?` Optional: The `item` is optional, it is used if it exists but ignored if not
	 - `!` AnyExcept: Anything except `item` is used (multiple Times)
		 - Example
			 - `*END_OF_LINE` 
			 - will collect all Tokens bevore the `END OF LINE` Token
	 - `~` Ignore: Ignores `item` completely it it exists
 - #### item
	 - is a refence to a Token or grammar
	 - Syntax: `[#]reference`
	 - The `#` is used when reference is an token
	 - If there is no `#` it is an grammar
 - #### multi
	 - Simply a `*`
	 - Indicates that `item` is captured more or less than (or exactly) one time
 - #### name_assign
	 - Assigns a name to the rule
	 - `> <name_to_assign>`
	 - `name_to_assign` needs to be a valid identifier
	 - Whitespace between the arrow and the name is allowed also before the arrow
 - #### array
	 - `{item1 item2, item3...}`
	 - Indicates that any of the items in the array has to match
	 - you can ***not*** mix grammars and tokens
	 - #### any_except
		 - is indicated by a `!`
		 - negates the statment that any token except the ones from the array are matched

## Example
A full example (building a JSON parser) can be found here, a short one is bellow.
Some of the tokens used are pseudocode

```java
Grammer
	.build("String")
	.matchToken(Tokens.STRING_DELIMITER)
	.anyExcept(Tokens.STRING_DELIMITER,"content")
	.matchToken(Tokens.STRING_DELIMITER);

Grammer
	.build("VariableAssignment")
	.matchToken(Tokens.IDENTIFYER,"name")
	.ignoreToken(Tokens.WHITESPACE)
	.matchToken(Tokens.EQUAL)// EQUAL is =
	.match("String","value")
	.matchToken(Tokens.SEMICOLON);
```
The same as GRM file

    STRING_DELMITTER = '"'
    IDENTIFYER = '[A-Za-z_]+'
    WHITESPACE = '[ \t]+'
    EQUAL = '='
    SEMICOLON = ';'
    
    String:
        #STRING_DELMITTER
        *#STRING_DELMITTER >content
        #STRING_DELMITTER
    
    VariableAssignment:
        #IDENTIFYER > name
        ~#WHITESPACE
        #EQUAL
        String > value
        #SEMICOLON


​	    

<!--stackedit_data:
eyJoaXN0b3J5IjpbMTQ4OTc2MDEzNSwtOTU0NjY2ODE1LC0xMz
gyNzIxNTIxLDczMDkxMjM2OCwtMTI1MDAzMzA2Ml19
-->