package eu.nitok.jitsu.compiler.graph

import kotlinx.serialization.Serializable

@Serializable
sealed interface Accessible<T : Access> {
    val accessToSelf: MutableList<T>
}

@Serializable
sealed interface Accessor {
    val accessFromSelf: MutableList<Access>
}

@Serializable
sealed interface Access {

    val target: Accessible<*>
    val accessor: Accessor

    sealed interface FunctionAccess : Access {
        override val target: Function;

        data class Invocation(override val target: Function, override val accessor: Accessor) : FunctionAccess {
        }
    }

    sealed interface VariableAccess : Access {
        data class Read(override val target: Accessible<VariableAccess>, override val accessor: Accessor) :
            VariableAccess {
        }

        data class Write(override val target: Accessible<VariableAccess>, override val accessor: Accessor) :
            VariableAccess {
        }
    }
}