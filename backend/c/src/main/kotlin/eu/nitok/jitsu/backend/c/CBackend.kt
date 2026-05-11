package eu.nitok.jitsu.backend.c

import eu.nitok.jitsu.common.indent
import eu.nitok.jitsu.compiler.bitcode.*
import eu.nitok.jitsu.compiler.transpile.Backend
import java.nio.file.Path
import kotlin.io.path.*

class CBackend : Backend {
    override fun transpile(modules: Collection<LoweredModule>, dir: Path): List<Path> {
        return modules.map { module ->
            transpile(module, dir)
        }
    }

    private fun transpile(module: LoweredModule, dir: Path): Path {
        val file = dir.resolve("${module.name}.c").createParentDirectories()
        file.deleteIfExists()
        file.createFile()
        file.bufferedWriter().use { writer ->
            val typeRegistry = TypeRegistry()
            writer.write("#include \"../../stdlib.c\"\n")

            val functions = module.functions
                .filter { it.body is LoweredBody.Implementation }
                .map { fn ->
                    transpileFunction(typeRegistry, fn)
                }

            writer.write(typeRegistry.typeDefs+"\n\n")
            writer.write(functions.joinToString("\n"){it.def}+"\n\n")
            writer.write(functions.joinToString("\n"){it.impl})
            writer.flush()
        }
        return file
    }
    private data class CFunc(val def: String, val impl: String)
    private fun transpileFunction(
        typeRegistry: TypeRegistry,
        function: LoweredFunction
    ): CFunc {
        val returnType = function.returnType?.let { typeRegistry.getUniqueName(it) }
        val body = function.body as LoweredBody.Implementation

        val def = "${returnType ?: "void"} ${function.name}(${
            function.parameters.joinToString(", ") { param ->
                typeRegistry.formatType(param.name, param.type)
            }
        })"
        return CFunc("$def;", """
$def {
${indent(1, body.instructions.joinToString("\n") { instruct -> instruct.toCCode(typeRegistry) })}
}

""".trimIndent());
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
            is LowLevelExpression.ReturnValue -> functionCall.toCCode(typeRegistry)
            is LowLevelExpression.Compare -> "${left.toCCode(typeRegistry)} == ${right.toCCode(typeRegistry)}"
            is LowLevelExpression.AllocHeap -> "malloc(sizeof(${typeRegistry.getUniqueName(layout)}))"
            is LowLevelExpression.AllocHeapArray -> "malloc(sizeof(${typeRegistry.getUniqueName(elementType)}) * ${size.toCCode(typeRegistry)})"

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
