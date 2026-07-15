package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.api.Function
import eu.nitok.jitsu.compiler.graph.api.JitsuModule
import java.util.*


/**
 * Lowers an entire JitsuFile to a LoweredModule.
 * This is the main entry point for lowering - backends should use this
 * instead of dealing with graph types directly.
 */
class ModuleLowering(private val module: JitsuModule) {
    private val functionNames = mutableMapOf<Function, String>()
    private var nameCounter = 0

    fun lower(): LoweredModule {
        val allRequiredModules = Collections.newSetFromMap(IdentityHashMap<JitsuModule, Boolean>())
        fun collectModules(mod: JitsuModule) {
            if (allRequiredModules.add(mod)) {
                mod.files.asSequence()
                    .flatMap { it.imports }
                    .mapNotNull { it.target }
                    .forEach { collectModules(it) }
            }
        }
        module.allModules.forEach { collectModules(it) }
        val allFunctions = allRequiredModules.asSequence()
            .flatMap { it.files }
            .flatMap { it.functions}

        val loweredFunctions = allFunctions
            .map { fn -> lowerFunction(fn) }
            .toList()

        return LoweredModule(name = module.fullyQualifiedName,functions = loweredFunctions)
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
            is Function.Body.Native -> LoweredBody.Native("_jitsu_${function.name}_${function.parameters.joinToString("__") { it.type.toString().replace(Regex("\\W"), "_") }}")
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
