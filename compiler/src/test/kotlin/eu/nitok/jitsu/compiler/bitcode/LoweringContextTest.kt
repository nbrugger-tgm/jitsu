package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.*
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement.IntConstant
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement.UIntConstant
import eu.nitok.jitsu.compiler.graph.elements.types.*
import eu.nitok.jitsu.compiler.graph.elements.types.Array
import eu.nitok.jitsu.compiler.graph.elements.types.Boolean
import eu.nitok.jitsu.compiler.graph.elements.types.Float
import eu.nitok.jitsu.compiler.graph.elements.types.Int
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

@DisplayName("LoweringContext")
class LoweringContextTest {
    val dummyLocation = Location(
        URI("file://x"),
        0, 0, 0, 0
    )

    private val graphI32 = Int(BitSize.BIT_32)
    private val graphI64 = Int(BitSize.BIT_64)
    private val graphF64 = Float(BitSize.BIT_64)
    private val graphBool = Boolean

    @Nested
    @DisplayName("nextTmpName()")
    inner class NextTmpName {

        @Test
        @DisplayName("first call returns tmp_0")
        fun firstCallReturnsTmp0() {
            val ctx = LoweringContext()
            assertThat(ctx.nextTmpName()).isEqualTo("tmp_0")
        }

        @Test
        @DisplayName("subsequent calls are sequential: tmp_0, tmp_1, tmp_2")
        fun subsequentCallsAreSequential() {
            val ctx = LoweringContext()
            assertThat(ctx.nextTmpName()).isEqualTo("tmp_0")
            assertThat(ctx.nextTmpName()).isEqualTo("tmp_1")
            assertThat(ctx.nextTmpName()).isEqualTo("tmp_2")
        }

        @Test
        @DisplayName("names are unique within a single context instance")
        fun namesAreUniqueWithinContext() {
            val ctx = LoweringContext()
            val names = (0 until 50).map { ctx.nextTmpName() }
            assertThat(names).doesNotHaveDuplicates()
        }

        @Test
        @DisplayName("different context instances have independent counters")
        fun differentInstancesHaveIndependentCounters() {
            val ctx1 = LoweringContext()
            val ctx2 = LoweringContext()

            // Advance ctx1 by a few steps
            ctx1.nextTmpName()
            ctx1.nextTmpName()

            // ctx2 should still start at tmp_0
            assertThat(ctx2.nextTmpName()).isEqualTo("tmp_0")
            // ctx1 should continue from where it left off
            assertThat(ctx1.nextTmpName()).isEqualTo("tmp_2")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createTmpVar(type)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTmpVar(type)")
    inner class CreateTmpVar {

        @Test
        @DisplayName("returns Variable with correct name and AllocStack for primitive int type")
        fun primitiveInt_returnsVariableAndAllocStack() {
            val ctx = LoweringContext()
            val type = I32

            val (variable, instructions) = ctx.createTmpVar(type)

            assertThat(variable.name).isEqualTo("tmp_0")
            assertThat(instructions).hasSize(1)
            assertThat(instructions[0]).isInstanceOf(AllocStack::class.java)
            val allocStack = instructions[0] as AllocStack
            assertThat(allocStack.name).isEqualTo("tmp_0")
            assertThat(allocStack.layout).isEqualTo(type)
        }

        @Test
        @DisplayName("returns Variable with correct name and AllocStack for bool type")
        fun primitiveBool_returnsVariableAndAllocStack() {
            val ctx = LoweringContext()
            val type = LLBool(Boolean)

            val (variable, instructions) = ctx.createTmpVar(type)

            assertThat(variable.name).isEqualTo("tmp_0")
            assertThat(instructions).hasSize(1)
            assertThat(instructions[0]).isEqualTo(AllocStack("tmp_0", LLBool(Boolean)))
        }

        @Test
        @DisplayName("returns Variable with correct name and AllocStack for float type")
        fun primitiveFloat_returnsVariableAndAllocStack() {
            val ctx = LoweringContext()
            val type = F64

            val (variable, instructions) = ctx.createTmpVar(type)

            assertThat(variable.name).isEqualTo("tmp_0")
            assertThat(instructions).containsExactly(AllocStack("tmp_0", F64))
        }

        @Test
        @DisplayName("for pointer type: instructions are [AllocStack, Write(var, AllocHeap)]")
        fun pointerType_returnsAllocStackAndWriteWithAllocHeap() {
            val ctx = LoweringContext()
            val pointeeType = I64
            val pointerType = LLPointer(pointeeType, graphI64)

            val (variable, instructions) = ctx.createTmpVar(pointerType)

            assertThat(variable.name).isEqualTo("tmp_0")
            assertThat(instructions).hasSize(2)

            val allocStack = instructions[0] as AllocStack
            assertThat(allocStack.name).isEqualTo("tmp_0")
            assertThat(allocStack.layout).isEqualTo(pointerType)

            val write = instructions[1] as Write
            assertThat(write.target).isEqualTo(Variable("tmp_0"))
            assertThat(write.value).isEqualTo(AllocHeap(pointeeType))
        }

        @Test
        @DisplayName("for struct type: instructions are [AllocStack] only (no extra alloc)")
        fun structType_returnsAllocStackOnly() {
            val ctx = LoweringContext()
            val structType = LLStruct(
                mapOf("x" to I32, "y" to I64),
                graphI32
            )

            val (variable, instructions) = ctx.createTmpVar(structType)

            assertThat(variable.name).isEqualTo("tmp_0")
            assertThat(instructions).hasSize(1)
            assertThat(instructions[0]).isEqualTo(AllocStack("tmp_0", structType))
        }

        @Test
        @DisplayName("for dynamic JitsuArray: instructions are [AllocStack] only (arrays need explicit alloc)")
        fun dynamicJitsuArray_returnsAllocStackOnly() {
            val ctx = LoweringContext()
            val arrayType = JitsuArray.dynamic(I32, I32, Array(graphI32, null))

            val (variable, instructions) = ctx.createTmpVar(arrayType)

            assertThat(variable.name).isEqualTo("tmp_0")
            // JitsuArray.dynamic delegates to LLStruct which has no allocate() override → only AllocStack
            assertThat(instructions).hasSize(1)
            assertThat(instructions[0]).isInstanceOf(AllocStack::class.java)
            val allocStack = instructions[0] as AllocStack
            assertThat(allocStack.name).isEqualTo("tmp_0")
        }

        @Test
        @DisplayName("for JitsuUnion: instructions are [AllocStack] only")
        fun jitsuUnion_returnsAllocStackOnly() {
            val ctx = LoweringContext()
            val unionGraphType = Union(listOf(graphI32, graphF64))
            val unionType = JitsuUnion.of(unionGraphType, listOf(I32, F64))

            val (variable, instructions) = ctx.createTmpVar(unionType)

            assertThat(variable.name).isEqualTo("tmp_0")
            assertThat(instructions).hasSize(1)
            assertThat(instructions[0]).isInstanceOf(AllocStack::class.java)
        }

        @Test
        @DisplayName("variable name matches AllocStack name")
        fun variableNameMatchesAllocStackName() {
            val ctx = LoweringContext()
            val (variable, instructions) = ctx.createTmpVar(I32)
            val allocStack = instructions[0] as AllocStack
            assertThat(variable.name).isEqualTo(allocStack.name)
        }

        @Test
        @DisplayName("type matches AllocStack layout for primitive")
        fun typeMatchesAllocStackLayout() {
            val ctx = LoweringContext()
            val type = I64
            val (_, instructions) = ctx.createTmpVar(type)
            val allocStack = instructions[0] as AllocStack
            assertThat(allocStack.layout).isEqualTo(type)
        }

        @Test
        @DisplayName("sequential calls produce different variable names: tmp_0, tmp_1, tmp_2")
        fun sequentialCallsProduceDifferentNames() {
            val ctx = LoweringContext()

            val (var0, instrs0) = ctx.createTmpVar(I32)
            val (var1, instrs1) = ctx.createTmpVar(I32)
            val (var2, instrs2) = ctx.createTmpVar(I32)

            assertThat(var0.name).isEqualTo("tmp_0")
            assertThat(var1.name).isEqualTo("tmp_1")
            assertThat(var2.name).isEqualTo("tmp_2")

            assertThat((instrs0[0] as AllocStack).name).isEqualTo("tmp_0")
            assertThat((instrs1[0] as AllocStack).name).isEqualTo("tmp_1")
            assertThat((instrs2[0] as AllocStack).name).isEqualTo("tmp_2")
        }

        @Test
        @DisplayName("pointer to struct: AllocStack + Write(var, AllocHeap(structType))")
        fun pointerToStruct_allocStackAndWriteAllocHeapStruct() {
            val ctx = LoweringContext()
            val structType = LLStruct(mapOf("a" to I32), graphI32)
            val pointerType = LLPointer(structType, graphI32)

            val (variable, instructions) = ctx.createTmpVar(pointerType)

            assertThat(instructions).hasSize(2)
            val write = instructions[1] as Write
            assertThat(write.value).isEqualTo(AllocHeap(structType))
            assertThat(write.target).isEqualTo(Variable(variable.name))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // asField(expression, type)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("asField(expression, type)")
    inner class AsField {

        @Test
        @DisplayName("if expression is already a Variable (Field), returns it unchanged with empty instructions")
        fun fieldExpression_returnsSameExpressionWithEmptyInstructions() {
            val ctx = LoweringContext()
            val variable = Variable("existingVar")

            val (field, instructions) = ctx.asField(variable, I32)

            assertThat(field).isSameAs(variable)
            assertThat(instructions).isEmpty()
            // Counter should not have advanced
            assertThat(ctx.nextTmpName()).isEqualTo("tmp_0")
        }

        @Test
        @DisplayName("if expression is Deref (Field), returns it unchanged with empty instructions")
        fun derefExpression_returnsSameExpressionWithEmptyInstructions() {
            val ctx = LoweringContext()
            val deref = Deref(Variable("ptr"))

            val (field, instructions) = ctx.asField(deref, I32)

            assertThat(field).isSameAs(deref)
            assertThat(instructions).isEmpty()
        }

        @Test
        @DisplayName("if expression is Read (Field), returns it unchanged with empty instructions")
        fun readExpression_returnsSameExpressionWithEmptyInstructions() {
            val ctx = LoweringContext()
            val read = Read(Variable("s"), "x")

            val (field, instructions) = ctx.asField(read, I32)

            assertThat(field).isSameAs(read)
            assertThat(instructions).isEmpty()
        }

        @Test
        @DisplayName("if expression is Ref (Field), returns it unchanged with empty instructions")
        fun refExpression_returnsSameExpressionWithEmptyInstructions() {
            val ctx = LoweringContext()
            val ref = Ref(Variable("v"))

            val (field, instructions) = ctx.asField(ref, I32)

            assertThat(field).isSameAs(ref)
            assertThat(instructions).isEmpty()
        }

        @Test
        @DisplayName("NumericalValue → creates tmp var and writes value to it")
        fun numericalValue_createsTmpVarAndWritesValue() {
            val ctx = LoweringContext()
            val numValue = NumericalValue(42L)

            val (field, instructions) = ctx.asField(numValue, I32)

            // field should be the new tmp variable
            assertThat(field).isEqualTo(Variable("tmp_0"))

            // instructions: [AllocStack(tmp_0, I32), Write(tmp_0, numValue)]
            assertThat(instructions).hasSize(2)
            assertThat(instructions[0]).isEqualTo(AllocStack("tmp_0", I32))
            val write = instructions[1] as Write
            assertThat(write.target).isEqualTo(Variable("tmp_0"))
            assertThat(write.value).isEqualTo(numValue)
        }

        @Test
        @DisplayName("ReturnValue → creates tmp var and writes return value to it")
        fun returnValue_createsTmpVarAndWritesValue() {
            val ctx = LoweringContext()
            val invoke = Invoke("someFunc", mapOf("arg" to NumericalValue(1L)))
            val retValue = ReturnValue(invoke)

            val (field, instructions) = ctx.asField(retValue, I64)

            assertThat(field).isEqualTo(Variable("tmp_0"))
            assertThat(instructions).hasSize(2)
            assertThat(instructions[0]).isEqualTo(AllocStack("tmp_0", I64))
            val write = instructions[1] as Write
            assertThat(write.target).isEqualTo(Variable("tmp_0"))
            assertThat(write.value).isEqualTo(retValue)
        }

        @Test
        @DisplayName("Compare → creates tmp var and writes result to it")
        fun compare_createsTmpVarAndWritesResult() {
            val ctx = LoweringContext()
            val compare = Compare(NumericalValue(1L), NumericalValue(2L))

            val (field, instructions) = ctx.asField(compare, LLBool(Boolean))

            assertThat(field).isEqualTo(Variable("tmp_0"))
            assertThat(instructions).hasSize(2)
            assertThat(instructions[0]).isEqualTo(AllocStack("tmp_0", LLBool(Boolean)))
            val write = instructions[1] as Write
            assertThat(write.value).isEqualTo(compare)
        }

        @Test
        @DisplayName("AllocHeap → creates tmp var and writes heap pointer to it (pointer type)")
        fun allocHeap_createsTmpVarAndWritesHeapPointer() {
            val ctx = LoweringContext()
            val allocHeap = AllocHeap(I32)
            val pointerType = LLPointer(I32, graphI32)

            val (field, instructions) = ctx.asField(allocHeap, pointerType)

            // field should be the new tmp variable
            assertThat(field).isEqualTo(Variable("tmp_0"))

            // instructions from createTmpVar(pointerType): [AllocStack, Write(var, AllocHeap(I32))]
            // instructions from write(pointerType, tmpVar, allocHeap): [Write(Deref(var), allocHeap)]
            // total: 3 instructions
            assertThat(instructions).hasSize(3)
            assertThat(instructions[0]).isEqualTo(AllocStack("tmp_0", pointerType))

            val heapInit = instructions[1] as Write
            assertThat(heapInit.target).isEqualTo(Variable("tmp_0"))
            assertThat(heapInit.value).isEqualTo(AllocHeap(I32))

            val writeValue = instructions[2] as Write
            assertThat(writeValue.target).isEqualTo(Deref(Variable("tmp_0")))
            assertThat(writeValue.value).isEqualTo(allocHeap)
        }

        @Test
        @DisplayName("AllocHeapArray → creates tmp var and writes it (non-Field expression needs capture)")
        fun allocHeapArray_createsTmpVarAndWritesIt() {
            val ctx = LoweringContext()
            val allocHeapArray = AllocHeapArray(I32, NumericalValue(10L))

            val (field, instructions) = ctx.asField(allocHeapArray, I32)

            assertThat(field).isEqualTo(Variable("tmp_0"))
            assertThat(instructions).hasSize(2)
            val write = instructions[1] as Write
            assertThat(write.value).isEqualTo(allocHeapArray)
        }

        @Test
        @DisplayName("non-Field with pointer type: write goes through Deref")
        fun nonFieldWithPointerType_writeThroughDeref() {
            val ctx = LoweringContext()
            val numValue = NumericalValue(99L)
            val pointerType = LLPointer(I32, graphI32)

            val (field, instructions) = ctx.asField(numValue, pointerType)

            assertThat(field).isEqualTo(Variable("tmp_0"))
            // AllocStack + Write(tmp_0, AllocHeap) + Write(Deref(tmp_0), numValue)
            assertThat(instructions).hasSize(3)
            val lastWrite = instructions[2] as Write
            assertThat(lastWrite.target).isEqualTo(Deref(Variable("tmp_0")))
            assertThat(lastWrite.value).isEqualTo(numValue)
        }

        @Test
        @DisplayName("ArraySlot (Field) → returned unchanged with empty instructions")
        fun arraySlot_returnsSameExpressionWithEmptyInstructions() {
            val ctx = LoweringContext()
            val arraySlot = ArraySlot(Variable("arr"), NumericalValue(0L))

            val (field, instructions) = ctx.asField(arraySlot, I32)

            assertThat(field).isSameAs(arraySlot)
            assertThat(instructions).isEmpty()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // write(targetType, target, value)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("write(targetType, target, value)")
    inner class WriteMethod {

        @Test
        @DisplayName("primitive type: Write(target, value) — no deref")
        fun primitiveType_writeDirectly() {
            val ctx = LoweringContext()
            val target = Variable("x")
            val value = NumericalValue(7L)

            val instructions = ctx.write(I32, target, value)

            assertThat(instructions).containsExactly(Write(target, value))
        }

        @Test
        @DisplayName("bool type: Write(target, value)")
        fun boolType_writeDirectly() {
            val ctx = LoweringContext()
            val target = Variable("flag")
            val value = NumericalValue(1L)

            val instructions = ctx.write(LLBool(Boolean), target, value)

            assertThat(instructions).containsExactly(Write(target, value))
        }

        @Test
        @DisplayName("pointer type: Write(Deref(target), value)")
        fun pointerType_writeThroughDeref() {
            val ctx = LoweringContext()
            val target = Variable("ptr")
            val value = NumericalValue(42L)
            val pointerType = LLPointer(I32, graphI32)

            val instructions = ctx.write(pointerType, target, value)

            assertThat(instructions).containsExactly(Write(Deref(target), value))
        }

        @Test
        @DisplayName("struct type: Write(target, value) — no deref")
        fun structType_writeDirectly() {
            val ctx = LoweringContext()
            val target = Variable("s")
            val value = Variable("other")
            val structType = LLStruct(mapOf("x" to I32), graphI32)

            val instructions = ctx.write(structType, target, value)

            assertThat(instructions).containsExactly(Write(target, value))
        }

        @Test
        @DisplayName("returns a list with exactly one Write instruction")
        fun returnsExactlyOneInstruction() {
            val ctx = LoweringContext()
            val instructions = ctx.write(I64, Variable("v"), NumericalValue(0L))
            assertThat(instructions).hasSize(1)
            assertThat(instructions[0]).isInstanceOf(Write::class.java)
        }

        @Test
        @DisplayName("uses targetType.writeAccess() — pointer to pointer: Write(Deref(ptr), value)")
        fun pointerToPointer_writeAccessUsesDeref() {
            val ctx = LoweringContext()
            val innerType = LLPointer(I32, graphI32)
            val outerType = LLPointer(innerType, graphI32)
            val target = Variable("pptr")
            val value = AllocHeap(I32)

            val instructions = ctx.write(outerType, target, value)

            // writeAccess for LLPointer returns Deref(target)
            assertThat(instructions).containsExactly(Write(Deref(target), value))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sequential calls / interaction between methods
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Sequential calls")
    inner class SequentialCalls {

        @Test
        @DisplayName("createTmpVar called multiple times produces incrementing names")
        fun multipleCreateTmpVar_incrementingNames() {
            val ctx = LoweringContext()

            val (v0, _) = ctx.createTmpVar(I32)
            val (v1, _) = ctx.createTmpVar(LLBool(Boolean))
            val (v2, _) = ctx.createTmpVar(F64)

            assertThat(v0.name).isEqualTo("tmp_0")
            assertThat(v1.name).isEqualTo("tmp_1")
            assertThat(v2.name).isEqualTo("tmp_2")
        }

        @Test
        @DisplayName("asField(non-Field) followed by createTmpVar produces consecutive names")
        fun asFieldThenCreateTmpVar_consecutiveNames() {
            val ctx = LoweringContext()

            val (field, _) = ctx.asField(NumericalValue(1L), I32)
            val (variable, _) = ctx.createTmpVar(I32)

            assertThat((field as Variable).name).isEqualTo("tmp_0")
            assertThat(variable.name).isEqualTo("tmp_1")
        }

        @Test
        @DisplayName("asField(Field) does NOT advance the counter")
        fun asFieldWithField_doesNotAdvanceCounter() {
            val ctx = LoweringContext()

            ctx.asField(Variable("existing"), I32) // should NOT advance counter

            assertThat(ctx.nextTmpName()).isEqualTo("tmp_0")
        }

        @Test
        @DisplayName("interleaved nextTmpName and createTmpVar share the same counter")
        fun interleavedCalls_shareCounter() {
            val ctx = LoweringContext()

            val name0 = ctx.nextTmpName()           // tmp_0
            val (var1, _) = ctx.createTmpVar(I32)   // tmp_1
            val name2 = ctx.nextTmpName()           // tmp_2
            val (var3, _) = ctx.createTmpVar(LLBool(Boolean)) // tmp_3

            assertThat(name0).isEqualTo("tmp_0")
            assertThat(var1.name).isEqualTo("tmp_1")
            assertThat(name2).isEqualTo("tmp_2")
            assertThat(var3.name).isEqualTo("tmp_3")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge cases
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCases {

        @Test
        @DisplayName("createTmpVar for nested pointer (pointer-to-pointer): AllocStack + Write(var, AllocHeap(inner))")
        fun nestedPointer_correctInstructions() {
            val ctx = LoweringContext()
            val innerPointer = LLPointer(I32, graphI32)
            val outerPointer = LLPointer(innerPointer, graphI32)

            val (variable, instructions) = ctx.createTmpVar(outerPointer)

            assertThat(instructions).hasSize(2)
            val write = instructions[1] as Write
            assertThat(write.target).isEqualTo(Variable(variable.name))
            assertThat(write.value).isEqualTo(AllocHeap(innerPointer))
        }

        @Test
        @DisplayName("createTmpVar for JitsuUnion: only AllocStack (no heap alloc)")
        fun jitsuUnion_onlyAllocStack() {
            val ctx = LoweringContext()
            val unionGraphType = Union(listOf(graphI32, graphI64))
            val unionType = JitsuUnion.of(unionGraphType, listOf(I32, I64))

            val (variable, instructions) = ctx.createTmpVar(unionType)

            assertThat(instructions).hasSize(1)
            val allocStack = instructions[0] as AllocStack
            assertThat(allocStack.name).isEqualTo(variable.name)
            assertThat(allocStack.layout).isEqualTo(unionType)
        }

        @Test
        @DisplayName("asField with Field does not create any tmp variable (counter stays at 0)")
        fun asFieldWithFieldNoSideEffects() {
            val ctx = LoweringContext()
            val field = Variable("someVar")

            val (result, instructions) = ctx.asField(field, I32)

            assertThat(result).isSameAs(field)
            assertThat(instructions).isEmpty()
            // Verify counter wasn't touched
            assertThat(ctx.nextTmpName()).isEqualTo("tmp_0")
        }

        @Test
        @DisplayName("write with LLUnion type: Write(target, value) — default writeAccess")
        fun llUnionType_writeDirectly() {
            val ctx = LoweringContext()
            val target = Variable("u")
            val value = NumericalValue(0L)
            val unionType = LLUnion(mapOf("a" to I32, "b" to I64), graphI32)

            val instructions = ctx.write(unionType, target, value)

            assertThat(instructions).containsExactly(Write(target, value))
        }

        @Test
        @DisplayName("createTmpVar with fixed JitsuArray: only AllocStack")
        fun fixedJitsuArray_onlyAllocStack() {
            val ctx = LoweringContext()
            val arrayType = JitsuArray.fixed(I32, I32, 4uL, Array(graphI32, UIntConstant(4u, dummyLocation)))

            val (variable, instructions) = ctx.createTmpVar(arrayType)

            assertThat(instructions).hasSize(1)
            assertThat(instructions[0]).isInstanceOf(AllocStack::class.java)
            val allocStack = instructions[0] as AllocStack
            assertThat(allocStack.name).isEqualTo(variable.name)
        }
    }
}
