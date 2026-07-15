package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement.IntConstant
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement.UIntConstant
import eu.nitok.jitsu.compiler.graph.elements.FunctionCall
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.compiler.graph.elements.JitsuFile
import eu.nitok.jitsu.compiler.graph.elements.Return
import eu.nitok.jitsu.compiler.graph.elements.VariableDeclaration
import eu.nitok.jitsu.compiler.graph.elements.VariableReference
import eu.nitok.jitsu.compiler.graph.elements.types.Array
import eu.nitok.jitsu.compiler.graph.elements.types.Int
import eu.nitok.jitsu.compiler.graph.elements.types.Float
import eu.nitok.jitsu.compiler.graph.elements.types.Boolean
import eu.nitok.jitsu.compiler.graph.elements.types.UInt
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.compiler.graph.elements.types.TypeReference
import eu.nitok.jitsu.compiler.graph.elements.types.Union
import eu.nitok.jitsu.parser.parseJitsuFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.net.URI

@DisplayName("Graph Builder")
class GraphBuilderTest {

    private fun buildFile(source: String): JitsuFile {
        val ast = parseJitsuFile(source, URI("test://sourcefile.jit"))
        ast.sequence().forEach {
            if(it.errors.isNotEmpty()) throw IllegalArgumentException("Syntax error(s)! ${it.errors.joinToString("\n")}")
        }
        val graph = buildJitsuModule(ast)
        if(graph.messages.errors.isNotEmpty()) throw IllegalArgumentException("Compilation error(s)! ${graph.messages.errors.joinToString("\n")}")
        return graph.files[0]
    }

    @Nested
    @DisplayName("buildGraph()")
    inner class BuildGraphTests {

        @Test
        fun `creates JitsuFile from minimal source`() {
            // The parser requires at least some token content; use a comment-only file via a simple noop
            val file = buildFile("fn noop() { }")
            assertThat(file).isNotNull()
            assertThat(file.scope).isNotNull()
        }

        @Test
        fun `creates JitsuFile with no functions when only type alias is present`() {
            val file = buildFile("type MyAlias = i32;")
            assertThat(file).isNotNull()
            assertThat(file.sequence().filterIsInstance<FunctionElement>().toList()).isEmpty()
        }

        @Test
        fun `creates JitsuFile with single function`() {
            val file = buildFile("fn hello() { }")
            val functions = file.sequence().filterIsInstance<FunctionElement>().toList()
            assertThat(functions).hasSize(1)
            assertThat(functions[0].name?.value).isEqualTo("hello")
        }

        @Test
        fun `creates JitsuFile with multiple functions`() {
            val file = buildFile("""
                fn a(): i32 { return 1; }
                fn b(): i32 { return 2; }
                fn c(): i32 { return 3; }
            """.trimIndent())
            val functions = file.sequence().filterIsInstance<FunctionElement>().toList()
            assertThat(functions).hasSize(3)
            val names = functions.map { it.name?.value }
            assertThat(names).containsExactlyInAnyOrder("a", "b", "c")
        }

        @Test
        fun `creates JitsuFile with type declarations`() {
            val file = buildFile("""
                type Alias = i32 | u64;
                fn noop() { }
            """.trimIndent())
            assertThat(file).isNotNull()
            val types = file.scope.allTypes
            assertThat(types).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("buildExpressionGraph()")
    inner class BuildExpressionGraphTests {

        private fun firstReturnExpression(source: String): Expression? {
            val file = buildFile(source)
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = (fn.body as FunctionElement.BodyElement.Implementation).instructions
            return (body.filterIsInstance<Return>().firstOrNull())?.value
        }

        @Test
        fun `resolves integer literal to UIntConstant`() {
            val expr = firstReturnExpression("fn f(): u32 { return 42; }")
            assertThat(expr).isInstanceOf(UIntConstant::class.java)
            assertThat((expr as UIntConstant).value).isEqualTo(42uL)
        }

        @Test
        fun `resolves negative integer to IntConstant`() {
            val expr = firstReturnExpression("fn f(): i32 { return -5; }")
            assertThat(expr).isInstanceOf(IntConstant::class.java)
            assertThat((expr as IntConstant).value).isEqualTo(-5L)
        }

        @Test
        fun `resolves unsigned integer zero to UIntConstant`() {
            val expr = firstReturnExpression("fn f(): u32 { return 0; }")
            assertThat(expr).isInstanceOf(UIntConstant::class.java)
            assertThat((expr as UIntConstant).value).isEqualTo(0uL)
        }

        @Test
        fun `resolves large unsigned integer to UIntConstant`() {
            val expr = firstReturnExpression("fn f(): u64 { return 9999999999; }")
            assertThat(expr).isInstanceOf(UIntConstant::class.java)
            assertThat((expr as UIntConstant).value).isEqualTo(9_999_999_999uL)
        }

        @Test
        fun `resolves variable reference`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = (fn.body as FunctionElement.BodyElement.Implementation).instructions
            val returnExpr = body.filterIsInstance<Return>().first().value
            assertThat(returnExpr).isInstanceOf(VariableReference::class.java)
            assertThat((returnExpr as VariableReference).reference.value).isEqualTo("x")
        }

        @Test
        fun `resolves variable reference target to parameter declaration`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = (fn.body as FunctionElement.BodyElement.Implementation).instructions
            val returnExpr = body.filterIsInstance<Return>().first().value as VariableReference
            assertThat(returnExpr.target).isNotNull()
            assertThat(returnExpr.target).isEqualTo(fn.parameters.first { it.name.value == "x" })
        }

        @Test
        fun `resolves binary operation with operator`() {
            val expr = firstReturnExpression("fn plus(a: i32, b: i32): i32 { return a + b; }")
            assertThat(expr).isInstanceOf(FunctionCall::class.java)
        }

        @Test
        fun `resolves binary operation left and right operands`() {
            val expr = firstReturnExpression("fn plus(a: i32, b: i32): i32 { return a + b; }") as FunctionCall
            assertThat(expr.callParameters[0]).isInstanceOf(VariableReference::class.java)
            assertThat(expr.callParameters[1]).isInstanceOf(VariableReference::class.java)
            assertThat((expr.callParameters[0] as VariableReference).reference.value).isEqualTo("a")
            assertThat((expr.callParameters[1] as VariableReference).reference.value).isEqualTo("b")
        }

        @Test
        fun `resolves function call expression`() {
            val file = buildFile("""
                fn helper(): u32 { return 1; }
                fn main(): u32 {
                    var result = helper();
                    return result;
                }
            """.trimIndent())
            val main = file.sequence().filterIsInstance<FunctionElement>().first { it.name?.value == "main" }
            val body = (main.body as FunctionElement.BodyElement.Implementation).instructions
            val varDecl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "result" }
            assertThat(varDecl.initialValue).isInstanceOf(FunctionCall::class.java)
        }

        @Test
        fun `resolves function call with arguments`() {
            val file = buildFile("""
                fn add(a: i32, b: i32): i32 { return a + b; }
                native fn plus(a: i32, b: i32): i32
                fn main(): i32 {
                    var result = add(1, 2);
                    return result;
                }
            """.trimIndent())
            val main = file.sequence().filterIsInstance<FunctionElement>().first { it.name?.value == "main" }
            val body = (main.body as FunctionElement.BodyElement.Implementation).instructions
            val varDecl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "result" }
            val call = varDecl.initialValue as FunctionCall
            assertThat(call.callParameters).hasSize(2)
        }
    }

    @Nested
    @DisplayName("resolveType()")
    inner class ResolveTypeTests {

        private fun returnType(source: String): TypeElement? {
            val file = buildFile(source)
            return file.sequence().filterIsInstance<FunctionElement>().first().returnTypeElement?.value
        }

        @Test
        fun `resolves i8 to Int type with 8 bits`() {
            val type = returnType("native fn f(): i8")
            assertThat(type).isInstanceOf(Int::class.java)
            assertThat((type as Int).size.bits).isEqualTo(8)
        }

        @Test
        fun `resolves i32 to Int type with 32 bits`() {
            val type = returnType("fn f(): i32 { return 0; }")
            assertThat(type).isInstanceOf(Int::class.java)
            assertThat((type as Int).size.bits).isEqualTo(32)
        }

        @Test
        fun `resolves i64 to Int type with 64 bits`() {
            val type = returnType("fn f(): i64 { return 0; }")
            assertThat(type).isInstanceOf(Int::class.java)
            assertThat((type as Int).size.bits).isEqualTo(64)
        }

        @Test
        fun `resolves u32 to UInt type with 32 bits`() {
            val type = returnType("fn f(): u32 { return 0; }")
            assertThat(type).isInstanceOf(UInt::class.java)
            assertThat((type as UInt).size.bits).isEqualTo(32)
        }

        @Test
        fun `resolves u64 to UInt type with 64 bits`() {
            val type = returnType("fn f(): u64 { return 0; }")
            assertThat(type).isInstanceOf(UInt::class.java)
            assertThat((type as UInt).size.bits).isEqualTo(64)
        }

        @Test
        fun `resolves f32 to Float type with 32 bits`() {
            val type = returnType("native fn f(): f32")
            assertThat(type).isInstanceOf(Float::class.java)
            assertThat((type as Float).size.bits).isEqualTo(32)
        }

        @Test
        fun `resolves f64 to Float type with 64 bits`() {
            val type = returnType("native fn f(): f64")
            assertThat(type).isInstanceOf(Float::class.java)
            assertThat((type as Float).size.bits).isEqualTo(64)
        }

        @Test
        fun `resolves boolean to Boolean type`() {
            val type = returnType("native fn f(): boolean")
            assertThat(type).isEqualTo(Boolean)
        }

        @Test
        fun `resolves array type with element type`() {
            val type = returnType("native fn f(): i32[]")
            assertThat(type).isInstanceOf(Array::class.java)
            assertThat((type as Array).elementType).isInstanceOf(Int::class.java)
        }

        @Test
        fun `resolves union type with two options`() {
            val type = returnType("fn f(): u32 | i32 { return 0; }")
            assertThat(type).isInstanceOf(Union::class.java)
            assertThat((type as Union).options).hasSize(2)
        }

        @Test
        fun `resolves union type options to correct types`() {
            val type = returnType("fn f(): u32 | i32 { return 0; }") as Union
            val typeClasses = type.options.map { it::class }
            assertThat(typeClasses).containsExactlyInAnyOrder(UInt::class, Int::class)
        }

        @Test
        fun `resolves named type reference`() {
            val file = buildFile("""
                type MyType = i32 | u32;
                fn f(): MyType { return 0; }
            """.trimIndent())
            val fn = file.sequence().filterIsInstance<FunctionElement>().first { it.name?.value == "f" }
            val type = fn.returnType?.value
            assertThat(type).isInstanceOf(TypeReference::class.java)
            assertThat((type as TypeReference).reference.value).isEqualTo("MyType")
        }

        @Test
        fun `function with no return type has null return type`() {
            val file = buildFile("fn noop() { }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.returnType).isNull()
        }

        @Test
        fun `parameter type resolves to correct type`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val paramType = fn.parameters.first().declaredType
            assertThat(paramType).isInstanceOf(Int::class.java)
            assertThat((paramType as Int).size.bits).isEqualTo(32)
        }
    }

    @Nested
    @DisplayName("buildFunctionGraph()")
    inner class BuildFunctionGraphTests {

        @Test
        fun `creates Function with name`() {
            val file = buildFile("fn myFunction() { }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.name?.value).isEqualTo("myFunction")
        }

        @Test
        fun `creates Function with no parameters`() {
            val file = buildFile("fn noArgs(): i32 { return 0; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.parameters).isEmpty()
        }

        @Test
        fun `creates Function with single parameter`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.parameters).hasSize(1)
            assertThat(fn.parameters[0].name.value).isEqualTo("x")
        }

        @Test
        fun `creates Function with multiple parameters`() {
            val file = buildFile("fn plus(a: i32, b: i32): i32 { return a + b; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.parameters).hasSize(2)
            val paramNames = fn.parameters.map { it.name.value }
            assertThat(paramNames).containsExactly("a", "b")
        }

        @Test
        fun `creates Function with parameter types`() {
            val file = buildFile("fn f(a: u32, b: i64): i64 { return b; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.parameters[0].declaredType).isInstanceOf(UInt::class.java)
            assertThat(fn.parameters[1].declaredType).isInstanceOf(Int::class.java)
        }

        @Test
        fun `creates Function with return type`() {
            val file = buildFile("fn f(): i32 { return 0; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.returnType).isNotNull()
            assertThat(fn.returnType!!.value).isInstanceOf(Int::class.java)
        }

        @Test
        fun `creates Function with Implementation body`() {
            val file = buildFile("fn f(): i32 { return 5; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.body).isInstanceOf(FunctionElement.BodyElement.Implementation::class.java)
        }

        @Test
        fun `creates Function body with return instruction`() {
            val file = buildFile("fn f(): i32 { return 5; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            val returnInstructions = body.instructions.filterIsInstance<Return>()
            assertThat(returnInstructions).hasSize(1)
        }

        @Test
        fun `creates Function with variable declaration in body`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 10; return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            val varDecls = body.instructions.filterIsInstance<VariableDeclaration>()
            assertThat(varDecls).hasSize(1)
            assertThat(varDecls[0].name.value).isEqualTo("x")
        }

        @Test
        fun `creates native Function with Native body`() {
            val file = buildFile("native fn nativeFunc(): i32")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            assertThat(fn.body).isInstanceOf(FunctionElement.BodyElement.Native::class.java)
        }

        //TODO
//        @Test
//        fun `native function body contains native target string`() {
//            val file = buildFile("native fn nativeFunc(): i32")
//            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
//            val native = fn.body as FunctionElement.BodyElement.Native
//            assertThat(native.nativeTarget).isNotBlank()
//            assertThat(native.nativeTarget).contains("nativeFunc")
//        }

        @Test
        fun `creates multiple functions each with correct name`() {
            val file = buildFile("""
                fn alpha(): i32 { return 1; }
                fn beta(): u32 { return 2; }
            """.trimIndent())
            val functions = file.sequence().filterIsInstance<FunctionElement>().toList()
            val alpha = functions.first { it.name?.value == "alpha" }
            val beta = functions.first { it.name?.value == "beta" }
            assertThat(alpha.returnType?.value).isInstanceOf(Int::class.java)
            assertThat(beta.returnType?.value).isInstanceOf(UInt::class.java)
        }
    }

    @Nested
    @DisplayName("buildCodeBlockGraph()")
    inner class BuildCodeBlockGraphTests {

        @Test
        fun `empty function body produces empty instruction list`() {
            val file = buildFile("fn noop() { }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            assertThat(body.instructions).isEmpty()
        }

        @Test
        fun `single return produces one instruction`() {
            val file = buildFile("fn f(): i32 { return 1; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            assertThat(body.instructions).hasSize(1)
        }

        @Test
        fun `variable declaration and return produce two instructions`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 5; return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            assertThat(body.instructions).hasSize(2)
        }

        @Test
        fun `variable declaration has correct initial value type`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 99; return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            val varDecl = body.instructions.filterIsInstance<VariableDeclaration>().first()
            assertThat(varDecl.initialValue).isInstanceOf(UIntConstant::class.java)
            assertThat((varDecl.initialValue as UIntConstant).value).isEqualTo(99uL)
        }

        @Test
        fun `variable declaration with explicit type stores declared type`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 0; return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            val varDecl = body.instructions.filterIsInstance<VariableDeclaration>().first()
            assertThat(varDecl.declaredType).isInstanceOf(Int::class.java)
        }

        @Test
        fun `return value with arithmetic expression`() {
            val file = buildFile("fn f(a: i32, b: i32): i32 { return a + b; } native fn plus(a:i32,b:i32):i32")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            val ret = body.instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isInstanceOf(FunctionCall::class.java)
        }

        @Test
        fun `multiple variable declarations are ordered correctly`() {
            val file = buildFile("""
                fn f(): u32 {
                    var first = 1;
                    var second = 2;
                    var third = 3;
                    return first;
                }
            """.trimIndent())
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            val vars = body.instructions.filterIsInstance<VariableDeclaration>()
            assertThat(vars).hasSize(3)
            assertThat(vars.map { it.name.value }).containsExactly("first", "second", "third")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("resolveIntConstant()")
    inner class ResolveIntConstantTests {

        private fun constantFrom(literal: String): Expression? {
            val file = buildFile("fn f(): u64 { return $literal; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            return body.instructions.filterIsInstance<Return>().first().value
        }

        @Test
        fun `positive integer resolves to UIntConstant`() {
            val const = constantFrom("10")
            assertThat(const).isInstanceOf(UIntConstant::class.java)
        }

        @Test
        fun `zero resolves to UIntConstant`() {
            val const = constantFrom("0")
            assertThat(const).isInstanceOf(UIntConstant::class.java)
            assertThat((const as UIntConstant).value).isEqualTo(0uL)
        }

        @Test
        fun `small positive value has correct ULong value`() {
            val const = constantFrom("255") as UIntConstant
            assertThat(const.value).isEqualTo(255uL)
        }

        @Test
        fun `negative integer resolves to IntConstant`() {
            val file = buildFile("fn f(): i64 { return -100; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val body = fn.body as FunctionElement.BodyElement.Implementation
            val const = body.instructions.filterIsInstance<Return>().first().value
            assertThat(const).isInstanceOf(IntConstant::class.java)
            assertThat((const as IntConstant).value).isEqualTo(-100L)
        }

        @Test
        fun `large unsigned integer resolves to UIntConstant`() {
            val const = constantFrom("4294967296") as UIntConstant
            assertThat(const.value).isEqualTo(4_294_967_296uL)
        }
    }
}
