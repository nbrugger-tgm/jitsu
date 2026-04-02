package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.Type
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
            val result = AbstractValue.NoValue.join(AbstractValue.Const("5", valueType = i32))
            assertThat(result).isEqualTo(AbstractValue.Const("5", valueType = i32))
        }

        @Test
        fun `Const join NoValue returns Const`() {
            val result = AbstractValue.Const("5", valueType = i32).join(AbstractValue.NoValue)
            assertThat(result).isEqualTo(AbstractValue.Const("5", valueType = i32))
        }

        @Test
        fun `same Const join same Const returns same Const`() {
            val result = AbstractValue.Const("5", valueType = i32).join(AbstractValue.Const("5", valueType = i32))
            assertThat(result).isEqualTo(AbstractValue.Const("5", valueType = i32))
        }

        @Test
        fun `different Consts join to Unknown`() {
            val result = AbstractValue.Const("5", valueType = i32).join(AbstractValue.Const("3", valueType = i32))
            assertThat(result).isEqualTo(AbstractValue.Unknown)
        }

        @Test
        fun `Unknown join Const returns Unknown`() {
            val result = AbstractValue.Unknown.join(AbstractValue.Const("5", valueType = i32))
            assertThat(result).isEqualTo(AbstractValue.Unknown)
        }

        @Test
        fun `Const join Unknown returns Unknown`() {
            val result = AbstractValue.Const("5", valueType = i32).join(AbstractValue.Unknown)
            assertThat(result).isEqualTo(AbstractValue.Unknown)
        }

        @Test
        fun `NoValue join NoValue returns NoValue`() {
            val result = AbstractValue.NoValue.join(AbstractValue.NoValue)
            assertThat(result).isEqualTo(AbstractValue.NoValue)
        }

        @Test
        fun `Unknown join Unknown returns Unknown`() {
            val result = AbstractValue.Unknown.join(AbstractValue.Unknown)
            assertThat(result).isEqualTo(AbstractValue.Unknown)
        }
    }

    @Nested
    inner class FunctionSummaryOptimisticTest {
        @Test
        fun `optimistic seed is deterministic`() {
            val summary = FunctionSummary.optimistic(listOf("x", "y"))
            assertThat(summary.deterministic.value).isTrue()
        }

        @Test
        fun `optimistic seed has no side effects`() {
            val summary = FunctionSummary.optimistic(listOf("x"))
            assertThat(summary.noSideEffects.value).isTrue()
        }

        @Test
        fun `optimistic seed is pure`() {
            val summary = FunctionSummary.optimistic(listOf())
            assertThat(summary.pure).isTrue()
        }

        @Test
        fun `optimistic seed has all params as BORROW`() {
            val summary = FunctionSummary.optimistic(listOf("a", "b", "c"))
            assertThat(summary.parameterModes).containsExactlyInAnyOrderEntriesOf(
                mapOf("a" to ParameterMode.BORROW, "b" to ParameterMode.BORROW, "c" to ParameterMode.BORROW)
            )
        }

        @Test
        fun `optimistic seed has empty parameter influence`() {
            val summary = FunctionSummary.optimistic(listOf("x"))
            assertThat(summary.parameterInfluence).isEmpty()
        }

        @Test
        fun `optimistic seed has no return summary`() {
            val summary = FunctionSummary.optimistic(listOf("x"))
            assertThat(summary.returnSummary).isNull()
        }

        @Test
        fun `optimistic seed has no callees`() {
            val summary = FunctionSummary.optimistic(listOf("x"))
            assertThat(summary.callees).isEmpty()
        }
    }

    @Nested
    inner class FunctionSummaryRefineWithTest {
        @Test
        fun `refining deterministic with non-deterministic produces non-deterministic`() {
            val optimistic = FunctionSummary.optimistic(listOf("x"))
            val nonDet = optimistic.copy(
                deterministic = ReasonedBoolean.False("callee is non-deterministic")
            )
            val refined = optimistic.refineWith(nonDet)
            assertThat(refined.deterministic.value).isFalse()
        }

        @Test
        fun `refining with MOVE param produces MOVE`() {
            val optimistic = FunctionSummary.optimistic(listOf("x"))
            val withMove = optimistic.copy(parameterModes = mapOf("x" to ParameterMode.MOVE))
            val refined = optimistic.refineWith(withMove)
            assertThat(refined.parameterModes["x"]).isEqualTo(ParameterMode.MOVE)
        }

        @Test
        fun `refining with parameter influence grows the set`() {
            val a = FunctionSummary.optimistic(listOf("x", "y")).copy(parameterInfluence = setOf("x"))
            val b = FunctionSummary.optimistic(listOf("x", "y")).copy(parameterInfluence = setOf("y"))
            val refined = a.refineWith(b)
            assertThat(refined.parameterInfluence).containsExactlyInAnyOrder("x", "y")
        }

        @Test
        fun `refining side effects with impure produces impure`() {
            val optimistic = FunctionSummary.optimistic(listOf())
            val impure = optimistic.copy(
                noSideEffects = ReasonedBoolean.False("callee has side effects"),
            )
            val refined = optimistic.refineWith(impure)
            assertThat(refined.noSideEffects.value).isFalse()
            assertThat(refined.pure).isFalse()
        }

        @Test
        fun `double refinement is idempotent`() {
            val a = FunctionSummary.optimistic(listOf("x")).copy(
                parameterInfluence = setOf("x"),
                parameterModes = mapOf("x" to ParameterMode.MOVE)
            )
            val once = a.refineWith(a)
            val twice = once.refineWith(a)
            assertThat(twice).usingRecursiveComparison().isEqualTo(once)
            assertThat(once.structurallyEquals(twice)).isTrue()
        }

        @Test
        fun `refining return summaries merges types`() {
            val a = FunctionSummary.optimistic(listOf()).copy(
                returnSummary = ReturnSummary(
                    possibleTypes = listOf(i32),
                    compileTimeValue = AbstractValue.Const("5", valueType = i32),
                    dependsOnParameters = emptySet()
                )
            )
            val b = FunctionSummary.optimistic(listOf()).copy(
                returnSummary = ReturnSummary(
                    possibleTypes = listOf(Type.Boolean),
                    compileTimeValue = AbstractValue.Const("true", valueType = Type.Boolean),
                    dependsOnParameters = emptySet()
                )
            )
            val refined = a.refineWith(b)
            assertThat(refined.returnSummary).isNotNull
            assertThat(refined.returnSummary!!.possibleTypes).containsExactlyInAnyOrder(i32, Type.Boolean)
            assertThat(refined.returnSummary!!.compileTimeValue).isEqualTo(AbstractValue.Unknown)
        }
    }

    @Nested
    inner class StructuralEqualityTest {
        @Test
        fun `identical summaries are structurally equal`() {
            val a = FunctionSummary.optimistic(listOf("x"))
            val b = FunctionSummary.optimistic(listOf("x"))
            assertThat(a.structurallyEquals(b)).isTrue()
        }

        @Test
        fun `different determinism is not structurally equal`() {
            val a = FunctionSummary.optimistic(listOf("x"))
            val b = a.copy(deterministic = ReasonedBoolean.False(""))
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
            val a = ReturnSummary(listOf(i32), AbstractValue.Const("5", valueType = i32), setOf("x"))
            val b = ReturnSummary(listOf(i32), AbstractValue.Const("5", valueType = i32), setOf("y"))
            val merged = a.mergeWith(b)
            // distinct() means duplicate i32 is removed
            assertThat(merged.possibleTypes).containsExactly(i32)
            assertThat(merged.compileTimeValue).isEqualTo(AbstractValue.Const("5", valueType = i32))
            assertThat(merged.dependsOnParameters).containsExactlyInAnyOrder("x", "y")
        }

        @Test
        fun `merging with different const values produces Unknown`() {
            val a = ReturnSummary(listOf(i32), AbstractValue.Const("5", valueType = i32), emptySet())
            val b = ReturnSummary(listOf(i32), AbstractValue.Const("3", valueType = i32), emptySet())
            val merged = a.mergeWith(b)
            assertThat(merged.compileTimeValue).isEqualTo(AbstractValue.Unknown)
        }
    }

    @Nested
    inner class SerializationTest {
        private val json = Json { prettyPrint = true }

        @Test
        fun `AbstractValue round-trips through JSON`() {
            val values = listOf(
                AbstractValue.NoValue,
                AbstractValue.Const("42", valueType = i32),
                AbstractValue.Unknown
            )
            for (value in values) {
                val serialized = json.encodeToString(AbstractValue.serializer(), value)
                val deserialized = json.decodeFromString(AbstractValue.serializer(), serialized)
                assertThat(deserialized).isEqualTo(value)
            }
        }

        @Test
        fun `ReturnSummary round-trips through JSON`() {
            val summary = ReturnSummary(
                possibleTypes = listOf(i32, Type.Boolean),
                compileTimeValue = AbstractValue.Const("5", valueType = i32),
                dependsOnParameters = setOf("x", "y")
            )
            val serialized = json.encodeToString(ReturnSummary.serializer(), summary)
            val deserialized = json.decodeFromString(ReturnSummary.serializer(), serialized)
            assertThat(deserialized).isEqualTo(summary)
        }

        @Test
        fun `VariableSummary round-trips through JSON`() {
            val summary = VariableSummary(
                declaredType = i32,
                narrowedType = i32,
                isEffectivelyConstant = true,
                effectivelyConstantReason = "Variable is never reassigned and initializer is constant",
                compileTimeValue = AbstractValue.Const("42", valueType = i32)
            )
            val serialized = json.encodeToString(VariableSummary.serializer(), summary)
            val deserialized = json.decodeFromString(VariableSummary.serializer(), serialized)
            assertThat(deserialized.declaredType).isEqualTo(i32)
            assertThat(deserialized.narrowedType).isEqualTo(i32)
            assertThat(deserialized.isEffectivelyConstant).isTrue()
            assertThat(deserialized.compileTimeValue).isEqualTo(AbstractValue.Const("42", valueType = i32))
        }

        @Test
        fun `FunctionSummary round-trips through JSON - transient fields reset to defaults`() {
            val summary = FunctionSummary.optimistic(listOf("x"))
            val serialized = json.encodeToString(FunctionSummary.serializer(), summary)
            val deserialized = json.decodeFromString(FunctionSummary.serializer(), serialized)
            // Transient fields (ReasonedBoolean) reset to defaults
            assertThat(deserialized.deterministic.value).isTrue()
            assertThat(deserialized.noSideEffects.value).isTrue()
            assertThat(deserialized.parameterModes).containsExactlyInAnyOrderEntriesOf(mapOf("x" to ParameterMode.BORROW))
        }
    }
}
