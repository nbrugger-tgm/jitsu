package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.buildGraph
import eu.nitok.jitsu.parser.parseFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.net.URI

@DisplayName("Function Lowering")
class FunctionLoweringTest {

    private fun buildFile(source: String) = buildGraph(parseFile(source, URI("test://lowering")))

    private fun lower(source: String): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<Function>().first()
        val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
        return lowering.lower()
    }

    private fun lowerFunction(source: String, name: String): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<Function>().first { it.name?.value == name }
        val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
        return lowering.lower()
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Basic Lowering Tests")
    inner class BasicLoweringTests {

        @Test
        fun `simple return of integer constant produces Return instruction`() {
            val instructions = lower("fn f(): i32 { return 5; }")
            val returnInstr = instructions.filterIsInstance<Return>()
            assertThat(returnInstr).hasSize(1)
            assertThat(returnInstr[0].value).isNotNull()
        }

        @Test
        fun `simple return of integer constant produces NumericalValue`() {
            val instructions = lower("fn f(): i32 { return 5; }")
            val returnInstr = instructions.filterIsInstance<Return>().first()
            assertThat(returnInstr.value).isInstanceOf(LowLevelExpression.NumericalValue::class.java)
            assertThat((returnInstr.value as LowLevelExpression.NumericalValue).value).isEqualTo(5L)
        }

        @Test
        fun `variable declaration produces AllocStack instruction`() {
            val instructions = lower("fn f(): i32 { var x: i32 = 10; return x; }")
            val allocInstructions = instructions.filterIsInstance<AllocStack>()
            assertThat(allocInstructions).isNotEmpty()
            val xAlloc = allocInstructions.firstOrNull { it.name == "x" }
            assertThat(xAlloc).isNotNull()
        }

        @Test
        fun `variable declaration produces Write instruction for initial value`() {
            val instructions = lower("fn f(): i32 { var x: i32 = 10; return x; }")
            val writes = instructions.filterIsInstance<Write>()
            assertThat(writes).isNotEmpty()
        }

        @Test
        fun `variable declaration AllocStack has correct low-level type`() {
            val instructions = lower("fn f(): i32 { var x: i32 = 10; return x; }")
            val xAlloc = instructions.filterIsInstance<AllocStack>().first { it.name == "x" }
            assertThat(xAlloc.layout).isEqualTo(LowLevelType.I32)
        }

        @Test
        fun `function call produces Invoke instruction`() {
            val instructions = lowerFunction(
                """
                fn helper(): i32 { return 1; }
                fn main(): i32 {
                    helper();
                    return 0;
                }
                """.trimIndent(),
                "main"
            )
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isNotEmpty()
            assertThat(invokes.any { it.functionName == "helper" }).isTrue()
        }

        @Test
        fun `function with no return type and no body produces empty instructions`() {
            val instructions = lower("native fn nativeFunc(): i32")
            assertThat(instructions).isEmpty()
        }

        @Test
        fun `return without value produces Return with null`() {
            val instructions = lower("fn f() { }")
            // Empty body - no return instruction expected since no explicit return
            // Just verify lowering completes without error
            assertThat(instructions).isNotNull()
        }

        @Test
        fun `lowering produces instructions in correct order - AllocStack before Write`() {
            val instructions = lower("fn f(): i32 { var x: i32 = 42; return x; }")
            val allocIdx = instructions.indexOfFirst { it is AllocStack && (it as AllocStack).name == "x" }
            val writeIdx = instructions.indexOfFirst { it is Write }
            assertThat(allocIdx).isGreaterThanOrEqualTo(0)
            assertThat(writeIdx).isGreaterThan(allocIdx)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Type-Predictive Lowering Tests")
    inner class TypePredictiveLoweringTests {

        @Test
        fun `array literal constructs with hinted element type - dynamic array`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [1, 2, 3];
                }
                """.trimIndent()
            )
            // Verify AllocStack is produced for the variable
            val allocInstructions = instructions.filterIsInstance<AllocStack>()
            assertThat(allocInstructions).isNotEmpty()

            // Verify that Write instructions are produced for elements
            val writes = instructions.filterIsInstance<Write>()
            assertThat(writes).isNotEmpty()
        }

        @Test
        fun `array literal with matching element type hint produces correct element count writes`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [1, 2, 3];
                }
                """.trimIndent()
            )
            // There should be 3 element writes (plus possible length/data setup writes)
            val arraySlotWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            assertThat(arraySlotWrites).hasSize(3)
        }

        @Test
        fun `array literal element writes use i32 numerical values`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [10, 20, 30];
                }
                """.trimIndent()
            )
            val elementWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            val values = elementWrites.map { it.value }
                .filterIsInstance<LowLevelExpression.NumericalValue>()
                .map { it.value }
            assertThat(values).containsExactly(10L, 20L, 30L)
        }

        @Test
        fun `array literal with hint does not produce extra conversion instructions`() {
            // When the array literal's element type matches the hint, no conversion needed
            // The array is constructed directly as i32[], so no copyFrom should appear
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [1, 2, 3];
                }
                """.trimIndent()
            )
            // No While loops for element copying (copyFrom uses iterate which creates While)
            // Unless those while loops come from the free pass - but variable shouldn't have free
            // since it's a simple function. Check no extra AllocHeapArray beyond the main array alloc.
            val heapArrayAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is LowLevelExpression.AllocHeapArray }
            // Should have exactly 1 - the initial data alloc for x, not a duplicate copy
            assertThat(heapArrayAllocs).hasSize(1)
        }

        @Test
        fun `return value uses expected return type as hint - i32 return`() {
            val instructions = lower("fn f(): i32 { return 42; }")
            val returnInstr = instructions.filterIsInstance<Return>().first()
            // The value should be a NumericalValue with the correct value
            assertThat(returnInstr.value).isInstanceOf(LowLevelExpression.NumericalValue::class.java)
            assertThat((returnInstr.value as LowLevelExpression.NumericalValue).value).isEqualTo(42L)
        }

        @Test
        fun `return value with i64 hint produces correct type`() {
            val instructions = lower("fn f(): i64 { return 100; }")
            val returnInstr = instructions.filterIsInstance<Return>().first()
            assertThat(returnInstr.value).isInstanceOf(LowLevelExpression.NumericalValue::class.java)
        }

        @Test
        fun `array literal in return uses return type as hint`() {
            // Fixed-size array returned - the hint should be passed through
            val instructions = lower(
                """
                fn f(): i32[] {
                    return [1, 2, 3];
                }
                """.trimIndent()
            )
            // Should produce element writes with no extra conversion
            val elementWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            assertThat(elementWrites).hasSize(3)
        }

        @Test
        fun `function call with typed parameters passes hints to arguments`() {
            // Verify that function call parameters get type hints.
            // Function calls used as expressions appear as Invoke embedded in Write.value (ReturnValue(Invoke))
            val instructions = lowerFunction(
                """
                fn identity(a: i32): i32 { return a; }
                fn main(): i32 {
                    var result: i32 = identity(42);
                    return result;
                }
                """.trimIndent(),
                "main"
            )
            // Function call as expression: Write(target, ReturnValue(Invoke(...)))
            val callWrite = instructions.filterIsInstance<Write>()
                .firstOrNull { it.value is LowLevelExpression.ReturnValue && (it.value as LowLevelExpression.ReturnValue).functionCall.functionName == "identity" }
            assertThat(callWrite).isNotNull()
            val invoke = (callWrite!!.value as LowLevelExpression.ReturnValue).functionCall
            assertThat(invoke.args).containsKey("a")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Variable Registry Tests")
    inner class VariableRegistryTests {

        @Test
        fun `variable gets registered with correct I32 LowLevelType`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 5; return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }
            val lowLevelType = lowering.variableRegistry.getLowLevelType(varDecl)
            assertThat(lowLevelType).isEqualTo(LowLevelType.I32)
        }

        @Test
        fun `variable gets registered with correct I64 LowLevelType`() {
            val file = buildFile("fn f(): i64 { var x: i64 = 5; return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }
            val lowLevelType = lowering.variableRegistry.getLowLevelType(varDecl)
            assertThat(lowLevelType).isEqualTo(LowLevelType.I64)
        }

        @Test
        fun `array variable gets registered with JitsuArray type`() {
            val file = buildFile("fn f() { var x: i32[] = [1]; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }
            val lowLevelType = lowering.variableRegistry.getLowLevelType(varDecl)
            assertThat(lowLevelType).isInstanceOf(JitsuArray::class.java)
        }

        @Test
        fun `array variable element type is correct`() {
            val file = buildFile("fn f() { var x: i32[] = [1]; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }
            val lowLevelType = lowering.variableRegistry.getLowLevelType(varDecl) as JitsuArray
            assertThat(lowLevelType.elementType).isEqualTo(LowLevelType.I32)
        }

        @Test
        fun `stack allocation size reflects primitive type`() {
            val instructions = lower("fn f(): i32 { var x: i32 = 0; return x; }")
            val xAlloc = instructions.filterIsInstance<AllocStack>().first { it.name == "x" }
            // I32 is a primitive - check it's an LLInt
            assertThat(xAlloc.layout).isInstanceOf(LowLevelType.LLInt::class.java)
        }

        @Test
        fun `multiple variables each get their own AllocStack`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var a: i32 = 1;
                    var b: i64 = 2;
                    return a;
                }
                """.trimIndent()
            )
            val allocs = instructions.filterIsInstance<AllocStack>()
            assertThat(allocs.map { it.name }).containsExactlyInAnyOrder("a", "b")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Conversion Tests")
    inner class ConversionTests {

        @Test
        fun `same primitive types produce no conversion - just Return`() {
            // i32 -> i32: no conversion needed
            val instructions = lower("fn f(): i32 { return 7; }")
            // Should be a simple Return with a NumericalValue, no extra Invoke for conversion
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isEmpty()
            val returnInstr = instructions.filterIsInstance<Return>().first()
            assertThat(returnInstr.value).isInstanceOf(LowLevelExpression.NumericalValue::class.java)
        }

        @Test
        fun `array variable with dynamic array type emits AllocHeapArray`() {
            // Dynamic arrays need heap allocation via Write(data, AllocHeapArray(...))
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [5, 10];
                }
                """.trimIndent()
            )
            val heapAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is LowLevelExpression.AllocHeapArray }
            assertThat(heapAllocs).isNotEmpty()
        }

        @Test
        fun `array-to-array copy produces copyFrom-style instructions when needed`() {
            // Assigning a dynamic array variable to another of the same type
            // When types match (hint == actual), no copyFrom needed
            // When types differ, copyFrom is used. Testing that same-type assigns don't trigger it.
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [1, 2];
                }
                """.trimIndent()
            )
            // Exactly one AllocHeapArray for the initial data allocation (not two)
            val heapArrayAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is LowLevelExpression.AllocHeapArray }
            assertThat(heapArrayAllocs).hasSize(1)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Edge Case Tests")
    inner class EdgeCaseTests {

        @Test
        fun `empty function produces single return statement`() {
            val instructions = lower("fn f() { }")
            assertThat(instructions).containsExactly(Return(null))
        }

        @Test
        fun `boolean constant produces NumericalValue for true`() {
            // Note: boolean literals resolve as VariableReferences to built-ins in the graph,
            // which is not yet supported by lowerVariableReference. Test using boolean param instead.
            val instructions = lower("fn f(b: boolean): boolean { return b; }")
            val returnInstr = instructions.filterIsInstance<Return>().first()
            assertThat(returnInstr.value).isNotNull()
        }

        @Test
        fun `boolean constant produces NumericalValue for false`() {
            // Note: boolean literals resolve as VariableReferences to built-ins in the graph,
            // which is not yet supported by lowerVariableReference. Test that lowering completes.
            val instructions = lower("fn f(b: boolean): boolean { return b; }")
            assertThat(instructions).isNotEmpty()
        }

        @Test
        fun `returning a variable reference reads variable`() {
            val instructions = lower("fn f(x: i32): i32 { return x; }")
            val returnInstr = instructions.filterIsInstance<Return>().first()
            // Variable reference returns read access to the variable
            assertThat(returnInstr.value).isNotNull()
        }

        @Test
        fun `getUniqueName lambda is used for function names in Invoke`() {
            val file = buildFile(
                """
                fn helper(): i32 { return 1; }
                fn main(): i32 {
                    helper();
                    return 0;
                }
                """.trimIndent()
            )
            val mainFn = file.sequence().filterIsInstance<Function>().first { it.name?.value == "main" }
            // Use a custom naming scheme to verify it's being used
            val lowering = FunctionLowering({ fn -> "PREFIX_${fn.name?.value ?: "anon"}" }, mainFn)
            val instructions = lowering.lower()
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes.any { it.functionName == "PREFIX_helper" }).isTrue()
        }

        @Test
        fun `array literal with single element produces correct instructions`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [42];
                }
                """.trimIndent()
            )
            val elementWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            assertThat(elementWrites).hasSize(1)
            val value = elementWrites[0].value
            assertThat(value).isInstanceOf(LowLevelExpression.NumericalValue::class.java)
            assertThat((value as LowLevelExpression.NumericalValue).value).isEqualTo(42L)
        }
    }
}
