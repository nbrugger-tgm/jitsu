# JainParse

Jain parse is a lib to create your own parsers (and writers).<br>
Its main features are

* Custom functional parser building
* Defining parsers as files
* Stream/OnDemand Parsing (Sockets/InputStreams)
* Tokenization using _REGEX_
* Model Autogeneration

## Installation

Use _Maven_ (or Gradle) and add this as dependency

```xml
<dependency>
    <groupId>com.niton</groupId>
    <artifactId>jainparse</artifactId>
    <version>1.0.4</version>
</dependency>
```

## Terminology

* **Token**: a single character or multiple characters of the same type (Letters/Numbers)
* **Grammar**: The "rule/s" how a string should be parsed -> The structural description

## Example

These steps are in the order you are most likely to do when you create a parser

1. ### Building a Grammar
   #### With code
    ```java
    GrammarReference ref = new GrammarReferenceMap()
        .map(
            Grammar.build("Number")
                .token(DefaultToken.NUMBER).add("value")
        )
        .map(
            Grammar.build("calc_expression")
                .token(DefaultToken.BRACKET_OPEN).add()
                .grammar("expression").add("firstExpression")
                .tokens(DefaultToken.STAR, DefaultToken.PLUS, DefaultToken.MINUS, DefaultToken.SLASH).matchAny().add("calculationType")
                .grammar("expression").add("secondExpression")
                .token(DefaultToken.BRACKET_CLOSED).add()
        )
        .map(
            Grammar.build("expression")
                .grammars(new String[]{"Number", "calc_expression"}).matchAny().add("content")
        );
    ```
   #### As Grammar File

[File Generator](https://github.com/nbrugger-tgm/JainParse/blob/master/FileGenerator.md)
[Grammar Files](https://github.com/nbrugger-tgm/JainParse/blob/master/GrammarFiles.md)

## Todo

- [ ] Changing onGet - Generation of the autogen classes to - build on constructor
