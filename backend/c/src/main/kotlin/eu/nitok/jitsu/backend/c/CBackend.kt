package eu.nitok.jitsu.backend.rust.eu.nitok.jitsu.backend.rust

import eu.nitok.jitsu.compiler.analysis.isMain
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.transpile.Backend
import java.io.BufferedWriter
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
            graph.scope.functions.values.flatten().forEach {
                it.transpile(writer)
            }
        }
        return file
    }

    override fun compile(files: Collection<Path>, dir: Path): Path {
        TODO("Not yet implemented")
    }
}

private fun Function.transpile(writer: BufferedWriter) {
    val body1 = body
    if(body1 is Function.Body.Implementation) writer.append(this.returnType.transpile()).append(" ")
        .append(this.name?.value).append(" ")
        .append(this.parameters.joinToString(",", "(",")") { it.transpile() })
        .append(" {\n")
        .append(body1.block.instructions.joinToString("\n") { it.transpile() })
        .append("\n}\n\n")
}

private fun Instruction.transpile(): String {
    return when (this) {
        is Function -> TODO()
        is Instruction.FunctionCall -> "${this.reference.value}(${this.parameters.values.joinToString { it.transpile() }});"
        is Instruction.Return -> "return ${this.value?.transpile() ?: ""};"
        is VariableDeclaration -> "${this.type.transpile()} ${this.name.value} ${if(this.initialValue != null) " = ${this.initialValue?.transpile()}" else ""};"
    }
}

private fun Expression.transpile(): String {
    return when (this) {
        is Constant.BooleanConstant -> when (this.value) {
            true -> "true"
            false -> "false"
        }
        is Constant.IntConstant -> this.value.toString()
        is Constant.StringConstant -> "\"${this.value}\""
        is Constant.UIntConstant -> this.value.toString()
        is Expression.Operation -> "${this.left.transpile()} ${this.operator.value.rune} ${this.right.transpile()}"
        is Expression.Undefined -> throw IllegalStateException("Undefined expression")
        is Expression.VariableReference -> this.target?.name?.value?: throw IllegalStateException("Undefined variable")
        is Instruction.FunctionCall -> (this as Instruction).transpile()
    }
}

private fun Type?.transpile(): String {
    return when (this) {
        is Type.Array -> this.type.transpile()+"[]"
        Type.Boolean -> "bool"
        is Type.Float -> "f${this.size.bits}"
        is Type.FunctionTypeSignature -> TODO()//(f*()) or smth like that
        is Type.Int -> "i${this.size.bits}"
        Type.Null -> "null"
        is Type.TypeReference -> this.reference.value
        is TypeDefinition.DirectTypeDefinition.Enum -> TODO()
        is Type.UInt -> "u${this.size.bits}"
        Type.Undefined -> "ERROR_TYPE"
        is Type.Value -> TODO()
        is Type.StructuralInterface -> TODO()
        is Type.Union -> "void*"
        null -> "void"
    };
}


private fun Function.Parameter.transpile(): String {
    return "${this.type.transpile()} ${this.name.value}"
}
