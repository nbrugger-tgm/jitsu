package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.parser.parseFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

class AnalysisIntegrationTest {

    private fun buildFile(source: String) = buildGraph(parseFile(source, URI("test://integration")))

    @Test
    fun `pure function returning constant gets deterministic summary`() {
        val file = buildFile("fn five(): i32 { return 5; }")
        assertThat(file.analysisRepository).isNotNull()

        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.noSideEffects.value).isTrue()
        assertThat(fn.summary!!.pure).isTrue()
    }

    @Test
    fun `function with parameter returns parameter value`() {
        val file = buildFile("fn identity(x: i32): i32 { return x; }")
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.parameterInfluence).contains("x")
    }

    @Test
    fun `function with parameter returns parameter-dependent value`() {
        val file = buildFile("fn identity(x: i32): i32 { return x + 20; }")
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.parameterInfluence).containsExactlyInAnyOrder("x")
    }


    @Test
    fun `function with multiple parameters returns parameter-dependent value`() {
        val file = buildFile("fn identity(x: i32,y: i32): i32 { return x + y; }")
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.parameterInfluence).containsExactlyInAnyOrder("x","y")
    }

    @Test
    fun `function with multiple parameters returns only parameter-dependent value`() {
        val file = buildFile("fn identity(x: i32,y: i32): i32 { return x + 30; }")
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.parameterInfluence).containsExactlyInAnyOrder("x")
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
        assertThat(bar.summary!!.deterministic.value).isTrue()
    }

    @Test
    fun `variable summary accessible via repository`() {
        val file = buildFile("fn test(): i32 { var x: i32 = 10; return x; }")
        val repo = file.analysisRepository!!

        val fn = file.sequence().filterIsInstance<Function>().first()
        val varDecl = (fn.body as Function.Body.Implementation).block.instructions
            .filterIsInstance<VariableDeclaration>()
            .first()

        val varSummary = repo.getVariableSummary(varDecl)
        assertThat(varSummary).isNotNull()
        assertThat(varSummary!!.effectivelyConstant.value).isTrue()
    }

    @Test
    fun `repository is set on JitsuFile`() {
        val file = buildFile("fn noop() { }")
        assertThat(file.analysisRepository).isNotNull()
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
        """.trimIndent())
        val fn = file.sequence().filterIsInstance<Function>().first()
        assertThat(fn.summary).isNotNull()
        assertThat(fn.summary!!.deterministic.value).isTrue()
        assertThat(fn.summary!!.parameterInfluence).containsExactlyInAnyOrder("a", "b")
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
                var input = 23470000;
                var number : u32 | i32 = input;
                var return1 = test(number, number);
                var return2 = testGeneric(number, number);
                return return1 + return2;
            }
            fn test(a: u32 | i64, b: u64 | i32): u32 {
                return a + b;
            }
            fn testGeneric(a: u32 | i64, b: u64 | i32): u32 {
                return a + b;
            }
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
        val returnOp = returnStmt.value as? Expression.Operation
        assertThat(returnOp).isNotNull()
        val return1Ref = returnOp!!.left as? Expression.VariableReference
        val return2Ref = returnOp.right as? Expression.VariableReference
        assertThat(return1Ref).isNotNull()
        assertThat(return2Ref).isNotNull()
        assertThat(return1Ref!!.target).isNotNull()
        assertThat(return2Ref!!.target).isNotNull()
    }
}
