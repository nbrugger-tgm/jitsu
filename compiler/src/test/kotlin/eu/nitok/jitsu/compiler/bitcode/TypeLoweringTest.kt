package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.BitSize.BIT_64
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.common.locating.locatedAt
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.*
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement.UIntConstant
import eu.nitok.jitsu.compiler.graph.elements.JitsuModule
import eu.nitok.jitsu.compiler.graph.elements.types.*
import eu.nitok.jitsu.compiler.graph.elements.types.Array
import eu.nitok.jitsu.compiler.graph.elements.types.Boolean
import eu.nitok.jitsu.compiler.graph.elements.types.Float
import eu.nitok.jitsu.compiler.graph.elements.types.Int
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.net.URI
import java.util.stream.Stream
import kotlin.streams.asStream

@DisplayName("TypeLowering")
class TypeLoweringTest {

    private val dummyLocation = Location(URI("memory://test.jit"), 1, 1, 1, 1)
    private fun locatedName(name: String): Located<String> = Located(name, dummyLocation)

    private fun structField(name: String, type: TypeElement): Struct.Field =
        Struct.Field(locatedName(name), mutable = false, typeElement = type)

    private fun structuralInterface(vararg fields: Pair<String, TypeElement>): StructuralInterface = StructuralInterface(fields.associate { (name, type) -> name to structField(name, type) })

    private fun unionType(vararg options: TypeElement): Union = Union(options.toList())

    internal class TypeCase(val element: TypeElement, val lowLevel: LowLevelType) {
        val graph: Type get() = element.asType
    }

    private inline fun <T : TypeElement> typeCase(graph: T, lowLevel: (T) -> LowLevelType): TypeCase {
        return TypeCase(graph, lowLevel(graph))
    }


    private val dummyModule = JitsuModule("test",listOf(),listOf())
    private var simpleTypes = BitSize.entries.map { bits -> typeCase(Int(bits)) { LLInt(bits, it) } } +
            BitSize.entries.map { bits -> typeCase(UInt(bits)) { LLUInt(bits, it) } } +
            typeCase(Boolean) { LLBool(Boolean) } +
            genericUnion()

    private fun genericUnion(): TypeCase {
        return typeCase(
            TypeReference(
                locatedName("Or"),
                listOf(UInt(BIT_64).locatedAt(dummyLocation), Boolean.locatedAt(dummyLocation))
            ).also {
                val a = TypeParameterElement("A".locatedAt(dummyLocation)).also { it.setEnclosingModule(dummyModule) }
                val b = TypeParameterElement("B".locatedAt(dummyLocation)).also { it.setEnclosingModule(dummyModule) }
                it.setEnclosingModule(dummyModule)
                it.setResolvedTarget(TypeAlias("".locatedAt(dummyLocation),listOf(a, b), Union(listOf(a, b))).also {
                    it.setEnclosingModule(dummyModule)
                })
                it.resolve(messages = CompilerMessages())
            }
        ) { JitsuUnion.of(Union(listOf(UInt(BIT_64), Boolean)), listOf(LLUInt(BIT_64, UInt(BIT_64)), LLBool(Boolean))) }
    }

    private val dynamicArrays = simpleTypes.map { element ->
        typeCase(Array(element.element, null)) { arr -> JitsuArray.dynamic(element.lowLevel,I64, arr) }
    }

    private val fixedArrays = simpleTypes.map { element ->
        typeCase(Array(element.element, UIntConstant(1u, dummyLocation))) { arr -> JitsuArray.fixed(element.lowLevel, I8, 1uL, arr) }
    } + simpleTypes.map { element ->
        typeCase(Array(element.element, UIntConstant(300u, dummyLocation))) { arr -> JitsuArray.fixed(element.lowLevel, I16, 300uL, arr) }
    }
    private var exampleTypes: Stream<TypeCase> = (simpleTypes.asSequence() + dynamicArrays + fixedArrays).asStream()


    @Nested
    @DisplayName("lower()")
    inner class Lower {
        @TestFactory
        fun `should preserve original type`(): Stream<DynamicTest> {
            return exampleTypes.filter { it.graph !is TypeReference }.map { type ->
                DynamicTest.dynamicTest(type.graph.toString()) {
                    val result = TypeLowering.lower(type.graph)
                    assertThat(result.graphType).isSameAs(type.graph)
                }
            }
        }

        @TestFactory
        fun `should return correct lower type structure`(): Stream<DynamicTest> {
            return exampleTypes.map { type ->
                DynamicTest.dynamicTest(type.graph.toString()) {
                    val result = TypeLowering.lower(type.graph)
                    assertThat(result)
                        .usingRecursiveComparison()
                        .ignoringFieldsMatchingRegexes(".+\\.asType(\\\$delegate)?\\..*")
                        .isEqualTo(type.lowLevel)
                }
            }
        }
    }

    @Nested
    @DisplayName("Signed Integer Types")
    inner class SignedIntegerTypes {

        @Test
        fun `i8 lowers to LLInt with BIT_8 size`() {
            val graphType = Int(BitSize.BIT_8)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLInt::class.java)
            assertThat((result as LLInt).size).isEqualTo(BitSize.BIT_8)
        }

        @Test
        fun `i16 lowers to LLInt with BIT_16 size`() {
            val graphType = Int(BitSize.BIT_16)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLInt::class.java)
            assertThat((result as LLInt).size).isEqualTo(BitSize.BIT_16)
        }

        @Test
        fun `i32 lowers to LLInt with BIT_32 size`() {
            val graphType = Int(BitSize.BIT_32)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLInt::class.java)
            assertThat((result as LLInt).size).isEqualTo(BitSize.BIT_32)
        }

        @Test
        fun `i64 lowers to LLInt with BIT_64 size`() {
            val graphType = Int(BIT_64)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLInt::class.java)
            assertThat((result as LLInt).size).isEqualTo(BIT_64)
        }

        @Test
        fun `i8 lowers with preserved graphType`() {
            val graphType = Int(BitSize.BIT_8)
            val result = TypeLowering.lower(graphType) as LLInt
            assertThat(result.graphType).isSameAs(graphType)
        }

        @Test
        fun `i32 lowers with preserved graphType`() {
            val graphType = Int(BitSize.BIT_32)
            val result = TypeLowering.lower(graphType) as LLInt
            assertThat(result.graphType).isSameAs(graphType)
        }

        @Test
        fun `i64 lowers with preserved graphType`() {
            val graphType = Int(BIT_64)
            val result = TypeLowering.lower(graphType) as LLInt
            assertThat(result.graphType).isSameAs(graphType)
        }
    }

    @Nested
    @DisplayName("Unsigned Integer Types")
    inner class UnsignedIntegerTypes {

        @Test
        fun `u8 lowers to LLUInt with BIT_8 size`() {
            val graphType = UInt(BitSize.BIT_8)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLUInt::class.java)
            assertThat((result as LLUInt).size).isEqualTo(BitSize.BIT_8)
        }

        @Test
        fun `u16 lowers to LLUInt with BIT_16 size`() {
            val graphType = UInt(BitSize.BIT_16)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLUInt::class.java)
            assertThat((result as LLUInt).size).isEqualTo(BitSize.BIT_16)
        }

        @Test
        fun `u32 lowers to LLUInt with BIT_32 size`() {
            val graphType = UInt(BitSize.BIT_32)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLUInt::class.java)
            assertThat((result as LLUInt).size).isEqualTo(BitSize.BIT_32)
        }
    }

    @Nested
    @DisplayName("Float Types")
    inner class FloatTypes {

        @Test
        fun `f32 lowers to LLFloat with BIT_32 size`() {
            val graphType = Float(BitSize.BIT_32)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLFloat::class.java)
            assertThat((result as LLFloat).size).isEqualTo(BitSize.BIT_32)
        }

        @Test
        fun `f64 lowers to LLFloat with BIT_64 size`() {
            val graphType = Float(BIT_64)
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLFloat::class.java)
            assertThat((result as LLFloat).size).isEqualTo(BIT_64)
        }
    }

    @Nested
    @DisplayName("Boolean")
    inner class BooleanType {

        @Test
        fun `boolean lowers to LLBool(Boolean)`() {
            val result = TypeLowering.lower(Boolean)
            assertThat(result).isEqualTo(LLBool(Boolean))
        }

        @Test
        fun `LLBool(Boolean) has BIT_1 size`() {
            assertThat(LLBool(Boolean).size).isEqualTo(BitSize.BIT_1)
        }

        @Test
        fun `LLBool(Boolean) graphType is Type Boolean`() {
            assertThat(LLBool(Boolean).graphType).isEqualTo(Boolean)
        }
    }

    @Nested
    @DisplayName("Array Types")
    inner class ArrayTypes {

        @Nested
        @DisplayName("Dynamic Arrays (size=null)")
        inner class DynamicArrays {

            @Test
            fun `lowers to JitsuArray`() {
                val graphType = Array(Int(BitSize.BIT_32), size = null)
                val result = TypeLowering.lower(graphType)
                assertThat(result).isInstanceOf(JitsuArray::class.java)
            }

            @Test
            fun `produces isDynamic=true`() {
                val graphType = Array(Int(BitSize.BIT_32), size = null)
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.isDynamic).isTrue()
            }

            @Test
            fun `produces isFixedSize=false`() {
                val graphType = Array(Int(BitSize.BIT_32), size = null)
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.isFixedSize).isFalse()
            }

            @Test
            fun `fixedSize is null`() {
                val graphType = Array(Int(BitSize.BIT_32), size = null)
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.fixedSize).isNull()
            }

            @Test
            fun `layout has length field of type i64`() {
                val graphType = Array(Int(BitSize.BIT_32), size = null)
                val result = TypeLowering.lower(graphType) as JitsuArray
                val layout = result.layout
                assertThat(layout.fields).containsKey("length")
                assertThat(layout.fields["length"]).isInstanceOf(LLInt::class.java)
                assertThat((layout.fields["length"] as LLInt).size).isEqualTo(BIT_64)
            }

            @Test
            fun `layout has data field as pointer to element type`() {
                val graphType = Array(Int(BitSize.BIT_32), size = null)
                val result = TypeLowering.lower(graphType) as JitsuArray
                val layout = result.layout
                assertThat(layout.fields).containsKey("data")
                assertThat(layout.fields["data"]).isInstanceOf(LLPointer::class.java)
            }

            @TestFactory
            fun `data pointer points to lowered element type`(): Stream<DynamicTest?>? {
                return exampleTypes.map {
                    DynamicTest.dynamicTest("${it.graph}[]") { dataPointerPointsToLoweredElementType(it) }
                }
            }

            internal fun dataPointerPointsToLoweredElementType(case: TypeCase) {
                val graphType = Array(case.element, size = null)
                val result = TypeLowering.lower(graphType) as JitsuArray
                val dataField = result.layout.fields["data"] as LLPointer<*>
                assertThat(dataField.pointeeType)
                    .usingRecursiveComparison()
                    .ignoringFieldsMatchingRegexes(".+\\.asType(\\\$delegate)?\\..*")
                    .isEqualTo(case.lowLevel)
            }

            @TestFactory
            fun `element type is lowered`(): Stream<DynamicTest> {
                return exampleTypes.map {
                    DynamicTest.dynamicTest("${it.graph}[]") { elementTypeIsLowered(it) }
                }
            }

            internal fun elementTypeIsLowered(type: TypeCase) {
                val graphType = Array(type.element, size = null)
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.elementType)
                    .usingRecursiveComparison()
                    .ignoringFieldsMatchingRegexes(".+\\.asType(\\\$delegate)?\\..*")
                    .isEqualTo(type.lowLevel)
            }

            @Test
            fun `layout only has length and data fields`() {
                val graphType = Array(Int(BitSize.BIT_32), size = null)
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.layout.fields.keys).containsExactlyInAnyOrder("length", "data")
            }
        }

        @Nested
        @DisplayName("Fixed-Size Arrays (size=N)")
        inner class FixedSizeArrays {

            @Test
            fun `lowers to JitsuArray`() {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(5u,dummyLocation))
                val result = TypeLowering.lower(graphType)
                assertThat(result).isInstanceOf(JitsuArray::class.java)
            }

            @Test
            fun `produces isFixedSize=true`() {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(5u, dummyLocation))
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.isFixedSize).isTrue()
            }

            @Test
            fun `produces isDynamic=false`() {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(5u, dummyLocation))
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.isDynamic).isFalse()
            }

            @ParameterizedTest
            @ValueSource(ints = [1, 20, 500])
            fun `fixedSize matches declared size`(size: ULong) {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(size, dummyLocation))
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.fixedSize).isEqualTo(size)
            }

            @Test
            fun `layout has data field of LLFixedArray type`() {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(5u, dummyLocation))
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.layout.fields).containsKey("data")
                assertThat(result.layout.fields["data"]).isInstanceOf(LLFixedArray::class.java)
            }

            @Test
            fun `LLFixedArray has correct element type`() {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(5u, dummyLocation))
                val result = TypeLowering.lower(graphType) as JitsuArray
                val dataField = result.layout.fields["data"] as LLFixedArray
                assertThat(dataField.elementType).isInstanceOf(LLInt::class.java)
                assertThat((dataField.elementType as LLInt).size).isEqualTo(BitSize.BIT_32)
            }

            @Test
            fun `LLFixedArray has correct size`() {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(5u, dummyLocation))
                val result = TypeLowering.lower(graphType) as JitsuArray
                val dataField = result.layout.fields["data"] as LLFixedArray
                assertThat(dataField.size).isEqualTo(5uL)
            }

            @Test
            fun `layout does not have a length field`() {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(5u, dummyLocation))
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.layout.fields.keys).doesNotContain("length")
            }

            @Test
            fun `layout only has data field`() {
                val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(5u, dummyLocation))
                val result = TypeLowering.lower(graphType) as JitsuArray
                assertThat(result.layout.fields.keys).containsExactly("data")
            }
        }

        @Nested
        @DisplayName("Nested Arrays")
        inner class NestedArrays {

            @Test
            fun `nested dynamic array lowers to JitsuArray of JitsuArray`() {
                // i32[][] = Array(Array(i32, null), null)
                val innerArrayType = Array(Int(BitSize.BIT_32), size = null)
                val outerArrayType = Array(innerArrayType, size = null)
                val result = TypeLowering.lower(outerArrayType) as JitsuArray
                assertThat(result.elementType).isInstanceOf(JitsuArray::class.java)
            }

            @Test
            fun `nested array inner element type is correctly lowered`() {
                val innerArrayType = Array(Int(BitSize.BIT_32), size = null)
                val outerArrayType = Array(innerArrayType, size = null)
                val result = TypeLowering.lower(outerArrayType) as JitsuArray
                val innerArray = result.elementType as JitsuArray
                assertThat(innerArray.elementType).isInstanceOf(LLInt::class.java)
                assertThat((innerArray.elementType as LLInt).size).isEqualTo(BitSize.BIT_32)
            }

            @Test
            fun `fixed nested array has correct sizes at each level`() {
                val innerArrayType = Array(Int(BitSize.BIT_32), size = UIntConstant(3u, dummyLocation))
                val outerArrayType = Array(innerArrayType, size = UIntConstant(4u, dummyLocation))
                val result = TypeLowering.lower(outerArrayType) as JitsuArray
                assertThat(result.fixedSize).isEqualTo(4uL)
                val innerArray = result.elementType as JitsuArray
                assertThat(innerArray.fixedSize).isEqualTo(3uL)
            }

            @Test
            fun `array with union element type recursively lowers union`() {
                val unionType = unionType(Int(BitSize.BIT_32), Float(BitSize.BIT_32))
                val arrayType = Array(unionType, size = null)
                val result = TypeLowering.lower(arrayType) as JitsuArray
                assertThat(result.elementType).isInstanceOf(JitsuUnion::class.java)
            }
        }
    }

    @Nested
    @DisplayName("Union")
    inner class UnionTypes {

        @Test
        fun `lowers to JitsuUnion`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(JitsuUnion::class.java)
        }

        @Test
        fun `layout has discriminant option field of type i8`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            val layout = result.layout
            assertThat(layout.fields).containsKey("option")
            assertThat(layout.fields["option"]).isInstanceOf(LLUInt::class.java)
            assertThat((layout.fields["option"] as LLUInt).size).isEqualTo(BitSize.BIT_8)
        }

        @Test
        fun `layout has value field of type LLUnion`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            assertThat(result.layout.fields).containsKey("value")
            assertThat(result.layout.fields["value"]).isInstanceOf(LLUnion::class.java)
        }

        @Test
        fun `value LLUnion has members for each option`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            val valueUnion = result.layout.fields["value"] as LLUnion
            assertThat(valueUnion.members.keys).containsExactlyInAnyOrder("o0", "o1")
        }

        @Test
        fun `option types are recursively lowered`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BIT_64))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            assertThat(result.options[0]).isInstanceOf(LLInt::class.java)
            assertThat(result.options[1]).isInstanceOf(LLFloat::class.java)
        }

        @Test
        fun `option order is preserved`() {
            val graphType = unionType(
                Int(BitSize.BIT_32),
                Float(BitSize.BIT_32),
                Boolean
            )
            val result = TypeLowering.lower(graphType) as JitsuUnion
            assertThat(result.options).hasSize(3)
            assertThat(result.options[0]).isInstanceOf(LLInt::class.java)
            assertThat(result.options[1]).isInstanceOf(LLFloat::class.java)
            assertThat(result.options[2]).isEqualTo(LLBool(Boolean))
        }

        @Test
        fun `o0 member in value LLUnion has correct type`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BIT_64))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            val valueUnion = result.layout.fields["value"] as LLUnion
            assertThat(valueUnion.members["o0"]).isInstanceOf(LLInt::class.java)
            assertThat((valueUnion.members["o0"] as LLInt).size).isEqualTo(BitSize.BIT_32)
        }

        @Test
        fun `o1 member in value LLUnion has correct type`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BIT_64))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            val valueUnion = result.layout.fields["value"] as LLUnion
            assertThat(valueUnion.members["o1"]).isInstanceOf(LLFloat::class.java)
            assertThat((valueUnion.members["o1"] as LLFloat).size).isEqualTo(BIT_64)
        }

        @Test
        fun `graphType is preserved on the JitsuUnion`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            assertThat(result.graphType).isSameAs(graphType)
        }

        @Test
        fun `with three options produces three LLUnion members`() {
            val graphType = unionType(
                Int(BitSize.BIT_8),
                Int(BitSize.BIT_16),
                Int(BitSize.BIT_32)
            )
            val result = TypeLowering.lower(graphType) as JitsuUnion
            val valueUnion = result.layout.fields["value"] as LLUnion
            assertThat(valueUnion.members.keys).containsExactlyInAnyOrder("o0", "o1", "o2")
        }

        @Test
        fun `with array option recursively lowers array`() {
            val arrayType = Array(Int(BitSize.BIT_32), size = null)
            val graphType = unionType(arrayType, Boolean)
            val result = TypeLowering.lower(graphType) as JitsuUnion
            assertThat(result.options[0]).isInstanceOf(JitsuArray::class.java)
        }

        @Test
        fun `layout only has option and value fields`() {
            val graphType = unionType(Int(BitSize.BIT_32), Float(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            assertThat(result.layout.fields.keys).containsExactlyInAnyOrder("option", "value")
        }
    }

    @Nested
    @DisplayName("StructuralInterface Types")
    inner class StructuralInterfaceTypes {

        @Test
        fun `struct with one field lowers to LLStruct`() {
            val graphType = structuralInterface("x" to Int(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(LLStruct::class.java)
        }

        @Test
        fun `struct field names are preserved`() {
            val graphType = structuralInterface(
                "x" to Int(BitSize.BIT_32),
                "y" to Float(BitSize.BIT_32)
            )
            val result = TypeLowering.lower(graphType) as LLStruct
            assertThat(result.fields.keys).containsExactlyInAnyOrder("x", "y")
        }

        @Test
        fun `struct field types are recursively lowered`() {
            val graphType = structuralInterface(
                "count" to Int(BitSize.BIT_32),
                "value" to Float(BIT_64)
            )
            val result = TypeLowering.lower(graphType) as LLStruct
            assertThat(result.fields["count"]).isInstanceOf(LLInt::class.java)
            assertThat((result.fields["count"] as LLInt).size).isEqualTo(BitSize.BIT_32)
            assertThat(result.fields["value"]).isInstanceOf(LLFloat::class.java)
            assertThat((result.fields["value"] as LLFloat).size).isEqualTo(BIT_64)
        }

        @Test
        fun `struct graphType is preserved`() {
            val graphType = structuralInterface("x" to Int(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType) as LLStruct
            assertThat(result.graphType).isSameAs(graphType)
        }

        @Test
        fun `struct with array field recursively lowers array`() {
            val arrayType = Array(Int(BitSize.BIT_32), size = null)
            val graphType = structuralInterface("data" to arrayType)
            val result = TypeLowering.lower(graphType) as LLStruct
            assertThat(result.fields["data"]).isInstanceOf(JitsuArray::class.java)
        }

        @Test
        fun `struct with union field recursively lowers union`() {
            val unionGraphType = unionType(Int(BitSize.BIT_32), Boolean)
            val graphType = structuralInterface("value" to unionGraphType)
            val result = TypeLowering.lower(graphType) as LLStruct
            assertThat(result.fields["value"]).isInstanceOf(JitsuUnion::class.java)
        }

        @Test
        fun `struct with boolean field lowers to LLBool(Boolean)`() {
            val graphType = structuralInterface("flag" to Boolean)
            val result = TypeLowering.lower(graphType) as LLStruct
            assertThat(result.fields["flag"]).isEqualTo(LLBool(Boolean))
        }

        @Test
        fun `empty struct lowers to LLStruct with no fields`() {
            val graphType = structuralInterface()
            val result = TypeLowering.lower(graphType) as LLStruct
            assertThat(result.fields).isEmpty()
        }

        @Test
        fun `struct with nested struct field recursively lowers`() {
            val innerStruct = structuralInterface("z" to Int(BitSize.BIT_8))
            val outerStruct = structuralInterface("inner" to innerStruct)
            val result = TypeLowering.lower(outerStruct) as LLStruct
            assertThat(result.fields["inner"]).isInstanceOf(LLStruct::class.java)
        }
    }

    @Nested
    @DisplayName("TypeReference")
    inner class TypeReferenceTests {

        @Test
        fun `TypeReference resolves and lowers the cached resolved type`() {
            val resolvedType = Int(BitSize.BIT_32)
            val ref = makeTypeRef("MyInt", resolvedType)
            val result = TypeLowering.lower(ref)
            assertThat(result).isInstanceOf(LLInt::class.java)
        }

        @Test
        fun `TypeReference to integer preserves correct bit size`() {
            val resolvedType = Int(BIT_64)
            val ref = makeTypeRef("MyBigInt", resolvedType)
            val result = TypeLowering.lower(ref) as LLInt
            assertThat(result.size).isEqualTo(BIT_64)
        }

        @Test
        fun `TypeReference to array resolves to JitsuArray`() {
            val resolvedType = Array(Int(BitSize.BIT_32), size = null)
            val ref = makeTypeRef("IntArray", resolvedType)
            val result = TypeLowering.lower(ref)
            assertThat(result).isInstanceOf(JitsuArray::class.java)
        }

        @Test
        fun `TypeReference to union resolves to JitsuUnion`() {
            val resolvedType = unionType(Int(BitSize.BIT_32), Boolean)
            val ref = makeTypeRef("IntOrBool", resolvedType)
            val result = TypeLowering.lower(ref)
            assertThat(result).isInstanceOf(JitsuUnion::class.java)
        }

        @Test
        fun `TypeReference to boolean resolves to LLBool(Boolean)`() {
            val ref = makeTypeRef("BoolAlias", Boolean)
            val result = TypeLowering.lower(ref)
            assertThat(result).isEqualTo(LLBool(Boolean))
        }

        // Creates a TypeReference with the resolvedCache pre-populated
        private fun makeTypeRef(name: String, resolved: TypeElement): TypeReference {
            val ref = TypeReference(
                reference = locatedName(name),
                genericParameters = emptyList()
            )
            ref.setEnclosingModule(dummyModule)
            ref.setResolvedTarget(TypeAlias("".locatedAt(dummyLocation),listOf(),resolved).also { it.setEnclosingModule(dummyModule) })
            ref.resolve(CompilerMessages())
            return ref
        }
    }

    @Nested
    @DisplayName("Null Type")
    @Disabled("Null not implemented yet")
    inner class NullTypeTests {
    }

    @Nested
    @DisplayName("Deeply Nested and Cross-Type Tests")
    inner class DeepNestingTests {

        @Test
        fun `union containing array containing union`() {
            val innerUnion = unionType(Int(BitSize.BIT_8), Boolean)
            val arrayOfUnion = Array(innerUnion, size = null)
            val outerUnion = unionType(arrayOfUnion, Int(BIT_64))
            val result = TypeLowering.lower(outerUnion) as JitsuUnion

            // First option should be JitsuArray
            assertThat(result.options[0]).isInstanceOf(JitsuArray::class.java)
            val innerArr = result.options[0] as JitsuArray
            assertThat(innerArr.elementType).isInstanceOf(JitsuUnion::class.java)
        }

        @Test
        fun `struct with union field containing array produces correct nested layout`() {
            val arrayType = Array(Float(BitSize.BIT_32), size = null)
            val unionField = unionType(arrayType, Boolean)
            val graphType = structuralInterface("items" to unionField)
            val result = TypeLowering.lower(graphType) as LLStruct
            val itemsField = result.fields["items"]
            assertThat(itemsField).isInstanceOf(JitsuUnion::class.java)
            val union = itemsField as JitsuUnion
            assertThat(union.options[0]).isInstanceOf(JitsuArray::class.java)
        }

        @Test
        fun `three-level nesting - struct containing array of union`() {
            val union = unionType(Int(BitSize.BIT_32), UInt(BitSize.BIT_32))
            val array = Array(union, size = UIntConstant(3u, dummyLocation))
            val struct = structuralInterface("table" to array)
            val result = TypeLowering.lower(struct) as LLStruct
            val tableField = result.fields["table"] as JitsuArray
            assertThat(tableField.isFixedSize).isTrue()
            assertThat(tableField.fixedSize).isEqualTo(3uL)
            assertThat(tableField.elementType).isInstanceOf(JitsuUnion::class.java)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `single-option union is lowered without simplification`() {
            // Type.Union with one option: JitsuUnion.of() requires non-empty list,
            // so single-option is valid to create but the switch() TODO hints it "shouldn't exist".
            // We verify lowering succeeds and produces a JitsuUnion regardless.
            val graphType = unionType(Int(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType)
            assertThat(result).isInstanceOf(JitsuUnion::class.java)
        }

        @Test
        fun `single-option union has exactly one lowered option`() {
            val graphType = unionType(Int(BitSize.BIT_32))
            val result = TypeLowering.lower(graphType) as JitsuUnion
            assertThat(result.options).hasSize(1)
        }

        @Test
        fun `array of size zero is a valid fixed array`() {
            val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(0u, dummyLocation))
            val result = TypeLowering.lower(graphType) as JitsuArray
            assertThat(result.isFixedSize).isTrue()
            assertThat(result.fixedSize).isEqualTo(0uL)
        }

        @Test
        fun `array of size one is a valid fixed array`() {
            val graphType = Array(Int(BitSize.BIT_32), size = UIntConstant(1u, dummyLocation))
            val result = TypeLowering.lower(graphType) as JitsuArray
            assertThat(result.fixedSize).isEqualTo(1uL)
        }

        @Test
        fun `lowering Undefined throws an error`() {
            assertThatThrownBy { TypeLowering.lower(Undefined) }
                .isInstanceOf(IllegalStateException::class.java)
        }

        @TestFactory
        fun `distinct graph type instances produce equal lowered types`(): Stream<DynamicTest> {
            return exampleTypes.map {
                DynamicTest.dynamicTest(it.graph.toString()) {
                    assertThat(TypeLowering.lower(it.graph)).usingRecursiveComparison()
                        .isEqualTo(TypeLowering.lower(it.graph))
                }
            }
        }
    }
}
