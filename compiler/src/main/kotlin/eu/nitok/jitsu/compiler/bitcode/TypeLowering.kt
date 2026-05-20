package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.bitcode.LowLevelType.Companion.I64
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.Companion.U64
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.Companion.U8
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.*
import eu.nitok.jitsu.compiler.graph.Type as GraphType
import eu.nitok.jitsu.compiler.graph.TypeDefinition

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
            is GraphType.Boolean -> LLBool

            is GraphType.Array -> lowerArray(type)
            is GraphType.Union -> lowerUnion(type)
            is GraphType.StructuralInterface -> lowerStruct(type)

            is GraphType.TypeReference -> lower(type.typeCache)

            is GraphType.Null -> TODO("null not yet supported") // null represented as null pointer
            is GraphType.Value -> lower(type.value.type)
            is GraphType.FunctionTypeSignature -> lowerFunctionPointer(type)
            is GraphType.Undefined -> error("Cannot lower undefined type")
            
            is TypeDefinition.DirectTypeDefinition.Enum -> lowerEnum(type)
            is TypeDefinition.DirectTypeDefinition.TypeParameter -> throw IllegalStateException("Generics should already be resolved when lowering")
        }
    }

    private fun lowerArray(type: GraphType.Array): JitsuArray {
        val elementType = lower(type.elementType)
        return if (type.size != null) {
            JitsuArray.fixed(elementType, type.size, type)
        } else {
            JitsuArray.dynamic(elementType, type)
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

    private fun lowerEnum(type: TypeDefinition.DirectTypeDefinition.Enum): LowLevelType {
        TODO()
    }
}
