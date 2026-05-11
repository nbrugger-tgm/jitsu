package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.parser.parseJitsuFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

class AnalysisIntegrationTest {

    private fun buildFile(source: String): JitsuFile {
        val ast = parseJitsuFile(source, URI("test://sourcefile.jit"))
        ast.sequence().forEach {
            if(it.errors.isNotEmpty()) throw IllegalArgumentException("Syntax error(s)! ${it.errors.joinToString("\n")}")
        }
        val graph = buildJitsuModule(ast)
        if(graph.messages.errors.isNotEmpty()) throw IllegalArgumentException("Compilation error(s)! ${graph.messages.errors.joinToString("\n")}")
        return graph.files[0]
    }

    @Test
    fun `pure function returning constant gets deterministic summary`() {
        val file = buildFile("fn five(): i32 { return 5; }")

        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.noSideEffects.value).isTrue()
        assertThat(fn.summary!!.pure).isTrue()
    }

    @Test
    fun `function with parameter returns parameter value`() {
        val file = buildFile("fn identity(x: i32): i32 { return x; }")
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.returnSummary!!.dependsOnParameters).contains("x")
    }

    @Test
    fun `function with parameter returns parameter-dependent value`() {
        val file = buildFile("fn identity(x: i32): i32 { return x + 20; } native fn plus(x: i32,y: i32): i32")
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.returnSummary!!.dependsOnParameters).containsExactlyInAnyOrder("x")
    }


    @Test
    fun `function with multiple parameters returns parameter-dependent value`() {
        val file = buildFile("fn identity(x: i32,y: i32): i32 { return x + y; } native fn plus(x: i32,y: i32): i32")
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.returnSummary!!.dependsOnParameters).containsExactlyInAnyOrder("x","y")
    }

    @Test
    fun `function with multiple parameters returns only parameter-dependent value`() {
        val file = buildFile("fn identity(x: i32,y: i32): i32 { return x + 30; } native fn plus(x: i32,y: i32): i32")
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.returnSummary!!.dependsOnParameters).containsExactlyInAnyOrder("x")
    }

    @Test
    fun `function calling another function gets callee in summary`() {
        val file = buildFile("""
            fn bar(): i32 { return 42; }
            fn foo(): i32 { return bar(); }
        """.trimIndent())
        val functions = file.sequence().filterIsInstance<Function>().toList()
        val foo = functions.first { it.name?.value == "foo" }
        val bar = functions.first { it.name?.value == "bar" }

        assertThat(foo.summary).isNotNull()
        assertThat(bar.summary).isNotNull()
        assertThat(foo.summary!!.callees).containsExactlyInAnyOrder(bar)
        assertThat(bar.summary!!.returnSummary!!.deterministic.value).isTrue()
    }

    @Test
    fun `all functions in file receive summaries`() {
        val file = buildFile("""
            fn a(): i32 { return 1; }
            fn b(): i32 { return 2; }
            fn c(): i32 { return 3; }
        """.trimIndent())
        val functions = file.sequence().filterIsInstance<Function>().toList()
        assertThat(functions).allSatisfy { fn ->
            assertThat(fn.summary).isNotNull()
        }
    }

    @Test
    fun `function with local variable declaration is analyzed`() {
        val file = buildFile("""
            fn compute(a: i32, b: i32): i32 {
                var sum: i32 = a + b;
                return sum;
            }
            native fn plus(a: i32, b: i32): i32
        """.trimIndent())
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.returnSummary!!.dependsOnParameters).containsExactlyInAnyOrder("a", "b")
    }

    // === Access resolution regression tests ===

    @Test
    fun `variable reference in code block resolves to declaration`() {
        val file = buildFile("""
            fn main(): u32 {
                var input = 23470000;
                var number: u32 = input;
                return number;
            }
        """.trimIndent())
        val fn = file.sequence().filterIsInstance<Function>().first { it.name?.value == "main" }
        val body = (fn.body as Function.Body.Implementation).block.instructions

        // Find the variable reference to 'input' in 'var number: u32 = input;'
        val numberDecl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "number" }
        val inputRef = numberDecl.initialValue as? Expression.VariableReference
        assertThat(inputRef).isNotNull()
        assertThat(inputRef!!.target).isNotNull()
            .withFailMessage("Variable reference 'input' should resolve to the variable declared above")

        val inputDecl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "input" }
        assertThat(inputRef.target).isSameAs(inputDecl)
    }

    @Test
    fun `function call in code block resolves to function target`() {
        val file = buildFile("""
            fn main(): u32 {
                var result = helper();
                return result;
            }
            fn helper(): u32 {
                return 42;
            }
        """.trimIndent())
        val fn = file.sequence().filterIsInstance<Function>().first { it.name?.value == "main" }
        val helperFn = file.sequence().filterIsInstance<Function>().first { it.name?.value == "helper" }

        // Find the function call 'helper()' in 'var result = helper();'
        val resultDecl = (fn.body as Function.Body.Implementation).block.instructions.filterIsInstance<VariableDeclaration>().first { it.name.value == "result" }
        val helperCall = resultDecl.initialValue as? Instruction.FunctionCall
        assertThat(helperCall).isNotNull()
        assertThat(helperCall!!.target).isNotNull()
            .withFailMessage("Function call 'helper()' should resolve to the helper function")
        assertThat(helperCall.target).isSameAs(helperFn)
    }

    @Test
    fun `simple_syntax_jit variable and function accesses resolve`() {
        val source = """
            fn main():u32 {
                var input = 10;
                var number : u32 | i32 = input;
                var return1 = test(number, number);
                var return2 = testGeneric(number, number);
                return return1 + return2;
            }
            fn test(a: u32 | i64, b: u32 | i32): u32 {
                return a + b;
            }
            type Or<A,B> = A | B;
            fn testGeneric(a: Or<u32, i64>, b: Or<u32, i32>): u32 {
                return a + b;
            }
            native fn plus(x: i64,y: i64): u32
        """.trimIndent()
        val file = buildFile(source)
        val mainFn = file.sequence().filterIsInstance<Function>().first { it.name?.value == "main" }
        val testFn = file.sequence().filterIsInstance<Function>().first { it.name?.value == "test" }
        val testGenericFn = file.sequence().filterIsInstance<Function>().first { it.name?.value == "testGeneric" }

        val body = (mainFn.body as Function.Body.Implementation).block.instructions

        // 'input' reference in 'var number : u32 | i32 = input;' resolves
        val numberDecl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "number" }
        val inputRef = numberDecl.initialValue as? Expression.VariableReference
        assertThat(inputRef).isNotNull()
        assertThat(inputRef!!.target).isNotNull()
            .withFailMessage("Variable reference 'input' on line 3 must resolve to the declaration on line 2")

        val inputDecl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "input" }
        assertThat(inputRef.target).isSameAs(inputDecl)

        // 'test(number, number)' resolves to test function
        val return1Decl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "return1" }
        val testCall = return1Decl.initialValue as? Instruction.FunctionCall
        assertThat(testCall).isNotNull()
        assertThat(testCall!!.target).isNotNull()
            .withFailMessage("Function call 'test(number, number)' must resolve to the test function")
        assertThat(testCall.target).isSameAs(testFn)

        // 'testGeneric(number, number)' resolves to testGeneric function
        val return2Decl = body.filterIsInstance<VariableDeclaration>().first { it.name.value == "return2" }
        val testGenericCall = return2Decl.initialValue as? Instruction.FunctionCall
        assertThat(testGenericCall).isNotNull()
        assertThat(testGenericCall!!.target).isNotNull()
            .withFailMessage("Function call 'testGeneric(number, number)' must resolve to the testGeneric function")
        assertThat(testGenericCall.target).isSameAs(testGenericFn)

        // Variable references in return statement resolve
        val returnStmt = body.filterIsInstance<Instruction.Return>().first()
        val returnOp = returnStmt.value as? Instruction.FunctionCall
        assertThat(returnOp).isNotNull()
        val return1Ref = returnOp!!.callParameters[0] as? Expression.VariableReference
        val return2Ref = returnOp.callParameters[1] as? Expression.VariableReference
        assertThat(return1Ref).isNotNull()
        assertThat(return2Ref).isNotNull()
        assertThat(return1Ref!!.target).isNotNull()
        assertThat(return2Ref!!.target).isNotNull()
    }
}
