package eu.nitok.jitsu.compiler.graph

import eu.nitok.jitsu.compiler.ast.CompilerMessages
import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.model.BitSize
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Type : Element {
    @Transient
    override val children: List<Element> = listOf()

    @Serializable
    data class Int(val bits: BitSize) : Type() {
        override fun toString(): String {
            return "i${bits.bits}"
        }
    }

    @Serializable
    data class UInt(val bits: BitSize) : Type() {
        override fun toString(): String {
            return "u${bits.bits}"
        }
    }

    @Serializable
    data class Float(val bits: BitSize = BitSize.BIT_32) : Type() {
        override fun toString(): String {
            return "f${bits.bits}"
        }
    }

    @Serializable
    data class Value(val value: Constant<@Contextual Any>) : Type() {
        val valueType: Type = value.type
        @Transient
        override val children: List<Element> = listOf(value)
        override fun toString(): String {
            return value.value.toString()
        }
    }

    @Serializable
    data object Null : Type() {
        override fun toString(): String {
            return "null"
        }
    }

    /**
     * This type is not usable in the language. It is the type used at compile time when a type is not resolvable
     */
    @Serializable
    data object Undefined : Type()

    @Serializable
    data class Array(
        val type: Type,
        val size: Expression?,
        val dimensions: kotlin.Int = 1
    ) : Type() {
        @Transient
        override val children: List<Element> = listOfNotNull(type, size)

        override fun toString(): String {
            return "$type[${size?.toString() ?: ""}]"
        }
    }

    @Serializable
    data object Boolean : Type() {
        override fun toString(): String {
            return "boolean"
        }
    }

    @Serializable
    data class FunctionTypeSignature(val returnType: Type?, val params: List<Parameter>) : Type() {
        @Serializable
        data class Parameter(val name: Located<String>, val type: Type) : Element {
            @Transient
            override val children: List<Element> = listOf(type)
            override fun toString(): String {
                return "$name: $type"
            }
        }

        @Transient
        override val children: List<Element> = params + listOfNotNull(returnType)

        override fun toString(): String {
            return "(${params.joinToString(", ") { "${it.type}" }}) -> ${returnType ?: "void"}"
        }
    }

    @Serializable
    data class TypeReference(
        override val reference: Located<String>,
        val genericParameters: Map<String, Type>
    ) : Type(), Access.TypeAccess, ScopeAware {
        @Transient override val children: List<Element> = genericParameters.values.toList()
        @Transient override var target: TypeDefinition? = null;
        @Transient override lateinit var accessor: Accessor;
        @Transient private lateinit var scope: Scope

        override fun resolve(messages: CompilerMessages) {
            target = scope.resolveType(reference, messages)
        }
        override fun setEnclosingScope(parent: Scope) {
            scope = parent
        }
        override fun toString(): String {
            return reference.value
        }
    }

    @Serializable
    class Union(var options: List<Type>) : Type() {
        override val children: List<Element> get() = options
        override fun toString(): String {
            return options.joinToString(" | ")
        }
    }
}
