package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.graph.api.Element
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.elements.types.Array
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.compiler.graph.elements.types.Undefined
import eu.nitok.jitsu.compiler.graph.elements.types.Union
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
internal data class ArrayLiteral(val elementExpressions: List<ExpressionElement>, override val location: Location) : Expression.ArrayLiteral, ExpressionElement {
    override val elements: List<Expression> by lazy { elementExpressions.map { it.asExpression } }
    override val isConstant: ReasonedBoolean
        get() = elements.map { it.isConstant }.reduce { acc, boolean -> acc.and(boolean) }

    override fun calculateType(
        context: Map<String, TypeElement>,
        messages: CompilerMessages,
        typeHint: TypeElement?
    ): Array {
        val type = (if (elementExpressions.isEmpty()) {
            if (typeHint is Array) typeHint.elementTypeElement
            else if(typeHint is Type.Array) throw IllegalStateException("typeHint is of wrong array type: ${typeHint::class}, expected an 'element type'(${Array::class}")
            else if(typeHint != null) throw IllegalStateException("typeHint for an arrayLiteral cannot be of non array type")
            else {
                messages.error("Cannot infer type of empty array", this.location)
                Undefined
            }
        } else elementExpressions.mapNotNull { element ->
            element.calculateType(context, messages, typeHint?.let { it as? Array }?.elementTypeElement)
        }.reduce { acc, type ->
            val aToB = acc.acceptsInstanceOf(type.asType)
            if (aToB.value) acc
            else {
                val bToA = type.acceptsInstanceOf(acc.asType)
                if (bToA.value) type
                else Union(listOf(acc, type))
            }
        }).let { Array(it, ConstantElement.IntConstant(elements.size.toLong(),elementExpressions.first().location.rangeTo(elementExpressions.last().location))) }
        this.typeElement = type
        return type
    }
    override lateinit var typeElement: TypeElement private set;
    override val type: Type get() = typeElement.asType

    override val children: List<Element> get() = elements
}