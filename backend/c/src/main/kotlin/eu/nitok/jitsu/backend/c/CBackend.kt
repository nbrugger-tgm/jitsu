package eu.nitok.jitsu.backend.c

import eu.nitok.jitsu.common.indent
import eu.nitok.jitsu.compiler.bitcode.*
import eu.nitok.jitsu.compiler.transpile.Backend
import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.relativeTo

class CBackend : Backend {
    override fun transpile(modules: Collection<LoweredModule>, dir: Path): List<Path> {
        return modules.map { module ->
            transpile(module, dir)
        }
    }

    private fun transpile(module: LoweredModule, dir: Path): Path {
        val code = dir.resolve("${module.name}.c").createParentDirectories()
        val headers = dir.resolve("${module.name}.private.h").createParentDirectories()
        val publicHeaders = dir.resolve("headers").resolve("${module.name}.public.h").createParentDirectories()
        try {
            code.createFile()
        } catch (_: FileAlreadyExistsException) {
        }
        try {
            headers.createFile()
        } catch (_: FileAlreadyExistsException) {
        }
        try {
            publicHeaders.createFile()
        } catch (_: FileAlreadyExistsException) {
        }

        val typeRegistry = TypeRegistry()
        val functions = module.functions.map { fn ->
            transpileFunction(typeRegistry, fn)
        }

        headers.bufferedWriter().use { writer ->
            writer.write(typeRegistry.typeDefs)
            writer.newLine()
            writer.newLine()
            functions.forEach {
                writer.write(it.def)
                writer.newLine()
            }
        }

        //TODO filter exported members
        publicHeaders.bufferedWriter().use { writer ->
            writer.write(typeRegistry.typeDefs)
            writer.newLine()
            writer.newLine()
            functions.forEach {
                writer.write(it.def)
                writer.newLine()
            }
        }
        val implementedFunctions = functions.filter { it.impl != null }

        code.bufferedWriter().use { writer ->
            writer.write("#include \"${headers.relativeTo(code.parent)}\"")
            writer.newLine()
            writer.write("#include <cstdlib>")
            writer.newLine()
            writer.newLine()
            writer.write(implementedFunctions.joinToString("\n") { it.impl!! })
            writer.flush()
        }

        return code
    }

    private data class CFunc(val def: String, val impl: String?)

    private fun transpileFunction(
        typeRegistry: TypeRegistry,
        function: LoweredFunction
    ): CFunc {
        val returnType = function.returnType
        val def = "${returnType?.let { typeRegistry.formatType(function.name, it) } ?: "void ${function.name}"} (${
            function.parameters.joinToString(", ") { param ->
                typeRegistry.formatType(param.name, param.type)
            }
        })"
        if (function.body is LoweredBody.Native) {
            return CFunc("$def;", null)
        }
        val body = function.body as LoweredBody.Implementation

        val impl = """
$def {
${indent(1, body.instructions.joinToString("\n") { instruct -> instruct.toCCode(typeRegistry) })}
}
""".trimIndent()
        return CFunc("$def;", impl);
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
                "${functionName}($params);"
            }

            is LowLevelInstruction.Free -> "free(${target.toCCode(typeRegistry)});"
            is LowLevelInstruction.Increase -> "${variable.toCCode(typeRegistry)}++;"
            is LowLevelInstruction.While -> """
while(${condition.toCCode(typeRegistry)}) {
${indent(1, body.joinToString("\n") { it.toCCode(typeRegistry) })}
}
                """.trimIndent()

            is LowLevelInstruction.Write -> "${target.toCCode(typeRegistry)} = ${value.toCCode(typeRegistry)};"
            is LowLevelInstruction.Conditional -> conditional(this, typeRegistry)
            is LowLevelInstruction.AllocStack -> "${typeRegistry.formatType(name, layout)};"
        }
    }

    private fun conditional(
        conditional: LowLevelInstruction.Conditional,
        typeRegistry: TypeRegistry
    ): String {
        val ifBlock = "if(${conditional.condition.toCCode(typeRegistry)}) {\n" +
                indent(1, conditional.thenInstructions.joinToString("\n") { it.toCCode(typeRegistry) }) + "\n" +
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
            is LowLevelExpression.ReturnValue -> functionCall.run {
                val params = args.values.joinToString(", ") { it.toCCode(typeRegistry) }
                "${functionName}($params)"
            }

            is LowLevelExpression.Compare -> "${left.toCCode(typeRegistry)} == ${right.toCCode(typeRegistry)}"
            is LowLevelExpression.AllocHeap -> "(${typeRegistry.formatType("", layout)} *) malloc(sizeof(${typeRegistry.formatType("",layout)}))"
            is LowLevelExpression.AllocHeapArray -> "(${typeRegistry.formatType("", elementType)} *) malloc(sizeof(${typeRegistry.formatType("",elementType)}) * ${
                size.toCCode(typeRegistry)
            })"

            is LowLevelExpression.CompareGreater -> "${left.toCCode(typeRegistry)} > ${right.toCCode(typeRegistry)}"
            is LowLevelExpression.ArraySlot -> "${this.array.toCCode(typeRegistry)}[${this.index.toCCode(typeRegistry)}]"
            is LowLevelExpression.Deref -> "*${this.name.toCCode(typeRegistry)}"
            is LowLevelExpression.Variable -> this.name
        }
    }

    override fun compile(files: Collection<Path>, dir: Path): Path {
        TODO("Not yet implemented")
    }
}
