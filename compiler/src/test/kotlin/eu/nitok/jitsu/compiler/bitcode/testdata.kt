package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.common.BitSize
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.LLFloat
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.LLInt
import eu.nitok.jitsu.compiler.bitcode.LowLevelType.LLUInt
import eu.nitok.jitsu.compiler.graph.elements.types.Float
import eu.nitok.jitsu.compiler.graph.elements.types.UInt
import eu.nitok.jitsu.compiler.graph.elements.types.Int

val I8 = LLInt(BitSize.BIT_8, Int(BitSize.BIT_8))
val I16 = LLInt(BitSize.BIT_16, Int(BitSize.BIT_16))
val I32 = LLInt(BitSize.BIT_32, Int(BitSize.BIT_32))
val I64 = LLInt(BitSize.BIT_64, Int(BitSize.BIT_64))

val U8 = LLUInt(BitSize.BIT_8, UInt(BitSize.BIT_8))
val U16 = LLUInt(BitSize.BIT_16, UInt(BitSize.BIT_16))
val U32 = LLUInt(BitSize.BIT_32, UInt(BitSize.BIT_32))
val U64 = LLUInt(BitSize.BIT_64, UInt(BitSize.BIT_64))

val F32 = LLFloat(BitSize.BIT_32, Float(BitSize.BIT_32))
val F64 = LLFloat(BitSize.BIT_64, Float(BitSize.BIT_64))