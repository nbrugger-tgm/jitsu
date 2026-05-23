package eu.nitok.jitsu.backend.c

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.compiler.bitcode.LowLevelType

class TypeRegistry {
    private val typeInfo = mutableMapOf<LowLevelType, String>()
    private val typedefs = mutableListOf<String>()
    private var synthNameIdx = 0

    fun getUniqueName(type: LowLevelType): String {
        return typeInfo[type]?:run {
            val name = "type${synthNameIdx++}"
            typedefs.add(toCTypedef(type, name))
            typeInfo[type] = name
            name
        }
    }

    val typeDefs: String get() = typedefs.joinToString ("\n\n")

    fun toCTypedef(type: LowLevelType, name: String): String {
        return when(type) {
            is LowLevelType.LLStruct -> "struct $name {\n ${type.fields.entries.joinToString("") {
                "    ${formatType(it.key,it.value)};\n"
            }}};"
            is LowLevelType.LLUnion -> "union $name {\n ${type.members.entries.joinToString("") {
                "    ${formatType(it.key,it.value)};\n"
            }}};"
            is LowLevelType.Custom -> toCTypedef(type.lowLevelType,name)
            else -> "typedef ${formatType(name, type)};"
        }
    }

    fun formatType(variable: String, type: LowLevelType): String {
        return when(type) {
            is LowLevelType.LLFixedArray -> formatType("($variable[])", type.elementType)
            is LowLevelType.LLPointer<*> -> formatType("(*$variable)", type.pointeeType)
            is LowLevelType.LLStruct -> "struct ${getUniqueName(type)} $variable"
            is LowLevelType.LLBool -> "bool $variable"
            is LowLevelType.LLFloat -> "${formatFloat(type.size)} $variable"
            is LowLevelType.LLInt -> "${formatInt(type.size)} $variable"
            is LowLevelType.LLUInt -> "${formatUInt(type.size)} $variable"
            is LowLevelType.LLUnion -> "union ${getUniqueName(type)} $variable"
            is LowLevelType.Custom -> formatType(variable, type.lowLevelType)
        }
    }


    private fun formatInt(int: BitSize): String {
        return when(int) {
            BitSize.BIT_1 -> "bool"
            BitSize.BIT_8 -> "signed char"
            BitSize.BIT_16 -> "signed int"
            BitSize.BIT_32 -> "signed long"
            BitSize.BIT_64 -> "signed long long"
            BitSize.BIT_128 -> "signed __int128"
            BitSize.BIT_256 -> error("256 bit not supported by C")
        }
    }


    private fun formatUInt(int: BitSize): String {
        return when(int) {
            BitSize.BIT_1 -> "bool"
            BitSize.BIT_8 -> "unsigned char"
            BitSize.BIT_16 -> "unsigned int"
            BitSize.BIT_32 -> "unsigned long"
            BitSize.BIT_64 -> "unsigned long long"
            BitSize.BIT_128 -> "unsigned __int128"
            BitSize.BIT_256 -> error("256 bit not supported by C")
        }
    }

    private fun formatFloat(int: BitSize): String {
        return when(int) {
            BitSize.BIT_1, BitSize.BIT_8, BitSize.BIT_16 -> TODO("C does not support ${int.bits} bit floats")
            BitSize.BIT_32 -> "float"
            BitSize.BIT_64 -> "double"
            else -> "_Decimal${int.bits}"
        }
    }

}

private fun String.structify(): String = replace(Regex("[ ()]"), "_")