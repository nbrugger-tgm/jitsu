# JSON
This is a full tutorial how to build a complete parser, in this case JSON as it is simple but covers everything except reursion this API is able to do.
## Preparation
We need the official JSON syntax, which is found [here](http://json.org)

Then we will need to decide what our parser should produce, the 2 most common ways are -> deserialisation or flexible model
### Deserialisation
Means that we have POJOs and we are shure the JSON follows the structure of the POJO and fill an instance of it with the JSON values via reflections. This is very very usefull and every professional Parser has such a feature but it is very time consuming and an extreme overkill for an tutorial
### Flexible Model
In this approach we create Classes like JsonArray JsonObject JsonValue and so on which are filled with flexible structures like HashMaps or Lists, means that we can fill this lists and maps up with everything we like to. This causes much of Casting and instanceof later on but is simple to realize at the parsing stage
> So we would clearly use the **Flexible Model**

Now we need to find the information how the model works.
 I will not explain this detailed as this are very trivial steps. Simply create Classes that represent all the elements which may ocour in a JSON.
 This is the Structure i will use:
 - `Json` (Top level container)
 -  `String` (sub of `Value`)
 - `Number` (sub of `Value`)
 - `Value`  (marking interface)
 - `Member`
 - `Array` (sub of `Value`, sub of `Element`)
 - `Object` (sub of `Value`, sub of `Element`)
 - `Element` (marking interface)

depending on how you design your Parser you may also create

 - `True` (sub of `Value`)
 - `False` (sub of `Value`)
 - `Null` (sub of `Value`)
 - `Members` (collection of `Member`s, used within Object)
 - `Elements` (collection of `Element`s, used within Array)

We won't do it in here for time reasons but normaly you would make this extra step of abstration
> We are done with the preperation!
## Grammar File
Now we create the main part of the Parser, the [Grammar File](https://github.com/nbrugger-tgm/JainParse/blob/master/GrammarFiles.md)
### Adding Tokens
There are a few basic tokens nearly every parser will need.
```js
    LINE_END_WIN='\r\n'
	LINE_END_UNIX='\n'
	LINE_END_OSX='\r'
	CHRACTERS='[A-Za-z]+'
	UNDERSCORE='_'
	EQUAL='[=]'
	WHITESPACE='[ \t]+'
	SLASH='\/'
	COMMA=','
	COLON=':'
	POINT='\.'
	EOF='\Z'
```
> Do **NOT**, use the `$` and `^` regex as they do not capture th \r or \n which is very neccesarry for my Parser to work!

> Also do not create Tokens which are overlapping (eg. TOK1='[A-Za-züöäß]+' NAME='[A-Z][a-z]+') and define them as simple as possible 

> At least avoid to allready specify the chars to a specific role in the parser except the char is really only used for one purpose like '[' as 'START_OBJECT'

> Written with [StackEdit](https://stackedit.io/).
<!--stackedit_data:
eyJoaXN0b3J5IjpbMTIxNDk1NzczMSwxNjk4MDE2MzM4XX0=
-->