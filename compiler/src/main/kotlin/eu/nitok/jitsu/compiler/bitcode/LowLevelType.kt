package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.compiler.graph.Type

/**
 * Low-level type system for bytecode generation.
 * These types are pure layout descriptions - they don't hold references to specific variables/fields.
 * Access methods take the expression as an argument.
 */
sealed interface LowLevelType {

    val graphType: Type

    /**
     * Generate instructions to free memory associated with a value of this type.
     * @param field The field expression pointing to the value
     * @param ctx The lowering context for creating temporaries
     * @return Instructions to free the memory, empty if nothing to free
     */
    fun free(field: LowLevelExpression.Field, ctx: LoweringContext): List<LowLevelInstruction> = emptyList()

    /**
     * Generate instructions to allocate/initialize a variable of this type after stack allocation.
     * @param variable The variable expression (already stack-allocated)
     * @return Instructions for heap allocation or initialization, empty if none needed
     */
    fun allocate(variable: LowLevelExpression.Variable): List<LowLevelInstruction> = emptyList()

    /**
     * Returns the expression to use when reading this variable's value.
     * For pointer types, this dereferences. For value types, returns the variable directly.
     */
    fun readAccess(variable: LowLevelExpression.Field): LowLevelExpression.Field = variable

    /**
     * Returns the expression to use when writing to this variable.
     * For pointer types, this dereferences. For value types, returns the variable directly.
     */
    fun writeAccess(variable: LowLevelExpression.Field): LowLevelExpression.Field = variable

    /**
     * Primitive numeric types (integers, floats, booleans).
     */
    sealed interface LLPrimitive : LowLevelType {
        val size: BitSize
    }

    data class LLInt(override val size: BitSize, override val graphType: Type) : LLPrimitive {
        override fun toString() = "i${size.bits}"
    }

    data class LLUInt(override val size: BitSize, override val graphType: Type) : LLPrimitive {
        override fun toString() = "u${size.bits}"
    }

    data class LLFloat(override val size: BitSize, override val graphType: Type) : LLPrimitive {
        override fun toString() = "f${size.bits}"
    }

    data object LLBool : LLPrimitive {
        override val size = BitSize.BIT_1
        override val graphType: Type get() = Type.Boolean
        override fun toString() = "bool"
    }

    /**
     * Pointer to another low-level type.
     */
    data class LLPointer<T : LowLevelType>(val pointeeType: T, override val graphType: Type) : LowLevelType {
        /**
         * Dereference the pointer at the given expression.
         */
        fun deref(ptr: LowLevelExpression.Field): LowLevelExpression.Deref {
            return LowLevelExpression.Deref(ptr)
        }

        /**
         * Get a reference to the value at the given expression.
         */
        fun ref(value: LowLevelExpression.Field): LowLevelExpression.Ref {
            return LowLevelExpression.Ref(value)
        }

        override fun allocate(variable: LowLevelExpression.Variable): List<LowLevelInstruction> {
            return listOf(LowLevelInstruction.Write(variable, LowLevelExpression.AllocHeap(pointeeType)))
        }

        override fun readAccess(variable: LowLevelExpression.Field): LowLevelExpression.Field {
            return LowLevelExpression.Deref(variable)
        }

        override fun writeAccess(variable: LowLevelExpression.Field): LowLevelExpression.Field {
            return LowLevelExpression.Deref(variable)
        }

        override fun free(field: LowLevelExpression.Field, ctx: LoweringContext): List<LowLevelInstruction> {
            return pointeeType.free(deref(field), ctx) + listOf(LowLevelInstruction.Free(field))
        }

        override fun toString() = "*$pointeeType"
    }

    /**
     * Struct with named fields, each having a low-level type.
     */
    data class LLStruct(val fields: Map<String, LowLevelType>, override val graphType: Type) : LowLevelType {
        /**
         * Access a field of the struct at the given expression.
         * Returns the field expression for the named field.
         */
        fun accessField(struct: LowLevelExpression.Field, fieldName: String): LowLevelExpression.Read {
            require(fields.containsKey(fieldName)) { "Struct does not have field '$fieldName'. Available: ${fields.keys}" }
            return LowLevelExpression.Read(struct, fieldName)
        }

        /**
         * Get the type of a specific field.
         */
        fun fieldType(fieldName: String): LowLevelType {
            return fields[fieldName] ?: error("Struct does not have field '$fieldName'. Available: ${fields.keys}")
        }

        override fun free(field: LowLevelExpression.Field, ctx: LoweringContext): List<LowLevelInstruction> {
            return fields.flatMap { (name, fieldType) ->
                val fieldExpr = accessField(field, name)
                fieldType.free(fieldExpr, ctx)
            }
        }

        override fun toString() = "{ ${fields.entries.joinToString(", ") { "${it.key}: ${it.value}" }} }"
    }

    /**
     * Fixed-size array type (for stack-allocated arrays).
     */
    data class LLFixedArray(val elementType: LowLevelType, val size: Int, override val graphType: Type) : LowLevelType {
        /**
         * Access an element at a given index.
         */
        fun accessIndex(array: LowLevelExpression.Field, index: LowLevelExpression): LowLevelExpression.ArraySlot {
            return LowLevelExpression.ArraySlot(array, index)
        }

        override fun free(field: LowLevelExpression.Field, ctx: LoweringContext): List<LowLevelInstruction> {
            return (0 until size).flatMap {
                elementType.free(accessIndex(field, LowLevelExpression.NumericalValue(it.toLong())), ctx)
            }
        }

        override fun toString() = "[$elementType; $size]"
    }

    /**
     * C-style union - overlapping memory for multiple types.
     * All members share the same memory location; size is the max of all member sizes.
     * Unlike LLStruct, only one member is valid at a time (determined externally).
     */
    data class LLUnion(val members: Map<String, LowLevelType>, override val graphType: Type) : LowLevelType {
        /**
         * Access a member of the union at the given expression.
         * Note: Caller is responsible for knowing which member is currently valid.
         */
        fun accessMember(union: LowLevelExpression.Field, memberName: String): LowLevelExpression.Read {
            require(members.containsKey(memberName)) { "Union does not have member '$memberName'. Available: ${members.keys}" }
            return LowLevelExpression.Read(union, memberName)
        }

        /**
         * Get the type of a specific member.
         */
        fun memberType(memberName: String): LowLevelType {
            return members[memberName] ?: error("Union does not have member '$memberName'. Available: ${members.keys}")
        }

        // Note: free() not implemented - unions don't know which member is active.
        // Higher-level constructs (like JitsuUnion with discriminant) handle freeing.

        override fun toString() = "union { ${members.entries.joinToString(", ") { "${it.key}: ${it.value}" }} }"
    }

    open class Custom(val lowLevelType: LowLevelType) : LowLevelType by lowLevelType

    companion object {
        /** Common primitive types */
        val I8 = LLInt(BitSize.BIT_8, Type.Int(BitSize.BIT_8))
        val I16 = LLInt(BitSize.BIT_16, Type.Int(BitSize.BIT_16))
        val I32 = LLInt(BitSize.BIT_32, Type.Int(BitSize.BIT_32))
        val I64 = LLInt(BitSize.BIT_64, Type.Int(BitSize.BIT_64))

        val U8 = LLUInt(BitSize.BIT_8, Type.UInt(BitSize.BIT_8))
        val U16 = LLUInt(BitSize.BIT_16, Type.UInt(BitSize.BIT_16))
        val U32 = LLUInt(BitSize.BIT_32, Type.UInt(BitSize.BIT_32))
        val U64 = LLUInt(BitSize.BIT_64, Type.UInt(BitSize.BIT_64))

        val F32 = LLFloat(BitSize.BIT_32, Type.Float(BitSize.BIT_32))
        val F64 = LLFloat(BitSize.BIT_64, Type.Float(BitSize.BIT_64))
    }
}

