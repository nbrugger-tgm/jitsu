package eu.nitok.jitsu.compiler.bitcode

import eu.nitok.jitsu.compiler.bitcode.LowLevelExpression.Field
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.graph.Variable

sealed interface LowLevelInstruction {
    data class Free(val name: Field) : LowLevelInstruction
    data class AllocStack(val name: String, val layout: Type): LowLevelInstruction
    data class Write(val name: Field, val value: LowLevelExpression): LowLevelInstruction
    data class Invoke(val name: String, val args: Map<String, LowLevelExpression>): LowLevelInstruction
    data class Return(val value: LowLevelExpression?): LowLevelInstruction
    data class Conditional(val condition: LowLevelExpression, val instructions: List<LowLevelInstruction>, val elseInstructions: List<LowLevelInstruction>?): LowLevelInstruction
    data class WriteAtIndex(val array: Field, val index: LowLevelExpression, val value: LowLevelExpression): LowLevelInstruction
    data class While(val conditional: LowLevelExpression, val instructions: List<LowLevelInstruction>): LowLevelInstruction
    data class Increase(var variable: Field): LowLevelInstruction
}
