package eu.nitok.jitsu.parser

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.parser.ast.StatementNode.NamedTypeDeclarationNode.TypeAliasNode
import eu.nitok.jitsu.parser.ast.TypeNode
import eu.nitok.jitsu.parser.parsers.parseExplicitType
import eu.nitok.jitsu.parser.parsers.parseIdentifier
import eu.nitok.jitsu.parser.parsers.parseType
import eu.nitok.jitsu.parser.parsers.parseTypeDeclaration
import eu.nitok.jitsu.parser.parsers.parseTypeParameterDefinition
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Type Parsing")
class TypeParserTest : ParsingTest() {
    @Nested
    @DisplayName("parseExplicitType()")
    inner class ParseExplicitType : MethodTest<TypeNode>() {
        @Test
        fun requiresColonOnStart() {
            var type = parseExplicitType(tokenize("i64"), messages)
            assertThat(type)
                .`as`("Without colon the type is still parsable")
                .isNotNull()
            assertThat(messages.errors)
                .singleElement()
                .extracting { it.message }
                .asString()
                .contains(":")
        }

        @Test
        fun shouldParseValidText() {
            val type = parseExplicitType(tokenize(":i64"), messages)
            assertThat(type)
                .isNotNull()
            assertThat(messages.errors)
                .isEmpty()
            assertThat(messages.warnings)
                .isEmpty()
            assertThat(type?.warnings)
                .isEmpty()
            assertThat(type?.errors)
                .isEmpty()
            assertThat(type)
                .isInstanceOf(TypeNode.IntTypeNode::class.java)
                .extracting { (it as TypeNode.IntTypeNode).bitSize }
                .isEqualTo(BitSize.BIT_64)
        }

        @Test
        fun shouldParseTextWithSpaces() {
            val type = parseExplicitType(tokenize(": \ti64"), messages)
            assertThat(type)
                .isNotNull()
            assertThat(messages.errors)
                .isEmpty()
            assertThat(messages.warnings)
                .isEmpty()
            assertThat(type?.warnings)
                .isEmpty()
            assertThat(type?.errors)
                .isEmpty()
            assertThat(type)
                .isInstanceOf(TypeNode.IntTypeNode::class.java)
                .extracting { (it as TypeNode.IntTypeNode).bitSize }
                .isEqualTo(BitSize.BIT_64)
        }

        @Test
        fun shouldProduceErrorWhenNonTypeAfterColon() {
            val res = parseExplicitType(tokenize(": []"), messages)
            assertThat(res).isNull()
            assertThat(messages.errors)
                .`as`("The type def is incomplete since the type afterwards is not valid")
                .isNotEmpty()
        }

        @Test
        fun shouldProduceErrorWhenMissingType() {
            val res = parseExplicitType(tokenize(":"), messages)
            assertThat(res).isNull()
            assertThat(messages.errors)
                .`as`("The type def is incomplete since the type afterwards is not valid")
                .isNotEmpty()
        }


        @Test
        fun shouldReturnErrornousTypeWhenIncomplete() {
            val res = parseExplicitType(tokenize(": Array<i64"), messages)
            assertThat(res).isNotNull()
            assertThat(messages.errors)
                .`as`("The errors should be part of the type node not the messages")
                .isEmpty()
            assertThat(res?.errors)
                .`as`("The errors should be part of the type node")
                .isNotEmpty()
        }

        override fun parseMethod(input: String) = parseExplicitType(tokenize(input), messages)

        override fun fullyValidInputs() = listOf(
            ":i64",
            ":   i64",
            ":\ti64",
            ":Array<i64>",
            ": Array<  i64  >",
            ": A | B",
            ": A | B | Array<i64>",
            ":A|B|Array<i64>"
        )

        override fun invalidInputs() = listOf(
            " ",
            "<:",
            " | A",
            ":",
            ": | A",
            ": \t "
        )

        override fun partiallyValidInputs(): List<Input> = listOf(
            Input("A", 1),
            Input(": 1Array", 1),
            Input(": A | ", 1),
            Input("A | ", 2),
            Input("Array<i64>", 1),
            Input("A:B", 1)
        )
    }

    @Nested
    @DisplayName("parseTypeParameterDefinition()")
    inner class ParseTypeParameterDefinition {
        @ParameterizedTest
        @ValueSource(strings = ["<>", "< >", "<    >", "< \t >"])
        fun shouldParseEmptyTypeParameters(txt: String) {
            val x = parseTypeParameterDefinition(tokenize(txt), messages)
            assertThat(x)
                .isNotNull()
                .isEmpty()
            assertThatNoErrors()
            assertThatNoWarnings()
        }

        @ParameterizedTest
        @ValueSource(strings = ["<a,b>", "<a ,b>", "<   a,b >", "< \ta,b,b,b >"])
        fun shouldParseMultipleTypeParameters(txt: String) {
            val x = parseTypeParameterDefinition(tokenize(txt), messages)
            assertThat(x)
                .isNotNull()
                .hasSizeGreaterThan(1)
                .let {
                    it.first().extracting { it.value }.isEqualTo("a")
                    it.last().extracting { it.value }.isEqualTo("b")
                }
            assertThatNoErrors()
            assertThatNoWarnings()
        }

        @ParameterizedTest
        @ValueSource(strings = ["<", "< ", "<    ", "< \t "])
        fun shouldNotFailOnMissingClosingWithoutItem(txt: String) {
            val x = parseTypeParameterDefinition(tokenize(txt), messages)
            assertThat(x)
                .isNotNull()
                .isEmpty()
            assertThat(messages.errors)
                .hasSizeGreaterThan(0)
                .first()
                .extracting { it.message }.asString()
                .contains(">")
            assertThatNoWarnings()
        }

        @ParameterizedTest
        @ValueSource(strings = ["<a", "<a ", "<   a ", "< \ta "])
        fun shouldNotFailOnMissingClosingWithItem(txt: String) {
            val x = parseTypeParameterDefinition(tokenize(txt), messages)
            assertThat(x)
                .isNotNull()
                .hasSize(1)
                .first()
                .extracting { it.value }
                .isEqualTo("a")
            assertThat(messages.errors)
                .hasSizeGreaterThan(0)
                .first()
                .extracting { it.message }.asString()
                .contains(">")
            assertThatNoWarnings()
        }

        @Test
        fun shouldReturnNullWhenOpeningBracketIsMissing() {
            val x = parseTypeParameterDefinition(tokenize("i64"), messages)
            assertThat(x).isNull()
            assertThatNoWarnings()
            assertThatNoErrors()
        }

        @ParameterizedTest
        @ValueSource(strings = ["<a>", "<a >", "<   a >", "< \ta >"])
        fun shouldParseSingleTypeParameters(txt: String) {
            val x = parseTypeParameterDefinition(tokenize(txt), messages)
            assertThat(x)
                .isNotNull()
                .hasSize(1)
                .first()
                .extracting { it.value }
                .isEqualTo("a")
            assertThatNoErrors()
            assertThatNoWarnings()
        }

        @ParameterizedTest
        @ValueSource(strings = ["<a b c>", "<a\tb c>", "<a b \t c>"])
        fun shouldReturnValidListEvenWithCommasMissing(text: String) {
            val nodes = parseTypeParameterDefinition(tokenize(text), messages)
            assertThat(nodes)
                .isNotNull()
                .hasSize(3)
            assertThat(messages.errors)
                .hasSize(2)
        }

        @ParameterizedTest
        @ValueSource(strings = ["<a b c", "<a\tb c", "<a b \t c"])
        fun shouldReturnValidListEvenWithCommasAndClosingBracketMissing(text: String) {
            val nodes = parseTypeParameterDefinition(tokenize(text), messages)
            assertThat(nodes)
                .isNotNull()
                .hasSize(3)
            assertThat(messages.errors)
                .hasSizeGreaterThanOrEqualTo(3)
        }
    }

    @Nested
    @DisplayName("parseTypeDeclaration()")
    inner class ParseTypeDeclaration {
        @ParameterizedTest
        @ValueSource(strings = ["My = i64", "My:i64", "My i64"])
        fun shouldReturnNullWhenTypeKeywordMissing(text: String) {
            val res = parseTypeDeclaration(tokenize(text), listOf())
            assertThat(res).isNull()
        }

        @Test
        fun shouldParseSimpleTypeDeclaration() {
            val res = parseTypeDeclaration(tokenize("type My = i64"), listOf())
            assertThat(res)
                .isNotNull()
                .asInstanceOf(type(TypeAliasNode::class.java))
                .extracting { it.type }.usingRecursiveComparison()
                .isEqualTo(parseType(tokenize("          i64").skipWhitespace()))
            assertThat(res?.name).usingRecursiveComparison()
                .isEqualTo(parseIdentifier(tokenize("     My").skipWhitespace()))
            assertThat(res?.warnings).isEmpty()
            assertThat(res?.errors).isEmpty()
        }

        @Test
        fun shouldParseGenericTypeDeclaration() {
            val res = parseTypeDeclaration(tokenize("type Box<T> = T"), listOf())
            assertThat(res)
                .asInstanceOf(type(TypeAliasNode::class.java))
                .let {
                    it.extracting { it.type }.isInstanceOf(TypeNode.NameTypeNode::class.java)
                    it.extracting { it.name }.usingRecursiveComparison()
                        .isEqualTo(parseIdentifier(tokenize("     Box").skipWhitespace()))
                    it.extracting { it.typeParameters }
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .isNotNull().hasSize(1)
                }
        }

        @Test
        fun shouldProduceErrorWhenEqualsMissing() {
            val res = parseTypeDeclaration(tokenize("type My i64"), listOf())
            assertThat(res)
                .`as`("Even with missing = the parser should still return a node but attach errors to it")
                .isNotNull()
            assertThat(res)
                .asInstanceOf(type(TypeAliasNode::class.java))
                .extracting { it.type }
                .asInstanceOf(type(TypeNode.IntTypeNode::class.java))
                .extracting { it.bitSize }
                .isEqualTo(BitSize.BIT_64)
            assertThat(res?.name).usingRecursiveComparison()
                .isEqualTo(parseIdentifier(tokenize("     My").skipWhitespace()))
            assertThat(res?.errors)
                .isNotEmpty()
                .first()
                .extracting { it.message }
                .asString()
                .contains("=")
        }

        @ParameterizedTest
        @ValueSource(strings = ["type = i64", "type   = i64", "type=i64"])
        fun shouldProduceErrorWhenMissingName(text: String) {
            val res = parseTypeDeclaration(tokenize(text), listOf())
            assertThat(res)
                .`as`("Even with missing = the parser should still return a node but attach errors to it")
                .isNotNull().let {
                    it.extracting { it?.name }.isNull()
                }
            assertThat(res?.errors)
                .isNotEmpty()
                .first()
                .extracting { it.message }
                .asString()
                .contains("name")
        }

        @ParameterizedTest
        @ValueSource(strings = ["type My =", "type   My=     "])
        fun shouldProduceErrorWhenMissingType(text: String) {
            val res = parseTypeDeclaration(tokenize(text), listOf())
            assertThat(res)
                .`as`("Even with missing the type itself the parser should still return a node but attach errors to it")
                .isNotNull().asInstanceOf(type(TypeAliasNode::class.java)).let {
                    it.extracting { it?.type }.isNull()
                    it.extracting { it?.name?.value }.isEqualTo("My")
                }
            assertThat(res?.errors)
                .isNotEmpty()
                .first()
                .extracting { it.message }
                .asString()
                .contains("type")
        }

        @Test
        fun shouldParseEvenWithJustTypeKeyword() {
            val res = parseTypeDeclaration(tokenize("type"), listOf())
            assertThat(res)
                .`as`("Even with just the type keyword the parser should still return a node but attach errors to it")
                .isNotNull().asInstanceOf(type(TypeAliasNode::class.java)).let {
                    it.extracting { it?.name }.isNull()
                    it.extracting { it?.type }.isNull()
                }
            assertThat(res?.errors)
                .isNotEmpty()
                .hasSizeGreaterThanOrEqualTo(3)
        }
    }

    @Nested
    @DisplayName("parseType()")
    inner class ParseType {
        @Nested
        @DisplayName("UNION")
        inner class UnionType : MethodTest<TypeNode.UnionTypeNode>() {
            override fun parseMethod(input: String) = parseType(tokenize(input)) as? TypeNode.UnionTypeNode

            override fun fullyValidInputs() = listOf(
                "A | B",
                "A|B",
                "A |B | C",
                "A |  \tB | C",
                "A|B|C",
                "Array<i64> | B | C",
                "Array<i64>|B|C",
                "Array<i64>|Array<A | B | C>|C"
            )

            override fun invalidInputs() = listOf(
                "| A",
                "| A | B",
            )

            override fun partiallyValidInputs(): List<Input> = listOf(
                Input("A | ", 1),
                Input("A | B | ", 1),
                Input("A | 12 | ", 2),//12 is not a valid type YET
            )

            @Test
            fun parsesSubTypesCorrectly() {
                val res = parseType(tokenize("A | Array<i64> | B"))
                assertThat(res)
                    .isNotNull()
                    .isInstanceOf(TypeNode.UnionTypeNode::class.java)
                    .extracting { (it as TypeNode.UnionTypeNode).types }
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .hasSize(3)
                    .let {
                        it.first().isInstanceOf(TypeNode.NameTypeNode::class.java)
                            .extracting { (it as TypeNode.NameTypeNode).name.value }
                            .isEqualTo("A")
                        it.element(1).isInstanceOf(TypeNode.NameTypeNode::class.java)
                            .extracting { (it as TypeNode.NameTypeNode).name.value }
                            .isEqualTo("Array")
                        it.last().isInstanceOf(TypeNode.NameTypeNode::class.java)
                            .extracting { (it as TypeNode.NameTypeNode).name.value }
                            .isEqualTo("B")
                    }
            }

        }

        @Nested
        @DisplayName("PRIMITIVE")
        inner class PrimitiveTypes : MethodTest<TypeNode.PrimitiveTypeNode>() {
            override fun parseMethod(input: String) = parseType(tokenize(input)) as? TypeNode.PrimitiveTypeNode

            override fun fullyValidInputs() = listOf(
                "i8", "i16", "i32", "i64", "i128",
                "u8", "u16", "u32", "u64", "u128",
                "f32", "f64", "boolean", "int", "float"
            )

            override fun invalidInputs(): List<String> = listOf(
                "integer", "bool", "bolean", "i9", "u33", "f512"
            )

            override fun partiallyValidInputs() = emptyList<Input>()

            @ParameterizedTest
            @CsvSource(
                """
                int, i32
                float, f32
            """
            )
            fun primitiveAliasesShouldMapCorrectly(input: String, perciseType: String) {
                val res = parseType(tokenize(input))
                assertThat(res)
                    .isNotNull()
                    .isInstanceOf(TypeNode.PrimitiveTypeNode::class.java)
                    .extracting { it.toString() }
                    .isEqualTo(perciseType)
            }

            @ParameterizedTest
            @CsvSource(
                """
                i8, 8
                i16, 16
                i32, 32
                i64, 64
                i128, 128
                u8, 8
                u16, 16
                u32, 32
                u64, 64
                u128, 128
                f32, 32
                f64, 64
                boolean, 1
                int, 32
                float, 32
            """
            )
            fun bitSizeIsReturnedCorrectly(input: String, bits: Int) {
                val res = parseType(tokenize(input))
                assertThat(res)
                    .isNotNull()
                    .isInstanceOf(TypeNode.PrimitiveTypeNode::class.java)
                    .extracting { (it as TypeNode.PrimitiveTypeNode).bitSize }
                    .isEqualTo(BitSize.byBits(bits))
            }

            @ParameterizedTest
            @ValueSource(strings = ["i8", "i16", "i32", "i64", "i128"])
            fun shouldReturnSignedIntegerTypes(input: String) {
                val res = parseType(tokenize(input))
                assertThat(res)
                    .isNotNull()
                    .isInstanceOf(TypeNode.IntTypeNode::class.java)
            }

            @ParameterizedTest
            @ValueSource(strings = ["u8", "u16", "u32", "u64", "u128"])
            fun shouldReturnUnsignedIntegerTypes(input: String) {
                val res = parseType(tokenize(input))
                assertThat(res)
                    .isNotNull()
                    .isInstanceOf(TypeNode.UIntTypeNode::class.java)
            }

            @ParameterizedTest
            @ValueSource(strings = ["f32", "f64", "f128"])
            fun shouldReturnFloatTypes(input: String) {
                val res = parseType(tokenize(input))
                assertThat(res)
                    .isNotNull()
                    .isInstanceOf(TypeNode.FloatTypeNode::class.java)
            }
        }

        @Nested
        @DisplayName("ARRAY")
        inner class ArrayType : MethodTest<TypeNode.ArrayTypeNode>() {
            override fun parseMethod(input: String) = parseType(tokenize(input)) as? TypeNode.ArrayTypeNode

            override fun fullyValidInputs() = listOf(
                "SomeType[]",
                "SomeType[10]",
                "List<i64[]>[]",
                "Array<Array<u8>[256]>[128]",
                "Matrix<f32>[10]",
                "Matrix<f32>[10][][]",
                "Matrix<f32>[10][][10]",
//Requires comptime constant resolution
//                "Matrix<f32>[10*10]",
//                "Matrix<f32>[10*10][][]",
//                "Matrix<f32>[10*10][][abc+10]",
            )

            override fun invalidInputs() = listOf<String>()

            override fun partiallyValidInputs(): List<Input> = listOf(
                Input("A[", 1),
                Input("A[10", 1),
                Input("A[10,", 1),
//Requires comptime constant resolution
//                Input("A[Variabl", 1),
//                Input("A[Variabl*10+10", 1),
                Input("A<B>[", 1),
                Input("A<B>[10", 1),
                Input("A<B>[10,", 1),
//Requires comptime constant resolution
//                Input("A<B>[Variabl", 1),
//                Input("Muli[][[]", 1)
            )

            @Test
            fun returnsMutliDimensionalArrayTypeCorrectly() {
                val res = parseType(tokenize("Matrix<f32>[][10][]"))
                assertThat(res)
                    .isNotNull()
                    .asInstanceOf(type(TypeNode.ArrayTypeNode::class.java))
                    .extracting { it.type }
                    .asInstanceOf(type(TypeNode.ArrayTypeNode::class.java))
                    .also {
                        it.extracting { it.fixedSize.toString() }.isEqualTo("10")
                        it.extracting { it.type }
                            .asInstanceOf(type(TypeNode.ArrayTypeNode::class.java))
                            .extracting { it.type }
                            .asInstanceOf(type(TypeNode.NameTypeNode::class.java))
                            .extracting { it.name.value }
                            .isEqualTo("Matrix")
                    }
            }

            @Test
            fun returnsArrayWithNullSizeCorrectly() {
                val res = parseType(tokenize("Matrix<f32>[]"))
                assertThat(res)
                    .isNotNull()
                    .asInstanceOf(type(TypeNode.ArrayTypeNode::class.java))
                    .extracting { it.fixedSize }
                    .isNull()
            }

//Requires comptime constant resolution
//            @Test
//            fun returnsArrayWithExpressionSizeCorrectly() {
//                val res = parseType(tokenize("Matrix<f32>[10*12]"))
//                assertThat(res)
//                    .isNotNull()
//                    .asInstanceOf(type(TypeNode.ArrayTypeNode::class.java))
//                    .extracting { it.fixedSize }
//                    .isNotNull()
//                    .extracting { it.toString() }
//                    .isEqualTo("(10 * 12)")
//            }

            @Test
            fun returnsArrayWithErrorWhenClosingBracketMissing() {
                val res = parseType(tokenize("Matrix<f32>[10"))
                assertThat(res)
                    .isNotNull()
                    .asInstanceOf(type(TypeNode.ArrayTypeNode::class.java))
                    .extracting { it.fixedSize }
                    .isNotNull()
                    .extracting { it.toString() }
                    .isEqualTo("10")
                assertThat(res?.errors)
                    .isNotEmpty()
                    .first()
                    .extracting { it.message }
                    .asString()
                    .contains("]")
            }
        }

        @Nested
        @DisplayName("TYPE REFERENCE")
        inner class TypeReference : MethodTest<TypeNode.NameTypeNode>() {
            override fun parseMethod(input: String) = parseType(tokenize(input)) as? TypeNode.NameTypeNode

            override fun fullyValidInputs() = listOf(
                "MyType",
                "MyType<AnotherType>",
                "MyType<AnotherType, ThirdType>",
                "MyType< A , B , C >",
                "MyType< Array<i64>, Map<u8, u8> >",
                "MyType< Array<i64> | B, Map<u8, u8>[] >"
            )

            override fun invalidInputs() = listOf<String>()

            override fun partiallyValidInputs(): List<Input> = listOf(
                Input("MyType<", 1),
                Input("MyType<AnotherType", 1),
                Input("MyType<AnotherType,", 1),
                Input("MyType<A,,B>", 1),
                Input("MyType<A,  ,B>", 1),
                Input("MyType<AnotherType,ThirdType", 1),
            )

            @Test
            fun returnsTypeReferenceWithGenericsCorrectly() {
                val res = parseType(tokenize("MyType<AnotherType, ThirdType>"))
                assertThat(res)
                    .isNotNull()
                    .asInstanceOf(type(TypeNode.NameTypeNode::class.java))
                    .extracting { it.name.value }
                    .isEqualTo("MyType")
                assertThat(res)
                    .asInstanceOf(type(TypeNode.NameTypeNode::class.java))
                    .extracting { it.genericTypes }
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .hasSize(2)
                    .let {
                        it.element(0).isInstanceOf(TypeNode.NameTypeNode::class.java)
                            .extracting { (it as TypeNode.NameTypeNode).name.value }
                            .isEqualTo("AnotherType")
                        it.element(1).isInstanceOf(TypeNode.NameTypeNode::class.java)
                            .extracting { (it as TypeNode.NameTypeNode).name.value }
                            .isEqualTo("ThirdType")
                    }
            }

            @Test
            fun returnTypeReferenceWithErrorWhenClosingBracketMissing() {
                val res = parseType(tokenize("MyType<AnotherType"))
                assertThat(res)
                    .isNotNull()
                    .asInstanceOf(type(TypeNode.NameTypeNode::class.java))
                    .extracting { it.name.value }
                    .isEqualTo("MyType")
                assertThat(res)
                    .asInstanceOf(type(TypeNode.NameTypeNode::class.java))
                    .extracting { it.genericTypes }
                    .asInstanceOf(InstanceOfAssertFactories.LIST)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(TypeNode.NameTypeNode::class.java)
                    .extracting { (it as TypeNode.NameTypeNode).name.value }
                    .isEqualTo("AnotherType")
                assertThat(res?.errors)
                    .isNotEmpty()
                    .first()
                    .extracting { it.message }
                    .asString()
                    .contains(">")
            }

            @Test
            fun shouldReturnEmptyGenericsWhenAngleBracketsAreEmptyOrMissing() {
                val empty = parseType(tokenize("MyType<>"))
                val none = parseType(tokenize("MyType"))
                assertThat(empty)
                    .usingRecursiveComparison()
                    .isEqualTo(none)
            }
        }
//        value_as_type
//        function_signature
//        structural_interface
//        type_reference
    }
}
