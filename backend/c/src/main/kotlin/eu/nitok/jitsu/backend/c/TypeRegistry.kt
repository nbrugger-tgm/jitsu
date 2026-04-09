package eu.nitok.jitsu.backend.c

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.common.indent
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.graph.TypeDefinition

class TypeRegistry {
    data class TypeEntry(val name: String, val heapAlloc: Boolean, val typeDef: String? = null)
    private val typeInfo = mutableMapOf<Type, TypeEntry>()
    private var synthNameIdx = 0
    fun getTypeInfo(layout: Type, userDefinedName: String? = null): TypeEntry {
        if(layout in typeInfo) {
            return typeInfo[layout]!!
        }
        val info = when (layout) {
            is Type.Array -> TODO()
            is Type.FunctionTypeSignature -> TODO()
            Type.Null -> TODO()
            Type.Boolean -> TypeEntry("bool", false)
            is Type.Float -> mapFloat(layout)
            is Type.Int -> mapInt(layout)
            is Type.UInt -> mapUInt(layout)
            is Type.StructuralInterface -> TODO()
            is Type.TypeReference -> mapReferenceType(layout)
            Type.Undefined -> TypeEntry("UNDEFINED", false)//TODO("Undefined types cannot be transpiled")
            is Type.Union -> mapUnion(layout, userDefinedName)
            is Type.Value -> getTypeInfo(layout.value.type)
            is TypeDefinition.DirectTypeDefinition.Enum -> mapEnum(layout)
        }
        typeInfo[layout] = info
        return info
    }

    private fun mapEnum(layout: TypeDefinition.DirectTypeDefinition.Enum): TypeEntry {
        return TypeEntry(
            name = layout.name.value,
            heapAlloc = false,
            typeDef = """
enum ${layout.name} {
${indent(1, layout.constants.joinToString(", ") { it.name.value })}
};
            """.trimIndent()
        )
    }

    private fun mapUnion(layout: Type.Union, userDefinedName: String?): TypeEntry {
        val optionTypes = layout.options.map { getTypeInfo(it) }
        val name =
            userDefinedName?.let { uniqueName(it) } ?:
            uniqueName("union_${optionTypes.joinToString("_") { it.name.replace(Regex("[ ()]"),"_") }}")
        return TypeEntry(
            name = "struct $name",
            heapAlloc = false,
            typeDef = """
struct $name {
    int option;
    union {
${indent(2, optionTypes.withIndex().joinToString("\n") { "${it.value.name} o${it.index};" })}
    } value;
};
            """.trimIndent()
        )
    }

    private fun uniqueName(string: String): String {
        val exisitingNames = typeInfo.values.map { it.name }.toSet()
        return if(!exisitingNames.contains(string)) string
        else string + synthNameIdx++
    }

    private fun mapReferenceType(layout: Type.TypeReference): TypeEntry {
        return getTypeInfo(layout.resolvedCache, layout.reference.value)
    }

    private fun mapInt(int: Type.Int): TypeEntry {
        return TypeEntry(when(int.size) {
            BitSize.BIT_1 -> "bool"
            BitSize.BIT_8 -> "signed char"
            BitSize.BIT_16 -> "signed int"
            BitSize.BIT_32 -> "signed long"
            BitSize.BIT_64 -> "signed long long"
            BitSize.BIT_128 -> "signed __int128"
            BitSize.BIT_256 -> error("256 bit not supported by C")
        }, false);
    }


    private fun mapUInt(int: Type.UInt): TypeEntry {
        return TypeEntry(when(int.size) {
            BitSize.BIT_1 -> "bool"
            BitSize.BIT_8 -> "unsigned char"
            BitSize.BIT_16 -> "unsigned int"
            BitSize.BIT_32 -> "unsigned long"
            BitSize.BIT_64 -> "unsigned long long"
            BitSize.BIT_128 -> "unsigned __int128"
            BitSize.BIT_256 -> error("256 bit not supported by C")
        }, false);
    }

    private fun mapFloat(int: Type.Float): TypeEntry {
        return TypeEntry(when(int.size) {
            BitSize.BIT_1, BitSize.BIT_8, BitSize.BIT_16 -> TODO("C does not support ${int.size.bits} bit floats")
            BitSize.BIT_32 -> "float"
            BitSize.BIT_64 -> "double"
            else -> "_Decimal${int.size.bits}"
        }, false);
    }

    fun getTypedefs(): List<String> {
        return typeInfo.values.distinctBy { it.name }.mapNotNull { it.typeDef }
    }
}