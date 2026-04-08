package eu.nitok.jitsu.parser

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.parser.ast.StatementNode.NamedTypeDeclarationNode.ClassDeclarationNode
import eu.nitok.jitsu.parser.ast.TypeNode
import eu.nitok.jitsu.parser.ast.TypeNode.StructuralInterfaceTypeNode.StructuralFieldNode
import eu.nitok.jitsu.parser.parsers.parseClass
import eu.nitok.jitsu.parser.parsers.parseField
import eu.nitok.jitsu.parser.parsers.parseMethod
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Class Parsing")
class ClassParserTest : ParsingTest() {

    @Nested
    @DisplayName("parseClass()")
    inner class ParseClass : MethodTest<ClassDeclarationNode>() {
        override fun parseMethod(input: String) = parseClass(tokenize(input))

        override fun fullyValidInputs() = listOf(
            "class MyClass {}",
            "class MyClass { }",
            "class Box<T> {}",
            "class Pair<A, B> {}",
            "class Point { x: i32; }",
            "class Point { x: i32; y: i64; }",
            "class Counter { mut count: i32; }",
            "class Named { public name: i32; }",
            "class Vis { public mut value: i64; }",
            "class WithMethod { fn increment() {} }",
            "class Full { x: i32; fn getX(): i32 {} }",
            "class Full<T> { x: T; fn getX(): T {} y: T; }",
            "class { x: i32; }",
            "class { x: i32; y: i64; }",
            "class { mut count: i32; }",
        )

        override fun invalidInputs() = listOf(
            "struct MyStruct {}",
            "interface IFoo {}",
            "fn foo() {}",
            "type My = i64",
            "let x = 5"
        )

        override fun partiallyValidInputs() = listOf(
            // Missing '{' for body → 1 error for missing opening brace
            Input("class MyClass", 1),
            // Missing '}' at EOF → 1 error for unclosed class body
            Input("class MyClass {", 1),
            Input("class InvalidContent { ; }",1),
            Input("class { ; ", 2)
        )

        // ── Keyword ───────────────────────────────────────────────────────────

        @Test
        @DisplayName("returns null when 'class' keyword is absent")
        fun returnsNullWhenClassKeywordAbsent() {
            assertThat(parseClass(tokenize("MyClass {}"))).isNull()
            assertThat(parseClass(tokenize("klass Foo {}"))).isNull()
        }

        // ── Name ─────────────────────────────────────────────────────────────

        @Test
        @DisplayName("parses class name correctly")
        fun parsesClassNameCorrectly() {
            val cls = parseClass(tokenize("class MyClass {}"))
            assertThat(cls).isNotNull()
            assertThat(cls?.name?.value).isEqualTo("MyClass")
        }

        @Test
        @DisplayName("class without name still produces a node")
        fun classWithoutNameStillProducesNode() {
            val cls = parseClass(tokenize("class {}"))
            assertThat(cls)
                .`as`("Parser should still produce a node even without a class name")
                .isNotNull()
            assertThat(cls?.name).isNull()
        }

        // ── Type parameters ───────────────────────────────────────────────────

        @Test
        @DisplayName("parses single type parameter correctly")
        fun parsesSingleTypeParameter() {
            val cls = parseClass(tokenize("class Box<T> {}"))
            assertThat(cls).isNotNull()
            assertThat(cls?.typeParameters)
                .isNotNull()
                .hasSize(1)
                .first()
                .extracting { it.value }
                .isEqualTo("T")
        }

        @Test
        @DisplayName("parses multiple type parameters correctly")
        fun parsesMultipleTypeParameters() {
            val cls = parseClass(tokenize("class Pair<A, B> {}"))
            assertThat(cls?.typeParameters)
                .isNotNull()
                .hasSize(2)
            assertThat(cls?.typeParameters?.get(0)?.value).isEqualTo("A")
            assertThat(cls?.typeParameters?.get(1)?.value).isEqualTo("B")
        }

        @Test
        @DisplayName("class without type parameters has empty type parameter list")
        fun classWithoutTypeParametersHasEmptyList() {
            val cls = parseClass(tokenize("class Simple {}"))
            assertThat(cls?.typeParameters)
                .isNotNull()
                .isEmpty()
        }

        // ── Empty body ────────────────────────────────────────────────────────

        @Test
        @DisplayName("class with empty body has no fields and no methods")
        fun emptyBodyHasNoFieldsOrMethods() {
            val cls = parseClass(tokenize("class Empty {}"))
            assertThat(cls?.fields).isNotNull().isEmpty()
            assertThat(cls?.methods).isNotNull().isEmpty()
        }

        // ── Fields ────────────────────────────────────────────────────────────

        @Test
        @DisplayName("parses single field inside class")
        fun parsesSingleFieldInsideClass() {
            val cls = parseClass(tokenize("class Point { x: i32; }"))
            assertThat(cls?.fields)
                .isNotNull()
                .hasSize(1)
                .first()
                .extracting { it.name.value }
                .isEqualTo("x")
        }

        @Test
        @DisplayName("parses multiple fields inside class")
        fun parsesMultipleFieldsInsideClass() {
            val cls = parseClass(tokenize("class Point { x: i32; y: i64; }"))
            assertThat(cls?.fields)
                .isNotNull()
                .hasSize(2)
            assertThat(cls?.fields?.get(0)?.name?.value).isEqualTo("x")
            assertThat(cls?.fields?.get(1)?.name?.value).isEqualTo("y")
        }

        @Test
        @DisplayName("parses mutable field inside class")
        fun parsesMutableFieldInsideClass() {
            val cls = parseClass(tokenize("class Counter { mut count: i32; }"))
            assertThat(cls?.fields)
                .isNotNull()
                .hasSize(1)
            assertThat(cls?.fields?.first()?.mutableKw)
                .`as`("Field should be marked as mutable")
                .isNotNull()
        }

        @Test
        @DisplayName("parses public field inside class")
        fun parsesPublicFieldInsideClass() {
            val cls = parseClass(tokenize("class Named { public name: i32; }"))
            val field = cls?.fields?.first()
            assertThat(field?.visibility)
                .`as`("Field should have public visibility")
                .isNotNull()
            assertThat(field?.visibility?.value).isEqualTo("public")
        }

        // ── Methods ───────────────────────────────────────────────────────────

        @Test
        @DisplayName("parses single method inside class")
        fun parsesSingleMethodInsideClass() {
            val cls = parseClass(tokenize("class Counter { fn increment() {} }"))
            assertThat(cls?.methods)
                .isNotNull()
                .hasSize(1)
            assertThat(cls?.methods?.first()?.function?.name?.value).isEqualTo("increment")
        }

        @Test
        @DisplayName("distinguishes fields from methods in the same class")
        fun distinguishesFieldsFromMethods() {
            val cls = parseClass(tokenize("class Mixed { x: i32; fn getX(): i32 {} }"))
            assertThat(cls?.fields).isNotNull().hasSize(1)
            assertThat(cls?.methods).isNotNull().hasSize(1)
        }

        // ── 'mut fn' method ─────────────────────────────────────────────────────

        @Test
        @DisplayName("'mut fn' inside class should be parsed as a mutable method")
        fun mutFnInsideClassShouldBeParsedAsMethod() {
            val cls = parseClass(tokenize("class Counter { mut fn reset() {} }"))
            assertThat(cls).isNotNull()
            assertThat(cls?.methods)
                .`as`("'mut fn' should be parsed as a MethodNode with mutableKw set")
                .hasSize(1)
            assertThat(cls?.methods?.first()?.mutableKw)
                .`as`("The method should have mutableKw set")
                .isNotNull()
        }

        // ── Errors ────────────────────────────────────────────────────────────

        @Test
        @DisplayName("attaches error when opening brace is missing")
        fun attachesErrorWhenOpeningBraceMissing() {
            val cls = parseClass(tokenize("class MyClass"))
            assertThat(cls)
                .`as`("Should still produce a node even when '{' is missing")
                .isNotNull()
            assertThat(cls?.errors)
                .isNotEmpty()
                .first()
                .extracting { it.message }
                .asString()
                .contains("{")
        }

        // ── Location ──────────────────────────────────────────────────────────

        @Test
        @DisplayName("location spans the entire class declaration")
        fun locationSpansEntireClass() {
            val cls = parseClass(tokenize("class MyClass {}"))
            assertThat(cls?.location).isNotNull()
        }

        @Test
        @DisplayName("keyword location is recorded separately")
        fun keywordLocationIsRecorded() {
            val cls = parseClass(tokenize("class MyClass {}"))
            assertThat(cls?.keywordLocation).isNotNull()
        }
    }

    @Nested
    @DisplayName("parseField()")
    inner class ParseField : MethodTest<StructuralFieldNode>() {
        override fun parseMethod(input: String) = parseField(tokenize(input))

        override fun fullyValidInputs() = listOf(
            "x: i32;",
            "x   :   i32   ;",
            "count: i64;",
            "mut x: i32;",
            "public x: i32;",
            "public mut value: i64;",
            "name: boolean;"
        )

        override fun invalidInputs() = listOf(
            "{ x: i32; }",
            "(count: i32;)",
            "= x: i32;"
        )

        override fun partiallyValidInputs() = listOf(
            Input("x: i32", 1),    // missing ';' → 1 error
            Input("x:;", 2),       // missing type after : and field needs type
            Input("test;", 1),     // missing type
            Input("x", 2),         // missing type & semicolon
            Input("x i32;", 1),    // missing : before type
        )

        @Test
        @DisplayName("parses field name correctly")
        fun parsesFieldNameCorrectly() {
            val field = parseField(tokenize("myField: i32;"))
            assertThat(field).isNotNull()
            assertThat(field?.name?.value).isEqualTo("myField")
        }

        @Test
        @DisplayName("returns null when identifier (name) is missing due to bracket token")
        fun returnsNullWhenTokenIsNotIdentifierStart() {
            // '{' is ROUND_BRACKET_OPEN, not accepted by parseIdentifier → null
            val field = parseField(tokenize("{ }"))
            assertThat(field)
                .`as`("Without a parseable field name the parser should return null")
                .isNull()
        }

        @Test
        @DisplayName("LETTERS keywords like 'fn' and 'class' are treated as valid field names")
        fun lettersKeywordsAreValidFieldNames() {
            // parseIdentifier only rejects non-LETTERS/NUMBER/UNDERSCORE/DOLLAR starts.
            // 'fn', 'class', etc. tokenize as LETTERS and are accepted as field names.
            val field = parseField(tokenize("fn: i32;"))
            assertThat(field)
                .`as`("'fn' is a reserved term, so it should error but produce a valid node")
                .isNotNull()
            assertThat(field?.name?.value).isEqualTo("fn")
        }

        @Test
        @DisplayName("parses field type correctly")
        fun parsesFieldTypeCorrectly() {
            val field = parseField(tokenize("x: i32;"))
            assertThat(field?.type)
                .isNotNull()
                .asInstanceOf(type(TypeNode.IntTypeNode::class.java))
                .extracting { it.bitSize }
                .isEqualTo(BitSize.BIT_32)
        }

        @ParameterizedTest
        @DisplayName("parses various primitive field types")
        @ValueSource(strings = ["i8", "i16", "i32", "i64", "u8", "u16", "u32", "u64", "f32", "f64", "boolean"])
        fun parsesVariousPrimitiveFieldTypes(typeName: String) {
            val field = parseField(tokenize("x: $typeName;"))
            assertThat(field)
                .`as`("Should parse field with type '$typeName'")
                .isNotNull()
            assertThat(field?.type)
                .`as`("Field type '$typeName' should be parsed")
                .isNotNull()
        }

        @Test
        @DisplayName("immutable field has null mutableKw")
        fun immutableFieldHasNullMutableKw() {
            val field = parseField(tokenize("x: i32;"))
            assertThat(field?.mutableKw).isNull()
        }

        @Test
        @DisplayName("mutable field has non-null mutableKw")
        fun mutableFieldHasNonNullMutableKw() {
            val field = parseField(tokenize("mut x: i32;"))
            assertThat(field?.mutableKw)
                .`as`("'mut' keyword should be recorded on a mutable field")
                .isNotNull()
        }

        @Test
        @DisplayName("field without visibility modifier has null visibility")
        fun fieldWithoutVisibilityHasNullVisibility() {
            val field = parseField(tokenize("x: i32;"))
            assertThat(field?.visibility).isNull()
        }

        @Test
        @DisplayName("public field has visibility value 'public'")
        fun publicFieldHasVisibilityPublic() {
            val field = parseField(tokenize("public x: i32;"))
            assertThat(field?.visibility)
                .`as`("'public' visibility should be recorded")
                .isNotNull()
            assertThat(field?.visibility?.value).isEqualTo("public")
        }

        @Test
        @DisplayName("public mutable field has both visibility and mutableKw set")
        fun publicMutableFieldHasBothModifiers() {
            val field = parseField(tokenize("public mut value: i64;"))
            assertThat(field?.visibility).isNotNull()
            assertThat(field?.mutableKw).isNotNull()
            assertThat(field?.name?.value).isEqualTo("value")
        }

        // ── Errors ────────────────────────────────────────────────────────────

        @Test
        @DisplayName("attaches error when semicolon is missing")
        fun attachesErrorWhenSemicolonMissing() {
            val field = parseField(tokenize("x: i32"))
            assertThat(field)
                .`as`("Should still produce a node even when ';' is missing")
                .isNotNull()
            assertThat(field?.errors)
                .isNotEmpty()
                .first()
                .extracting { it.message }
                .asString()
                .contains(";")
        }

        @Test
        @DisplayName("attaches error when type is missing after colon")
        fun attachesErrorWhenTypeIsMissingAfterColon() {
            val field = parseField(tokenize("x:;"))
            assertThat(field)
                .`as`("Should still produce a node even when type is missing")
                .isNotNull()
            assertThat(field?.errors).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("parseMethod()")
    inner class ParseMethod : MethodTest<ClassDeclarationNode.MethodNode>() {
        override fun parseMethod(input: String) = parseMethod(tokenize(input))

        override fun fullyValidInputs() = listOf(
            "fn increment() {}",
            "fn getX(): i32 {}",
            "fn compute(a: i32, b: i64): u8 {}"
        )

        override fun invalidInputs() = listOf(
            "x: i32;",              // field declaration, not a method
            "public fn foo() {}",   // 'public' is not 'mut'; 'public' not a fn keyword → null
            "class Foo {}",         // class declaration → null
            "let x = 5"             // variable binding → null
        )

        override fun partiallyValidInputs() = listOf(
            Input("fn foo {}", 1),   // fn without parameter list → 1 error
            Input("fn foo()", 1),    // fn without body → 1 error
            Input("fn foo(,){}", 2), // invalid/no param before/after ,
            Input("fn (){}", 1),
        )

        // ── Mutable method ────────────────────────────────────────────────────

        @Test
        @DisplayName("'mut fn ...' should be parsed as a mutable method")
        fun mutFnShouldBeParsedAsMethod() {
            val method = parseMethod(tokenize("mut fn write() {}"))
            assertThat(method)
                .`as`("'mut fn ...' should be parsed as a valid method")
                .isNotNull()
            assertThat(method?.mutableKw)
                .`as`("Method should have mutableKw set")
                .isNotNull()
            assertThat(method?.function?.name?.value).isEqualTo("write")
        }

        @Test
        @DisplayName("'mut fn ...' with parameters should be parsed correctly")
        fun mutFnWithParametersShouldBeParsed() {
            val method = parseMethod(tokenize("mut fn setX(x: i32) {}"))
            assertThat(method).isNotNull()
            assertThat(method?.function?.parameters).hasSize(1)
        }

        @Test
        @DisplayName("wraps the underlying function declaration node")
        fun wrapsUnderlyingFunctionNode() {
            val method = parseMethod(tokenize("fn myMethod() {}"))
            assertThat(method).isNotNull()
            assertThat(method?.function).isNotNull()
            assertThat(method?.function?.name?.value).isEqualTo("myMethod")
        }

        @Test
        @DisplayName("parses method parameters correctly")
        fun parsesMethodParametersCorrectly() {
            val method = parseMethod(tokenize("fn add(a: i32, b: i32): i32 {}"))
            assertThat(method?.function?.parameters)
                .isNotNull()
                .hasSize(2)
            assertThat(method?.function?.parameters?.get(0)?.name?.value).isEqualTo("a")
            assertThat(method?.function?.parameters?.get(1)?.name?.value).isEqualTo("b")
        }

        @Test
        @DisplayName("parses method return type correctly")
        fun parsesMethodReturnTypeCorrectly() {
            val method = parseMethod(tokenize("fn getValue(): i64 {}"))
            assertThat(method?.function?.returnType)
                .`as`("Return type should be parsed")
                .isNotNull()
        }

        @Test
        @DisplayName("non-mutable method has null mutableKw")
        fun nonMutableMethodHasNullMutableKw() {
            val method = parseMethod(tokenize("fn read() {}"))
            assertThat(method?.mutableKw).isNull()
        }

        // ── Location ──────────────────────────────────────────────────────────

        @Test
        @DisplayName("method location is set")
        fun methodLocationIsSet() {
            val method = parseMethod(tokenize("fn write() {}"))
            assertThat(method?.location).isEqualTo(Range(1,1,13,1))
        }
    }
}
