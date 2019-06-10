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

<array><name_assign>
```

 - Optionals:
	 - specification
	 - multi
	 - name_assign
 - #### specification
	 - `~` Ignore: Ignores `item` completely it it exists
	 - `?` Optional: The `item` is optional, it is used if it exists but ignored if not
	 - `*` AnyExcept: Anything except `item` is used
	 - `~` Ignore: Ignores `item` completely it it exists

## Example

    Grammer.build("String").matchToken(Tokens.STRING_DELIMITER).anyExcept()

<!--stackedit_data:
eyJoaXN0b3J5IjpbLTk4OTE2Nzc4NCw3MzA5MTIzNjgsLTEyNT
AwMzMwNjJdfQ==
-->