package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.JitsuFile
import java.nio.file.Path

/**
 * Lowers an entire JitsuFile to a LoweredModule.
 * This is the main entry point for lowering - backends should use this
 * instead of dealing with graph types directly.
 */
class ModuleLowering(
    private val file: JitsuFile,
    private val sourcePath: Path
) {
    private val functionNames = mutableMapOf<Function, String>()
    private var nameCounter = 0

    fun lower(): LoweredModule {
        // First pass: assign unique names to all functions
        file.scope.functions.values.flatten().forEach { fn ->
            getUniqueName(fn)
        }

        // Second pass: lower all functions
        val loweredFunctions = file.scope.functions.values.flatten().map { fn ->
            lowerFunction(fn)
        }

        return LoweredModule(
            sourcePath = sourcePath.toString(),
            functions = loweredFunctions
        )
    }

    private fun getUniqueName(function: Function): String {
        return functionNames.computeIfAbsent(function) {
            val baseName = function.name?.value ?: "anon"
            if (functionNames.values.contains(baseName)) {
                "${baseName}_${nameCounter++}"
            } else {
                baseName
            }
        }
    }

    private fun lowerFunction(function: Function): LoweredFunction {
        val name = getUniqueName(function)

        val parameters = function.parameters.map { param ->
            LoweredParameter(
                name = param.name.value,
                type = TypeLowering.lower(param.declaredType!!)
            )
        }

        val returnType = function.returnType?.value?.let { TypeLowering.lower(it) }

        val body = when (val b = function.body) {
            is Function.Body.Implementation -> {
                val lowering = FunctionLowering(::getUniqueName, function)
                LoweredBody.Implementation(lowering.lower())
            }
            is Function.Body.Native -> LoweredBody.Native(b.nativeTarget)
            is Function.Body.Missing -> LoweredBody.Implementation(emptyList())
        }

        return LoweredFunction(
            name = name,
            parameters = parameters,
            returnType = returnType,
            body = body
        )
    }
}
