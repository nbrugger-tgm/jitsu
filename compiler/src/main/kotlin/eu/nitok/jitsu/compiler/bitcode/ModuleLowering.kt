package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.Function
import eu.nitok.jitsu.compiler.graph.api.JitsuModule
import eu.nitok.jitsu.compiler.graph.api.Variable
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
            .flatMap { it.functions }
            .toList()

        val reservedNames = allFunctions.map { getUniqueName(it) }.toSet()

        val loweredFunctions = allFunctions
            .map { fn -> lowerFunction(fn, reservedNames) }

        return LoweredModule(name = module.fullyQualifiedName, functions = loweredFunctions)
    }

    private fun getUniqueName(function: Function): String {
        return functionNames.computeIfAbsent(function) {
            if (function.body is Function.Body.Native) {
                return@computeIfAbsent function.getNativeFunctionName()
            }
            val baseName = function.name?.value ?: "anon"
            if (functionNames.values.contains(baseName)) {
                "${baseName}_${nameCounter++}"
            } else {
                baseName
            }
        }
    }

    val invalidFunctionChars = Regex("\\W")
    private fun Function.getNativeFunctionName(): String {
        val nativeAttribute = attributes.find {
            it.target?.fullyQualifiedName == "jitsu.ffm.Native"
        }
        return when (val nameExpression = nativeAttribute?.getPropertyValue("name")) {
            null -> {
                "jitsu_${fullyQualifiedName?.replace(invalidFunctionChars, "_")?.lowercase()}_${
                    parameters.joinToString("__") {
                        it.type.toString().replace(
                            invalidFunctionChars, "_"
                        ).lowercase()
                    }
                }"
            }

            is Expression.Constant.StringConstant -> {
                nameExpression.value
            }

            else -> error("The 'name' property of the Native attribute must be a string literal ${nameExpression.location}")

        }
    }

    private fun lowerFunction(function: Function, reservedNames: Set<String>): LoweredFunction {
        val name = getUniqueName(function)
        val variableMappings = mutableMapOf<Variable, String>()

        val parameters = function.parameters.map { param ->
            var name = param.name.value
            while(reservedNames.contains(name) || variableMappings.values.contains(name)) {
                name = "p_${name}"
            }
            variableMappings[param] = name
            LoweredParameter(
                name = name,
                type = TypeLowering.lower(param.declaredType!!)
            )
        }

        val returnType = function.returnType?.value?.let { TypeLowering.lower(it) }

        val body = when (function.body) {
            is Function.Body.Implementation -> {
                val lowering = FunctionLowering(::getUniqueName, function, variableMappings, reservedNames)
                LoweredBody.Implementation(lowering.lower())
            }

            is Function.Body.Native -> LoweredBody.Native
            is Function.Body.Missing -> error("Function bodies should not be missing when transpiling")
        }

        return LoweredFunction(
            name = name,
            parameters = parameters,
            returnType = returnType,
            body = body
        )
    }
}
