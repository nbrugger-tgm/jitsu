package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located

/**
 * Element that expresses access to another element through a name
 */
interface Access<T : Accessible<T>>{
    val target: T?
    val reference: Located<String>

    interface FunctionAccess : Access<Function>
    interface VariableAccess : Access<Variable> {
        val accessKind : AccessKind
        enum class AccessKind {
            /**
             * The parameter does not leave the scope and the callee can still use/manage the variable afterward
             */
            BORROW,

            /**
             * The parameter needs to be managed by the function and the callee needs to either give controll to the function
             * or hand the function a copy.
             */
            MOVE
        }
    }
    interface TypeAccess : Access<TypeDefinition>
}

