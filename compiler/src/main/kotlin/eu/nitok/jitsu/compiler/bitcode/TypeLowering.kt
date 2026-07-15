package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.bitcode.LowLevelType.*
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.DirectTypeDefinition
import eu.nitok.jitsu.compiler.graph.api.TypeDefinition.DirectTypeDefinition.TypeParameter
import eu.nitok.jitsu.compiler.graph.api.Type as GraphType

/**
 * Converts graph types (from the semantic analysis phase) to low-level types
 * suitable for bytecode generation.
 *
 * This is the bridge between the high-level type system and the low-level IR.
 */
object TypeLowering {

    /**
     * Convert a graph type to its low-level representation.
     */
    fun lower(type: GraphType): LowLevelType {
        return when (type) {
            is GraphType.Int -> LLInt(type.size, type)
            is GraphType.UInt -> LLUInt(type.size, type)
            is GraphType.Float -> LLFloat(type.size, type)
            is GraphType.Boolean -> LLBool(type)

            is GraphType.Array -> lowerArray(type)
            is GraphType.Union -> lowerUnion(type)
            is GraphType.StructuralInterface -> lowerStruct(type)

            is GraphType.TypeReference -> lower(type.rawType)

            is GraphType.Null -> TODO("null not yet supported") // null represented as null pointer
            is GraphType.Value -> lower(type.value.type)
            is GraphType.FunctionTypeSignature -> lowerFunctionPointer(type)

            is DirectTypeDefinition.Enum -> lowerEnum(type)
            is GraphType.Undefined ->
                error("Cannot lower undefined type")
            is TypeParameter -> throw IllegalStateException("Generics should already be resolved when lowering")
        }
    }

    private fun lowerArray(type: GraphType.Array): JitsuArray {
        val elementType = lower(type.elementType)
        val sizeType = lower(type.sizeType)
        val size = type.size
        return if (size != null) {
            JitsuArray.fixed(elementType, sizeType, size.value, type)
        } else {
            JitsuArray.dynamic(elementType, sizeType, type)
        }
    }

    private fun lowerUnion(type: GraphType.Union): JitsuUnion {
        val options = type.options.map { lower(it) }
        return JitsuUnion.of(type, options)
    }

    private fun lowerStruct(type: GraphType.StructuralInterface): LLStruct {
        val fields = type.fields.mapValues { (_, field) ->
            lower(field.type)
        }
        return LLStruct(fields, type)
    }

    private fun lowerFunctionPointer(type: GraphType.FunctionTypeSignature): LLPointer<LLStruct> {
        TODO("Create proper function type representation if needed")
    }

    private fun lowerEnum(type: DirectTypeDefinition.Enum): LowLevelType {
        TODO()
    }
}
