package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.analysis.*
import eu.nitok.jitsu.compiler.graph.buildJitsuModule
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.compiler.graph.elements.JitsuFile
import eu.nitok.jitsu.compiler.graph.elements.VariableDeclaration
import eu.nitok.jitsu.compiler.graph.elements.types.Boolean
import eu.nitok.jitsu.compiler.graph.elements.types.Int
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.parser.parseJitsuFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.net.URI

@DisplayName("VariableRegistry")
class VariableRegistryTest {

    private val dummyLocation = Location(URI("memory://test.jit"),1,1,1,1)
    private fun <T> loc(value: T) = Located(value, dummyLocation)

    private fun buildFile(source: String): JitsuFile {
        val ast = parseJitsuFile(source, URI("test://sourcefile.jit"))
        ast.sequence().forEach {
            if(it.errors.isNotEmpty()) throw IllegalArgumentException("Syntax error(s)! ${it.errors.joinToString("\n")}")
        }
        val graph = buildJitsuModule(ast)
        if(graph.messages.errors.isNotEmpty()) throw IllegalArgumentException("Compilation error(s)! ${graph.messages.errors.joinToString("\n")}")
        return graph.module.files[0]
    }

    private fun firstFunction(source: String): FunctionElement =
        buildFile(source).sequence().filterIsInstance<FunctionElement>().first()

    private fun functionNamed(source: String, name: String): FunctionElement =
        buildFile(source).sequence().filterIsInstance<FunctionElement>().first { it.name?.value == name }

    /** Attach a [FunctionSummaryElement] to the function via reflection (field is `internal set`). */
    private fun FunctionElement.withSummary(summary: FunctionSummaryElement): FunctionElement {
        val field = FunctionElement::class.java.getDeclaredField("summary")
        field.isAccessible = true
        field.set(this, summary)
        return this
    }

    private fun minimalSummary(variableSummary: Map<String, VariableSummaryElement> = emptyMap()) =
        FunctionSummaryElement(
            returnSummary = ReturnSummaryElement(deterministic = ReasonedBoolean.True("test")),
            noSideEffects = ReasonedBoolean.True("test"),
            variableSummary = variableSummary
        )

    private fun ownedSummary(vararg names: String) =
        minimalSummary(names.associateWith { variableSummary(OwnershipState.OWNS) })

    private fun borrowedSummary(vararg names: String) =
        minimalSummary(names.associateWith { variableSummary(OwnershipState.BORROWS) })

    private fun variableSummary(ownershipState: OwnershipState) =
        VariableSummaryElement(
            ownershipState = ownershipState,
            compileTimeValueElement = AbstractValueElement.Unknown
        )

    private fun variableDecl(name: String, type: TypeElement): VariableDeclaration =
        VariableDeclaration(
            reassignable = true,
            name = loc(name),
            declaredTypeElement = type,
            initialValueElement = null
        )

    private fun parameter(name: String, type: TypeElement): FunctionElement.Parameter =
        FunctionElement.Parameter(
            name = loc(name),
            declaredTypeElement = type,
            initialValueElement = null
        )

    @Nested
    @DisplayName("variablesToFree")
    inner class VariablesToFree {

        @Test
        fun `is empty when no variables have been registered`() {
            val fn = firstFunction("fn f() { }")
            val registry = VariableRegistry(fn, mutableMapOf(), setOf())

            assertThat(registry.variablesToFree).isEmpty()
        }

        @Test
        fun `is empty when all registered variables have requiresFree=false`() {
            val fn = firstFunction("fn f() { }")
            fn.withSummary(borrowedSummary("a", "b"))
            val registry = VariableRegistry(fn, mutableMapOf(), setOf())
            registry.getEntry(variableDecl("a", Int(BitSize.BIT_32)))
            registry.getEntry(variableDecl("b", Int(BitSize.BIT_64)))

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
            val registry = VariableRegistry(fn, mutableMapOf(), setOf())
            val ownedDecl = variableDecl("owned", Int(BitSize.BIT_32))
            val borrowedDecl = variableDecl("borrowed", Int(BitSize.BIT_32))
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
            val registry = VariableRegistry(fn, mutableMapOf(), setOf())
            registry.getEntry(variableDecl("x", Int(BitSize.BIT_32)))
            registry.getEntry(variableDecl("y", Int(BitSize.BIT_64)))
            registry.getEntry(variableDecl("z", Boolean))

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
            val registry = VariableRegistry(fn, mutableMapOf(), setOf())
            registry.getEntry(variableDecl("moved", Int(BitSize.BIT_32)))
            registry.getEntry(variableDecl("owned", Int(BitSize.BIT_32)))

            assertThat(registry.variablesToFree.map { it.name }).containsExactly("owned")
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {


        @Test
        fun `variable with Undefined type throws in TypeLowering`() {
            val fn = firstFunction("fn f() { }")
            val registry = VariableRegistry(fn, mutableMapOf(), setOf())
            // declaredType=null and implicitType=null → type is Type.Undefined
            val decl = VariableDeclaration(
                reassignable = false,
                name = loc("undeclared"),
                declaredTypeElement = null,
                initialValueElement = null
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
            val registry = VariableRegistry(fn, mutableMapOf(), setOf())

            // Initially empty
            assertThat(registry.variablesToFree).isEmpty()

            registry.getEntry(variableDecl("a", Int(BitSize.BIT_32)))
            assertThat(registry.variablesToFree).hasSize(1)

            registry.getEntry(variableDecl("b", Int(BitSize.BIT_64)))
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
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn, mutableMapOf(), setOf())
            lowering.lower()

            assertThat(lowering.variableRegistry).isNotNull
        }

        @Test
        fun `registry tracks variable declared in function body`() {
            val file = buildFile("fn f(): i32 { var x: i32 = 5; return x; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn, mutableMapOf(), setOf())
            lowering.lower()

            val varDecl = (fn.body as FunctionElement.BodyElement.Implementation).instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }

            val entry = lowering.variableRegistry.getEntry(varDecl)
            assertThat(entry.name).isEqualTo("x")
            assertThat(entry.lowLevelType).isEqualTo(I32)
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
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn, mutableMapOf(), setOf())
            lowering.lower()

            val decls = (fn.body as FunctionElement.BodyElement.Implementation).instructions
                .filterIsInstance<VariableDeclaration>()

            val declA = decls.first { it.name.value == "a" }
            val declB = decls.first { it.name.value == "b" }
            assertThat(lowering.variableRegistry.getLowLevelType(declA)).isEqualTo(I32)
            assertThat(lowering.variableRegistry.getLowLevelType(declB)).isEqualTo(I64)
        }

        @Test
        fun `variablesToFree only contains entries that appear in the function`() {
            val fn = firstFunction("fn f(): i32 { var x: i32 = 1; return x; }")
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn, mutableMapOf(), setOf())
            lowering.lower()

            // variablesToFree must be a subset of all registered entries
            val allEntries = (fn.body as FunctionElement.BodyElement.Implementation).instructions
                .filterIsInstance<VariableDeclaration>()
                .map { lowering.variableRegistry.getEntry(it) }
            val toFree = lowering.variableRegistry.variablesToFree
            assertThat(allEntries).containsAll(toFree)
        }

        @Test
        fun `registry getLowLevelType for array variable returns JitsuArray`() {
            val file = buildFile("fn f() { var arr: i32[] = [1, 2]; }")
            val fn = file.sequence().filterIsInstance<FunctionElement>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn, mutableMapOf(), setOf())
            lowering.lower()

            val varDecl = (fn.body as FunctionElement.BodyElement.Implementation).instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "arr" }

            assertThat(lowering.variableRegistry.getLowLevelType(varDecl))
                .isInstanceOf(JitsuArray::class.java)
        }
    }
}
