# Grammar Files
Grammar Files are used to define a Grammar in a File reather than just cerate them with code.

## Syntax
### File Over All
```
[Token Definitions]
[Grammar]
```
### Token Definitions
```
<token_name> = '<regex>'
```

 - Spaces do not matter
 - only one token definition per line
#### Example
```
LINE_END='\r\n'
IDENTIFYER='[A-Za-z_]+'
```

	 

## Example

    Grammer.build("String").matchToken(Tokens.STRING_DELIMITER).anyExcept()

<!--stackedit_data:
eyJoaXN0b3J5IjpbNDE2NDA5OTUyLDczMDkxMjM2OCwtMTI1MD
AzMzA2Ml19
-->