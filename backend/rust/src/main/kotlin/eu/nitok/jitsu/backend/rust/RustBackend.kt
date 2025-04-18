package eu.nitok.jitsu.backend.rust.eu.nitok.jitsu.backend.rust

import eu.nitok.jitsu.compiler.analysis.isMain
import eu.nitok.jitsu.compiler.graph.*
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.transpile.Backend
import java.io.BufferedWriter
import java.nio.file.Path
import kotlin.io.path.*

class RustBackend : Backend {
    override fun transpile(graphs: Collection<Pair<JitsuFile, Path>>, dir: Path): List<Path> {
        return graphs.map {
            transpile(it.first, it.second, dir);
        }
    }

    fun transpile(graph: JitsuFile, path: Path, dir: Path): Path {
        val file = dir.resolve("${path.nameWithoutExtension}.rs").createParentDirectories();
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
    if (name?.value == "main") writer.appendLine("use std::process::ExitCode;")
    writer.append("fn ${this.name?.value}(")
    writer.append(parameters.joinToString(", ") { it.transpile() })
    writer.append(") ")
    this.returnType?.let {
        if (name?.value == "main") writer.append("-> ExitCode ")
        else writer.append("-> ${it.transpile()} ")
    }
    writer.append("{\n")
    writer.append(this.body.instructions.joinToString("\n") { "  ${it.transpile()}" })
    writer.append("\n}\n\n")
}

private fun Instruction.transpile(): String {
    return when (this) {
        is Instruction.Return -> {
            if (this.function.isMain && this.value != null) {
                return "ExitCode::from(${this.value!!.transpile()})"
            }
            this.value?.transpile() ?: "return"
        }

        is Instruction.VariableDeclaration -> "let ${this.variable.name.value}${
            this.variable.declaredType?.transpile()?.let { ": $it" } ?: ""
        } = ${this.value.transpile()};"

        is Instruction.FunctionCall -> "${this.transpile()};"
        is Function -> ""
    }
}

private fun Instruction.FunctionCall.transpile(): String = "${this.target?.name?.value ?: "undefined"}(${
    this.target?.parameters?.map {
        parameters[it.name.value]?.transpile()
    }?.joinToString(", ")
})"

private fun Expression.transpile(): String {
    return when (this) {
        is Constant.BooleanConstant -> when (this.value) {
            true -> "true"
            false -> "false"
        }

        is Constant.IntConstant -> this.value.toString()
        is Constant.StringConstant -> "\"${this.value}\""
        is Constant.UIntConstant -> this.value.toString()
        is Expression.Operation -> "${this.left.transpile()} ${this.operator.rune} ${this.right.transpile()}"
        Expression.Undefined -> throw IllegalStateException("Undefined expression")
        is Expression.VariableReference -> this.target?.name?.value?: throw IllegalStateException("Undefined variable")
        is Instruction.FunctionCall -> this.transpile()
    }
}

private fun Type.transpile(): String {
    return when (this) {
        is Type.Array -> TODO()
        Type.Boolean -> TODO()
        is Type.Float -> TODO()
        is Type.FunctionTypeSignature -> TODO()
        is Type.Int -> "i${this.size.bits}"
        Type.Null -> TODO()
        is Type.TypeReference -> this.reference.value
        is Type.UInt -> "u${this.size.bits}"
        Type.Undefined -> TODO()
        is Type.Value -> TODO()
        is Type.StructuralInterface -> TODO()
        is Type.Union -> TODO()
    };
}


private fun Function.Parameter.transpile(): String {
    return "${this.name.value}: ${this.type}"
}
