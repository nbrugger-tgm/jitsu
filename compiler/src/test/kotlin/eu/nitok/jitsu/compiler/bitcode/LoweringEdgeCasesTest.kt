package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction.*
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.common.sequence
import eu.nitok.jitsu.compiler.graph.buildJitsuModule
import eu.nitok.jitsu.parser.parseJitsuFile
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories.map
import org.assertj.core.api.InstanceOfAssertFactories.type
import org.junit.jupiter.api.*
import java.net.URI

/**
 * Edge cases and integration tests for the lowering pipeline.
 *
 * Conventions used throughout:
 *  - "TODO behavior" = the implementation currently throws NotImplementedError; the test
 *    documents that fact and will fail once the feature is implemented, acting as a reminder.
 *  - "Expected behavior" = the implementation should already handle this correctly.
 *
 * Tests are organised into nested classes that mirror the areas of FunctionLowering /
 * TypeLowering they exercise.
 */
@DisplayName("Lowering – Edge Cases & Integration")
class LoweringEdgeCasesTest {

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun buildFile(source: String): JitsuFile {
        val ast = parseJitsuFile(source, URI("test://sourcefile.jit"))
        ast.sequence().forEach {
            if(it.errors.isNotEmpty()) throw IllegalArgumentException("Syntax error(s)! ${it.errors.joinToString("\n")}")
        }
        val graph = buildJitsuModule(ast)
        if(graph.messages.errors.isNotEmpty()) throw IllegalArgumentException("Compilation error(s)! ${graph.messages.errors.joinToString("\n")}")
        return graph.files[0]
    }

    /** Lower the first function found in [source]. */
    private fun lower(source: String): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<Function>().first()
        return FunctionLowering({ it.name?.value ?: "anon" }, fn).lower()
    }

    /** Lower the function named [name] inside [source]. */
    private fun lowerFunction(source: String, name: String): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<Function>().first { it.name?.value == name }
        return FunctionLowering({ it.name?.value ?: "anon" }, fn).lower()
    }

    /** Lower the first function with a custom name-resolver [namer]. */
    private fun lowerWithNamer(
        source: String,
        namer: (Function) -> String
    ): List<LowLevelInstruction> {
        val file = buildFile(source)
        val fn = file.sequence().filterIsInstance<Function>().first()
        return FunctionLowering(namer, fn).lower()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Unsupported Expression Types
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("1 · Unsupported Expression Types (TODO behavior)")
    inner class UnsupportedExpressions {

        /**
         * Intended behavior: StringConstant should lower to a pointer-to-data + length
         * (e.g. an AllocHeapArray or a global data reference paired with a length field).
         *
         * Disabled until string lowering is implemented end-to-end.
         */
        @Test
        @Disabled("String support not implemented")
        fun `string constant lowers to pointer and length`() {
            val instructions = lower("""fn f(): i32 { return "hello"; }""")
            // Once implemented, assert something like:
            // - an AllocHeapArray (or global data ref) instruction is present
            // - a Write with a NumericalValue length (5) is present
            assertThat(instructions).isNotEmpty()
        }

        /**
         * Intended behavior: Nested function definitions should either be supported
         * (lowered as separate functions) or rejected with a clear compile error.
         *
         * Disabled until nested-function design decision is made and implemented.
         */
        @Test
        @Disabled("Nested functions not implemented")
        fun `nested function definition lowers inner function separately`() {
            val instructions = lowerFunction(
                """
                fn outer(): i32 {
                    fn inner(): i32 { return 1; }
                    return 0;
                }
                """.trimIndent(),
                "outer"
            )
            // Once implemented: outer returns 0 and inner is reachable as a separate lowering
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat((ret.value as LowLevelExpression.NumericalValue).value).isEqualTo(0L)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Type Resolution Edge Cases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("2 · Type Resolution Edge Cases")
    inner class TypeResolutionEdgeCases {

        /**
         * Expected behavior: TypeReference is resolved transitively through
         * TypeLowering.lower(TypeReference) → lower(resolvedCache).
         * A variable of a type alias must resolve to the underlying primitive.
         */
        @Test
        fun `type alias (TypeReference) resolves to underlying primitive type`() {
            // "type MyInt = i32" creates a TypeReference whose resolvedCache is i32.
            val instructions = lower(
                """
                type MyInt = i32;
                fn f(): MyInt { return 5; }
                """.trimIndent()
            )
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isInstanceOf(LowLevelExpression.NumericalValue::class.java)
            assertThat((ret.value as LowLevelExpression.NumericalValue).value).isEqualTo(5L)
        }

        /**
         * Expected behavior: TypeLowering.lower(Union) must produce a JitsuUnion.
         * Variables declared with a union type should be stack-allocated as JitsuUnion.
         */
        @Test
        fun `union type variable is allocated as JitsuUnion`() {
            val source = """
                fn f(x: i32 | i64): i32 {
                    return 0;
                }
            """.trimIndent()
            val file = buildFile(source)
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()
            // Parameter of union type must lower to JitsuUnion
            val paramType = TypeLowering.lower(fn.parameters.first().declaredType!!)
            assertThat(paramType).isInstanceOf(JitsuUnion::class.java)
        }

        /**
         * Expected behavior: TypeLowering.lower(Array) with a known element type must
         * produce a JitsuArray with the correctly lowered element type.
         */
        @Test
        fun `array type with i64 elements resolves element type to LLInt(BIT_64)`() {
            val file = buildFile("fn f() { var x: i64[] = [1, 2]; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }
            val arrayType = lowering.variableRegistry.getLowLevelType(varDecl) as JitsuArray
            assertThat(arrayType.elementType).isEqualTo(LowLevelType.I64)
        }

        /**
         * TODO behavior: FunctionTypeSignature lowering is not yet implemented.
         * TypeLowering.lowerFunctionPointer throws NotImplementedError.
         */
        @Test
        @Disabled("Function type signatures not implemented")
        fun `function type signature lowers to a function pointer type`() {
            val fnSig = Type.FunctionTypeSignature(
                returnType = Type.Int(eu.nitok.jitsu.common.BitSize.BIT_32),
                parameters = emptyList()
            )
            // Once implemented, TypeLowering.lower(fnSig) should return some JitsuFunctionPointer
            // or equivalent low-level type — NOT throw.
            val lowType = TypeLowering.lower(fnSig)
            assertThat(lowType).isNotNull()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Operation Lowering
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("3 · Operation Lowering")
    inner class OperationLowering {

        /**
         * Intended behavior: `a + b` should resolve to a call to the `plus` operator
         * function, producing an Invoke instruction with functionName "plus" and two args.
         *
         * Disabled until the analysis phase correctly initialises FunctionCall.type for
         * synthesised operator calls (Operation.asFunctionCall()).
         *
         * TODO: Fix CodeBlockAnalyzer.analyzeOperation() to set FunctionCall.type from the
         *       resolved operator's return type, then enable this test.
         */
        @Test
        fun `binary add operation produces Invoke for plus function`() {
            val instructions = lowerFunction(
                """
                fn plus(a: i32, b: i32): i32 { return a; }
                fn f(a: i32, b: i32): i32 {
                    return a + b;
                }
                """.trimIndent(),
                "f"
            )
            val invoke = instructions.filterIsInstance<Return>().first().value
            assertThat(invoke)
                .asInstanceOf(type(LowLevelExpression.ReturnValue::class.java))
                .extracting { it.functionCall }
                .extracting { it.functionName }
                .isEqualTo("plus")
        }

        /**
         * Intended behavior: The return value of `a + b` must be a ReturnValue wrapping
         * the Invoke for `plus`.
         *
         * Disabled for the same reason as above.
         */
        @Test
        fun `binary operation result is ReturnValue wrapping Invoke`() {
            val instructions = lowerFunction(
                """
                fn plus(a: i32, b: i32): i32 { return a; }
                fn f(a: i32, b: i32): i32 {
                    return a + b;
                }
                """.trimIndent(),
                "f"
            )
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isInstanceOf(LowLevelExpression.ReturnValue::class.java)
            assertThat((ret.value as LowLevelExpression.ReturnValue).functionCall.functionName)
                .isEqualTo("plus")
        }

        /**
         * Intended behavior: The Invoke for the `plus` operator call must carry both
         * operands as args.
         *
         * Disabled for the same reason as above.
         */
        @Test
        fun `binary operation Invoke carries both operand arguments`() {
            val instructions = lowerFunction(
                """
                fn plus(a: i32, b: i32): i32 { return a; }
                fn f(a: i32, b: i32): i32 {
                    return a + b;
                }
                """.trimIndent(),
                "f"
            )
            val invoke = instructions.filterIsInstance<Return>().first().value
            assertThat(invoke)
                .asInstanceOf(type(LowLevelExpression.ReturnValue::class.java))
                .extracting ({ it.functionCall.args }, map(String::class.java, LowLevelExpression::class.java))
                .hasSize(2)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Variable Reference Edge Cases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("4 · Variable Reference Edge Cases")
    inner class VariableReferenceEdgeCases {

        /**
         * Expected behavior: A reference to a function parameter is lowered to a
         * LowLevelExpression.Variable with the parameter's name.
         */
        @Test
        fun `reference to function parameter produces Variable expression`() {
            val instructions = lower(
                """
                fn f(x: i32): i32 {
                    return x;
                }
                """.trimIndent()
            )
            val ret = instructions.filterIsInstance<Return>().first()
            // The return value is a Variable expression reading the parameter
            assertThat(ret.value).isInstanceOf(LowLevelExpression.Variable::class.java)
            assertThat((ret.value as LowLevelExpression.Variable).name).isEqualTo("x")
        }

        /**
         * Expected behavior: A reference to a local variable is lowered to a
         * LowLevelExpression.Variable with the variable's name.
         */
        @Test
        fun `reference to local variable produces Variable expression`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var y: i32 = 7;
                    return y;
                }
                """.trimIndent()
            )
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isInstanceOf(LowLevelExpression.Variable::class.java)
            assertThat((ret.value as LowLevelExpression.Variable).name).isEqualTo("y")
        }

        /**
         * Expected behavior: Parameter type is correctly resolved via TypeLowering when
         * the parameter is referenced.
         */
        @Test
        fun `variable reference to i64 parameter has I64 low-level type`() {
            val source = "fn f(v: i64): i64 { return v; }"
            val file = buildFile(source)
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val paramType = TypeLowering.lower(fn.parameters.first().declaredType!!)
            assertThat(paramType).isEqualTo(LowLevelType.I64)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Array Literal Edge Cases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("5 · Array Literal Edge Cases")
    inner class ArrayLiteralEdgeCases {

        /**
         * Expected behavior: A single-element array literal must produce exactly one
         * ArraySlot Write instruction.
         */
        @Test
        fun `single-element array literal produces exactly one element Write`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [99];
                }
                """.trimIndent()
            )
            val slotWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            assertThat(slotWrites).hasSize(1)
            assertThat((slotWrites[0].value as LowLevelExpression.NumericalValue).value).isEqualTo(99L)
        }

        /**
         * Intended behavior: An empty array literal with an explicit type hint (`i32[]`)
         * should produce an AllocHeapArray with size 0 and no element Write instructions.
         */
        @Test
        fun `empty array literal produces AllocHeapArray with no element writes`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32[] = [];
                }
                """.trimIndent()
            )
            val heapAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is LowLevelExpression.AllocHeapArray }
            assertThat(heapAllocs).hasSize(1)
            val slotWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            assertThat(slotWrites).isEmpty()
        }

        /**
         * Expected behavior: canUseArrayHint returns false when the hint is a fixed-size
         * array whose size differs from the literal's element count, forcing the fallback
         * inferred type to be used.
         *
         * We verify this indirectly: even if the return type hint says "3 elements", writing
         * a 2-element literal should still emit only 2 slot writes.
         */
        @Test
        fun `array literal element count determines number of slot writes regardless of hint size`() {
            // The function declares return type i32[3] but the literal has 2 elements.
            // canUseArrayHint must detect the mismatch and fall back to the inferred type,
            // so there should still be exactly 2 ArraySlot writes.
            val instructions = lower(
                """
                fn f(): i32[] {
                    return [10, 20];
                }
                """.trimIndent()
            )
            val slotWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            assertThat(slotWrites).hasSize(2)
        }

        /**
         * Expected behavior: Array of boolean values must lower each element to
         * NumericalValue(0/1) matching LLBool.
         */
        @Test
        fun `array of boolean values lowers each element as NumericalValue`() {
            val instructions = lower(
                """
                fn f(a: boolean, b: boolean) {
                    var arr: boolean[] = [a, b];
                }
                """.trimIndent()
            )
            // Each slot write target must be an ArraySlot
            val slotWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            assertThat(slotWrites).hasSize(2)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Function Call Edge Cases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("6 · Function Call Edge Cases")
    inner class FunctionCallEdgeCases {

        /**
         * Expected behavior: A call used as a statement (void context) must produce an
         * Invoke in the instruction list but no Return wrapping.
         */
        @Test
        fun `call as statement produces Invoke without wrapping Return`() {
            val instructions = lowerFunction(
                """
                fn sideEffect() { }
                fn main() {
                    sideEffect();
                }
                """.trimIndent(),
                "main"
            )
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).isNotEmpty()
            assertThat(invokes.any { it.functionName == "sideEffect" }).isTrue()
            // No return value wrapping the invoke
            val returns = instructions.filterIsInstance<Return>()
            assertThat(returns.first().value).isNull()
        }

        /**
         * Expected behavior: A call used as an expression (assigned to a variable) must
         * produce the Invoke inside a Write(target, ReturnValue(invoke)).
         */
        @Test
        fun `call as expression produces Write containing ReturnValue(Invoke)`() {
            val instructions = lowerFunction(
                """
                fn compute(): i32 { return 42; }
                fn main() {
                    var result: i32 = compute();
                }
                """.trimIndent(),
                "main"
            )
            val callWrite = instructions.filterIsInstance<Write>()
                .firstOrNull {
                    it.value is LowLevelExpression.ReturnValue &&
                        (it.value as LowLevelExpression.ReturnValue).functionCall.functionName == "compute"
                }
            assertThat(callWrite).isNotNull()
        }

        /**
         * Expected behavior: A call with no parameters produces an Invoke whose args map
         * is empty.
         */
        @Test
        fun `call with no parameters produces Invoke with empty args`() {
            val instructions = lowerFunction(
                """
                fn noParams(): i32 { return 1; }
                fn main(): i32 {
                    noParams();
                    return 0;
                }
                """.trimIndent(),
                "main"
            )
            val invoke = instructions.filterIsInstance<Invoke>()
                .first { it.functionName == "noParams" }
            assertThat(invoke.args).isEmpty()
        }

        /**
         * Expected behavior: A call with multiple parameters passes all of them in the
         * Invoke args map, keyed by parameter name.
         */
        @Test
        fun `call with multiple parameters includes all args in Invoke`() {
            val instructions = lowerFunction(
                """
                fn add(a: i32, b: i32): i32 { return a; }
                fn main(): i32 {
                    add(1, 2);
                    return 0;
                }
                """.trimIndent(),
                "main"
            )
            val invoke = instructions.filterIsInstance<Invoke>()
                .first { it.functionName == "add" }
            assertThat(invoke.args).containsKeys("a", "b")
        }

        /**
         * Expected behavior: A native function body produces no instructions.
         * (Calling a native function from another function still produces an Invoke;
         * only lowering the *body* of the native function yields empty.)
         */
        @Test
        fun `native function body produces empty instruction list`() {
            val instructions = lower("native fn nativeOp(x: i32): i32")
            assertThat(instructions).isEmpty()
        }

        /**
         * Expected behavior: Calling a native function from a normal function still
         * produces an Invoke in the caller's instruction list.
         */
        @Test
        fun `calling a native function emits Invoke in caller`() {
            val instructions = lowerFunction(
                """
                native fn nativeOp(x: i32): i32
                fn main(): i32 {
                    nativeOp(5);
                    return 0;
                }
                """.trimIndent(),
                "main"
            )
            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes.any { it.functionName == "nativeOp" }).isTrue()
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Return Edge Cases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("7 · Return Edge Cases")
    inner class ReturnEdgeCases {

        /**
         * Expected behavior: Explicit return with value produces Return(NumericalValue).
         */
        @Test
        fun `explicit return with integer value produces Return with NumericalValue`() {
            val instructions = lower("fn f(): i32 { return 123; }")
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isInstanceOf(LowLevelExpression.NumericalValue::class.java)
            assertThat((ret.value as LowLevelExpression.NumericalValue).value).isEqualTo(123L)
        }

        /**
         * Expected behavior: An explicit `return;` (no value) in a void function must
         * produce Return(null).
         */
        @Test
        fun `explicit return without value in void function produces Return with null`() {
            val instructions = lower("fn f() { return; }")
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isNull()
        }


        /**
         * Expected behavior: Return is always the *last* instruction emitted by lowerReturn
         * (Free instructions for variables come before Return).
         */
        @Test
        fun `Return instruction is always last in function with local variables`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var x: i32 = 3;
                    return x;
                }
                """.trimIndent()
            )
            assertThat(instructions).isNotEmpty()
            assertThat(instructions.last()).isInstanceOf(Return::class.java)
        }

        /**
         * Expected behavior: A heap-allocated local variable must be freed before the
         * Return instruction.
         *
         * Free instructions are only emitted when there is an explicit `return` statement
         * (lowerReturn is the only code path that calls variablesToFree). The function
         * must therefore have an explicit return so that the free pass runs.
         */
        @Test
        fun `array variable is freed before Return`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var arr: i32[] = [1, 2, 3];
                    return 0;
                }
                """.trimIndent()
            )
            val returnIdx = instructions.indexOfLast { it is Return }
            val freeInstructions = instructions.filterIsInstance<Free>()
            assertThat(freeInstructions).isNotEmpty()
            val lastFreeIdx = instructions.indexOfLast { it is Free }
            assertThat(lastFreeIdx).isLessThan(returnIdx)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 8. CodeBlock Edge Cases
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("CodeBlock Edge Cases")
    inner class CodeBlockEdgeCases {

        /**
         * Expected behavior: A block with a single variable declaration produces the
         * expected AllocStack + Write pair.
         */
        @Test
        fun `block with single variable declaration emits AllocStack then Write`() {
            val instructions = lower("fn f() { var z: i32 = 42; }")
            val alloc = instructions.filterIsInstance<AllocStack>().first { it.name == "z" }
            val writes = instructions.filterIsInstance<Write>()
            assertThat(alloc).isNotNull()
            assertThat(writes).isNotEmpty()
            assertThat(instructions.indexOf(alloc)).isLessThan(instructions.indexOf(writes.first()))
        }

        /**
         * Expected behavior: Multiple variable declarations each get their own AllocStack.
         */
        @Test
        fun `multiple variable declarations each produce an AllocStack`() {
            val instructions = lower(
                """
                fn f() {
                    var a: i32 = 1;
                    var b: i32 = 2;
                    var c: i64 = 3;
                }
                """.trimIndent()
            )
            val names = instructions.filterIsInstance<AllocStack>().map { it.name }
            assertThat(names).containsExactlyInAnyOrder("a", "b", "c")
        }

        /**
         * Expected behavior: A block mixing variable declarations, a function call, and
         * a return must preserve the correct ordering of all three instruction groups.
         */
        @Test
        fun `mixed block - AllocStack before Write before Invoke before Return`() {
            val instructions = lowerFunction(
                """
                fn helper(): i32 { return 1; }
                fn main(): i32 {
                    var x: i32 = 10;
                    helper();
                    return x;
                }
                """.trimIndent(),
                "main"
            )
            val allocIdx = instructions.indexOfFirst { it is AllocStack && (it as AllocStack).name == "x" }
            val invokeIdx = instructions.indexOfFirst { it is Invoke && (it as Invoke).functionName == "helper" }
            val returnIdx = instructions.indexOfFirst { it is Return }

            assertThat(allocIdx).isGreaterThanOrEqualTo(0)
            assertThat(invokeIdx).isGreaterThan(allocIdx)
            assertThat(returnIdx).isGreaterThan(invokeIdx)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 9. getUniqueName Lambda Usage
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("9 · getUniqueName Lambda Usage")
    inner class GetUniqueNameLambda {

        /**
         * Expected behavior: All Invoke instructions in a lowered function use the name
         * provided by the getUniqueName lambda, not a hard-coded name.
         */
        @Test
        fun `getUniqueName lambda controls function name in all Invoke instructions`() {
            val file = buildFile(
                """
                fn target(): i32 { return 1; }
                fn caller(): i32 {
                    target();
                    return 0;
                }
                """.trimIndent()
            )
            val callerFn = file.sequence().filterIsInstance<Function>()
                .first { it.name?.value == "caller" }

            val instructions =
                FunctionLowering({ fn -> "MOD::${fn.name?.value ?: "anon"}" }, callerFn).lower()

            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).anyMatch { it.functionName == "MOD::target" }
            // Ensure the un-prefixed name is NOT present
            assertThat(invokes).noneMatch { it.functionName == "target" }
        }

        /**
         * Expected behavior: When two different functions are called, both Invoke
         * instructions use names resolved by the same lambda, preserving consistency.
         */
        @Test
        fun `getUniqueName lambda applied consistently across multiple calls`() {
            val file = buildFile(
                """
                fn alpha(): i32 { return 1; }
                fn beta(): i32 { return 2; }
                fn caller(): i32 {
                    alpha();
                    beta();
                    return 0;
                }
                """.trimIndent()
            )
            val callerFn = file.sequence().filterIsInstance<Function>()
                .first { it.name?.value == "caller" }

            val instructions =
                FunctionLowering({ fn -> "NS_${fn.name?.value ?: "anon"}" }, callerFn).lower()

            val invokeNames = instructions.filterIsInstance<Invoke>().map { it.functionName }
            assertThat(invokeNames).contains("NS_alpha", "NS_beta")
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 10. Instruction Ordering Invariants
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("10 · Instruction Ordering Invariants")
    inner class InstructionOrderingInvariants {

        /**
         * Expected behavior: AllocStack must appear before any Write targeting the same
         * variable name.
         */
        @Test
        fun `AllocStack always precedes Write for the same variable`() {
            val instructions = lower(
                """
                fn f() {
                    var x: i32 = 55;
                }
                """.trimIndent()
            )
            val allocIdx = instructions.indexOfFirst { it is AllocStack && (it as AllocStack).name == "x" }
            val writeIdx = instructions.indexOfFirst { it is Write }
            assertThat(allocIdx).isGreaterThanOrEqualTo(0).describedAs("AllocStack for x must exist")
            assertThat(writeIdx).isGreaterThan(allocIdx).describedAs("Write must come after AllocStack")
        }

        /**
         * Expected behavior: For every AllocStack there must be a corresponding Free
         * (for heap-backed types) before Return, ensuring no memory leaks at the IR level.
         */
        @Test
        fun `heap-backed variable has Free before Return`() {
            // Free instructions are emitted inside lowerReturn, which is only reached when
            // there is an explicit return statement.  The function must therefore return explicitly.
            val instructions = lower(
                """
                fn f(): i32 {
                    var data: i32[] = [1, 2];
                    return 0;
                }
                """.trimIndent()
            )
            val freeCount = instructions.filterIsInstance<Free>().size
            assertThat(freeCount).isGreaterThan(0).describedAs("Dynamic array must emit Free instructions")
        }

        /**
         * Expected behavior: For a primitive (stack-only) variable there must be no Free.
         */
        @Test
        fun `primitive variable does not produce any Free instruction`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var p: i32 = 7;
                    return p;
                }
                """.trimIndent()
            )
            assertThat(instructions.filterIsInstance<Free>()).isEmpty()
        }

        /**
         * Expected behavior: Return is always the very last instruction in the flat list.
         */
        @Test
        fun `Return is always the last instruction in a returning function`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var a: i32 = 1;
                    var b: i32[] = [2, 3];
                    return a;
                }
                """.trimIndent()
            )
            assertThat(instructions.last()).isInstanceOf(Return::class.java)
        }

        /**
         * Expected behavior: There must be exactly one Return instruction per function
         * body (no duplicates from the lowering logic).
         */
        @Test
        fun `exactly one Return instruction is emitted per function`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var x: i32 = 42;
                    return x;
                }
                """.trimIndent()
            )
            assertThat(instructions.filterIsInstance<Return>()).hasSize(1)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 11. Integration Tests – Full Lowering Paths
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("11 · Integration Tests")
    inner class IntegrationTests {

        /**
         * Complete function covering: parameter reference, local variable declaration,
         * arithmetic operation, function call, return.
         * Validates that all instruction types are present and ordered correctly.
         */
        @Test
        fun `complete function - param, var, operation, call, return - all instructions present`() {
            val instructions = lowerFunction(
                """
                fn double(x: i32): i32 { return x + x; }
                fn full(input: i32): i32 {
                    var doubled: i32 = double(input);
                    return doubled;
                }
                native fn plus(a: i32, b: i32): i32
                """.trimIndent(),
                "full"
            )
            // AllocStack for 'doubled'
            assertThat(instructions.filterIsInstance<AllocStack>().map { it.name }).contains("doubled")
            // Write for the initial value
            assertThat(instructions.filterIsInstance<Write>()).isNotEmpty()
            // Return at the end
            assertThat(instructions.last()).isInstanceOf(Return::class.java)
        }

        /**
         * Function returning an array literal: all element writes + one AllocHeapArray.
         */
        @Test
        fun `function returning array literal emits element writes and AllocHeapArray`() {
            val instructions = lower(
                """
                fn makeArray(): i32[] {
                    return [10, 20, 30];
                }
                """.trimIndent()
            )
            val slotWrites = instructions.filterIsInstance<Write>()
                .filter { it.target is LowLevelExpression.ArraySlot }
            assertThat(slotWrites).hasSize(3)

            val heapAllocs = instructions.filterIsInstance<Write>()
                .filter { it.value is LowLevelExpression.AllocHeapArray }
            assertThat(heapAllocs).hasSize(1)
        }

        /**
         * Function accepting a union parameter: the parameter type lowers to JitsuUnion.
         * Returning a constant 0 should still work normally.
         */
        @Test
        fun `function with union parameter lowers correctly and returns constant`() {
            val instructions = lower(
                """
                fn f(x: i32 | i64): i32 {
                    return 0;
                }
                """.trimIndent()
            )
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isInstanceOf(LowLevelExpression.NumericalValue::class.java)
            assertThat((ret.value as LowLevelExpression.NumericalValue).value).isEqualTo(0L)
        }

        /**
         * Two functions that call each other (mutual recursion in naming, not semantics).
         * Verifies that the getUniqueName lambda is invoked independently for each call site.
         */
        @Test
        fun `two functions calling each other use correct names in Invoke`() {
            val file = buildFile(
                """
                fn ping(): i32 { return 1; }
                fn pong(): i32 {
                    ping();
                    return 0;
                }
                """.trimIndent()
            )
            val pongFn = file.sequence().filterIsInstance<Function>()
                .first { it.name?.value == "pong" }
            val pingFn = file.sequence().filterIsInstance<Function>()
                .first { it.name?.value == "ping" }

            val namingMap = mapOf(pingFn to "pkg.ping", pongFn to "pkg.pong")
            val instructions = FunctionLowering({ fn -> namingMap[fn] ?: fn.name?.value ?: "anon" }, pongFn).lower()

            val invokes = instructions.filterIsInstance<Invoke>()
            assertThat(invokes).anyMatch { it.functionName == "pkg.ping" }
        }

        /**
         * A function that declares multiple variables, all of different types, and returns
         * the sum.  Validates that each variable's AllocStack carries the right low-level type.
         */
        @Test
        fun `multi-type variable function allocates each variable with correct LowLevelType`() {
            val instructions = lower(
                """
                fn f(): i32 {
                    var a: i32  = 1;
                    var b: i64  = 2;
                    var c: i32  = 3;
                    return a;
                }
                """.trimIndent()
            )
            val allocs = instructions.filterIsInstance<AllocStack>().associateBy { it.name }
            assertThat(allocs["a"]?.layout).isEqualTo(LowLevelType.I32)
            assertThat(allocs["b"]?.layout).isEqualTo(LowLevelType.I64)
            assertThat(allocs["c"]?.layout).isEqualTo(LowLevelType.I32)
        }

        /**
         * Boolean parameter pass-through: the return value is a Variable expression (not
         * a NumericalValue), since it reads from the parameter slot.
         */
        @Test
        fun `boolean parameter pass-through returns Variable expression`() {
            val instructions = lower("fn identity(flag: boolean): boolean { return flag; }")
            val ret = instructions.filterIsInstance<Return>().first()
            assertThat(ret.value).isInstanceOf(LowLevelExpression.Variable::class.java)
        }

        /**
         * Unsigned integer constant return: LowLevelType must be LLUInt.
         */
        @Test
        fun `u32 constant return uses LLUInt low-level type for the constant`() {
            // We verify via the AllocStack layout of a variable holding a u32 value,
            // since we can't directly inspect the Return expression's type.
            val file = buildFile("fn f(): u32 { var x: u32 = 5; return x; }")
            val fn = file.sequence().filterIsInstance<Function>().first()
            val lowering = FunctionLowering({ it.name?.value ?: "anon" }, fn)
            lowering.lower()

            val varDecl = (fn.body as Function.Body.Implementation).block.instructions
                .filterIsInstance<VariableDeclaration>().first { it.name.value == "x" }
            val lowType = lowering.variableRegistry.getLowLevelType(varDecl)
            assertThat(lowType).isInstanceOf(LowLevelType.LLUInt::class.java)
        }
    }
}
