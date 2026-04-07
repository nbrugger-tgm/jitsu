package eu.nitok.jitsu.backend.rust.eu.nitok.jitsu.backend.rust

import eu.nitok.jitsu.compiler.bitcode.FunctionLowering
import eu.nitok.jitsu.compiler.bitcode.FunctionNameRegistry
import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression
import eu.nitok.jitsu.compiler.bitcode.LowLevelInstruction
import eu.nitok.jitsu.compiler.bitcode.MemoryFragment
import eu.nitok.jitsu.compiler.bitcode.MemoryLayout
import eu.nitok.jitsu.compiler.bitcode.layout
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
    private inner class StructRegistry {
        private val structNames = mutableMapOf<MemoryLayout, String>()
        private var structIdx = 0
        fun getStruct(layout: MemoryLayout, writer: Writer): String {
            if(layout in structNames) {
                return structNames[layout]!!
            }
            val name = "jitsu_struct_$structIdx"
            structIdx++
            structNames[layout] = name

            writer.write("typedef struct $name {\n")
            layout.segements.forEachIndexed { idx, segment ->
                val type = mapType(writer, segment.layout, this)
                val fieldName = segment.name ?: "field_${idx}"
                writer.write("    $type $fieldName;\n")
            }
            writer.write("} $name;\n\n")
            return name
        }
    }
    fun transpile(graph: JitsuFile, path: Path, dir: Path): Path {
        val file = dir.resolve("${path.nameWithoutExtension}.c").createParentDirectories();
        file.deleteIfExists()
        file.createFile()
        file.bufferedWriter().use { writer ->
            val nameRegistry = FunctionNameRegistry()
            val structRegistry = StructRegistry()
            graph.scope.functions.forEach { (_, functions) ->
                functions.forEach { writer.write(transpileFunction(writer, nameRegistry,structRegistry, it)) }
            }
        }
        return file
    }

    private fun transpileFunction(
        writer: Writer,
        functionName: FunctionNameRegistry,
        structRegistry: StructRegistry,
        function: Function
    ) : String {
        val instructions = FunctionLowering(functionName, function)
        val returnType = function.returnType?.value?.layout?.let { mapType(writer, it, structRegistry) }
        return """
            ${returnType ?: "void"} ${functionName.getUniqueName(function)}(${function.parameters.joinToString(", ") { param ->
            val type = mapType(writer, param.type.layout, structRegistry)
            "$type ${param.name.value}"
        }}) {
        ${instructions.lower().joinToString("\n    ") { instruct -> instruct.toCCode(writer, structRegistry) }}
        }
        """.trimIndent()
    }
    private fun LowLevelInstruction.toCCode(writer: Writer, structRegistry: StructRegistry): String {
        return when(this) {
            is LowLevelInstruction.Return -> {
                val value = value
                if (value == null) {
                    "return;"
                } else {
                    "return ${value.toCCode(writer, structRegistry)};"
                }
            }
            is LowLevelInstruction.Invoke -> {
                val params = args.joinToString(", ") { it.value.toCCode(writer, structRegistry) }
                "${name}($params)"
            }
            is LowLevelInstruction.Alloc -> {
                "void* $name = malloc(sizeof(${mapType(writer, layout, structRegistry)}));"
            }
            is LowLevelInstruction.Free -> "free($name);"
            is LowLevelInstruction.StackAlloc -> "${mapType(writer, layout, structRegistry)} $name;"
            is LowLevelInstruction.WriteHeap -> "*($name) = ${value.toCCode(writer, structRegistry)};"
            is LowLevelInstruction.WriteStack -> "$name = ${value.toCCode(writer, structRegistry)};"
        }
    }
    private fun LowLevelExpression.toCCode(writer: Writer, structRegistry: StructRegistry): String {
        return when(this) {
            is LowLevelExpression.Alloc -> {
                "malloc(sizeof(${mapType(writer, layout, structRegistry)}));"
            }
            is LowLevelExpression.NumericalValue -> value.toString()
            is LowLevelExpression.ReadHeap -> "*$name"
            is LowLevelExpression.ReadStack -> name
            is LowLevelExpression.Ref -> "&$name"
            is LowLevelExpression.ReturnValue -> functionCall.toCCode(writer, structRegistry)
        }
    }

    private fun mapType(writer: Writer, layout: MemoryFragment, registry: StructRegistry): String {
        return when(layout) {
            is MemoryFragment.Reference -> "void*"
            is MemoryFragment.Value -> "_BitInt(${layout.size})"
            is MemoryLayout -> registry.getStruct(layout, writer)
        }
    }

    override fun compile(files: Collection<Path>, dir: Path): Path {
        TODO("Not yet implemented")
    }
}