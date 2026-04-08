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
            graph.scope.functions.forEach { (_, functions) ->
                functions.forEach { writer.write(transpileFunction(writer, nameRegistry,typeRegistry, it)) }
            }
            writer.write(typeRegistry.getTypedefs().joinToString("\n"))
            writer.flush()
        }
        return file
    }

    private fun transpileFunction(
        writer: Writer,
        functionRegistry: FunctionNameRegistry,
        typeRegistry: TypeRegistry,
        function: Function
    ) : String {
        val instructions = FunctionLowering(
            getUniqueName = functionRegistry::getUniqueName,
            isReferenceType = { typeRegistry.getTypeInfo(it).heapAlloc }
        )
        val returnType = function.returnType?.value?.let { typeRegistry.getTypeInfo(it) }
        return """
${returnType?.name ?: "void"} ${functionRegistry.getUniqueName(function)}(${function.parameters.joinToString(", ") { param ->
    val type = typeRegistry.getTypeInfo(param.type)
    "${type.name} ${param.name.value}" 
}}) {
${indent(1,instructions.lower(function).joinToString("\n") { instruct -> instruct.toCCode(writer, typeRegistry) })}
}


        """.trimIndent()
    }
    private fun LowLevelInstruction.toCCode(writer: Writer, typeRegistry: TypeRegistry): String {
        return when(this) {
            is LowLevelInstruction.Return -> {
                val value = value
                if (value == null) {
                    "return;"
                } else {
                    "return ${value.toCCode(writer, typeRegistry)};"
                }
            }
            is LowLevelInstruction.Invoke -> {
                val params = args.values.joinToString(", ") { it.toCCode(writer, typeRegistry) }
                "${name}($params)"
            }
            is LowLevelInstruction.Alloc -> {
                "void* $name = malloc(sizeof(${typeRegistry.getTypeInfo(layout).name}));"
            }
            is LowLevelInstruction.Free -> "free($name);"
            is LowLevelInstruction.StackAlloc -> "${typeRegistry.getTypeInfo(layout).name} $name;"
            is LowLevelInstruction.WriteHeap -> "*($name) = ${value.toCCode(writer, typeRegistry)};"
            is LowLevelInstruction.WriteStack -> "$name = ${value.toCCode(writer, typeRegistry)};"
        }
    }
    private fun LowLevelExpression.toCCode(writer: Writer, typeRegistry: TypeRegistry): String {
        return when(this) {
            is LowLevelExpression.NumericalValue -> value.toString()
            is LowLevelExpression.ReadHeap -> "*$name"
            is LowLevelExpression.ReadStack -> name
            is LowLevelExpression.Ref -> "&$name"
            is LowLevelExpression.ReturnValue -> functionCall.toCCode(writer, typeRegistry)
        }
    }

    override fun compile(files: Collection<Path>, dir: Path): Path {
        TODO("Not yet implemented")
    }
}