package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.merge

interface Scope {
    val parent: Scope?
    val types: Map<String, TypeDefinition>
    val functions: Map<String, List<Function>>
    val variables: Map<String, Variable>
    val imports: List<Import>

    val allFunctions: Map<String, List<Function>>
        get() = merge(
            functions,
            parent?.allFunctions ?: emptyMap(),
            *imports.mapNotNull { it.target?.exportScope?.functions }.toTypedArray()
        ) { a, b -> a + b }
    val allTypes: Map<String, TypeDefinition>
        get() = merge(
            types,
            parent?.allTypes ?: emptyMap(),
            *imports.mapNotNull { it.target?.exportScope?.types }.toTypedArray()
        ) { a, _ -> a } //shadowing
    val allVariables: Map<String, Variable>
        get() = merge(
            variables,
            parent?.allVariables ?: emptyMap(),
            *imports.mapNotNull { it.target?.exportScope?.variables }.toTypedArray()
        ) { a, _ -> a }//shadowing
}