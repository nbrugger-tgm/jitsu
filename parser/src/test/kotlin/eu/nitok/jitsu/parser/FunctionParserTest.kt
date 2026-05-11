package eu.nitok.jitsu.parser

import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.parser.ast.StatementNode.Declaration.FunctionDeclarationNode
import eu.nitok.jitsu.parser.ast.StatementNode.Declaration.FunctionDeclarationNode.FunctionBodyNode
import eu.nitok.jitsu.parser.ast.StatementNode.InstructionNode.CodeBlockNode.StatementsCodeBlock
import eu.nitok.jitsu.parser.ast.TypeNode
import eu.nitok.jitsu.parser.parsers.parseFunction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Function Parsing")
class FunctionParserTest : ParsingTest() {

    @Nested
    @DisplayName("parseFunction()")
    inner class ParseFunction : MethodTest<FunctionDeclarationNode>() {
        override fun parseMethod(input: String) = parseFunction(tokenize(input))

        override fun fullyValidInputs() = listOf(
            "fn foo() {}",
            "fn bar(x: i32) {}",
            "fn baz(): i64 {}",
            "fn qux(a: i32, b: i64): u8 {}",
            "native fn external()",
            "fn multiParam(a: i32, b: i64, c: f32) {}",
            "fn withReturnAndBody(): boolean {}"
        )

        override fun invalidInputs() = listOf(
            "foo() {}",       // "foo" is not in fnKeywords → null
            "class Foo {}",   // not fn-like keyword → null
            "def foo() {}",   // not fn-like keyword → null
            "let x = 5",      // not fn-like keyword → null
            "type My = i64"   // not fn-like keyword → null
        )

        override fun partiallyValidInputs() = listOf(
            Input("fn () {}", 1),             // missing name → 1 error
            Input("fn foo {}", 1),            // missing '(' → 1 error
            Input("fn foo()", 1),             // missing body (non-native) → 1 error
            Input("native fn foo() {}", 1),   // native with body → 1 error
            Input("func foo() {}", 1),        // wrong keyword → 1 error
            Input("fun foo() {}", 1),         // wrong keyword → 1 error
            Input("function foo() {}", 1),    // wrong keyword → 1 error
            Input("fn foo() }", 1),           // missing {
            Input("fn foo() {", 1),           // missing }
        )
    }

    @Test
    @DisplayName("parses function name correctly")
    fun parsesFunctionNameCorrectly() {
        val fn = parseFunction(tokenize("fn myFunction() {}"))
        assertThat(fn).isNotNull()
        assertThat(fn?.name)
            .isNotNull()
            .extracting { it?.value }
            .isEqualTo("myFunction")
    }

    @Test
    @DisplayName("attaches error when function name is missing")
    fun attachesErrorWhenNameMissing() {
        val fn = parseFunction(tokenize("fn () {}"))
        assertThat(fn)
            .`as`("Should still produce a node even when name is missing")
            .isNotNull()
        assertThat(fn?.name)
            .isNull()
        assertThat(fn?.errors)
            .isNotEmpty()
            .first()
            .extracting { it.message }
            .asString()
            .contains("name")
    }

    @Test
    @DisplayName("parses empty parameter list")
    fun parsesEmptyParameterList() {
        val fn = parseFunction(tokenize("fn foo() {}"))
        assertThat(fn?.parameters)
            .isNotNull()
            .isEmpty()
    }

    @Test
    @DisplayName("parses single parameter correctly")
    fun parsesSingleParameterCorrectly() {
        val fn = parseFunction(tokenize("fn foo(x: i32) {}"))
        assertThat(fn?.parameters)
            .isNotNull()
            .hasSize(1)
            .first()
            .extracting { it.name.value }
            .isEqualTo("x")
    }

    @Test
    @DisplayName("parses multiple parameters correctly")
    fun parsesMultipleParametersCorrectly() {
        val fn = parseFunction(tokenize("fn test(a: i32, b: boolean) {}"))
        assertThat(fn?.parameters)
            .isNotNull()
            .hasSize(2)
        assertThat(fn?.parameters?.get(0)?.name?.value).isEqualTo("a")
        assertThat(fn?.parameters?.get(0)?.type).isInstanceOf(TypeNode.IntTypeNode::class.java)
        assertThat(fn?.parameters?.get(1)?.name?.value).isEqualTo("b")
        assertThat(fn?.parameters?.get(1)?.type).isInstanceOf(TypeNode.BooleanTypeNode::class.java)
    }

    @Test
    @DisplayName("parses three parameters correctly")
    fun parsesThreeParametersCorrectly() {
        val fn = parseFunction(tokenize("fn test(a: i32, b: i64, c: f32) {}"))
        assertThat(fn?.parameters)
            .isNotNull()
            .hasSize(3)
    }

    @Test
    @DisplayName("attaches error when opening parenthesis is missing")
    fun attachesErrorWhenParenthesisMissing() {
        val fn = parseFunction(tokenize("fn foo {}"))
        assertThat(fn)
            .`as`("Should still produce a node even when '(' is missing")
            .isNotNull()
        assertThat(fn?.errors)
            .isNotEmpty()
            .first()
            .extracting { it.message }
            .asString()
            .contains("(")
    }

    @ParameterizedTest
    @DisplayName("parses parameter types correctly for primitive types")
    @ValueSource(strings = ["i8", "i16", "i32", "i64", "u8", "u16", "u32", "u64", "f32", "f64", "boolean"])
    fun parsesParameterTypesCorrectly(typeName: String) {
        val fn = parseFunction(tokenize("fn foo(x: $typeName) {}"))
        assertThat(fn?.parameters)
            .isNotNull()
            .hasSize(1)
        assertThat(fn?.parameters?.first()?.type)
            .`as`("Parameter type should be parsed for '$typeName'")
            .isNotNull()
    }

    @Test
    @DisplayName("has no return type when not specified")
    fun hasNoReturnTypeWhenNotSpecified() {
        val fn = parseFunction(tokenize("fn foo() {}"))
        assertThat(fn?.returnType).isNull()
    }

    @Test
    @DisplayName("parses return type correctly")
    fun parsesReturnTypeCorrectly() {
        val fn = parseFunction(tokenize("fn test(): i32 {}"))
        assertThat(fn?.returnType)
            .`as`("Return type should be parsed")
            .isNotNull()
            .isInstanceOf(TypeNode.IntTypeNode::class.java)
    }

    @ParameterizedTest
    @DisplayName("parses various return types")
    @ValueSource(strings = ["i8", "i16", "i32", "i64", "u8", "u16", "u32", "u64", "f32", "f64", "boolean", "MyType"])
    fun parsesVariousReturnTypes(typeName: String) {
        val fn = parseFunction(tokenize("fn foo(): $typeName {}"))
        assertThat(fn)
            .`as`("Should parse function with return type '$typeName'")
            .isNotNull()
        assertThat(fn?.returnType)
            .`as`("Return type '$typeName' should be parsed")
            .isNotNull()
    }

    @Test
    @DisplayName("parses function body for non-native function")
    fun parsesFunctionBodyForNonNative() {
        val fn = parseFunction(tokenize("fn foo() {}"))
        assertThat(fn?.body)
            .`as`("Non-native function should have a body")
            .isNotNull()
        assertThat(fn?.body)
            .isNotInstanceOf(FunctionBodyNode.NativeImplementation::class.java)
            .isInstanceOf(StatementsCodeBlock::class.java)
    }

    @Test
    @DisplayName("attaches error when body is missing for non-native function")
    fun attachesErrorWhenBodyMissingForNonNative() {
        val fn = parseFunction(tokenize("fn foo()"))
        assertThat(fn)
            .`as`("Should still produce a node even when body is missing")
            .isNotNull()
        assertThat(fn?.errors)
            .isNotEmpty()
            .first()
            .extracting { it.message }
            .asString()
            .contains("{")
    }

    // ── Native functions ──────────────────────────────────────────────────────

    @Test
    @DisplayName("parses native function without body")
    fun parsesNativeFunctionWithoutBody() {
        val fn = parseFunction(tokenize("native fn ext()"))
        assertThat(fn)
            .`as`("Native function should be parsed")
            .isNotNull()
        assertThat(fn?.body)
            .isInstanceOf(FunctionBodyNode.NativeImplementation::class.java)
    }

    @Test
    @DisplayName("native function produces no errors when body is absent")
    fun nativeFunctionProducesNoErrorsWhenBodyAbsent() {
        val fn = parseFunction(tokenize("native fn external()"))
        assertThat(fn).isNotNull()
        assertThat(fn?.errors).isEmpty()
        assertThat(fn?.warnings).isEmpty()
    }

    @Test
    @DisplayName("native function with body produces an error")
    fun nativeFunctionWithBodyProducesError() {
        val fn = parseFunction(tokenize("native fn foo() {}"))
        assertThat(fn)
            .`as`("Should still produce a node even with invalid body")
            .isNotNull()
        assertThat(fn?.errors)
            .isNotEmpty()
            .first()
            .extracting { it.message }
            .asString()
            .containsIgnoringCase("native")
    }

    @Test
    @DisplayName("native function preserves NativeImplementation body type")
    fun nativeFunctionHasNativeImplementationBody() {
        val fn = parseFunction(tokenize("native fn compute(a: i32, b: i64)"))
        assertThat(fn?.body)
            .isNotNull()
            .asInstanceOf(type(FunctionBodyNode.NativeImplementation::class.java))
    }

    @ParameterizedTest
    @DisplayName("wrong fn-like keywords produce an error but still parse")
    @ValueSource(strings = ["func", "fun", "function"])
    fun wrongFnKeywordsProduceErrorButStillParse(keyword: String) {
        val fn = parseFunction(tokenize("$keyword foo() {}"))
        assertThat(fn)
            .`as`("'$keyword' is close enough to 'fn' to still parse, but should produce an error")
            .isNotNull()
        assertThat(fn?.errors)
            .isNotEmpty()
            .first()
            .extracting { it.message }
            .asString()
            .contains("fn")
    }

    @Test
    @DisplayName("parses full function with params and return type")
    fun parsesFullFunctionWithParamsAndReturnType() {
        val fn = parseFunction(tokenize("fn compute(a: i32, b: i64): u8 {}"))
        assertThat(fn).isNotNull()
        assertThat(fn?.name?.value).isEqualTo("compute")
        assertThat(fn?.parameters).hasSize(2)
        assertThat(fn?.returnType).isNotNull()
        assertThat(fn?.body).isNotNull()
        assertThat(fn?.errors).isEmpty()
        assertThat(fn?.warnings).isEmpty()
    }

    @Test
    @DisplayName("location spans the entire function declaration")
    fun locationSpansEntireFunction() {
        val fn = parseFunction(tokenize("fn foo(x: i32): i64 {}"))
        assertThat(fn).isNotNull()
        assertThat(fn?.location)
            .isNotNull()
            .isEqualTo(Location(url,1, 1, 22, 1))
    }
}
