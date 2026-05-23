package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.analysis.ParameterMode
import eu.nitok.jitsu.compiler.graph.elements.types.Type
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AnalysisTypesTest {

    private val i32 = Type.Int(BitSize.BIT_32)

    @Nested
    inner class AbstractValueJoinTest {
        @Test
        fun `NoValue join Const returns Const`() {
            val result = AbstractValueElement.NoValue.join(AbstractValueElement.Const("5", valueType = i32))
            assertThat(result).isEqualTo(AbstractValueElement.Const("5", valueType = i32))
        }

        @Test
        fun `Const join NoValue returns Const`() {
            val result = AbstractValueElement.Const("5", valueType = i32).join(AbstractValueElement.NoValue)
            assertThat(result).isEqualTo(AbstractValueElement.Const("5", valueType = i32))
        }

        @Test
        fun `same Const join same Const returns same Const`() {
            val result = AbstractValueElement.Const("5", valueType = i32).join(AbstractValueElement.Const("5", valueType = i32))
            assertThat(result).isEqualTo(AbstractValueElement.Const("5", valueType = i32))
        }

        @Test
        fun `different Consts join to Unknown`() {
            val result = AbstractValueElement.Const("5", valueType = i32).join(AbstractValueElement.Const("3", valueType = i32))
            assertThat(result).isEqualTo(AbstractValueElement.Unknown)
        }

        @Test
        fun `Unknown join Const returns Unknown`() {
            val result = AbstractValueElement.Unknown.join(AbstractValueElement.Const("5", valueType = i32))
            assertThat(result).isEqualTo(AbstractValueElement.Unknown)
        }

        @Test
        fun `Const join Unknown returns Unknown`() {
            val result = AbstractValueElement.Const("5", valueType = i32).join(AbstractValueElement.Unknown)
            assertThat(result).isEqualTo(AbstractValueElement.Unknown)
        }

        @Test
        fun `NoValue join NoValue returns NoValue`() {
            val result = AbstractValueElement.NoValue.join(AbstractValueElement.NoValue)
            assertThat(result).isEqualTo(AbstractValueElement.NoValue)
        }

        @Test
        fun `Unknown join Unknown returns Unknown`() {
            val result = AbstractValueElement.Unknown.join(AbstractValueElement.Unknown)
            assertThat(result).isEqualTo(AbstractValueElement.Unknown)
        }
    }

    @Nested
    inner class FunctionSummaryOptimisticTest {

        @Test
        fun `optimistic seed has no side effects`() {
            val summary = FunctionSummaryElement.optimistic(listOf("x"))
            assertThat(summary.noSideEffects.value).isTrue()
        }

        @Test
        fun `optimistic seed is pure`() {
            val summary = FunctionSummaryElement.optimistic(listOf())
            assertThat(summary.pure).isTrue()
        }

        @Test
        fun `optimistic seed has all params as BORROW`() {
            val summary = FunctionSummaryElement.optimistic(listOf("a", "b", "c"))
            assertThat(summary.parameterModes).containsExactlyInAnyOrderEntriesOf(
                mapOf("a" to ParameterMode.BORROW, "b" to ParameterMode.BORROW, "c" to ParameterMode.BORROW)
            )
        }

        @Test
        fun `optimistic seed has no return summary`() {
            val summary = FunctionSummaryElement.optimistic(listOf("x"))
            assertThat(summary.returnSummary).isNull()
        }

        @Test
        fun `optimistic seed has no callees`() {
            val summary = FunctionSummaryElement.optimistic(listOf("x"))
            assertThat(summary.callees).isEmpty()
        }
    }

    @Nested
    inner class FunctionSummaryRefineWithTest {
        @Test
        fun `refining deterministic with non-deterministic produces non-deterministic`() {
            val optimistic = FunctionSummaryElement.optimistic(listOf("x"))
            val nonDet = optimistic.copy(
                returnSummary = ReturnSummaryElement(
                    deterministic = ReasonedBoolean.False("callee is non-deterministic")
                )
            )
            val refined = optimistic.refineWith(nonDet)
            assertThat(refined.returnSummary!!.deterministic.value).isFalse()
        }

        @Test
        fun `refining with MOVE param produces MOVE`() {
            val optimistic = FunctionSummaryElement.optimistic(listOf("x"))
            val withMove = optimistic.copy(parameterModes = mapOf("x" to ParameterMode.MOVE))
            val refined = optimistic.refineWith(withMove)
            assertThat(refined.parameterModes["x"]).isEqualTo(ParameterMode.MOVE)
        }

        @Test
        fun `refining with parameter influence grows the set`() {
            val a = FunctionSummaryElement.optimistic(listOf("x", "y")).copy(
                returnSummary = ReturnSummaryElement(
                    dependsOnParameters = setOf("x"),
                    deterministic = ReasonedBoolean.True("Optimistic")
                )
            )
            val b = FunctionSummaryElement.optimistic(listOf("x", "y")).copy(
                returnSummary = ReturnSummaryElement(
                    dependsOnParameters = setOf("y"),
                    deterministic = ReasonedBoolean.True("Optimistic")
                )
            )
            val refined = a.refineWith(b)
            assertThat(refined.returnSummary!!.dependsOnParameters).containsExactlyInAnyOrder("x", "y")
        }

        @Test
        fun `refining side effects with impure produces impure`() {
            val optimistic = FunctionSummaryElement.optimistic(listOf())
            val impure = optimistic.copy(
                noSideEffects = ReasonedBoolean.False("callee has side effects"),
            )
            val refined = optimistic.refineWith(impure)
            assertThat(refined.noSideEffects.value).isFalse()
            assertThat(refined.pure).isFalse()
        }

        @Test
        fun `double refinement is idempotent`() {
            val a = FunctionSummaryElement.optimistic(listOf("x")).copy(
                returnSummary = ReturnSummaryElement(
                    dependsOnParameters = setOf("x"),
                    deterministic = ReasonedBoolean.True("Optimistic")
                ),
                parameterModes = mapOf("x" to ParameterMode.MOVE)
            )
            val once = a.refineWith(a)
            val twice = once.refineWith(a)
            assertThat(twice).usingRecursiveComparison().isEqualTo(once)
            assertThat(once.structurallyEquals(twice)).isTrue()
        }

        @Test
        fun `refining return summaries merges types`() {
            val a = FunctionSummaryElement.optimistic(listOf()).copy(
                returnSummary = ReturnSummaryElement(
                    possibleTypes = listOf(i32),
                    compileTimeValue = AbstractValueElement.Const("5", valueType = i32),
                    dependsOnParameters = emptySet(),
                    deterministic = ReasonedBoolean.True("optimistic")
                )
            )
            val b = FunctionSummaryElement.optimistic(listOf()).copy(
                returnSummary = ReturnSummaryElement(
                    possibleTypes = listOf(Type.Boolean),
                    compileTimeValue = AbstractValueElement.Const("true", valueType = Type.Boolean),
                    dependsOnParameters = emptySet(),
                    deterministic = ReasonedBoolean.True("optimistic")
                )
            )
            val refined = a.refineWith(b)
            assertThat(refined.returnSummary).isNotNull
            assertThat(refined.returnSummary!!.possibleTypes).containsExactlyInAnyOrder(i32, Type.Boolean)
            assertThat(refined.returnSummary!!.compileTimeValue).isEqualTo(AbstractValueElement.Unknown)
        }
    }

    @Nested
    inner class StructuralEqualityTest {
        @Test
        fun `identical summaries are structurally equal`() {
            val a = FunctionSummaryElement.optimistic(listOf("x"))
            val b = FunctionSummaryElement.optimistic(listOf("x"))
            assertThat(a.structurallyEquals(b)).isTrue()
        }

        @Test
        fun `different determinism is not structurally equal`() {
            val a = FunctionSummaryElement.optimistic(listOf("x"))
            val b = a.copy(returnSummary = ReturnSummaryElement(
                deterministic = ReasonedBoolean.False("")
            ))
            assertThat(a.structurallyEquals(b)).isFalse()
        }
    }

    @Nested
    inner class ParameterModeTest {
        @Test
        fun `BORROW refine BORROW is BORROW`() {
            assertThat(ParameterMode.BORROW.refineWith(ParameterMode.BORROW)).isEqualTo(ParameterMode.BORROW)
        }

        @Test
        fun `BORROW refine MOVE is MOVE`() {
            assertThat(ParameterMode.BORROW.refineWith(ParameterMode.MOVE)).isEqualTo(ParameterMode.MOVE)
        }

        @Test
        fun `MOVE refine BORROW is MOVE`() {
            assertThat(ParameterMode.MOVE.refineWith(ParameterMode.BORROW)).isEqualTo(ParameterMode.MOVE)
        }

        @Test
        fun `MOVE refine MOVE is MOVE`() {
            assertThat(ParameterMode.MOVE.refineWith(ParameterMode.MOVE)).isEqualTo(ParameterMode.MOVE)
        }
    }

    @Nested
    inner class ReturnSummaryMergeTest {
        @Test
        fun `merging with same type keeps single entry`() {
            val a = ReturnSummaryElement(listOf(i32), AbstractValueElement.Const("5", valueType = i32), setOf("x"), ReasonedBoolean.True(""))
            val b = ReturnSummaryElement(listOf(i32), AbstractValueElement.Const("5", valueType = i32), setOf("y"), ReasonedBoolean.True(""))
            val merged = a.mergeWith(b)
            // distinct() means duplicate i32 is removed
            assertThat(merged.possibleTypes).containsExactly(i32)
            assertThat(merged.compileTimeValue).isEqualTo(AbstractValueElement.Const("5", valueType = i32))
            assertThat(merged.dependsOnParameters).containsExactlyInAnyOrder("x", "y")
        }

        @Test
        fun `merging with different const values produces Unknown`() {
            val a = ReturnSummaryElement(listOf(i32), AbstractValueElement.Const("5", valueType = i32), emptySet(), ReasonedBoolean.True(""))
            val b = ReturnSummaryElement(listOf(i32), AbstractValueElement.Const("3", valueType = i32), emptySet(), ReasonedBoolean.True(""))
            val merged = a.mergeWith(b)
            assertThat(merged.compileTimeValue).isEqualTo(AbstractValueElement.Unknown)
        }
    }

    @Nested
    inner class SerializationTest {
        private val json = Json { prettyPrint = true }

        @Test
        fun `AbstractValue round-trips through JSON`() {
            val values = listOf(
                AbstractValueElement.NoValue,
                AbstractValueElement.Const("42", valueType = i32),
                AbstractValueElement.Unknown
            )
            for (value in values) {
                val serialized = json.encodeToString(AbstractValueElement.serializer(), value)
                val deserialized = json.decodeFromString(AbstractValueElement.serializer(), serialized)
                assertThat(deserialized).isEqualTo(value)
            }
        }

        @Test
        fun `ReturnSummary round-trips through JSON`() {
            val summary = ReturnSummaryElement(
                possibleTypes = listOf(i32, Type.Boolean),
                compileTimeValue = AbstractValueElement.Const("5", valueType = i32),
                dependsOnParameters = setOf("x", "y"),
                deterministic =  ReasonedBoolean.True("abc")
            )
            val serialized = json.encodeToString(ReturnSummaryElement.serializer(), summary)
            val deserialized = json.decodeFromString(ReturnSummaryElement.serializer(), serialized)
            assertThat(deserialized).isEqualTo(summary)
        }

        @Test
        fun `VariableSummary round-trips through JSON`() {
            val summary = VariableSummaryElement(
                declaredType = i32,
                narrowedType = i32,
                effectivelyConstant = ReasonedBoolean.True("it is how it is"),
                compileTimeValue = AbstractValueElement.Const("42", valueType = i32),
                ownershipState = OwnershipState.OWNS
            )
            val serialized = json.encodeToString(VariableSummaryElement.serializer(), summary)
            val deserialized = json.decodeFromString(VariableSummaryElement.serializer(), serialized)
            assertThat(deserialized.declaredType).isEqualTo(i32)
            assertThat(deserialized.narrowedType).isEqualTo(i32)
            assertThat(deserialized.effectivelyConstant.value).isTrue()
            assertThat(deserialized.compileTimeValue).isEqualTo(AbstractValueElement.Const("42", valueType = i32))
        }

        @Test
        fun `FunctionSummary round-trips through JSON - transient fields reset to defaults`() {
            val summary = FunctionSummaryElement.optimistic(listOf("x"))
            val serialized = json.encodeToString(FunctionSummaryElement.serializer(), summary)
            val deserialized = json.decodeFromString(FunctionSummaryElement.serializer(), serialized)
            // Transient fields (ReasonedBoolean) reset to defaults
            assertThat(deserialized.noSideEffects.value).isTrue()
            assertThat(deserialized.parameterModes).containsExactlyInAnyOrderEntriesOf(mapOf("x" to ParameterMode.BORROW))
        }
    }
}
