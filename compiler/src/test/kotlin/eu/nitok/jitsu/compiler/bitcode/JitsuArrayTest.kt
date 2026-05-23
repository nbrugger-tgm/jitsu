package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.*
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.Companion.I32
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.Companion.I64
import eu.nitok.jitsu.compiler.graph.elements.types.Type
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("JitsuArray")
class JitsuArrayTest : LowLevelTypeTest() {

    private val dummyGraphType: Type = Type.Boolean
    private val elemGraphType: Type = Type.Int(BitSize.BIT_32)

    /** Primitive element type – free() is a no-op. */
    private val primitiveElem: LowLevelType = I32

    /** Pointer element type – free() emits a Free instruction. */
    private val pointerElem: LowLevelType = LLPointer(I32, elemGraphType)

    private lateinit var ctx: LoweringContext

    /** A named variable that acts as the "array field" argument in all tests. */
    private val arrayVar: Field = Variable("myArray")

    @BeforeEach
    fun setUp() {
        ctx = LoweringContext()
    }

    @Nested
    @DisplayName("fixed()")
    inner class Fixed {

        @Test
        fun `isFixedSize is true`() {
            val arr = JitsuArray.fixed(primitiveElem, 5, dummyGraphType)
            assertThat(arr.isFixedSize).isTrue()
        }

        @Test
        fun `isDynamic is false`() {
            val arr = JitsuArray.fixed(primitiveElem, 5, dummyGraphType)
            assertThat(arr.isDynamic).isFalse()
        }

        @Test
        fun `fixedSize equals the provided size`() {
            val arr = JitsuArray.fixed(primitiveElem, 7, dummyGraphType)
            assertThat(arr.fixedSize).isEqualTo(7)
        }

        @Test
        fun `layout is LLStruct`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            assertThat(arr.layout).isInstanceOf(LLStruct::class.java)
        }

        @Test
        fun `layout has only a data field`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            assertThat(arr.layout.fields.keys).containsExactly("data")
        }

        @Test
        fun `data field is LLFixedArray with correct element type and size`() {
            val arr = JitsuArray.fixed(primitiveElem, 4, dummyGraphType)
            val dataField = arr.layout.fields["data"]
            assertThat(dataField).isInstanceOf(LLFixedArray::class.java)
            dataField as LLFixedArray
            assertThat(dataField.elementType).isEqualTo(primitiveElem)
            assertThat(dataField.size).isEqualTo(4)
        }

        @Test
        fun `layout does not contain a length field`() {
            val arr = JitsuArray.fixed(primitiveElem, 2, dummyGraphType)
            assertThat(arr.layout.fields).doesNotContainKey("length")
        }

        @Test
        fun `elementType is the provided element type`() {
            val arr = JitsuArray.fixed(primitiveElem, 1, dummyGraphType)
            assertThat(arr.elementType).isEqualTo(primitiveElem)
        }
    }

    @Nested
    @DisplayName("dynamic()")
    inner class Dynamic {

        @Test
        fun `isFixedSize is false`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            assertThat(arr.isFixedSize).isFalse()
        }

        @Test
        fun `isDynamic is true`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            assertThat(arr.isDynamic).isTrue()
        }

        @Test
        fun `fixedSize is null`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            assertThat(arr.fixedSize).isNull()
        }

        @Test
        fun `layout is LLStruct`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            assertThat(arr.layout).isInstanceOf(LLStruct::class.java)
        }

        @Test
        fun `layout has length and data fields`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            assertThat(arr.layout.fields.keys).containsExactly("length", "data")
        }

        @Test
        fun `length field is I64`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            assertThat(arr.layout.fields["length"]).isEqualTo(I64)
        }

        @Test
        fun `data field is LLPointer to element type`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val dataField = arr.layout.fields["data"]
            assertThat(dataField).isInstanceOf(LLPointer::class.java)
            dataField as LLPointer<*>
            assertThat(dataField.pointeeType).isEqualTo(primitiveElem)
        }

        @Test
        fun `elementType is the provided element type`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            assertThat(arr.elementType).isEqualTo(primitiveElem)
        }
    }

    @Nested
    @DisplayName("length()")
    inner class Length {

        @Test
        fun `dynamic array returns Read expression with name length`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val result = arr.length(arrayVar)
            assertThat(result).isInstanceOf(Read::class.java)
            assertThat(result.name).isEqualTo("length")
            assertThat(result.struct).isEqualTo(arrayVar)
        }

        @Test
        fun `calling length on a fixed-size array throws IllegalStateException`() {
            val arr = JitsuArray.fixed(primitiveElem, 5, dummyGraphType)
            assertThatIllegalStateException()
                .isThrownBy { arr.length(arrayVar) }
                .withMessageContaining("Fixed-size arrays don't have a length field")
        }
    }

    @Nested
    @DisplayName("data()")
    inner class Data {

        @Test
        fun `dynamic array returns Read expression with name data`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val result = arr.data(arrayVar)
            assertThat(result).isInstanceOf(Read::class.java)
            assertThat(result.name).isEqualTo("data")
            assertThat(result.struct).isEqualTo(arrayVar)
        }

        @Test
        fun `fixed array returns Read expression with name data`() {
            val arr = JitsuArray.fixed(primitiveElem, 5, dummyGraphType)
            val result = arr.data(arrayVar)
            assertThat(result).isInstanceOf(Read::class.java)
            assertThat(result.name).isEqualTo("data")
            assertThat(result.struct).isEqualTo(arrayVar)
        }
    }

    @Nested
    @DisplayName("accessIndex()")
    inner class AccessIndex {

        @Test
        fun `returns ArraySlot whose array is the data field`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val index = NumericalValue(2)
            val slot = arr.accessIndex(arrayVar, index)
            assertThat(slot).isInstanceOf(ArraySlot::class.java)
            assertThat(slot.array).isEqualTo(Read(arrayVar, "data"))
        }

        @Test
        fun `ArraySlot index matches the provided index expression`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val index = NumericalValue(3)
            val slot = arr.accessIndex(arrayVar, index)
            assertThat(slot.index).isEqualTo(index)
        }

        @Test
        fun `works for fixed-size arrays`() {
            val arr = JitsuArray.fixed(primitiveElem, 5, dummyGraphType)
            val index = NumericalValue(1)
            val slot = arr.accessIndex(arrayVar, index)
            assertThat(slot.array).isEqualTo(Read(arrayVar, "data"))
            assertThat(slot.index).isEqualTo(index)
        }
    }

    @Nested
    @DisplayName("sizeExpression()")
    inner class SizeExpression {

        @Test
        fun `fixed array returns NumericalValue equal to fixedSize`() {
            val arr = JitsuArray.fixed(primitiveElem, 10, dummyGraphType)
            val expr = arr.sizeExpression(arrayVar)
            assertThat(expr).isEqualTo(NumericalValue(10L))
        }

        @Test
        fun `fixed array with size 0 returns NumericalValue(0)`() {
            val arr = JitsuArray.fixed(primitiveElem, 0, dummyGraphType)
            val expr = arr.sizeExpression(arrayVar)
            assertThat(expr).isEqualTo(NumericalValue(0L))
        }

        @Test
        fun `dynamic array returns Read of length field`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val expr = arr.sizeExpression(arrayVar)
            assertThat(expr).isEqualTo(Read(arrayVar, "length"))
        }
    }

    @Nested
    @DisplayName("iterate()")
    inner class Iterate {

        @Test
        fun `produces AllocStack for I32 counter as first instruction`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            val result = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }
            val alloc = result.filterIsInstance<AllocStack>()
            assertThat(alloc).hasSize(1)
            assertThat(alloc[0].layout).isEqualTo(I32)
        }

        @Test
        fun `initialises counter to 0 after AllocStack`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            val result = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }
            // AllocStack comes first, then Write(counter, 0)
            val allocIdx = result.indexOfFirst { it is AllocStack }
            val counterName = (result[allocIdx] as AllocStack).name
            val writeInstr = result.drop(allocIdx + 1).filterIsInstance<Write>().firstOrNull {
                it.target == Variable(counterName) && it.value == NumericalValue(0)
            }
            assertThat(writeInstr).isNotNull
        }

        @Test
        fun `produces exactly one While loop`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            val result = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }
            assertThat(result.filterIsInstance<While>()).hasSize(1)
        }

        @Test
        fun `While condition is CompareGreater(sizeExpr, counter)`() {
            val arr = JitsuArray.fixed(primitiveElem, 5, dummyGraphType)
            val result = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }
            val whileInstr = result.filterIsInstance<While>().first()
            val condition = whileInstr.condition
            assertThat(condition).isInstanceOf(CompareGreater::class.java)
            condition as CompareGreater
            // left should be the size expression (NumericalValue(5) for fixed)
            assertThat(condition.left).isEqualTo(NumericalValue(5L))
            // right should be the counter variable
            assertThat(condition.right).isInstanceOf(Variable::class.java)
        }

        @Test
        fun `dynamic array While condition uses length field as size`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val result = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }
            val whileInstr = result.filterIsInstance<While>().first()
            val condition = whileInstr.condition as CompareGreater
            assertThat(condition.left).isEqualTo(Read(arrayVar, "length"))
        }

        @Test
        fun `While body ends with Increase of counter`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            val result = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }
            val whileInstr = result.filterIsInstance<While>().first()
            assertThat(whileInstr.body.last()).isInstanceOf(Increase::class.java)
        }

        @Test
        fun `While body contains body instructions before the Increase`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            val sentinel = AllocStack("sentinel", I32)
            val result = arr.iterate(arrayVar, ctx) { _, _ -> listOf(sentinel) }
            val body = result.filterIsInstance<While>().first().body
            val sentinelIdx = body.indexOf(sentinel)
            val increaseIdx = body.indexOfFirst { it is Increase }
            assertThat(sentinelIdx).isGreaterThanOrEqualTo(0)
            assertThat(sentinelIdx).isLessThan(increaseIdx)
        }

        @Test
        fun `body receives ArraySlot element expression pointing through data field`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            var capturedElement: Field? = null
            arr.iterate(arrayVar, ctx) { element, _ ->
                capturedElement = element
                emptyList()
            }
            assertThat(capturedElement).isInstanceOf(ArraySlot::class.java)
            val slot = capturedElement as ArraySlot
            assertThat(slot.array).isEqualTo(Read(arrayVar, "data"))
        }

        @Test
        fun `body receives counter variable as index`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            var capturedIndex: LowLevelExpression? = null
            arr.iterate(arrayVar, ctx) { _, index ->
                capturedIndex = index
                emptyList()
            }
            assertThat(capturedIndex).isInstanceOf(Variable::class.java)
        }
    }

    @Nested
    @DisplayName("alloc()")
    inner class Alloc {

        @Test
        fun `fixed array alloc returns empty list`() {
            val arr = JitsuArray.fixed(primitiveElem, 5, dummyGraphType)
            val result = arr.alloc(arrayVar, 5)
            assertThat(result).isEmpty()
        }

        @Test
        fun `dynamic array alloc emits two Write instructions`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val result = arr.alloc(arrayVar, 4)
            assertThat(result.filterIsInstance<Write>()).hasSize(2)
        }

        @Test
        fun `dynamic array alloc sets length field to provided size`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val result = arr.alloc(arrayVar, 6)
            val lengthWrite = result.filterIsInstance<Write>().firstOrNull {
                it.target == Read(arrayVar, "length")
            }
            assertThat(lengthWrite).isNotNull
            assertThat(lengthWrite!!.value).isEqualTo(NumericalValue(6L))
        }

        @Test
        fun `dynamic array alloc writes AllocHeapArray to data field`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val result = arr.alloc(arrayVar, 3)
            val dataWrite = result.filterIsInstance<Write>().firstOrNull {
                it.target == Read(arrayVar, "data")
            }
            assertThat(dataWrite).isNotNull
            assertThat(dataWrite!!.value).isInstanceOf(AllocHeapArray::class.java)
            dataWrite.value as AllocHeapArray
            assertThat((dataWrite.value as AllocHeapArray).elementType).isEqualTo(primitiveElem)
        }

        @Test
        fun `dynamic array alloc with size 0 still sets length to 0`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val result = arr.alloc(arrayVar, 0)
            val lengthWrite = result.filterIsInstance<Write>().firstOrNull {
                it.target == Read(arrayVar, "length")
            }
            assertThat(lengthWrite).isNotNull
            assertThat(lengthWrite!!.value).isEqualTo(NumericalValue(0L))
        }
    }

    @Nested
    @DisplayName("free()")
    inner class Free {

        @Nested
        @DisplayName("Fixed array with primitive element type")
        inner class FixedPrimitive {

            @Test
            fun `produces no loop if elements do not require free`() {
                val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                assertThat(result.filterIsInstance<While>()).isEmpty()
            }

            @Test
            fun `does not emit any Free instruction (no heap data to free)`() {
                val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                assertThat(result.filterIsInstance<LowLevelInstruction.Free>()).isEmpty()
            }
        }

        @Nested
        @DisplayName("Fixed array with pointer element type")
        inner class FixedPointer {

            @Test
            fun `emits Free for each element inside While body`() {
                val arr = JitsuArray.fixed(pointerElem, 3, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                val whileInstr = result.filterIsInstance<While>().first()
                val freeInstructions = whileInstr.body.filterIsInstance<LowLevelInstruction.Free>()
                assertThat(freeInstructions).isNotEmpty
            }

            @Test
            fun `does not emit Free for data array itself (stack allocated)`() {
                val arr = JitsuArray.fixed(pointerElem, 3, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                // Any Free outside the While loop body would be for the data pointer
                val whileIdx = result.indexOfFirst { it is While }
                val instructionsAfterWhile = result.drop(whileIdx + 1)
                assertThat(instructionsAfterWhile.filterIsInstance<LowLevelInstruction.Free>()).isEmpty()
            }
        }

        @Nested
        @DisplayName("Dynamic array with primitive element type")
        inner class DynamicPrimitive {

            @Test
            fun `emits Free for data pointer after the While loop`() {
                val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                val dataFree = result.filterIsInstance<LowLevelInstruction.Free>().firstOrNull {
                    it.target == Read(arrayVar, "data")
                }
                assertThat(dataFree).isNotNull
            }

            @Test
            fun `no while if elements do not require free`() {
                val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                val whileInstr = result.filterIsInstance<While>()
                assertThat(whileInstr).isEmpty()
            }
        }

        @Nested
        @DisplayName("Dynamic array with pointer element type")
        inner class DynamicPointer {

            @Test
            fun `While body contains Free for each element`() {
                val arr = JitsuArray.dynamic(pointerElem, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                val whileInstr = result.filterIsInstance<While>().first()
                assertThat(whileInstr.body.filterIsInstance<LowLevelInstruction.Free>()).isNotEmpty
            }

            @Test
            fun `emits Free for data pointer after the While loop`() {
                val arr = JitsuArray.dynamic(pointerElem, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                val whileIdx = result.indexOfFirst { it is While }
                val afterWhile = result.drop(whileIdx + 1)
                val dataFree = afterWhile.filterIsInstance<LowLevelInstruction.Free>().firstOrNull {
                    it.target == Read(arrayVar, "data")
                }
                assertThat(dataFree).isNotNull
            }

            @Test
            fun `element Free precedes data pointer Free`() {
                val arr = JitsuArray.dynamic(pointerElem, dummyGraphType)
                val result = arr.free(arrayVar, ctx)
                val whileIdx = result.indexOfFirst { it is While }
                val dataFreeIdx = result
                    .indexOfFirst { it is LowLevelInstruction.Free && it.target == Read(arrayVar, "data") }
                // While (containing element frees) is before the data Free
                assertThat(dataFreeIdx).isGreaterThan(whileIdx)
            }
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCases {

        @Test
        fun `fixed array with size 0 - iterate produces no loop`() {
            val arr = JitsuArray.fixed(primitiveElem, 0, dummyGraphType)
            val result = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }
            val whileInstr = result.filterIsInstance<While>()
            assertThat(whileInstr).isEmpty()
        }

        @Test
        fun `fixed array with size 0 - free produces no Free instructions`() {
            val arr = JitsuArray.fixed(primitiveElem, 0, dummyGraphType)
            val result = arr.free(arrayVar, ctx)
            assertThat(result.filterIsInstance<LowLevelInstruction.Free>()).isEmpty()
        }

        @Test
        fun `dynamic array with size 0 alloc - sets length to 0 and allocates empty heap array`() {
            val arr = JitsuArray.dynamic(primitiveElem, dummyGraphType)
            val result = arr.alloc(arrayVar, 0)
            val heapAlloc = result.filterIsInstance<Write>()
                .firstOrNull { it.value is AllocHeapArray }
            assertThat(heapAlloc).isNotNull
            val alloc = heapAlloc!!.value as AllocHeapArray
            assertThat(alloc.size).isEqualTo(NumericalValue(0L))
        }

        @Test
        fun `nested array as element type - fixed outer free produces non-empty instructions for pointer inner elements`() {
            // Inner: dynamic array of pointer elements – has meaningful free
            val innerArr = JitsuArray.dynamic(pointerElem, dummyGraphType)
            // Outer: fixed array of inner arrays
            val outerArr = JitsuArray.fixed(innerArr, 2, dummyGraphType)
            val result = outerArr.free(arrayVar, ctx)
            // Outer iterate should run inner array free for each element
            assertThat(result).isNotEmpty
            assertThat(result.filterIsInstance<While>()).isNotEmpty
        }

        @Test
        fun `nested array as element type - dynamic outer free ends with Free of outer data pointer`() {
            val innerArr = JitsuArray.dynamic(pointerElem, dummyGraphType)
            val outerArr = JitsuArray.dynamic(innerArr, dummyGraphType)
            val result = outerArr.free(arrayVar, ctx)
            // Last Free instruction should target the outer data field
            val lastFree = result.filterIsInstance<LowLevelInstruction.Free>().lastOrNull()
            assertThat(lastFree).isNotNull
            assertThat(lastFree!!.target).isEqualTo(Read(arrayVar, "data"))
        }

        @Test
        fun `multiple calls to iterate use unique counter variable names`() {
            val arr = JitsuArray.fixed(primitiveElem, 3, dummyGraphType)
            val result1 = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }
            val result2 = arr.iterate(arrayVar, ctx) { _, _ -> listOf(AllocStack("a", I32)) }

            val name1 = result1.filterIsInstance<AllocStack>().first().name
            val name2 = result2.filterIsInstance<AllocStack>().first().name
            assertThat(name1).isNotEqualTo(name2)
        }

        @Test
        fun `alloc for fixed array is a no-op regardless of size argument`() {
            val arr = JitsuArray.fixed(primitiveElem, 10, dummyGraphType)
            assertThat(arr.alloc(arrayVar, 10)).isEmpty()
            assertThat(arr.alloc(arrayVar, 0)).isEmpty()
            assertThat(arr.alloc(arrayVar, 99)).isEmpty()
        }
    }
}
