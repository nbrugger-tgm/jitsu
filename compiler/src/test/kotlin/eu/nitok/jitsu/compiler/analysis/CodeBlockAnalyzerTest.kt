package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.locating.Position
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.elements.CodeBlockElement
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeAware
import eu.nitok.jitsu.compiler.graph.elements.ConstantElement
import eu.nitok.jitsu.compiler.graph.elements.ExpressionElement
import eu.nitok.jitsu.compiler.graph.elements.FunctionCall
import eu.nitok.jitsu.compiler.graph.elements.InstructionElement
import eu.nitok.jitsu.compiler.graph.elements.JitsuModule
import eu.nitok.jitsu.compiler.graph.elements.Return
import eu.nitok.jitsu.compiler.graph.elements.VariableDeclaration
import eu.nitok.jitsu.compiler.graph.elements.VariableReference
import eu.nitok.jitsu.compiler.graph.elements.types.Int
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

class CodeBlockAnalyzerTest {

    private val dummyLoc = Position(1, 1, URI("memory://test.jit"))
    private val dummyLocation = Location(dummyLoc, dummyLoc)
    private val i8 = Int(BitSize.BIT_8)
    private val i32 = Int(BitSize.BIT_32)
    private val messages = CompilerMessages()
    private val noOracle: (FunctionElement) -> FunctionSummaryElement? = { null }
    private var module = JitsuModule("test", listOf(), listOf())

    private fun <T> T.modularized():T {
        if(this is ModuleAware) setEnclosingModule(this@CodeBlockAnalyzerTest.module)
        if(this is ScopeAware) setEnclosingScope(module.scope)
        return this
    }
    private fun <T> loc(s: T) = Located(s, dummyLocation)

    private fun buildFunction(
        name: String? = "foo",
        returnType: TypeElement? = null,
        parameters: List<FunctionElement.Parameter> = emptyList(),
        instructions: List<InstructionElement> = emptyList()
    ): FunctionElement = FunctionElement(
        name = name?.let { loc(it) },
        returnTypeElement = returnType?.let { loc(it) },
        parameters = parameters,
        bodyElement = FunctionElement.BodyElement.Implementation(CodeBlockElement(instructions)),
        dummyLocation
    ).modularized()

    private fun param(name: String, type: TypeElement): FunctionElement.Parameter =
        FunctionElement.Parameter(loc(name), type, null).modularized()

    private fun constInt(value: Long): ConstantElement.IntConstant =
        ConstantElement.IntConstant(value, dummyLocation).modularized()

    private fun varRef(name: String): VariableReference =
        VariableReference(loc(name)).modularized()

    private fun varDecl(
        name: String,
        type: TypeElement? = null,
        initialValue: ExpressionElement? = null,
        reassignable: Boolean = false
    ): VariableDeclaration = VariableDeclaration(
        reassignable = reassignable,
        name = loc(name),
        declaredTypeElement = type,
        initialValueElement = initialValue
    ).modularized()

    private fun ret(value: ExpressionElement? = null): Return =
        Return(value, dummyLocation).modularized()

    private fun analyze(
        fn: FunctionElement,
        oracle: (FunctionElement) -> FunctionSummaryElement? = noOracle
    ): CodeBlockAnalyzer.AnalysisResult =
        CodeBlockAnalyzer(fn, oracle, messages).analyze()

    @Nested
    inner class EmptyFunction {

        @Test
        fun `empty function has no side effects`() {
            val fn = buildFunction()
            val result = analyze(fn)
            assertThat(result.functionSummary.noSideEffects.value).isTrue()
        }

        @Test
        fun `empty function is pure`() {
            val fn = buildFunction()
            val result = analyze(fn)
            assertThat(result.functionSummary.pure).isTrue()
        }

        @Test
        fun `empty function has null return summary`() {
            val fn = buildFunction()
            val result = analyze(fn)
            assertThat(result.functionSummary.returnSummary).isNull()
        }
    }

    @Nested
    inner class ConstantReturn {

        @Test
        fun `returning constant 5 yields deterministic summary`() {
            val fn = buildFunction(
                returnType = i32,
                instructions = listOf(ret(constInt(5)))
            )
            val result = analyze(fn)
            assertThat(result.functionSummary.returnSummary!!.deterministic.value).isTrue()
        }

        @Test
        fun `returning constant 5 yields Const abstract value`() {
            val fn = buildFunction(
                returnType = i32,
                instructions = listOf(ret(constInt(5)))
            )
            val result = analyze(fn)
            val returnSummary = result.functionSummary.returnSummary
            assertThat(returnSummary).isNotNull()
            assertThat(returnSummary!!.compileTimeValue).isEqualTo(AbstractValueElement.Const("5", i32))
        }

        @Test
        fun `returning constant 5 includes i32 in possible types`() {
            val fn = buildFunction(
                returnType = i32,
                instructions = listOf(ret(constInt(5)))
            )
            val result = analyze(fn)
            assertThat(result.functionSummary.returnSummary!!.possibleTypes).contains(i32)
        }
    }

    @Nested
    inner class ParameterPassthrough {

        @Test
        fun `returning parameter is deterministic`() {
            val xParam = param("x", i32)
            val ref = varRef("x")
            ref.setResolvedTarget(xParam)
            val fn = buildFunction(
                returnType = i32,
                parameters = listOf(xParam),
                instructions = listOf(ret(ref))
            )
            val result = analyze(fn)
            assertThat(result.functionSummary.returnSummary!!.deterministic.value).isTrue()
        }

        @Test
        fun `returning parameter has Unknown compile time value`() {
            val xParam = param("x", i32)
            val ref = varRef("x")
            ref.setResolvedTarget(xParam)
            val fn = buildFunction(
                returnType = i32,
                parameters = listOf(xParam),
                instructions = listOf(ret(ref))
            )
            val result = analyze(fn)
            assertThat(result.functionSummary.returnSummary!!.compileTimeValue)
                .isEqualTo(AbstractValueElement.Unknown)
        }

        @Test
        fun `returning parameter shows x in dependsOnParameters`() {
            val xParam = param("x", i32)
            val ref = varRef("x")
            ref.setResolvedTarget(xParam)
            val fn = buildFunction(
                returnType = i32,
                parameters = listOf(xParam),
                instructions = listOf(ret(ref))
            )
            val result = analyze(fn)
            assertThat(result.functionSummary.returnSummary!!.dependsOnParameters).contains("x")
        }
    }

    @Nested
    inner class VariableDeclarationAndReturn {

        @Test
        fun `let x = 5 makes variable effectively constant`() {
            val decl = varDecl("x", i32, constInt(5), reassignable = false)
            val fn = buildFunction(instructions = listOf(decl))
            val result = analyze(fn)
            val xSummary = result.functionSummary.variableSummary.entries
                .first { it.key == "x" }.value
            assertThat(xSummary.effectivelyConstant.value).isTrue()
        }

        @Test
        fun `let x = 5 captures compile time value`() {
            val decl = varDecl("x", i32, constInt(5), reassignable = false)
            val fn = buildFunction(instructions = listOf(decl))
            val result = analyze(fn)
            val xSummary = result.functionSummary.variableSummary.entries
                .first { it.key == "x" }.value
            assertThat(xSummary.compileTimeValue).isEqualTo(AbstractValueElement.Const("5", i32))
            assertThat(xSummary.narrowedTypeElement).isEqualTo(i8)
        }

        @Test
        fun `returning constant variable yields deterministic function`() {
            val decl = varDecl("x", i32, constInt(5), reassignable = false)
            val ref = varRef("x")
            ref.setResolvedTarget(decl)
            val fn = buildFunction(
                returnType = i32,
                instructions = listOf(decl, ret(ref))
            )
            val result = analyze(fn)
            assertThat(result.functionSummary.returnSummary!!.deterministic.value).isTrue()
        }

        @Test
        fun `var x is not effectively constant`() {
            val decl = varDecl("x", i32, constInt(5), reassignable = true)
            val fn = buildFunction(instructions = listOf(decl))
            val result = analyze(fn)
            val xSummary = result.functionSummary.variableSummary.entries
                .first { it.key == "x" }.value
            assertThat(xSummary.effectivelyConstant.value).isFalse()
        }
    }

    @Nested
    inner class PureCallee {

        private val pureSummary = FunctionSummaryElement(
            returnSummary = ReturnSummaryElement(deterministic = ReasonedBoolean.True("pure")),
            noSideEffects = ReasonedBoolean.True("pure"),
            variableSummary = mapOf()
        )

        @Test
        fun `calling pure callee keeps function deterministic`() {
            val xParam = param("x", i32)
            val calleeFunc = buildFunction("bar", i32, listOf(param("a", i32)))
            val ref = varRef("x")
            ref.setResolvedTarget(xParam)

            val callInstr = FunctionCall(loc("bar"), listOf(ref), dummyLocation).modularized()
            callInstr.setResolvedTarget(calleeFunc)

            val fn = buildFunction(
                returnType = i32,
                parameters = listOf(xParam),
                instructions = listOf(Return(callInstr, dummyLocation))
            )
            val result = analyze(fn) { if (it == calleeFunc) pureSummary else null }
            assertThat(result.functionSummary.returnSummary!!.deterministic.value).isTrue()
        }

        @Test
        fun `calling pure callee keeps function side-effect-free`() {
            val xParam = param("x", i32)
            val calleeFunc = buildFunction("bar", i32, listOf(param("a", i32)))
            val ref = varRef("x")
            ref.setResolvedTarget(xParam)

            val callInstr = FunctionCall(loc("bar"), listOf(ref), dummyLocation).modularized()
            callInstr.setResolvedTarget(calleeFunc)

            val fn = buildFunction(
                returnType = i32,
                parameters = listOf(xParam),
                instructions = listOf(Return(callInstr, dummyLocation))
            )
            val result = analyze(fn) { if (it == calleeFunc) pureSummary else null }
            assertThat(result.functionSummary.noSideEffects.value).isTrue()
        }

        @Test
        fun `callee name appears in callees list`() {
            val calleeFunc = buildFunction("bar", i32)
            val callInstr = FunctionCall(loc("bar"), emptyList(), dummyLocation).modularized()
            callInstr.setResolvedTarget(calleeFunc)

            val fn = buildFunction(instructions = listOf(callInstr))
            val result = analyze(fn) { pureSummary }
            assertThat(result.functionSummary.callees)
                .singleElement()
                .extracting { it.name?.value }
                .isEqualTo("bar")
        }
    }

    @Nested
    inner class ImpureCallee {

        private val impureSummary = FunctionSummaryElement(
            returnSummary = ReturnSummaryElement(deterministic = ReasonedBoolean.True("det")),
            noSideEffects = ReasonedBoolean.False("side effects"),
            variableSummary = mapOf()
        )

        @Test
        fun `calling impure callee taints function with side effects`() {
            val calleeFunc = buildFunction("bar")
            val callInstr = FunctionCall(loc("bar"), emptyList(), dummyLocation).modularized()
            callInstr.setResolvedTarget(calleeFunc)

            val fn = buildFunction(instructions = listOf(callInstr))
            val result = analyze(fn) { if (it == calleeFunc) impureSummary else null }
            assertThat(result.functionSummary.noSideEffects.value).isFalse()
        }

        @Test
        fun `calling impure callee makes function impure`() {
            val calleeFunc = buildFunction("bar")
            val callInstr = FunctionCall(loc("bar"), emptyList(), dummyLocation).modularized()
            callInstr.setResolvedTarget(calleeFunc)

            val fn = buildFunction(instructions = listOf(callInstr))
            val result = analyze(fn) { if (it == calleeFunc) impureSummary else null }
            assertThat(result.functionSummary.pure).isFalse()
        }
    }

    @Nested
    inner class VoidFunction {

        @Test
        fun `void function has null return summary`() {
            val decl = varDecl("x", i32, constInt(42))
            val fn = buildFunction(instructions = listOf(decl))
            val result = analyze(fn)
            assertThat(result.functionSummary.returnSummary).isNull()
        }
    }

    @Nested
    inner class UseSiteInfoTracking {

        @Test
        fun `use site info recorded for variable reference`() {
            val decl = varDecl("x", i32, constInt(7), reassignable = false)
            val ref = varRef("x")
            ref.setResolvedTarget(decl)
            val fn = buildFunction(
                returnType = i32,
                instructions = listOf(decl, ret(ref))
            )
            val result = analyze(fn)
            val useSite = result.useSiteInfos[ref]
            assertThat(useSite).isNotNull()
            assertThat(useSite!!.narrowedType).isEqualTo(i8)
        }
    }
}
