package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.common.Location
import eu.nitok.jitsu.common.Range
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.analysis.AbstractValue
import eu.nitok.jitsu.compiler.analysis.FunctionSummary
import eu.nitok.jitsu.compiler.analysis.OwnershipState
import eu.nitok.jitsu.compiler.analysis.ReturnSummary
import eu.nitok.jitsu.compiler.analysis.VariableSummary
import eu.nitok.jitsu.compiler.graph.Expression
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.JitsuFile
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.graph.VariableDeclaration
import eu.nitok.jitsu.compiler.graph.buildGraph
import eu.nitok.jitsu.parser.parseFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.*
import java.net.URI

@DisplayName("VariableRegistry")
class VariableRegistryTest {

    private val dummyRange = Range(Location(1, 1), Location(1, 1))
    private fun <T> loc(value: T) = Located(value, dummyRange)

    private fun buildFile(source: String): JitsuFile {
        val ast = parseFile(source, URI("test://sourcefile.jit"))
        ast.sequence().forEach {
            if(it.errors.isNotEmpty()) throw IllegalArgumentException("Syntax error(s)! ${it.errors.joinToString("\n")}")
        }
        val graph = buildGraph(ast)
        if(graph.messages.errors.isNotEmpty()) throw IllegalArgumentException("Compilation error(s)! ${graph.messages.errors.joinToString("\n")}")
        return graph
    }

    private fun firstFunction(source: String): Function =
        buildFile(source).sequence().filterIsInstance<Function>().first()

    private fun functionNamed(source: String, name: String): Function =
        buildFile(source).sequence().filterIsInstance<Function>().first { it.name?.value == name }

    /** Attach a [FunctionSummary] to the function via reflection (field is `internal set`). */
    private fun Function.withSummary(summary: FunctionSummary): Function {
        val field = Function::class.java.getDeclaredField("summary")
        field.isAccessible = true
        field.set(this, summary)
        return this
    }

    private fun minimalSummary(variableSummary: Map<String, VariableSummary> = emptyMap()) =
        FunctionSummary(
            returnSummary = ReturnSummary(deterministic = ReasonedBoolean.True("test")),
            noSideEffects = ReasonedBoolean.True("test"),
            variableSummary = variableSummary
        )

    private fun ownedSummary(vararg names: String) =
        minimalSummary(names.associateWith { variableSummary(OwnershipState.OWNS) })

    private fun borrowedSummary(vararg names: String) =
        minimalSummary(names.associateWith { variableSummary(OwnershipState.BORROWS) })

    private fun variableSummary(ownershipState: OwnershipState) =
        VariableSummary(
            ownershipState = ownershipState,
            compileTimeValue = AbstractValue.Unknown
        )

    private fun variableDecl(name: String, type: Type): VariableDeclaration =
        VariableDeclaration(
            reassignable = true,
            name = loc(name),
            declaredType = type,
            initialValue = null
        )

    private fun parameter(name: String, type: Type): Function.Parameter =
        Function.Parameter(
            name = loc(name),
            declaredType = type,
            initialValue = null
        )

    @Nested
    @DisplayName("variablesToFree")
    inner class VariablesToFree {

        @Test
        fun `is empty when no variables have been registered`() {
            val fn = firstFunction("fn f() { }")
            val registry = VariableRegistry(fn)

            assertThat(registry.variablesToFree).isEmpty()
        }

        @Test
        fun `is empty when all registered variables have requiresFree=false`() {
            val fn = firstFunction("fn f() { }")
            fn.withSummary(borrowedSummary("a", "b"))
            val registry = VariableRegistry(fn)
            registry.getEntry(variableDecl("a", Type.Int(BitSize.BIT_32)))
            registry.getEntry(variableDecl("b", Type.Int(BitSize.BIT_64)))

            assertThat(registry.variablesToFree).isEmpty()
        }

        @Test
        fun `includes only entries where requiresFree is true`() {
            val fn = firstFunction("fn f() { }")
            fn.withSummary(
                minimalSummary(
                    mapOf(
                        "owned" to variableSummary(OwnershipState.OWNS),
                        "borrowed" to variableSummary(OwnershipState.BORROWS)
                    )
                )
            )
            val registry = VariableRegistry(fn)
            val ownedDecl = variableDecl("owned", Type.Int(BitSize.BIT_32))
            val borrowedDecl = variableDecl("borrowed", Type.Int(BitSize.BIT_32))
            registry.getEntry(ownedDecl)
            registry.getEntry(borrowedDecl)

            val toFree = registry.variablesToFree
            assertThat(toFree).hasSize(1)
            assertThat(toFree.first().name).isEqualTo("owned")
        }

        @Test
        fun `includes all owned variables when multiple are registered`() {
            val fn = firstFunction("fn f() { }")
            fn.withSummary(ownedSummary("x", "y", "z"))
            val registry = VariableRegistry(fn)
            registry.getEntry(variableDecl("x", Type.Int(BitSize.BIT_32)))
            registry.getEntry(variableDecl("y", Type.Int(BitSize.BIT_64)))
            registry.getEntry(variableDecl("z", Type.Boolean))

            assertThat(registry.variablesToFree).hasSize(3)
            assertThat(registry.variablesToFree.map { it.name })
                .containsExactlyInAnyOrder("x", "y", "z")
        }

        @Test
        fun `excludes moved variables`() {
            val fn = firstFunction("fn f() { }")
            fn.withSummary(
                minimalSummary(
                    mapOf(
                        "moved" to variableSummary(OwnershipState.MOVED),
                        "owned" to variableSummary(OwnershipState.OWNS)
                    )
                )
            )
            val registry = VariableRegistry(fn)
            registry.getEntry(variableDecl("moved", Type.Int(BitSize.BIT_32)))
            registry.getEntry(variableDecl("owned", Type.Int(BitSize.BIT_32)))

            assertThat(registry.variablesToFree.map { it.name }).containsExactly("owned")
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {


        @Test
        fun `variable with Undefined type throws in TypeLowering`() {
            val fn = firstFunction("fn f() { }")
            val registry = VariableRegistry(fn)
            // declaredType=null and implicitType=null → type is Type.Undefined
            val decl = VariableDeclaration(
                reassignable = false,
                name = loc("undeclared"),
                declaredType = null,
                initialValue = null
            )
            // implicitType stays null → type resolves to Type.Undefined

            assertThatThrownBy { registry.getEntry(decl) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("undefined")
        }

        @Test
        fun `variablesToFree reflects new entries added after first access`() {
            val fn = firstFunction("fn f() { }")
            fn.withSummary(ownedSummary("a", "b"))
            val registry = VariableRegistry(fn)

            // Initially empty
            assertThat(registry.variablesToFree).isEmpty()

            registry.getEntry(variableDecl("a", Type.Int(BitSize.BIT_32)))
            assertThat(registry.variablesToFree).hasSize(1)

            registry.getEntry(variableDecl("b", Type.Int(BitSize.BIT_64)))
            assertThat(registry.variablesToFree).hasSize(2)
        }
    }

    //TODO: move/rename to FunctionLoweringTest
    @Nested
    @DisplayName("Integration with FunctionLowering")
    inner class FunctionLoweringIntegration {

        @Test
        fun `registry is accessible via lowering variableRegistry`() {
            val fn = firstFunction("fn f(): i32 { var x: i32 = 1; return x; }")
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            assertThat(lowering.variableRegistry).isNotNull
        }

        @Test
        fun `registry tracks variable declared in function body`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 5; return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }

            val entry = lowering.variableRegistry.getEntry(varDecl)
            assertThat(entry.name).isEqualTo("x")
            assertThat(entry.lowLevelType).isEqualTo(LowLevelType.I32)
        }

        @Test
        fun `registry tracks all variables declared in multi-variable function`() {
            val file = buildFile(
                """
                fn f(): i32 {
                    var a: i32 = 1;
                    var b: i64 = 2;
                    return a;
                }
                """.trimIndent()
            )
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val decls = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>()

            val declA = decls.first { it.name.value == "a" }
            val declB = decls.first { it.name.value == "b" }
            assertThat(lowering.variableRegistry.getLowLevelType(declA)).isEqualTo(LowLevelType.I32)
            assertThat(lowering.variableRegistry.getLowLevelType(declB)).isEqualTo(LowLevelType.I64)
        }

        @Test
        fun `variablesToFree only contains entries that appear in the function`() {
            val fn = firstFunction("fn f(): i32 { var x: i32 = 1; return x; }")
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            // variablesToFree must be a subset of all registered entries
            val allEntries = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>()
                .map { lowering.variableRegistry.getEntry(it) }
            val toFree = lowering.variableRegistry.variablesToFree
            assertThat(allEntries).containsAll(toFree)
        }

        @Test
        fun `registry getLowLevelType for array variable returns JitsuArray`() {
            val file = buildFile("fn f() { var arr: i32[] = [1, 2]; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "arr" }

            assertThat(lowering.variableRegistry.getLowLevelType(varDecl))
                .isInstanceOf(JitsuArray::class.java)
        }
    }
}
