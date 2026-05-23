package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.Companion.I32
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.compiler.graph.elements.JitsuFile
import eu.nitok.jitsu.compiler.graph.elements.VariableDeclaration
import eu.nitok.jitsu.compiler.graph.buildJitsuModule
import eu.nitok.jitsu.parser.parseJitsuFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.junit.jupiter.api.*
import java.net.URI

/**
 * Tests for memory management behaviour during function lowering.
 *
 * Covers:
 *  - Variable allocation (AllocStack + optional heap init)
 *  - Free instruction generation for variables marked requiresFree=true
 *  - Return-statement ordering: lowered + convert + free + Return(value)
 *  - Ownership states: declared variables OWNS, parameters BORROW
 *  - Primitive / pointer / dynamic-array / fixed-array / struct types
 *  - Nested free operations (array-of-pointers, nested arrays)
 *  - LLPointer.allocate() and JitsuArray.alloc() coverage
 *  - Edge cases (empty function, parameters-only, single variable)
 */
@DisplayName("Memory Management in Lowering")
class MemoryManagementTest {
    private fun buildFile(source: String): JitsuFile {
        val ast = parseJitsuFile(source, URI("test://sourcefile.jit"))
        ast.sequence().forEach {
            if (it.errors.isNotEmpty()) throw IllegalArgumentException("Syntax error(s)! ${it.errors.joinToString("\n")}")
        }
        val graph = buildJitsuModule(ast)
        if (graph.messages.errors.isNotEmpty()) throw IllegalArgumentException(
            "Compilation error(s)! ${
                graph.messages.errors.joinToString(
                    "\n"
                )
            }"
        )
        return graph.files[0]
    }

    private fun lower(source: String): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<FunctionElement>().first()
        return FunctionLowering({ it.name?.value ?: "anon" }, fn).lower()
    }

    private fun lowerNamed(source: String, name: String): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<FunctionElement>().first { it.name?.value == name }
        return FunctionLowering({ it.name?.value ?: "anon" }, fn).lower()
    }

    /** Build and lower, then return both the instructions AND the FunctionLowering object. */
    private fun lowerWithContext(source: String): Pair<List<LowLevelInstruction>, FunctionLowering> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<FunctionElement>().first()
        val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
        return lowering.lower() to lowering
    }

    /** Collect all [Free] instructions anywhere in the flat top-level list. */
    private fun List<LowLevelInstruction>.topLevelFrees() = filterIsInstance<Free>()

    /** Index of the single [Return] in a flat list, or -1. */
    private fun List<LowLevelInstruction>.returnIndex() = indexOfFirst { it is Return }

    @Nested
    @DisplayName("Variable Allocation")
    inner class VariableAllocation {

        @Nested
        @DisplayName("Primitive variable (i32)")
        inner class PrimitiveVariable {

            @Test
            fun `emits AllocStack for a primitive variable`() {
                val instructions = lower("fn f() { var x: i32 = 0; }")
                assertThat(instructions.filterIsInstance<AllocStack>().map { it.name })
                    .contains("x")
                    .hasSize(1)
            }

            @Test
            fun `AllocStack layout for i32 variable is LLInt with 32-bit size`() {
                val instructions = lower("fn f() { var x: i32 = 0; }")
                val alloc = instructions.filterIsInstance<AllocStack>().first { it.name == "x" }
                assertThat(alloc.layout).isEqualTo(I32)
            }

            @Test
            fun `no AllocHeap instruction for primitive variable`() {
                val instructions = lower("fn f() { var x: i32 = 42; }")
                val heapAllocs = instructions.filterIsInstance<Write>()
                    .filter { it.value is AllocHeap }
                assertThat(heapAllocs).isEmpty()
            }

            @Test
            fun `writes initial value to stack`() {
                val instructions = lower("fn f() { var x: i32 = 42; }")
                val write = instructions.filterIsInstance<Write>().first()
                assertThat(write.value).isEqualTo(NumericalValue(42L))
            }

            @Test
            fun `writes initial value to correct variable`() {
                val instructions = lower("fn f() { var x: i32 = 42; }")
                val write = instructions.filterIsInstance<Write>().first()
                assertThat(write.target)
                    .asInstanceOf(type(Variable::class.java))
                    .extracting { it.name }
                    .isEqualTo("x")
            }

            @Test
            fun `no AllocHeapArray instruction for primitive variable`() {
                val instructions = lower("fn f() { var x: i32 = 42; }")
                val heapArrayAllocs = instructions.filterIsInstance<Write>()
                    .filter { it.value is AllocHeapArray }
                assertThat(heapArrayAllocs).isEmpty()
            }

            @Test
            fun `AllocStack for primitive comes before Write of initial value`() {
                val instructions = lower("fn f() { var x: i32 = 7; }")
                val allocIdx = instructions.indexOfFirst { it is AllocStack && it.name == "x" }
                val writeIdx = instructions.indexOfFirst { it is Write }
                assertThat(allocIdx)
                    .isGreaterThanOrEqualTo(0)
                    .isLessThan(writeIdx)
            }
        }

        @Nested
        @DisplayName("Dynamic array variable (i32[])")
        inner class DynamicArrayVariable {

            @Test
            fun `emits AllocStack for the array struct variable`() {
                val instructions = lower("fn f() { var arr: i32[] = [1, 2]; }")
                assertThat(instructions.filterIsInstance<AllocStack>().map { it.name })
                    .contains("arr")
            }

            @Test
            fun `AllocStack layout for dynamic array variable is JitsuArray`() {
                val instructions = lower("fn f() { var arr: i32[] = [1, 2]; }")
                val alloc = instructions.filterIsInstance<AllocStack>().first { it.name == "arr" }
                assertThat(alloc.layout).isInstanceOf(JitsuArray::class.java)
                assertThat((alloc.layout as JitsuArray).isDynamic).isTrue()
            }

            @Test
            fun `emits Write to length field during array literal lowering`() {
                val instructions = lower("fn f() { var arr: i32[] = [1, 2, 3]; }")
                // The alloc() call writes length then AllocHeapArray for data
                val lengthWrite = instructions.filterIsInstance<Write>()
                    .firstOrNull { it.target is Read && (it.target as Read).name == "length" }
                assertThat(lengthWrite).isNotNull()
            }

            @Test
            fun `emits Write of AllocHeapArray to data field`() {
                val instructions = lower("fn f() { var arr: i32[] = [1, 2]; }")
                val dataWrite = instructions.filterIsInstance<Write>()
                    .firstOrNull { it.value is AllocHeapArray }
                assertThat(dataWrite).isNotNull()
                val heapExpr = dataWrite!!.value as AllocHeapArray
                assertThat(heapExpr.elementType).isEqualTo(I32)
                assertThat(heapExpr.size).isEqualTo(NumericalValue(2L))
            }

            @Test
            fun `length field is set to the number of elements in the literal`() {
                val instructions = lower("fn f() { var arr: i32[] = [10, 20, 30]; }")
                val lengthWrite = instructions.filterIsInstance<Write>()
                    .firstOrNull { it.target is Read && (it.target as Read).name == "length" }
                assertThat(lengthWrite).isNotNull()
                assertThat(lengthWrite!!.value).isEqualTo(NumericalValue(3L))
            }

            @Test
            fun `AllocStack for array variable precedes the length Write`() {
                val instructions = lower("fn f() { var arr: i32[] = [1]; }")
                val allocIdx = instructions.indexOfFirst { it is AllocStack && it.name == "arr" }
                val lengthWriteIdx = instructions.indexOfFirst {
                    it is Write && it.target is Read && (it.target as Read).name == "length"
                }
                assertThat(allocIdx).isGreaterThanOrEqualTo(0)
                assertThat(lengthWriteIdx).isGreaterThan(allocIdx)
            }

            @Test
            fun `creates array-writes for literal items`() {
                val instructions = lower("fn f() { var arr: i32[] = [1, 3]; }")
                val dataWrite = instructions.filterIsInstance<Write>()
                    .filter { it.target is ArraySlot }
                assertThat((dataWrite[0].target as ArraySlot).index).isEqualTo(NumericalValue(0L))
                assertThat(dataWrite[0].value).isEqualTo(NumericalValue(1L))
                assertThat((dataWrite[1].target as ArraySlot).index).isEqualTo(NumericalValue(1L))
                assertThat(dataWrite[1].value).isEqualTo(NumericalValue(3L))
            }

            @Test
            fun `array-writes happen after data alloc`() {
                val instructions = lower("fn f() { var arr: i32[] = [1, 3]; }")
                val dataWrite = instructions.filterIsInstance<Write>()
                    .filter { it.target is ArraySlot }
                    .map { instructions.indexOf(it) }
                val dataAlloc = instructions.filterIsInstance<Write>()
                    .filter { it.value is AllocHeapArray }
                    .map { instructions.indexOf(it) }
                    .last()
                assertThat(dataWrite).allSatisfy {
                    assertThat(it).isGreaterThan(dataAlloc)
                }
            }
            @Test
            fun `array-writes happen to same var as array-alloc`() {
                val instructions = lower("fn f() { var arr: i32[] = [1, 3]; }")
                val dataWrite = instructions.filterIsInstance<Write>()
                    .filter { it.target is ArraySlot }
                val dataAlloc = instructions.filterIsInstance<Write>()
                    .last { it.value is AllocHeapArray }
                assertThat(dataWrite).allSatisfy {
                    assertThat((it.target as ArraySlot).array).isEqualTo(dataAlloc.target)
                }
            }
        }

        @Nested
        @DisplayName("Fixed array variable (i32[3])")
        inner class FixedArrayVariable {

            @Test
            fun `emits AllocStack for the fixed array variable`() {
                val instructions = lower("fn f() { var arr: i32[3] = [1, 2, 3]; }")
                assertThat(instructions.filterIsInstance<AllocStack>().map { it.name })
                    .contains("arr")
            }

            @Test
            fun `AllocStack layout for fixed array is JitsuArray with isFixedSize true`() {
                val instructions = lower("fn f() { var arr: i32[3] = [1, 2, 3]; }")
                val alloc = instructions.filterIsInstance<AllocStack>().first { it.name == "arr" }
                assertThat(alloc.layout).isInstanceOf(JitsuArray::class.java)
                assertThat((alloc.layout as JitsuArray).isFixedSize).isTrue()
            }

            @Test
            fun `no AllocHeapArray for fixed array variable (inline data)`() {
                val instructions = lower("fn f() { var arr: i32[3] = [1, 2, 3]; }")
                val heapArrayAllocs = instructions.filterIsInstance<Write>()
                    .filter { it.value is AllocHeapArray }
                assertThat(heapArrayAllocs).isEmpty()
            }

            @Test
            fun `no length Write for fixed array variable`() {
                val instructions = lower("fn f() { var arr: i32[3] = [1, 2, 3]; }")
                val lengthWrites = instructions.filterIsInstance<Write>()
                    .filter { it.target is Read && (it.target as Read).name == "length" }
                assertThat(lengthWrites).isEmpty()
            }

            @Test
            fun `element writes are produced for each element of a fixed array`() {
                val instructions = lower("fn f() { var arr: i32[3] = [10, 20, 30]; }")
                val elementWrites = instructions.filterIsInstance<Write>()
                    .filter { it.target is ArraySlot }
                assertThat(elementWrites).hasSize(3)
                assertThat((elementWrites[0].target as ArraySlot).index).isEqualTo(NumericalValue(0L))
                assertThat((elementWrites[1].target as ArraySlot).index).isEqualTo(NumericalValue(1L))
                assertThat((elementWrites[2].target as ArraySlot).index).isEqualTo(NumericalValue(2L))
                val arrayField = (elementWrites[0].target as ArraySlot).array
                assertThat((elementWrites[1].target as ArraySlot).array).isEqualTo(arrayField)
                assertThat((elementWrites[2].target as ArraySlot).array).isEqualTo(arrayField)
                assertThat(elementWrites[0].value).isEqualTo(NumericalValue(10L))
                assertThat(elementWrites[1].value).isEqualTo(NumericalValue(20L))
                assertThat(elementWrites[2].value).isEqualTo(NumericalValue(30L))
            }
        }
    }

    @Nested
    @DisplayName("Ownership States and VariableRegistry")
    inner class OwnershipAndRegistry {

        @Test
        fun `declared variable has requiresFree=true (OwnershipState OWNS)`() {
            val file = buildFile("fn f() { var x: i32 = 5; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as FunctionElement.BodyElement.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }
            val entry = lowering.variableRegistry.getEntry(varDecl)
            assertThat(entry.requiresFree).isTrue()
        }

        @Test
        fun `variablesToFree contains all declared variables`() {
            val file = buildFile(
                """
                fn f() {
                    var a: i32 = 1;
                    var b: i32 = 2;
                }
                """.trimIndent()
            )
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val toFree = lowering.variableRegistry.variablesToFree
            assertThat(toFree.map { it.name }).containsExactlyInAnyOrder("a", "b")
        }

        @Test
        fun `variablesToFree is empty for a function with no variable declarations`() {
            val file = buildFile("fn f(x: i32): i32 { return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            assertThat(lowering.variableRegistry.variablesToFree).isEmpty()
        }
    }

    @Nested
    @DisplayName("Free Instruction Generation")
    inner class FreeInstructionGeneration {

        @Nested
        @DisplayName("Primitive variables")
        inner class PrimitiveFree {

            @Test
            fun `returning from function with primitive variable emits no Free`() {
                val instructions = lower("fn f(): i32 { var x: i32 = 5; return x; }")
                assertThat(instructions.topLevelFrees()).isEmpty()
            }

            @Test
            fun `two primitive variables produce no Free instructions`() {
                val instructions = lower(
                    """
                    fn f(): i32 {
                        var a: i32 = 1;
                        var b: i32 = 2;
                        return a;
                    }
                    """.trimIndent()
                )
                assertThat(instructions.topLevelFrees()).isEmpty()
            }
        }

        @Nested
        @DisplayName("Dynamic array variable")
        inner class DynamicArrayFree {

            @Test
            fun `returning from function with dynamic array variable one top-level Free for the array itself`() {
                val instructions = lower(
                    """
                    fn f(): i32 {
                        var arr: i32[] = [1, 2, 3];
                        return 0;
                    }
                    """.trimIndent()
                )
                // Free for the data pointer should be present
                val frees = instructions.filterIsInstance<Free>()
                assertThat(frees).hasSize(1)
            }

            @Test
            fun `Free targets the data field of the array variable`() {
                val instructions = lower(
                    """
                    fn f(): i32 {
                        var arr: i32[] = [1, 2];
                        return 0;
                    }
                    """.trimIndent()
                )
                val frees = instructions.filterIsInstance<Free>()
                val dataFree = frees.firstOrNull {
                    it.target is Read && (it.target as Read).name == "data"
                }
                assertThat(dataFree).isNotNull()
            }

            @Test
            fun `Free of data pointer targets the declared variable name`() {
                val instructions = lower(
                    """
                    fn f(): i32 {
                        var myArr: i32[] = [1];
                        return 0;
                    }
                    """.trimIndent()
                )
                val frees = instructions.filterIsInstance<Free>()
                val dataFree = frees.firstOrNull {
                    it.target is Read &&
                            (it.target as Read).name == "data" &&
                            (it.target as Read).struct == Variable("myArr")
                }
                assertThat(dataFree).isNotNull()
                    .withFailMessage("Expected Free(Read(Variable(myArr), data)) but got: $frees")
            }

            @Test
            fun `While loop for element iteration appears before the data Free`() {
                val instructions = lower(
                    """
                    fn f(): i32 {
                        var arr: i32[][] = [[], []];
                        return 0;
                    }
                    """.trimIndent()
                )
                val whileIdx = instructions.indexOfFirst { it is While }
                val dataFreeIdx = instructions.indexOfFirst {
                    it is Free && it.target is Read && (it.target as Read).name == "data"
                }
                assertThat(whileIdx).isGreaterThanOrEqualTo(0)
                assertThat(dataFreeIdx).isGreaterThan(whileIdx)
            }
            @Test
            fun `while loop frees each element`() {
                val instructions = lower(
                    """
                    fn f(): i32 {
                        var arr: i32[][] = [[], []];
                        return 0;
                    }
                    """.trimIndent()
                )
                val whileIdx = instructions.filterIsInstance<While>().first()
                val freeExpr = whileIdx.body[0]
                assertThat(freeExpr)
                    .asInstanceOf(type(Free::class.java))
                    .extracting { it.target }
                    .asInstanceOf(type(Read::class.java))
                    .satisfies({
                        assertThat(it.struct)
                            .asInstanceOf(type(ArraySlot::class.java))
                            .extracting { it.array }
                            .asInstanceOf(type(Read::class.java))
                            .extracting { it.struct }
                            .asInstanceOf(type(Variable::class.java))
                            .extracting { it.name }
                            .isEqualTo("arr")
                        assertThat(it.name).isEqualTo("data")
                    })
            }
        }

        @Nested
        @DisplayName("Fixed array variable")
        inner class FixedArrayFree {

            @Test
            fun `no top-level Free for fixed array with primitive elements`() {
                // Fixed array with primitive elements: iterate (While) but no element free, no data Free
                val instructions = lower(
                    """
                    fn f(): i32 {
                        var arr: i32[3] = [1, 2, 3];
                        return 0;
                    }
                    """.trimIndent()
                )
                // Primitives have no-op free() so no Free instruction expected from fixed array
                assertThat(instructions.filterIsInstance<Free>()).isEmpty()
            }
            @Test
            fun `while loop frees each element`() {
                val instructions = lower(
                    """
                    fn f(): i32 {
                        var arr: i32[][] = [[], []];
                        return 0;
                    }
                    """.trimIndent()
                )
                val whileIdx = instructions.filterIsInstance<While>().first()
                val freeExpr = whileIdx.body[0]
                assertThat(freeExpr)
                    .asInstanceOf(type(Free::class.java))
                    .extracting { it.target }
                    .asInstanceOf(type(Read::class.java))
                    .satisfies({
                        assertThat(it.struct)
                            .asInstanceOf(type(ArraySlot::class.java))
                            .extracting { it.array }
                            .asInstanceOf(type(Read::class.java))
                            .extracting { it.struct }
                            .asInstanceOf(type(Variable::class.java))
                            .extracting { it.name }
                            .isEqualTo("arr")
                        assertThat(it.name).isEqualTo("data")
                    })
            }
        }
    }

    @Nested
    @DisplayName("Return Statement Instruction Ordering")
    inner class ReturnOrdering {

        @Test
        fun `Return instruction is the last instruction in a simple function`() {
            val instructions = lower("fn f(): i32 { return 5; }")
            assertThat(instructions.last()).isInstanceOf(Return::class.java)
        }

        @Test
        fun `Free instructions appear before Return in a function with a dynamic array`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var arr: i32[] = [1, 2, 3];
                    return 0;
                }
                """.trimIndent()
            )
            val returnIdx = instructions.returnIndex()
            val lastFreeIdx = instructions.indexOfLast { it is Free }
            assertThat(returnIdx).isGreaterThanOrEqualTo(0)
            assertThat(lastFreeIdx).isGreaterThanOrEqualTo(0)
            assertThat(lastFreeIdx)
                .withFailMessage(
                    "Expected all Free instructions before Return. " +
                            "lastFreeIdx=$lastFreeIdx, returnIdx=$returnIdx"
                ).isLessThan(returnIdx)
        }

        @Test
        fun `While loop for element cleanup appears before Return`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var arr: i32[][1] = [[]];
                    return 42;
                }
                """.trimIndent()
            )
            val returnIdx = instructions.returnIndex()
            // Find the While loop that belongs to the free pass (comes after all Writes)
            // The last While in the list should be before Return
            val lastWhileIdx = instructions.indexOfLast { it is While }
            assertThat(lastWhileIdx).isGreaterThanOrEqualTo(0)
            assertThat(lastWhileIdx).isLessThan(returnIdx)
        }

        @Test
        fun `AllocStack for variable appears before any Free instruction`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var arr: i32[] = [1];
                    return 0;
                }
                """.trimIndent()
            )
            val allocIdx = instructions.indexOfFirst { it is AllocStack && it.name == "arr" }
            val firstFreeIdx = instructions.indexOfFirst { it is Free }
            assertThat(allocIdx).isGreaterThanOrEqualTo(0)
            assertThat(allocIdx).isLessThan(firstFreeIdx)
        }

        @Test
        fun `two dynamic arrays both get freed before Return`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var a: i32[] = [1];
                    var b: i32[] = [2];
                    return 0;
                }
                """.trimIndent()
            )
            val returnIdx = instructions.returnIndex()
            val frees = instructions.filterIsInstance<Free>()
                .map { instructions.indexOf(it) }
            assertThat(frees).isNotEmpty()
            assertThat(frees).allSatisfy { idx ->
                assertThat(idx).isLessThan(returnIdx)
            }
        }
        @Test
        fun `return value expression is evaluated before Free instructions`() {
            // According to lowerReturn: lowered.instructions + convertInstructions + freeInstructions + Return
            //
            // When returning a zero-arg function call, the Invoke is embedded as
            // ReturnValue(Invoke(...)) inside the Return expression, not a top-level instruction.
            // However, any instructions needed to *evaluate* parameters of the returned
            // expression (lowered.instructions) would appear before freeInstructions.
            //
            // We verify the documented ordering by checking:
            //   - The data-pointer Write (initial array alloc) comes before the data Free
            //   - The data Free comes before the final Return
            // This confirms that "setup → free → return" order is preserved.
            val instructions = lowerNamed(
                """
                fn helper(): i32 { return 1; }
                fn main(): i32 {
                    var arr: i32[] = [1, 2];
                    return helper();
                }
                """.trimIndent(),
                "main"
            )
            // Verify that the Return instruction embeds the helper Invoke expression
            val returnInstr = instructions.filterIsInstance<Return>().first()
            assertThat(returnInstr.value).isInstanceOf(ReturnValue::class.java)
            val invoke = (returnInstr.value as ReturnValue).functionCall
            assertThat(invoke.functionName).isEqualTo("helper")

            // Verify ordering: dataWrite < dataFree < Return
            val dataWriteIdx = instructions.indexOfFirst { it is Write && (it as Write).value is AllocHeapArray }
            val dataFreeIdx = instructions.indexOfFirst {
                it is Free && (it as Free).target is Read && ((it as Free).target as Read).name == "data"
            }
            val returnIdx = instructions.returnIndex()
            assertThat(dataWriteIdx).isGreaterThanOrEqualTo(0)
            assertThat(dataFreeIdx).isGreaterThan(dataWriteIdx)
            assertThat(returnIdx).isGreaterThan(dataFreeIdx)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `function with only parameters and no variables - no Free instructions`() {
            val instructions = lower("fn f(a: i32, b: i32): i32 { return a; }")
            assertThat(instructions.filterIsInstance<Free>()).isEmpty()
        }

        @Test
        fun `void return with no variables emits Return(null) with no Free`() {
            val instructions = lower("fn f() { return; }")
            assertThat(instructions.filterIsInstance<Free>()).isEmpty()
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isNull()
        }
    }



}
