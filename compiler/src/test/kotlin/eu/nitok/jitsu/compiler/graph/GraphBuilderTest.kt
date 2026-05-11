package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.common.sequence
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
            assertThat(file.sequence().filterIsInstance<Function>().toList()).isEmpty()
        }

        @Test
        fun `creates JitsuFile with single function`() {
            val file = buildFile("fn hello() { }")
            val functions = file.sequence().filterIsInstance<Function>().toList()
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
            val functions = file.sequence().filterIsInstance<Function>().toList()
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
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = (fn.body as Function.Body.Implementation).block.instructions
            return (body.filterIsInstance<Instruction.Return>().firstOrNull())?.value
        }

        @Test
        fun `resolves integer literal to UIntConstant`() {
            val expr = firstReturnExpression("fn f(): u32 { return 42; }")
            assertThat(expr).isInstanceOf(Constant.UIntConstant::class.java)
            assertThat((expr as Constant.UIntConstant).value).isEqualTo(42uL)
        }

        @Test
        fun `resolves negative integer to IntConstant`() {
            val expr = firstReturnExpression("fn f(): i32 { return -5; }")
            assertThat(expr).isInstanceOf(Constant.IntConstant::class.java)
            assertThat((expr as Constant.IntConstant).value).isEqualTo(-5L)
        }

        @Test
        fun `resolves unsigned integer zero to UIntConstant`() {
            val expr = firstReturnExpression("fn f(): u32 { return 0; }")
            assertThat(expr).isInstanceOf(Constant.UIntConstant::class.java)
            assertThat((expr as Constant.UIntConstant).value).isEqualTo(0uL)
        }

        @Test
        fun `resolves large unsigned integer to UIntConstant`() {
            val expr = firstReturnExpression("fn f(): u64 { return 9999999999; }")
            assertThat(expr).isInstanceOf(Constant.UIntConstant::class.java)
            assertThat((expr as Constant.UIntConstant).value).isEqualTo(9_999_999_999uL)
        }

        @Test
        fun `resolves variable reference`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = (fn.body as Function.Body.Implementation).block.instructions
            val returnExpr = body.filterIsInstance<Instruction.Return>().first().value
            assertThat(returnExpr).isInstanceOf(Expression.VariableReference::class.java)
            assertThat((returnExpr as Expression.VariableReference).reference.value).isEqualTo("x")
        }

        @Test
        fun `resolves variable reference target to parameter declaration`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = (fn.body as Function.Body.Implementation).block.instructions
            val returnExpr = body.filterIsInstance<Instruction.Return>().first().value as Expression.VariableReference
            assertThat(returnExpr.target).isNotNull()
            assertThat(returnExpr.target).isEqualTo(fn.parameters.first { it.name.value == "x" })
        }

        @Test
        fun `resolves binary operation with operator`() {
            val expr = firstReturnExpression("fn plus(a: i32, b: i32): i32 { return a + b; }")
            assertThat(expr).isInstanceOf(Instruction.FunctionCall::class.java)
        }

        @Test
        fun `resolves binary operation left and right operands`() {
            val expr = firstReturnExpression("fn plus(a: i32, b: i32): i32 { return a + b; }") as Instruction.FunctionCall
            assertThat(expr.callParameters[0]).isInstanceOf(Expression.VariableReference::class.java)
            assertThat(expr.callParameters[1]).isInstanceOf(Expression.VariableReference::class.java)
            assertThat((expr.callParameters[0] as Expression.VariableReference).reference.value).isEqualTo("a")
            assertThat((expr.callParameters[1] as Expression.VariableReference).reference.value).isEqualTo("b")
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
            val main = file.sequence().filterIsInstance<Function>().first { it.name?.value == "main" }
            val body = (main.body as Function.Body.Implementation).block.instructions
            val varDecl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "result" }
            assertThat(varDecl.initialValue).isInstanceOf(Instruction.FunctionCall::class.java)
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
            val main = file.sequence().filterIsInstance<Function>().first { it.name?.value == "main" }
            val body = (main.body as Function.Body.Implementation).block.instructions
            val varDecl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "result" }
            val call = varDecl.initialValue as Instruction.FunctionCall
            assertThat(call.callParameters).hasSize(2)
        }
    }

    @Nested
    @DisplayName("resolveType()")
    inner class ResolveTypeTests {

        private fun returnType(source: String): Type? {
            val file = buildFile(source)
            return file.sequence().filterIsInstance<Function>().first().returnType?.value
        }

        @Test
        fun `resolves i8 to Int type with 8 bits`() {
            val type = returnType("native fn f(): i8")
            assertThat(type).isInstanceOf(Type.Int::class.java)
            assertThat((type as Type.Int).size.bits).isEqualTo(8)
        }

        @Test
        fun `resolves i32 to Int type with 32 bits`() {
            val type = returnType("fn f(): i32 { return 0; }")
            assertThat(type).isInstanceOf(Type.Int::class.java)
            assertThat((type as Type.Int).size.bits).isEqualTo(32)
        }

        @Test
        fun `resolves i64 to Int type with 64 bits`() {
            val type = returnType("fn f(): i64 { return 0; }")
            assertThat(type).isInstanceOf(Type.Int::class.java)
            assertThat((type as Type.Int).size.bits).isEqualTo(64)
        }

        @Test
        fun `resolves u32 to UInt type with 32 bits`() {
            val type = returnType("fn f(): u32 { return 0; }")
            assertThat(type).isInstanceOf(Type.UInt::class.java)
            assertThat((type as Type.UInt).size.bits).isEqualTo(32)
        }

        @Test
        fun `resolves u64 to UInt type with 64 bits`() {
            val type = returnType("fn f(): u64 { return 0; }")
            assertThat(type).isInstanceOf(Type.UInt::class.java)
            assertThat((type as Type.UInt).size.bits).isEqualTo(64)
        }

        @Test
        fun `resolves f32 to Float type with 32 bits`() {
            val type = returnType("native fn f(): f32")
            assertThat(type).isInstanceOf(Type.Float::class.java)
            assertThat((type as Type.Float).size.bits).isEqualTo(32)
        }

        @Test
        fun `resolves f64 to Float type with 64 bits`() {
            val type = returnType("native fn f(): f64")
            assertThat(type).isInstanceOf(Type.Float::class.java)
            assertThat((type as Type.Float).size.bits).isEqualTo(64)
        }

        @Test
        fun `resolves boolean to Boolean type`() {
            val type = returnType("native fn f(): boolean")
            assertThat(type).isEqualTo(Type.Boolean)
        }

        @Test
        fun `resolves array type with element type`() {
            val type = returnType("native fn f(): i32[]")
            assertThat(type).isInstanceOf(Type.Array::class.java)
            assertThat((type as Type.Array).elementType).isInstanceOf(Type.Int::class.java)
        }

        @Test
        fun `resolves union type with two options`() {
            val type = returnType("fn f(): u32 | i32 { return 0; }")
            assertThat(type).isInstanceOf(Type.Union::class.java)
            assertThat((type as Type.Union).options).hasSize(2)
        }

        @Test
        fun `resolves union type options to correct types`() {
            val type = returnType("fn f(): u32 | i32 { return 0; }") as Type.Union
            val typeClasses = type.options.map { it::class }
            assertThat(typeClasses).containsExactlyInAnyOrder(Type.UInt::class, Type.Int::class)
        }

        @Test
        fun `resolves named type reference`() {
            val file = buildFile("""
                type MyType = i32 | u32;
                fn f(): MyType { return 0; }
            """.trimIndent())
            val fn = file.sequence().filterIsInstance<Function>().first { it.name?.value == "f" }
            val type = fn.returnType?.value
            assertThat(type).isInstanceOf(Type.TypeReference::class.java)
            assertThat((type as Type.TypeReference).reference.value).isEqualTo("MyType")
        }

        @Test
        fun `function with no return type has null return type`() {
            val file = buildFile("fn noop() { }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.returnType).isNull()
        }

        @Test
        fun `parameter type resolves to correct type`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val paramType = fn.parameters.first().declaredType
            assertThat(paramType).isInstanceOf(Type.Int::class.java)
            assertThat((paramType as Type.Int).size.bits).isEqualTo(32)
        }
    }

    @Nested
    @DisplayName("buildFunctionGraph()")
    inner class BuildFunctionGraphTests {

        @Test
        fun `creates Function with name`() {
            val file = buildFile("fn myFunction() { }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.name?.value).isEqualTo("myFunction")
        }

        @Test
        fun `creates Function with no parameters`() {
            val file = buildFile("fn noArgs(): i32 { return 0; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.parameters).isEmpty()
        }

        @Test
        fun `creates Function with single parameter`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.parameters).hasSize(1)
            assertThat(fn.parameters[0].name.value).isEqualTo("x")
        }

        @Test
        fun `creates Function with multiple parameters`() {
            val file = buildFile("fn plus(a: i32, b: i32): i32 { return a + b; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.parameters).hasSize(2)
            val paramNames = fn.parameters.map { it.name.value }
            assertThat(paramNames).containsExactly("a", "b")
        }

        @Test
        fun `creates Function with parameter types`() {
            val file = buildFile("fn f(a: u32, b: i64): i64 { return b; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.parameters[0].declaredType).isInstanceOf(Type.UInt::class.java)
            assertThat(fn.parameters[1].declaredType).isInstanceOf(Type.Int::class.java)
        }

        @Test
        fun `creates Function with return type`() {
            val file = buildFile("fn f(): i32 { return 0; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.returnType).isNotNull()
            assertThat(fn.returnType!!.value).isInstanceOf(Type.Int::class.java)
        }

        @Test
        fun `creates Function with Implementation body`() {
            val file = buildFile("fn f(): i32 { return 5; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.body).isInstanceOf(Function.Body.Implementation::class.java)
        }

        @Test
        fun `creates Function body with return instruction`() {
            val file = buildFile("fn f(): i32 { return 5; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            val returnInstructions = body.block.instructions.filterIsInstance<Instruction.Return>()
            assertThat(returnInstructions).hasSize(1)
        }

        @Test
        fun `creates Function with variable declaration in body`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 10; return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            val varDecls = body.block.instructions.filterIsInstance<VariableDeclaration>()
            assertThat(varDecls).hasSize(1)
            assertThat(varDecls[0].name.value).isEqualTo("x")
        }

        @Test
        fun `creates native Function with Native body`() {
            val file = buildFile("native fn nativeFunc(): i32")
            val fn = file.sequence().filterIsInstance<Function>().first()
            assertThat(fn.body).isInstanceOf(Function.Body.Native::class.java)
        }

        @Test
        fun `native function body contains native target string`() {
            val file = buildFile("native fn nativeFunc(): i32")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val native = fn.body as Function.Body.Native
            assertThat(native.nativeTarget).isNotBlank()
            assertThat(native.nativeTarget).contains("nativeFunc")
        }

        @Test
        fun `creates multiple functions each with correct name`() {
            val file = buildFile("""
                fn alpha(): i32 { return 1; }
                fn beta(): u32 { return 2; }
            """.trimIndent())
            val functions = file.sequence().filterIsInstance<Function>().toList()
            val alpha = functions.first { it.name?.value == "alpha" }
            val beta = functions.first { it.name?.value == "beta" }
            assertThat(alpha.returnType?.value).isInstanceOf(Type.Int::class.java)
            assertThat(beta.returnType?.value).isInstanceOf(Type.UInt::class.java)
        }
    }

    @Nested
    @DisplayName("buildCodeBlockGraph()")
    inner class BuildCodeBlockGraphTests {

        @Test
        fun `empty function body produces empty instruction list`() {
            val file = buildFile("fn noop() { }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            assertThat(body.block.instructions).isEmpty()
        }

        @Test
        fun `single return produces one instruction`() {
            val file = buildFile("fn f(): i32 { return 1; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            assertThat(body.block.instructions).hasSize(1)
        }

        @Test
        fun `variable declaration and return produce two instructions`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 5; return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            assertThat(body.block.instructions).hasSize(2)
        }

        @Test
        fun `variable declaration has correct initial value type`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 99; return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            val varDecl = body.block.instructions.filterIsInstance<VariableDeclaration>().first()
            assertThat(varDecl.initialValue).isInstanceOf(Constant.UIntConstant::class.java)
            assertThat((varDecl.initialValue as Constant.UIntConstant).value).isEqualTo(99uL)
        }

        @Test
        fun `variable declaration with explicit type stores declared type`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 0; return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            val varDecl = body.block.instructions.filterIsInstance<VariableDeclaration>().first()
            assertThat(varDecl.declaredType).isInstanceOf(Type.Int::class.java)
        }

        @Test
        fun `return value with arithmetic expression`() {
            val file = buildFile("fn f(a: i32, b: i32): i32 { return a + b; } native fn plus(a:i32,b:i32):i32")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            val ret = body.block.instructions.filterIsInstance<Instruction.Return>().first()
            assertThat(ret.value).isInstanceOf(Instruction.FunctionCall::class.java)
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
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            val vars = body.block.instructions.filterIsInstance<VariableDeclaration>()
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
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            return body.block.instructions.filterIsInstance<Instruction.Return>().first().value
        }

        @Test
        fun `positive integer resolves to UIntConstant`() {
            val const = constantFrom("10")
            assertThat(const).isInstanceOf(Constant.UIntConstant::class.java)
        }

        @Test
        fun `zero resolves to UIntConstant`() {
            val const = constantFrom("0")
            assertThat(const).isInstanceOf(Constant.UIntConstant::class.java)
            assertThat((const as Constant.UIntConstant).value).isEqualTo(0uL)
        }

        @Test
        fun `small positive value has correct ULong value`() {
            val const = constantFrom("255") as Constant.UIntConstant
            assertThat(const.value).isEqualTo(255uL)
        }

        @Test
        fun `negative integer resolves to IntConstant`() {
            val file = buildFile("fn f(): i64 { return -100; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val body = fn.body as Function.Body.Implementation
            val const = body.block.instructions.filterIsInstance<Instruction.Return>().first().value
            assertThat(const).isInstanceOf(Constant.IntConstant::class.java)
            assertThat((const as Constant.IntConstant).value).isEqualTo(-100L)
        }

        @Test
        fun `large unsigned integer resolves to UIntConstant`() {
            val const = constantFrom("4294967296") as Constant.UIntConstant
            assertThat(const.value).isEqualTo(4_294_967_296uL)
        }
    }
}
