package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.Location
import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AnalysisRepositoryTest {

    private val dummyLoc = Location(1, 1)
    private val dummyRange = Range(dummyLoc, dummyLoc)
    private val i32 = Type.Int(BitSize.BIT_32)
    private val i8 = Type.Int(BitSize.BIT_8)
    private lateinit var messages: CompilerMessages
    private lateinit var repo: AnalysisRepository

    @BeforeEach
    fun setup() {
        messages = CompilerMessages()
        repo = AnalysisRepository()
    }

    private fun <T> located(s: T) = Located(s, dummyRange)

    private fun buildFunction(
        name: String,
        returnType: Type? = null,
        parameters: List<Function.Parameter> = emptyList(),
        instructions: List<Instruction> = emptyList()
    ): Function = Function(located(name), returnType?.let { located(it) }, parameters, Function.Body.Implementation(CodeBlock(instructions)))

    private fun param(name: String, type: Type): Function.Parameter =
        Function.Parameter(located(name), type, null)

    private fun constI32(value: Long): Constant.IntConstant =
        Constant.IntConstant(value, dummyRange)

    private fun ret(value: Expression? = null): Instruction.Return =
        Instruction.Return(value, dummyRange)

    private fun call(name: String, args: List<Expression> = emptyList()): Instruction.FunctionCall =
        Instruction.FunctionCall(located(name), args, dummyRange)

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
        callInstr.target = bar

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
        callInstr.target = fib

        repo.analyzeAll(listOf(fib), messages)

        assertThat(repo.getFunctionSummary(fib)).isNotNull()
    }

    @Test
    fun `self-recursive function produces a summary`() {
        val callInstr = call("fib")
        val fib = buildFunction("fib", i32, listOf(param("n", i32)), listOf(ret(callInstr)))
        callInstr.target = fib

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

        callBFromA.target = b
        callAFromB.target = a

        repo.analyzeAll(listOf(a, b), messages)

        assertThat(repo.getFunctionSummary(a)).isNotNull()
        assertThat(repo.getFunctionSummary(b)).isNotNull()
    }

    @Test
    fun `scc ordering - bar is leaf, foo calls bar, foo uses bar summary`() {
        val bar = buildFunction("bar", i32, instructions = listOf(ret(constI32(42))))

        val callInstr = call("bar")
        val foo = buildFunction("foo", i32, instructions = listOf(ret(callInstr)))
        callInstr.target = bar

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
        )
        val ref = Expression.VariableReference(located("x"))
        ref.target = decl
        val foo = buildFunction("foo", i32, instructions = listOf(decl, ret(ref)))
        repo.analyzeAll(listOf(foo), messages)

        val useSite = repo.getUseSiteInfo(ref)
        assertThat(useSite).isNotNull()
        assertThat(useSite!!.narrowedType).isEqualTo(i8)
    }
}
