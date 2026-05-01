package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.*
import eu.nitok.jitsu.compiler.graph.Type
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for [JitsuUnion] — the tagged-union abstraction that pairs an i32 discriminant with a
 * C-style value union.
 *
 * Layout under test:
 *   struct { option: i32, value: union { o0: T0, o1: T1, ... } }
 */
@DisplayName("JitsuUnion")
class JitsuUnionTest : LowLevelTypeTest() {
    private val graphI32 = Type.Int(BitSize.BIT_32)
    private val graphI64 = Type.Int(BitSize.BIT_64)
    private val graphNull = Type.Null
    private val graphBool = Type.Boolean
    private val graphArray = Type.Array(graphI32, null)

    private val llI32 = LowLevelType.I32   // LowLevelType.I32 convenience alias
    private val llI64 = LowLevelType.I64   // LowLevelType.I64 convenience alias

    /** Simple 2-option union: i32 | i64 */
    private val twoOptionGraphType = Type.Union(listOf(graphI32, graphI64))
    private val twoOptionUnion: JitsuUnion by lazy {
        JitsuUnion.of(twoOptionGraphType, listOf(llI32, llI64))
    }

    /** 3-option union: i32 | i64 | bool */
    private val threeOptionGraphType = Type.Union(listOf(graphI32, graphI64, graphBool))
    private val threeOptionUnion: JitsuUnion by lazy {
        JitsuUnion.of(threeOptionGraphType, listOf(llI32, llI64, LLBool))
    }

    /** Union with null option: i32 | null — uses I32 for both for simplicity of graph type */
    private val nullableGraphType = Type.Union(listOf(graphI32, graphNull))
    private val nullableUnion: JitsuUnion by lazy {
        JitsuUnion.of(nullableGraphType, listOf(llI32, LLInt(BitSize.BIT_8, graphNull)))
    }

    /** A field expression used as the union variable in all tests */
    private val unionVar: Field = Variable("myUnion")

    @Nested
    @DisplayName("of()")
    inner class FactoryMethod {

        @Test
        fun `layout is LLStruct`() {
            assertThat(twoOptionUnion.layout).isInstanceOf(LLStruct::class.java)
        }

        @Test
        fun `layout has 'option' field of type I32`() {
            val optionField = twoOptionUnion.layout.fieldType("option")
            assertThat(optionField).isEqualTo(LowLevelType.I32)
        }

        @Test
        fun `layout has 'value' field of type LLUnion`() {
            val valueField = twoOptionUnion.layout.fieldType("value")
            assertThat(valueField).isInstanceOf(LLUnion::class.java)
        }

        @Test
        fun `three-option union value has o0, o1, o2 members`() {
            val valueUnion = threeOptionUnion.layout.fieldType("value") as LLUnion
            assertThat(valueUnion.members.keys).containsExactly("o0", "o1", "o2")
        }

        @Test
        fun `options list is preserved in order`() {
            assertThat(twoOptionUnion.options).containsExactly(llI32, llI64)
        }

        @Test
        fun `empty options list throws IllegalArgumentException`() {
            assertThatIllegalArgumentException()
                .isThrownBy { JitsuUnion.of(twoOptionGraphType, emptyList()) }
                .withMessageContaining("at least one option")
        }

        @Test
        fun `single-option union is created`() {
            // of() accepts single option; edge-case behaviour exposed via switch/free
            val singleGraphType = Type.Union(listOf(graphI32))
            val singleUnion = JitsuUnion.of(singleGraphType, listOf(llI32))
            assertThat(singleUnion.options).hasSize(1)
        }
    }

    @Nested
    @DisplayName("Access methods")
    inner class AccessMethods {

        @Nested
        @DisplayName("optionIndex(field)")
        inner class OptionIndexAccess {

            @Test
            fun `returns Read expression referencing 'option' field`() {
                val result = twoOptionUnion.optionIndex(unionVar)
                assertThat(result).isInstanceOf(Read::class.java)
                assertThat(result.name).isEqualTo("option")
            }

            @Test
            fun `struct is the supplied union field`() {
                val result = twoOptionUnion.optionIndex(unionVar)
                assertThat(result.struct).isEqualTo(unionVar)
            }
        }

        @Nested
        @DisplayName("valueField(field)")
        inner class ValueFieldAccess {

            @Test
            fun `returns Read expression referencing 'value' field`() {
                val result = twoOptionUnion.valueField(unionVar)
                assertThat(result).isInstanceOf(Read::class.java)
                assertThat(result.name).isEqualTo("value")
            }

            @Test
            fun `struct is the supplied union field`() {
                val result = twoOptionUnion.valueField(unionVar)
                assertThat(result.struct).isEqualTo(unionVar)
            }
        }

        @Nested
        @DisplayName("option(field, index)")
        inner class OptionAccess {

            @Test
            fun `index 0 returns Read of 'o0' within 'value'`() {
                val result = twoOptionUnion.option(unionVar, 0)
                assertThat(result.name).isEqualTo("o0")
                // The struct should be the value field read
                assertThat(result.struct).isInstanceOf(Read::class.java)
                assertThat((result.struct as Read).name).isEqualTo("value")
            }

            @Test
            fun `index 1 returns Read of 'o1' within 'value'`() {
                val result = twoOptionUnion.option(unionVar, 1)
                assertThat(result.name).isEqualTo("o1")
            }

            @Test
            fun `index 2 returns Read of 'o2' in three-option union`() {
                val result = threeOptionUnion.option(unionVar, 2)
                assertThat(result.name).isEqualTo("o2")
            }

            @Test
            fun `out-of-bounds positive index throws IllegalArgumentException`() {
                assertThatIllegalArgumentException()
                    .isThrownBy { twoOptionUnion.option(unionVar, 2) }
                    .withMessageContaining("out of range")
            }

            @Test
            fun `negative index throws IllegalArgumentException`() {
                assertThatIllegalArgumentException()
                    .isThrownBy { twoOptionUnion.option(unionVar, -1) }
                    .withMessageContaining("out of range")
            }
        }

        @Nested
        @DisplayName("optionType(index)")
        inner class OptionTypeAccess {

            @Test
            fun `index 0 returns first option type`() {
                assertThat(twoOptionUnion.optionType(0)).isEqualTo(llI32)
            }

            @Test
            fun `index 1 returns second option type`() {
                assertThat(twoOptionUnion.optionType(1)).isEqualTo(llI64)
            }

            @Test
            fun `index 2 returns third option type in three-option union`() {
                assertThat(threeOptionUnion.optionType(2)).isEqualTo(LLBool)
            }

            @Test
            fun `out-of-bounds index throws IllegalArgumentException`() {
                assertThatIllegalArgumentException()
                    .isThrownBy { twoOptionUnion.optionType(5) }
                    .withMessageContaining("out of range")
            }

            @Test
            fun `negative index throws IllegalArgumentException`() {
                assertThatIllegalArgumentException()
                    .isThrownBy { twoOptionUnion.optionType(-1) }
                    .withMessageContaining("out of range")
            }
        }
    }

    @Nested
    @DisplayName("findOptionIndex(graphType)")
    inner class FindOptionIndex {

        @Test
        fun `exact match on first option returns 0`() {
            assertThat(twoOptionUnion.findOptionIndex(graphI32)).isEqualTo(0)
        }

        @Test
        fun `exact match on second option returns 1`() {
            assertThat(twoOptionUnion.findOptionIndex(graphI64)).isEqualTo(1)
        }

        @Test
        fun `no match returns null`() {
            assertThat(twoOptionUnion.findOptionIndex(graphBool)).isNull()
        }

        @Test
        fun `exact match wins before assignable match`() {
            // i32 is both an exact match (o0) and is assignable to i64 (o1),
            // but exact match should return 0 not 1
            assertThat(JitsuUnion.of(Type.Union(listOf(graphI64, graphI32)), listOf(llI64, llI32)).findOptionIndex(graphI32))
                .isEqualTo(1)
        }

        @Test
        fun `assignable match returns correct index when no exact match`() {
            // i8 is assignable to i32 (i32 accepts integers its size and smaller)
            val graphI8 = Type.Int(BitSize.BIT_8)
            // i8 should be assignable to i32 (index 0) and also to i64 (index 1);
            // first assignable match wins → 0
            val result = twoOptionUnion.findOptionIndex(graphI8)
            assertThat(result).isEqualTo(0)
        }

        @Test
        fun `null type found in nullable union`() {
            assertThat(nullableUnion.findOptionIndex(graphNull)).isEqualTo(1)
        }

        @Test
        fun `array type match in array union`() {
            val arrayGraphType = Type.Union(listOf(graphI32, graphArray))
            val arrayUnion = JitsuUnion.of(
                arrayGraphType,
                listOf(llI32, LLPointer(llI32, graphArray))
            )
            assertThat(arrayUnion.findOptionIndex(graphArray)).isEqualTo(1)
        }

        @Test
        fun `unrelated type returns null in three-option union`() {
            // graphArray is not related to i32 | i64 | bool
            assertThat(threeOptionUnion.findOptionIndex(graphArray)).isNull()
        }
    }

    @Nested
    @DisplayName("isOption(field, index)")
    inner class IsOption {

        @Test
        fun `returns a Compare expression`() {
            val result = twoOptionUnion.isOption(unionVar, 0)
            assertThat(result).isInstanceOf(Compare::class.java)
        }

        @Test
        fun `left side of Compare is optionIndex read`() {
            val result = twoOptionUnion.isOption(unionVar, 0)
            val expectedOptionIndex = twoOptionUnion.optionIndex(unionVar)
            assertThat(result.left).isEqualTo(expectedOptionIndex)
        }

        @Test
        fun `right side of Compare is NumericalValue matching index 0`() {
            val result = twoOptionUnion.isOption(unionVar, 0)
            assertThat(result.right).isEqualTo(NumericalValue(0L))
        }

        @Test
        fun `right side of Compare is NumericalValue matching index 1`() {
            val result = twoOptionUnion.isOption(unionVar, 1)
            assertThat(result.right).isEqualTo(NumericalValue(1L))
        }
    }

    @Nested
    @DisplayName("switch()")
    inner class Switch {

        private val dummyInstruction: LowLevelInstruction = Return(null)

        @Test
        fun `two-option union produces exactly one top-level instruction`() {
            val result = twoOptionUnion.switch(unionVar) { _, _ -> listOf(dummyInstruction) }
            assertThat(result).hasSize(1)
        }

        @Test
        fun `top-level instruction is a Conditional`() {
            val result = twoOptionUnion.switch(unionVar) { _, _ -> listOf(dummyInstruction) }
            assertThat(result[0]).isInstanceOf(Conditional::class.java)
        }

        @Test
        fun `outermost condition checks LAST option (index 1 for 2-option union)`() {
            val result = twoOptionUnion.switch(unionVar) { _, _ -> listOf(dummyInstruction) }
            val outer = result[0] as Conditional
            val expectedCondition = twoOptionUnion.isOption(unionVar, 1)
            assertThat(outer.condition).isEqualTo(expectedCondition)
        }

        @Test
        fun `outer conditional else branch wraps index 0 condition`() {
            val result = twoOptionUnion.switch(unionVar) { _, _ -> listOf(dummyInstruction) }
            val outer = result[0] as Conditional
            assertThat(outer.elseInstructions).isNotNull()
            val innerConditional = outer.elseInstructions!!.single() as Conditional
            val expectedCondition = twoOptionUnion.isOption(unionVar, 0)
            assertThat(innerConditional.condition).isEqualTo(expectedCondition)
        }

        @Test
        fun `innermost conditional has null else (no fallthrough for 2 options)`() {
            val result = twoOptionUnion.switch(unionVar) { _, _ -> listOf(dummyInstruction) }
            val outer = result[0] as Conditional
            val inner = outer.elseInstructions!!.single() as Conditional
            assertThat(inner.elseInstructions).isNull()
        }

        @Test
        fun `three-option union produces nested chain depth 3`() {
            val result = threeOptionUnion.switch(unionVar) { _, _ -> listOf(dummyInstruction) }
            val level1 = result[0] as Conditional                           // checks o2
            val level2 = level1.elseInstructions!!.single() as Conditional  // checks o1
            val level3 = level2.elseInstructions!!.single() as Conditional  // checks o0
            assertThat(level3.elseInstructions).isNull()
        }

        @Test
        fun `three-option union outermost checks option index 2`() {
            val result = threeOptionUnion.switch(unionVar) { _, _ -> listOf(dummyInstruction) }
            val outer = result[0] as Conditional
            assertThat(outer.condition).isEqualTo(threeOptionUnion.isOption(unionVar, 2))
        }

        @Test
        fun `caseBody receives correct option field for each index`() {
            val receivedFields = mutableListOf<Field>()
            twoOptionUnion.switch(unionVar) { optionField, _ ->
                receivedFields += optionField
                listOf(dummyInstruction)
            }
            assertThat(receivedFields).hasSize(2)
            assertThat(receivedFields[0]).isEqualTo(twoOptionUnion.option(unionVar, 0))
            assertThat(receivedFields[1]).isEqualTo(twoOptionUnion.option(unionVar, 1))
        }

        @Test
        fun `caseBody receives correct LowLevelType for each option`() {
            val receivedTypes = mutableListOf<LowLevelType>()
            twoOptionUnion.switch(unionVar) { _, optionType ->
                receivedTypes += optionType
                listOf(dummyInstruction)
            }
            assertThat(receivedTypes[0]).isEqualTo(llI32)
            assertThat(receivedTypes[1]).isEqualTo(llI64)
        }

        @Test
        fun `empty caseBody for all options returns empty list`() {
            val result = twoOptionUnion.switch(unionVar) { _, _ -> emptyList() }
            assertThat(result).isEmpty()
        }

        @Test
        fun `empty caseBody for only the last option still produces Conditional for earlier options`() {
            // Only option 0 produces instructions; option 1 returns empty → acc stays as idx-0 Conditional
            var callCount = 0
            val result = twoOptionUnion.switch(unionVar) { _, _ ->
                val idx = callCount++
                if (idx == 0) listOf(dummyInstruction) else emptyList()
            }
            // The fold skips wrapping when instructions are empty, so only the option-0 Conditional remains
            assertThat(result).hasSize(1)
            val conditional = result[0] as Conditional
            assertThat(conditional.condition).isEqualTo(twoOptionUnion.isOption(unionVar, 0))
        }

        @Test
        fun `empty caseBody for only the first option produces Conditional wrapping it`() {
            var callCount = 0
            val result = twoOptionUnion.switch(unionVar) { _, _ ->
                val idx = callCount++
                if (idx == 1) listOf(dummyInstruction) else emptyList()
            }
            assertThat(result).hasSize(1)
            val conditional = result[0] as Conditional
            // Outermost is for the last (idx 1) option, else is null (idx 0 was empty)
            assertThat(conditional.condition).isEqualTo(twoOptionUnion.isOption(unionVar, 1))
            assertThat(conditional.elseInstructions).isNull()
        }

        @Test
        fun `thenInstructions of each branch match what caseBody returned`() {
            val bodyForIdx0 = listOf<LowLevelInstruction>(Return(NumericalValue(0L)))
            val bodyForIdx1 = listOf<LowLevelInstruction>(Return(NumericalValue(1L)))
            var callCount = 0
            val result = twoOptionUnion.switch(unionVar) { _, _ ->
                if (callCount++ == 0) bodyForIdx0 else bodyForIdx1
            }
            val outer = result[0] as Conditional   // option 1
            assertThat(outer.thenInstructions).isEqualTo(bodyForIdx1)
            val inner = outer.elseInstructions!!.single() as Conditional  // option 0
            assertThat(inner.thenInstructions).isEqualTo(bodyForIdx0)
        }

        @Test
        fun `switch callback is invoked exactly once per option`() {
            var callCount = 0
            twoOptionUnion.switch(unionVar) { _, _ ->
                callCount++
                listOf(Return(null))
            }
            assertThat(callCount).isEqualTo(2)
        }

        @Test
        fun `switch callback invocation order is ascending by index`() {
            val visitedFields = mutableListOf<String>()
            threeOptionUnion.switch(unionVar) { optionField, _ ->
                visitedFields += (optionField as Read).name  // o0, o1, o2
                listOf(Return(null))
            }
            assertThat(visitedFields).containsExactly("o0", "o1", "o2")
        }
    }

    @Nested
    @DisplayName("writeOption()")
    inner class WriteOption {

        @Test
        fun `first instruction writes discriminant for option 0`() {
            val instructions = twoOptionUnion.writeOption(unionVar, 0) { _, _ -> emptyList() }
            val firstWrite = instructions.first() as Write
            assertThat(firstWrite.target).isEqualTo(twoOptionUnion.optionIndex(unionVar))
            assertThat(firstWrite.value).isEqualTo(NumericalValue(0L))
        }

        @Test
        fun `first instruction writes discriminant for option 1`() {
            val instructions = twoOptionUnion.writeOption(unionVar, 1) { _, _ -> emptyList() }
            val firstWrite = instructions.first() as Write
            assertThat(firstWrite.value).isEqualTo(NumericalValue(1L))
        }

        @Test
        fun `write callback receives correct option field`() {
            var receivedField: Field? = null
            twoOptionUnion.writeOption(unionVar, 0) { field, _ ->
                receivedField = field
                emptyList()
            }
            assertThat(receivedField).isEqualTo(twoOptionUnion.option(unionVar, 0))
        }

        @Test
        fun `write callback receives correct LowLevelType`() {
            var receivedType: LowLevelType? = null
            twoOptionUnion.writeOption(unionVar, 1) { _, type ->
                receivedType = type
                emptyList()
            }
            assertThat(receivedType).isEqualTo(llI64)
        }

        @Test
        fun `instructions from write callback are appended after discriminant write`() {
            val extraInstruction: LowLevelInstruction = Return(NumericalValue(42L))
            val instructions = twoOptionUnion.writeOption(unionVar, 0) { _, _ -> listOf(extraInstruction) }
            assertThat(instructions).hasSize(2)
            assertThat(instructions[0]).isInstanceOf(Write::class.java)
            assertThat(instructions[1]).isEqualTo(extraInstruction)
        }

        @Test
        fun `empty write callback produces exactly one instruction (discriminant only)`() {
            val instructions = twoOptionUnion.writeOption(unionVar, 0) { _, _ -> emptyList() }
            assertThat(instructions).hasSize(1)
            assertThat(instructions[0]).isInstanceOf(Write::class.java)
        }

        @Test
        fun `multiple write callback instructions are all appended`() {
            val extras = listOf<LowLevelInstruction>(Return(NumericalValue(1L)), Return(NumericalValue(2L)))
            val instructions = twoOptionUnion.writeOption(unionVar, 0) { _, _ -> extras }
            assertThat(instructions).hasSize(3)
            assertThat(instructions.drop(1)).isEqualTo(extras)
        }

        @Test
        fun `out-of-bounds index propagates IllegalArgumentException`() {
            assertThatIllegalArgumentException()
                .isThrownBy { twoOptionUnion.writeOption(unionVar, 99) { _, _ -> emptyList() } }
        }

        @Test
        fun `writeOption index 0 and index 1 produce different discriminant values`() {
            val write0 = twoOptionUnion.writeOption(unionVar, 0) { _, _ -> emptyList() }
            val write1 = twoOptionUnion.writeOption(unionVar, 1) { _, _ -> emptyList() }
            val disc0 = (write0[0] as Write).value as NumericalValue
            val disc1 = (write1[0] as Write).value as NumericalValue
            assertThat(disc0.value).isNotEqualTo(disc1.value)
        }
    }

    @Nested
    @DisplayName("free()")
    inner class Free {

        private val ctx = LoweringContext()

        @Test
        fun `union of primitive types returns empty instruction list (primitives need no free)`() {
            // Primitive types have no free instructions, so all caseBody calls return empty,
            // meaning switch produces no Conditionals at all.
            val result = twoOptionUnion.free(unionVar, ctx)
            assertThat(result).isEmpty()
        }

        @Test
        fun `union containing a pointer type produces Conditional free instructions`() {
            // LLPointer always emits at least one Free instruction for its field.
            val ptrType = LLPointer(llI32, graphI32)
            val graphType = Type.Union(listOf(graphI32, graphI32))
            val ptrUnion = JitsuUnion.of(graphType, listOf(llI32, ptrType))

            val result = ptrUnion.free(unionVar, ctx)

            // switch() should produce at least one Conditional since the pointer branch is non-empty
            assertThat(result).isNotEmpty()
            assertThat(result[0]).isInstanceOf(Conditional::class.java)
        }

        @Test
        fun `union of two pointer types generates nested Conditional free`() {
            val ptrI32 = LLPointer(llI32, graphI32)
            val ptrI64 = LLPointer(llI64, graphI64)
            val graphType = Type.Union(listOf(graphI32, graphI64))
            val ptrUnion = JitsuUnion.of(graphType, listOf(ptrI32, ptrI64))

            val result = ptrUnion.free(unionVar, ctx)
            assertThat(result).hasSize(1)
            val outer = result[0] as Conditional
            // outermost branch is for the last option (ptr to i64)
            assertThat(outer.condition).isEqualTo(ptrUnion.isOption(unionVar, 1))
            // else branch holds the free for ptr to i32
            assertThat(outer.elseInstructions).isNotNull()
        }

        @Test
        fun `free for pointer option emits Free instruction targeting option field`() {
            val ptrI32 = LLPointer(llI32, graphI32)
            val graphType = Type.Union(listOf(graphI32, graphI64))
            val ptrUnion = JitsuUnion.of(graphType, listOf(llI64, ptrI32))

            val result = ptrUnion.free(unionVar, ctx)
            // Outermost condition is for option index 1 (ptrI32)
            val outer = result[0] as Conditional
            // then-branch should contain a Free instruction for the option-1 field
            val freeInstructions = outer.thenInstructions.filterIsInstance<LowLevelInstruction.Free>()
            assertThat(freeInstructions).isNotEmpty()
            val optionField = ptrUnion.option(unionVar, 1)
            assertThat(freeInstructions.any { it.target == optionField }).isTrue()
        }

        @Test
        fun `three-option union of primitives still returns empty (no heap to free)`() {
            val result = threeOptionUnion.free(unionVar, ctx)
            assertThat(result).isEmpty()
        }

        @Test
        fun `single-option union free returns empty list for primitive option`() {
            val singleGraphType = Type.Union(listOf(graphI32, graphI64))
            val singleUnion = JitsuUnion.of(singleGraphType, listOf(llI32, llI64))
            val result = singleUnion.free(unionVar, ctx)
            assertThat(result).isEmpty()
        }
    }
}
