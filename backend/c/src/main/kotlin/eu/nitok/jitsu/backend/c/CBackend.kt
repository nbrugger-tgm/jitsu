package eu.nitok.jitsu.backend.c

import eu.nitok.jitsu.common.indent
import eu.nitok.jitsu.compiler.bitcode.FunctionLowering
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression
import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.JitsuFile
import eu.nitok.jitsu.compiler.transpile.Backend
import java.io.Writer
import java.nio.file.Path
import kotlin.io.path.*


class CBackend : Backend {
    override fun transpile(graphs: Collection<Pair<JitsuFile, Path>>, dir: Path): List<Path> {
        return graphs.map {
            transpile(it.first, it.second, dir);
        }
    }

    fun transpile(graph: JitsuFile, path: Path, dir: Path): Path {
        val file = dir.resolve("${path.nameWithoutExtension}.c").createParentDirectories();
        file.deleteIfExists()
        file.createFile()
        file.bufferedWriter().use { writer ->
            val nameRegistry = FunctionNameRegistry()
            val typeRegistry = TypeRegistry()
            writer.flush()
            writer.write("#include \"../../stdlib.c\"\n")
            val functions = graph.scope.functions.flatMap { (_, functions) ->
                functions.filter { it.body !is Function.Body.Native }.map {
                    transpileFunction(writer, nameRegistry, typeRegistry, it)
                }
            }.joinToString("\n")
            writer.write(typeRegistry.getTypedefs().joinToString("\n"))
            writer.write(functions)
            writer.flush()
        }
        return file
    }

    private fun transpileFunction(
        writer: Writer,
        functionRegistry: FunctionNameRegistry,
        typeRegistry: TypeRegistry,
        function: Function
    ): String {
        val lowering = FunctionLowering(
            getUniqueName = functionRegistry::getUniqueName,
            isReferenceType = { typeRegistry.getTypeInfo(it).heapAlloc },
            function
        )
        val returnType = function.returnType?.value?.let { typeRegistry.getTypeInfo(it) }
        return """
${returnType?.name ?: "void"} ${functionRegistry.getUniqueName(function)}(${
            function.parameters.joinToString(", ") { param ->
                val type = typeRegistry.getTypeInfo(param.type)
                "${type.name} ${param.name.value}"
            }
        }) {
${indent(1, lowering.lower().joinToString("\n") { instruct -> instruct.toCCode(typeRegistry) })}
}

        """.trimIndent()
    }

    private fun LowLevelInstruction.toCCode(typeRegistry: TypeRegistry): String {
        return when (this) {
            is LowLevelInstruction.Return -> {
                val value = value
                if (value == null) {
                    "return;"
                } else {
                    "return ${value.toCCode(typeRegistry)};"
                }
            }

            is LowLevelInstruction.Invoke -> {
                val params = args.values.joinToString(", ") { it.toCCode(typeRegistry) }
                "${name}($params)"
            }

            is LowLevelInstruction.Free -> "free(${name.toCCode(typeRegistry)});"
            is LowLevelInstruction.Increase -> "$variable++;"
            is LowLevelInstruction.While -> """
    while(${conditional.toCCode(typeRegistry)}) {
    ${indent(1, this.instructions.joinToString("\n") { it.toCCode(typeRegistry) })}
    }
                """.trimIndent()

            is LowLevelInstruction.Write -> "${this.name.toCCode(typeRegistry)} = ${value.toCCode(typeRegistry)};"
            is LowLevelInstruction.Conditional -> conditional(this, typeRegistry)
            is LowLevelInstruction.AllocStack -> "${typeRegistry.getTypeInfo(layout).name} $name;"
            is LowLevelInstruction.WriteAtIndex -> "${array.toCCode(typeRegistry)}[${index.toCCode(typeRegistry)}] = ${
                value.toCCode(
                    typeRegistry
                )
            };"
        }
    }

    private fun conditional(
        conditional: LowLevelInstruction.Conditional,
        typeRegistry: TypeRegistry
    ): String {
        val ifBlock = "if(${conditional.condition.toCCode(typeRegistry)}) {\n" +
                indent(1, conditional.instructions.joinToString("\n") { it.toCCode(typeRegistry) }) + "\n" +
                "}"
        val elseBlock = conditional.elseInstructions?.let {
            " else {\n" +
                    indent(1, it.joinToString("\n") { it.toCCode(typeRegistry) }) + "\n" +
                    "}"
        }
        return ifBlock + (elseBlock ?: "")
    }

    private fun LowLevelExpression.toCCode(typeRegistry: TypeRegistry): String {
        return when (this) {
            is LowLevelExpression.NumericalValue -> value.toString()
            is LowLevelExpression.Read -> "${this.struct.toCCode(typeRegistry)}.${this.name}"
            is LowLevelExpression.Ref -> "&${name.toCCode(typeRegistry)}"
            is LowLevelExpression.ReturnValue -> functionCall.toCCode(typeRegistry)
            is LowLevelExpression.Compare -> "${left.toCCode(typeRegistry)} == ${right.toCCode(typeRegistry)}"
            is LowLevelExpression.AllocHeap -> "malloc(sizeof(${typeRegistry.getTypeInfo(type).name}))"
            is LowLevelExpression.AllocHeapArray -> "malloc(sizeof(${typeRegistry.getTypeInfo(type).name}) * ${this.arraySize})"
            is LowLevelExpression.CompareGreater -> "${left.toCCode(typeRegistry)} > left.toCode(righthhhh"
            is LowLevelExpression.ArraySlot -> "${this.array.toCCode(typeRegistry)}[${this.index}]"
            is LowLevelExpression.Deref -> "*${this.name.toCCode(typeRegistry)}"
            is LowLevelExpression.Variable -> this.name
        }
    }

    override fun compile(files: Collection<Path>, dir: Path): Path {
        TODO("Not yet implemented")
    }
}