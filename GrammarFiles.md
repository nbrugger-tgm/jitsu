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
<token_name> = '<regex>
```

 - Spaces do not matter

	 

## Example

    Grammer.build("String").matchToken(Tokens.STRING_DELIMITER).anyExcept()

<!--stackedit_data:
eyJoaXN0b3J5IjpbMTU3MTM5MDI0Miw3MzA5MTIzNjgsLTEyNT
AwMzMwNjJdfQ==
-->