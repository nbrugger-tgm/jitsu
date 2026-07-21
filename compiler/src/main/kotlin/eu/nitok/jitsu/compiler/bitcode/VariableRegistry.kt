package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.analysis.OwnershipState
import eu.nitok.jitsu.compiler.graph.api.Function
import eu.nitok.jitsu.compiler.graph.api.Variable
import eu.nitok.jitsu.compiler.graph.api.VariableDeclaration

/**
 * Registry for variables during function lowering.
 * Maps variable declarations to their low-level types and manages
 * ownership information for memory cleanup.
 */
class VariableRegistry(val function: Function, variableMappings: MutableMap<Variable, String>, val reservedNames: Set<String>) {
    private val entries = mutableMapOf<Variable, Entry>()

    init {
        variableMappings.forEach { (variable, name) ->
            getEntry(variable, name)
        }
    }
    /**
     * Entry for a registered variable.
     */
    data class Entry(
        val name: String,
        val lowLevelType: LowLevelType,
        val requiresFree: Boolean
    ) {
        /**
         * Get the variable expression for this entry.
         */
        fun asVariable(): LowLevelExpression.Variable = LowLevelExpression.Variable(name)
    }
    /**
     * Get or create an entry for a variable declaration.
     */
    fun getEntry(variable: Variable, forcedName: String? = null): Entry {
        return entries.getOrPut(variable) {
            val lowLevelType = TypeLowering.lower(variable.type)
            val varSummary = function.summary?.variableSummary
                ?: throw IllegalStateException("Function $function has no summary - lowering not possible")
            val declarationSummary = varSummary[variable.name.value]
                ?: throw IllegalStateException("Variable $variable has not variable state analysis in function $function! - lowering not possible")
            val requiresFree = declarationSummary.ownershipState == OwnershipState.OWNS
            Entry(
                name = forcedName?: nonConflictingName(variable),
                lowLevelType = lowLevelType,
                requiresFree = requiresFree
            )
        }
    }

    private fun nonConflictingName(variable: Variable): String {
        var name = variable.name.value
        while(name in reservedNames || entries.values.any { it.name == name }) {
            name = "var_$name"
        }
        return name
    }

    /**
     * Get the low-level type for a variable declaration.
     */
    fun getLowLevelType(variable: VariableDeclaration): LowLevelType {
        return getEntry(variable).lowLevelType
    }

    /**
     * All variables that require freeing when going out of scope.
     */
    val variablesToFree: Collection<Entry>
        get() = entries.values.filter { it.requiresFree }

}
