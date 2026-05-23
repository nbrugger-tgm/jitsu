package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.locating.Position
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.elements.CodeBlockElement
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.Instruction
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import eu.nitok.jitsu.compiler.graph.elements.types.Type
import eu.nitok.jitsu.compiler.graph.behaviour.ScopeAware
import eu.nitok.jitsu.compiler.graph.elements.JitsuModule
import eu.nitok.jitsu.compiler.graph.elements.VariableDeclaration
import eu.nitok.jitsu.compiler.graph.elements.expressions.Constant
import eu.nitok.jitsu.compiler.graph.elements.expressions.VariableReference
import eu.nitok.jitsu.compiler.graph.elements.instructions.FunctionCall
import eu.nitok.jitsu.compiler.graph.elements.instructions.Return
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class AnalysisRepositoryTest {

    private val dummyLoc = Position(1, 1, URI("memory://test.jit"))
    private val dummyLocation = Location(dummyLoc, dummyLoc)
    private val i32 = Type.Int(BitSize.BIT_32)
    private val i8 = Type.Int(BitSize.BIT_8)
    private lateinit var messages: CompilerMessages
    private lateinit var repo: AnalysisRepository
    private var module = JitsuModule("test", listOf(), listOf())


    private fun <T> T.modularized():T {
        if(this is ModuleAware) setEnclosingModule(this@AnalysisRepositoryTest.module)
        if(this is ScopeAware) setEnclosingScope(module.scope)
        return this
    }
    @BeforeEach
    fun setup() {
        messages = CompilerMessages()
        repo = AnalysisRepository()
        module = JitsuModule("test", listOf(), listOf())
    }

    private fun <T> located(s: T) = Located(s, dummyLocation)

    private fun buildFunction(
        name: String,
        returnType: Type? = null,
        parameters: List<FunctionElement.Parameter> = emptyList(),
        instructions: List<Instruction> = emptyList()
    ): FunctionElement {
        val function = FunctionElement(
            located(name),
            returnType?.let { located(it) },
            parameters,
            FunctionElement.BodyElement.Implementation(CodeBlockElement(instructions)),
            dummyLocation
        )
        function.setEnclosingModule(module)
        function.setEnclosingScope(module.scope)
        return function
    }
    private fun param(name: String, type: Type): FunctionElement.Parameter =
        FunctionElement.Parameter(located(name), type, null).modularized()

    private fun constI32(value: Long): Constant.IntConstant =
        Constant.IntConstant(value, dummyLocation).modularized()

    private fun ret(value: Expression? = null): Return =
        Return(value, dummyLocation).modularized()

    private fun call(name: String, args: List<Expression> = emptyList()): FunctionCall =
        FunctionCall(located(name), args, dummyLocation).modularized()

    @Test
    fun `single non-recursive function gets summary`() {
        val foo = buildFunction("foo", i32, instructions = listOf(ret(constI32(5))))
        repo.analyzeAll(listOf(foo), messages)
        val summary = repo.getFunctionSummary(foo)
        assertThat(summary).isNotNull()
        assertThat(summary!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(summary.noSideEffects.value).isTrue()
    }

    @Test
    fun `two independent functions both get summaries`() {
        val foo = buildFunction("foo", i32, instructions = listOf(ret(constI32(5))))
        val bar = buildFunction("bar", i32, instructions = listOf(ret(constI32(10))))
        repo.analyzeAll(listOf(foo, bar), messages)
        assertThat(repo.getFunctionSummary(foo)).isNotNull()
        assertThat(repo.getFunctionSummary(bar)).isNotNull()
        assertThat(repo.getFunctionSummary(foo)!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(repo.getFunctionSummary(bar)!!.returnSummary!!.deterministic.value).isTrue()
    }

    @Test
    fun `linear call chain foo calls bar - bar analyzed before foo`() {
        val bar = buildFunction("bar", i32, instructions = listOf(ret(constI32(5))))

        val callInstr = call("bar")
        val foo = buildFunction("foo", i32, instructions = listOf(ret(callInstr)))
        callInstr.setResolvedTarget(bar)

        repo.analyzeAll(listOf(foo, bar), messages)

        assertThat(repo.getFunctionSummary(foo)).isNotNull()
        assertThat(repo.getFunctionSummary(bar)).isNotNull()
        assertThat(repo.getFunctionSummary(foo)!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(repo.getFunctionSummary(bar)!!.returnSummary!!.deterministic.value).isTrue()
    }

    @Test
    fun `self-recursive function converges without stack overflow`() {
        val callInstr = call("fib")
        val fib = buildFunction("fib", i32, listOf(param("n", i32)), listOf(ret(callInstr)))
        callInstr.setResolvedTarget(fib)

        repo.analyzeAll(listOf(fib), messages)

        assertThat(repo.getFunctionSummary(fib)).isNotNull()
    }

    @Test
    fun `self-recursive function produces a summary`() {
        val callInstr = call("fib")
        val fib = buildFunction("fib", i32, listOf(param("n", i32)), listOf(ret(callInstr)))
        callInstr.setResolvedTarget(fib)

        repo.analyzeAll(listOf(fib), messages)

        val summary = repo.getFunctionSummary(fib)
        assertThat(summary).isNotNull()
        assertThat(summary!!.callees)
            .singleElement()
            .extracting { it.name?.value }
            .isEqualTo("fib")
    }

    @Test
    fun `mutually recursive functions both converge`() {
        val callBFromA = call("b")
        val a = buildFunction("a", null, emptyList(), listOf(callBFromA))

        val callAFromB = call("a")
        val b = buildFunction("b", null, emptyList(), listOf(callAFromB))

        callBFromA.setResolvedTarget(b)
        callAFromB.setResolvedTarget(a)

        repo.analyzeAll(listOf(a, b), messages)

        assertThat(repo.getFunctionSummary(a)).isNotNull()
        assertThat(repo.getFunctionSummary(b)).isNotNull()
    }

    @Test
    fun `scc ordering - bar is leaf, foo calls bar, foo uses bar summary`() {
        val bar = buildFunction("bar", i32, instructions = listOf(ret(constI32(42))))

        val callInstr = call("bar")
        val foo = buildFunction("foo", i32, instructions = listOf(ret(callInstr)))
        callInstr.setResolvedTarget(bar)

        repo.analyzeAll(listOf(foo, bar), messages)

        assertThat(repo.getFunctionSummary(foo)!!.returnSummary!!.deterministic.value).isTrue()
        assertThat(repo.getFunctionSummary(bar)!!.returnSummary!!.deterministic.value).isTrue()
    }

    @Test
    fun `empty function list produces no summaries`() {
        repo.analyzeAll(emptyList(), messages)
        assertThat(repo.getFunctionSummary(buildFunction("ghost"))).isNull()
    }

    @Test
    fun `variable summaries stored for analyzed functions`() {
        val decl = VariableDeclaration(
            reassignable = false,
            name = located("x"),
            declaredType = i32,
            initialValue = constI32(7)
        )
        val foo = buildFunction("foo", instructions = listOf(decl))
        repo.analyzeAll(listOf(foo), messages)

        val varEntry = repo.getFunctionSummary(foo)?.variableSummary?.get(decl.name.value)
        assertThat(varEntry).isNotNull()
        assertThat(varEntry!!.effectivelyConstant.value).isTrue()
    }

    @Test
    fun `use site infos stored for analyzed functions`() {
        val decl = VariableDeclaration(
            reassignable = false,
            name = located("x"),
            declaredType = i32,
            initialValue = constI32(7)
        ).modularized()
        val ref = VariableReference(located("x")).modularized()
        ref.setResolvedTarget(decl)
        val foo = buildFunction("foo", i32, instructions = listOf(decl, ret(ref)))
        repo.analyzeAll(listOf(foo), messages)

        val useSite = repo.getUseSiteInfo(ref)
        assertThat(useSite).isNotNull()
        assertThat(useSite!!.narrowedType).isEqualTo(i8)
    }
}
