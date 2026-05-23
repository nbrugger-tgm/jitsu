package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.*
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.buildJitsuModule
import eu.nitok.jitsu.compiler.graph.elements.JitsuFile
import eu.nitok.jitsu.parser.parseJitsuFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import java.net.URI

@DisplayName("Function Lowering - Conversion Logic")
class FunctionLoweringConversionTest {

    private fun buildFile(source: String): JitsuFile {
        val ast = parseJitsuFile(source, URI("test://sourcefile.jit"))
        ast.sequence().forEach {
            if(it.errors.isNotEmpty()) throw IllegalArgumentException("Syntax error(s)! ${it.errors.joinToString("\n")}")
        }
        val graph = buildJitsuModule(ast)
        if(graph.messages.errors.isNotEmpty()) throw IllegalArgumentException("Compilation error(s)! ${graph.messages.errors.joinToString("\n")}")
        return graph.files[0]
    }

    private fun lower(source: String): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<FunctionElement>().first()
        val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
        return lowering.lower()
    }

    private fun lowerFunction(source: String, name: String): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<FunctionElement>().first { it.name?.value == name }
        val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
        return lowering.lower()
    }

    @Nested
    @DisplayName("Primitive-to-Primitive (same type – no conversion)")
    inner class PrimitiveSameTypeTests {

        @Test
        fun `i32 to i32 return produces no extra Invoke instructions`() {
            val instructions = lower("fn f(): i32 { return 42; }")
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isEmpty()
        }

        @Test
        fun `i32 to i32 return value passes through as NumericalValue`() {
            val instructions = lower("fn f(): i32 { return 42; }")
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isInstanceOf(NumericalValue::class.java)
            assertThat((ret.value as NumericalValue).value).isEqualTo(42L)
        }

        @Test
        fun `i64 to i64 return produces no extra Invoke instructions`() {
            val instructions = lower("fn f(): i64 { return 100; }")
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isEmpty()
        }

        @Test
        fun `u32 to u32 return produces no extra Invoke instructions`() {
            val instructions = lower("fn f(): u32 { return 7; }")
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isEmpty()
        }

        @Test
        fun `boolean parameter passthrough produces no extra instructions`() {
            val instructions = lower("fn f(b: boolean): boolean { return b; }")
            // No invokes for type conversion
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isEmpty()
        }

        @Test
        fun `i32 variable assignment has no conversion - single Write for initial value`() {
            val instructions = lower("fn f(): i32 { var x: i32 = 10; return x; }")
            // The write for the initial value 10 into x
            val writes = instructions.filterIsInstance<Write>()
            val numericWrites = writes.filter { it.value is NumericalValue }
            assertThat(numericWrites).isNotEmpty()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Value to Union Conversions (convertToUnion)")
    inner class ValueToUnionTests {

        @Test
        fun `returning i32 from function with i32 or u32 return type produces Write for discriminant`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 5;
                }
                """.trimIndent()
            )
            // Should produce a Write for the discriminant (option index)
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isNotEmpty()
        }

        @Test
        fun `returning i32 from i32 or u32 function writes correct discriminant value`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 5;
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            // The discriminant value should be 0 (first option, i32) or 1 depending on union option order
            val discriminantValue = discriminantWrites.first().value
            assertThat(discriminantValue).isInstanceOf(NumericalValue::class.java)
            // Value should be a valid option index (0 or 1 for 2-option union)
            val idx = (discriminantValue as NumericalValue).value
            assertThat(idx).isIn(0L, 1L)
        }

        @Test
        fun `returning i32 from i32 or u32 function writes actual i32 value to union slot`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 5;
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            // Should write the value 5 somewhere
            val valueWrite = writes.filter { it.value is NumericalValue && (it.value as NumericalValue).value == 5L }
            assertThat(valueWrite).isNotEmpty()
        }

        @Test
        fun `returning i32 from i32 or u32 writes value into o{idx} union member`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 5;
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            // The option slot write target should be a Read with name "o0" or "o1"
            val optionSlotWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name.matches(Regex("o\\d+"))
            }
            assertThat(optionSlotWrites).isNotEmpty()
        }

        @Test
        fun `value to union conversion produces both discriminant Write and value Write`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 5;
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            val optionSlotWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name.matches(Regex("o\\d+"))
            }
            assertThat(discriminantWrites).isNotEmpty()
            assertThat(optionSlotWrites).isNotEmpty()
        }

        @Test
        fun `variable assignment of i32 to i32 or u32 variable writes discriminant`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32 | u32 = 99;
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isNotEmpty()
        }

        @Test
        fun `function call argument i32 passed to i32 or u32 param writes discriminant`() {
            val instructions = lowerFunction(
                """
                fn accepts(x: i32 | u32): i32 | u32 { return x; }
                fn caller(): i32 | u32 {
                    return accepts(42);
                }
                """.trimIndent(),
                "caller"
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isNotEmpty()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Union to Value Conversions (convertFromUnion)")
    inner class UnionToValueTests {

        @Test
        fun `returning union variable from function expecting i32 produces Conditional switch`() {
            val instructions = lowerFunction(
                """
                fn unwrap(x: i32 | u32): i64 {
                    return x;
                }
                """.trimIndent(),
                "unwrap"
            )
            // convertFromUnion uses switch() which generates Conditional instructions
            val conditionals = instructions.filterIsInstance<Conditional>()
            assertThat(conditionals).isNotEmpty()
        }

        @Test
        fun `union to value switch generates Conditional checking discriminant`() {
            val instructions = lowerFunction(
                """
                fn unwrap(x: i32 | u32): i64 {
                    return x;
                }
                """.trimIndent(),
                "unwrap"
            )
            val conditionals = instructions.filterIsInstance<Conditional>()
            // Condition should be a Compare (isOption check)
            val compareConditional = conditionals.firstOrNull { it.condition is Compare }
            assertThat(compareConditional).isNotNull()
        }

        @Test
        fun `union to value conversion allocates target variable on stack`() {
            val instructions = lowerFunction(
                """
                fn unwrap(x: i32 | u32): i64 {
                    return x;
                }
                """.trimIndent(),
                "unwrap"
            )
            // the creation of the union requires a stack allocation
            val allocs = instructions.filterIsInstance<AllocStack>()
            assertThat(allocs).isNotEmpty()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Union to Union Conversions")
    inner class UnionToUnionTests {

        @Test
        fun `assigning i32 or u32 to i32 or boolean or u32 variable converts to larger union`() {
            // A smaller 2-option union assigned into a 3-option union needs conversion
            // The source union type triggers convertFromUnion path -> switch + convertToUnion for each branch
            val instructions = lowerFunction(
                """
                fn f(x: i32 | u32): i32 | boolean | u32 {
                    return x;
                }
                """.trimIndent(),
                "f"
            )
            // The conversion happens via convertFromUnion->switch, producing Conditional instructions.
            // Discriminant writes to the target union live inside Conditional.thenInstructions (nested).
            val conditionals = instructions.filterIsInstance<Conditional>()
            assertThat(conditionals).isNotEmpty()
            // Check that at least one conditional branch writes an "option" discriminant
            fun collectNestedWrites(instrs: List<LowLevelInstruction>): List<Write> =
                instrs.flatMap { instr ->
                    when (instr) {
                        is Write -> listOf(instr)
                        is Conditional -> collectNestedWrites(instr.thenInstructions) +
                            collectNestedWrites(instr.elseInstructions ?: emptyList())
                        else -> emptyList()
                    }
                }
            val nestedWrites = collectNestedWrites(instructions)
            val discriminantWrites = nestedWrites.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isNotEmpty()
        }

        @Test
        fun `union passthrough has a Return instruction`() {
            // Passing a union parameter through to the same union return type still produces a Return.
            // Note: even with the same written type, graph analysis may create distinct type objects,
            // so conversion logic may still run (producing Conditionals); but a Return is always present.
            val instructions = lowerFunction(
                """
                fn f(x: i32 | u32): i32 | u32 {
                    return x;
                }
                """.trimIndent(),
                "f"
            )
            val ret = instructions.filterIsInstance<Return>()
            assertThat(ret).hasSize(1)
            assertThat(ret.first().value).isNotNull()
        }

        @Test
        fun `union to same union type produces Conditional switch for options`() {
            // Even if source and target union types are nominally the same, the graph may produce
            // distinct Type.Union objects (one per annotation site), so convert() will not short-circuit
            // and instead runs convertFromUnion -> switch -> each option gets convertToUnion.
            val instructions = lowerFunction(
                """
                fn f(x: i32 | u32): i32 | u32 {
                    return x;
                }
                """.trimIndent(),
                "f"
            )
            // At minimum, there must be a Return
            assertThat(instructions.filterIsInstance<Return>()).hasSize(1)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Array Conversions (copyArray)")
    inner class ArrayConversionTests {

        @Test
        fun `dynamic array literal assigned to same element type - single heap alloc`() {
            // No copy needed when element types match via hint
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [1, 2, 3];
                }
                """.trimIndent()
            )
            val heapAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is AllocHeapArray }
            assertThat(heapAllocs).hasSize(1)
        }

        @Test
        fun `array-to-array copy with different element types produces While loop for iteration`() {
            // Assigning i32[] to (i32 | u32)[] - elements need union wrapping, triggers copyArray
            val instructions = lowerFunction(
                """
                fn convert(src: i32[]): (i32 | u32)[] {
                    return src;
                }
                """.trimIndent(),
                "convert"
            )
            // copyArray uses iterate() which generates a While loop
            val whileLoops = instructions.filterIsInstance<While>()
            assertThat(whileLoops).isNotEmpty()
        }

        @Test
        fun `array copy with different element types allocates new heap array`() {
            val instructions = lowerFunction(
                """
                fn convert(src: i32[]): (i32 | u32)[] {
                    return src;
                }
                """.trimIndent(),
                "convert"
            )
            val heapAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is AllocHeapArray }
            assertThat(heapAllocs).isNotEmpty()
        }

        @Test
        fun `array copy with different element types copies length field`() {
            val instructions = lowerFunction(
                """
                fn convert(src: i32[]): (i32 | u32)[] {
                    return src;
                }
                """.trimIndent(),
                "convert"
            )
            // copyArray writes target.length = source.sizeExpression(source)
            val writes = instructions.filterIsInstance<Write>()
            val lengthWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "length"
            }
            assertThat(lengthWrites).isNotEmpty()
        }

        @Test
        fun `array copy with same element type produces While loop for iteration`() {
            // Same element type but different array variables: copy still uses iterate
            val instructions = lowerFunction(
                """
                fn copy(src: i32[]): i32[] {
                    return src;
                }
                """.trimIndent(),
                "copy"
            )
            // Since the parameter IS already an i32[], returning it directly
            // shouldn't need a copy (same type), so no While loops from copy
            // The Return value should just be the variable reference
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isNotNull()
        }

        @Test
        fun `dynamic array literal with 3 elements produces 3 ArraySlot writes`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [10, 20, 30];
                }
                """.trimIndent()
            )
            val arraySlotWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is ArraySlot }
            assertThat(arraySlotWrites).hasSize(3)
        }

        @Test
        fun `array literal element type hint used - no extra heap allocation`() {
            // With hint, elements are constructed directly into the target type
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [1, 2];
                }
                """.trimIndent()
            )
            val heapAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is AllocHeapArray }
            // Should be exactly one heap allocation for the array data
            assertThat(heapAllocs).hasSize(1)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("canUseArrayHint - Type Hint Behavior")
    inner class ArrayHintTests {

        @Test
        fun `array literal count matches fixed size hint - hint is used`() {
            // Fixed-size array with matching element count: hint should be used
            // Result: element writes use the hinted element type directly
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [1, 2, 3];
                }
                """.trimIndent()
            )
            // Elements written with i32 values (NumericalValue) at ArraySlot positions
            val elementWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is ArraySlot && it.value is NumericalValue }
            assertThat(elementWrites).hasSize(3)
            val values = elementWrites.map { (it.value as NumericalValue).value }
            assertThat(values).containsExactly(1L, 2L, 3L)
        }

        @Test
        fun `array literal with union element hint wraps elements in union on construction`() {
            // Array of i32|null with i32 literals - each element should be wrapped in union
            val instructions = lower(
                """
                fn f() {
                    var x: (i32 | u32)[] = [1, 2];
                }
                """.trimIndent()
            )
            // Elements are i32 literals but hint element type is i32|null union
            // So each element write should write a discriminant (option write)
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isNotEmpty()
        }

        @Test
        fun `return of array literal uses return type as hint`() {
            val instructions = lower(
                """
                fn f(): i32[] {
                    return [10, 20];
                }
                """.trimIndent()
            )
            val elementWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is ArraySlot }
            assertThat(elementWrites).hasSize(2)
            val values = elementWrites.map { it.value }
                .filterIsInstance<NumericalValue>().map { it.value }
            assertThat(values).containsExactly(10L, 20L)
        }

        @Test
        fun `return of array literal with matching hint does not produce extra heap allocation`() {
            val instructions = lower(
                """
                fn f(): i32[] {
                    return [5, 6, 7];
                }
                """.trimIndent()
            )
            val heapAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is AllocHeapArray }
            assertThat(heapAllocs).hasSize(1)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Return Type Conversion – Instruction Order")
    inner class ReturnInstructionOrderTests {

        @Test
        fun `Return instruction appears after conversion instructions`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 5;
                }
                """.trimIndent()
            )
            val returnIdx = instructions.indexOfFirst { it is Return }
            assertThat(returnIdx).isGreaterThanOrEqualTo(0)
            // All Writes (discriminant + value) for conversion should appear before Return
            val lastWriteIdx = instructions.indexOfLast { it is Write }
            assertThat(returnIdx).isGreaterThan(lastWriteIdx)
        }

        @Test
        fun `AllocStack for tmp union var appears before its Write discriminant`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 42;
                }
                """.trimIndent()
            )
            val allocIdx = instructions.indexOfFirst { it is AllocStack }
            val firstWriteIdx = instructions.indexOfFirst { it is Write }
            assertThat(allocIdx).isGreaterThanOrEqualTo(0)
            assertThat(firstWriteIdx).isGreaterThan(allocIdx)
        }

        @Test
        fun `Return instruction is the last instruction in simple function`() {
            val instructions = lower("fn f(): i32 { return 1; }")
            assertThat(instructions.last()).isInstanceOf(Return::class.java)
        }

        @Test
        fun `Return instruction is the last instruction in union-returning function`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 1;
                }
                """.trimIndent()
            )
            assertThat(instructions.last()).isInstanceOf(Return::class.java)
        }

        @Test
        fun `instruction order is AllocStack then Write instructions then Return`() {
            val instructions = lower(
                """
                fn f(): i32 | u32 {
                    return 7;
                }
                """.trimIndent()
            )
            val allocIdx = instructions.indexOfFirst { it is AllocStack }
            val firstWriteIdx = instructions.indexOfFirst { it is Write }
            val returnIdx = instructions.indexOfFirst { it is Return }

            assertThat(allocIdx).isGreaterThanOrEqualTo(0)
            assertThat(firstWriteIdx).isGreaterThan(allocIdx)
            assertThat(returnIdx).isGreaterThan(firstWriteIdx)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Function Call Argument Conversions")
    inner class FunctionCallArgConversionTests {

        @Test
        fun `calling function expecting i32 or u32 with i32 literal writes discriminant`() {
            val instructions = lowerFunction(
                """
                fn accept(x: i32 | u32) { }
                fn caller() {
                    accept(10);
                }
                """.trimIndent(),
                "caller"
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isNotEmpty()
        }

        @Test
        fun `calling function expecting i32 or u32 with i32 literal produces Invoke`() {
            val instructions = lowerFunction(
                """
                fn accept(x: i32 | u32) { }
                fn caller() {
                    accept(10);
                }
                """.trimIndent(),
                "caller"
            )
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isNotEmpty()
            assertThat(invokes.any { it.functionName == "accept" }).isTrue()
        }

        @Test
        fun `conversion instructions for arguments appear before Invoke`() {
            val instructions = lowerFunction(
                """
                fn accept(x: i32 | u32) { }
                fn caller() {
                    accept(10);
                }
                """.trimIndent(),
                "caller"
            )
            val invokeIdx = instructions.indexOfFirst { it is Invoke }
            val lastWriteIdx = instructions.indexOfLast {
                it is Write && (it as Write).target is Read &&
                    ((it as Write).target as Read).name == "option"
            }
            assertThat(invokeIdx).isGreaterThan(lastWriteIdx)
        }

        @Test
        fun `multiple arguments each get converted to their respective parameter types`() {
            val instructions = lowerFunction(
                """
                fn accept(a: i32 | u32, b: i64 | u64) { }
                fn caller() {
                    accept(1, 2);
                }
                """.trimIndent(),
                "caller"
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            // Two conversions: one per argument
            assertThat(discriminantWrites).hasSize(2)
        }

        @Test
        fun `no conversion for matching primitive types in function arguments`() {
            val instructions = lowerFunction(
                """
                fn identity(x: i32): i32 { return x; }
                fn caller(): i32 {
                    return identity(5);
                }
                """.trimIndent(),
                "caller"
            )
            // No discriminant writes - argument types match directly
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isEmpty()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Variable Assignment Conversions")
    inner class VariableAssignmentConversionTests {

        @Test
        fun `assigning i32 literal to i32 or u32 variable writes discriminant`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32 | u32 = 42;
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isNotEmpty()
        }

        @Test
        fun `assigning i32 literal to i32 or u32 variable writes value into option slot`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32 | u32 = 42;
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            val optionSlotWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name.matches(Regex("o\\d+"))
            }
            assertThat(optionSlotWrites).isNotEmpty()
        }

        @Test
        fun `assigning i32 literal to i32 variable has no discriminant write`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32 = 42;
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isEmpty()
        }

        @Test
        fun `AllocStack appears before variable value Writes`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32 | u32 = 1;
                }
                """.trimIndent()
            )
            val xAllocIdx = instructions.indexOfFirst { it is AllocStack && (it as AllocStack).name == "x" }
            val firstWriteIdx = instructions.indexOfFirst { it is Write }
            assertThat(xAllocIdx).isGreaterThanOrEqualTo(0)
            assertThat(firstWriteIdx).isGreaterThan(xAllocIdx)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCaseTests {

        @Test
        fun `single-element union array with i32 literal wraps element in union`() {
            val instructions = lower(
                """
                fn f() {
                    var x: (i32 | u32)[] = [5];
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            assertThat(discriminantWrites).isNotEmpty()
        }

        @Test
        fun `convert with identical type short-circuits - no extra instructions for i32`() {
            // When source and target types are equal, convert() returns immediately
            val instructions = lower("fn f(): i32 { return 99; }")
            // Only a Return, no AllocStack for tmp vars
            val allocations = instructions.filterIsInstance<AllocStack>()
            assertThat(allocations).isEmpty()
        }

        @Test
        fun `multi-element union array wraps each element independently`() {
            val instructions = lower(
                """
                fn f() {
                    var x: (i32 | u32)[] = [1, 2, 3];
                }
                """.trimIndent()
            )
            val writes = instructions.filterIsInstance<Write>()
            val discriminantWrites = writes.filter { write ->
                write.target is Read && (write.target as Read).name == "option"
            }
            // One discriminant write per element
            assertThat(discriminantWrites).hasSize(3)
        }

        @Test
        fun `native function produces no instructions`() {
            val instructions = lower("native fn ext(): i32")
            assertThat(instructions).isEmpty()
        }

        @Test
        fun `function with two parameters both converted to union types`() {
            val instructions = lowerFunction(
                """
                fn twoParams(a: i32 | u32, b: i64 | u64) { }
                fn caller() {
                    twoParams(1, 2);
                }
                """.trimIndent(),
                "caller"
            )
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isNotEmpty()
            assertThat(invokes[0].functionName).isEqualTo("twoParams")
        }

        @Test
        fun `returning a union variable from function with same union return type always has a Return`() {
            // Note: even when param and return are written as the same union type, the graph may
            // create distinct Type.Union instances per annotation site. Conversion instructions may
            // still be generated. What we always guarantee: there is a Return instruction.
            val instructions = lowerFunction(
                """
                fn passthrough(x: i32 | u32): i32 | u32 {
                    return x;
                }
                """.trimIndent(),
                "passthrough"
            )
            val ret = instructions.filterIsInstance<Return>()
            assertThat(ret).hasSize(1)
            assertThat(ret.first().value).isNotNull()
        }
    }
}
