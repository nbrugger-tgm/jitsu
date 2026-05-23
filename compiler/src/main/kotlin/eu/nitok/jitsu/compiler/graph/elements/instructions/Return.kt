package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.graph.elements.FunctionElement
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.Instruction
import eu.nitok.jitsu.compiler.graph.behaviour.FunctionAware
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient



@Serializable
internal data class Return(val valueExpression: ExpressionElement?, override val location: Location) : FunctionAware, Instruction.Return, InstructionElement {
    override val children: List<Element> get() = listOfNotNull(value)
    override val value: Expression? get() = valueExpression?.asExpression
    @Transient
    lateinit var function: FunctionElement
    override fun setEnclosingFunction(parent: FunctionElement) {
        function = parent
    }
}