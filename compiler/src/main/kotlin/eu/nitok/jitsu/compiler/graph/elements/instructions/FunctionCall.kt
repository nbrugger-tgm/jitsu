package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.common.locating.locatedAt
import eu.nitok.jitsu.compiler.graph.api.*
import eu.nitok.jitsu.compiler.graph.api.Function
import eu.nitok.jitsu.compiler.graph.elements.AccessElement.FunctionAccessElement
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import eu.nitok.jitsu.compiler.graph.elements.types.Undefined
import kotlinx.serialization.Serializable
import kotlin.math.min

@Serializable
internal class FunctionCall private constructor(
    private val access: FunctionAccessElement,
    val callParameterElements: List<ExpressionElement>,
    override val location: Location
) : AccessElement.FunctionAccess by access,
    Instruction.FunctionCall,
    InstructionElement,
    ExpressionElement {

    override val callParameters by lazy { callParameterElements.map{ it.asExpression } }

    constructor(
        reference: Located<String>,
        callParameters: List<ExpressionElement>,
        location: Location
    ) : this(FunctionAccessElement(reference), callParameters, location)

    override val parameters: Map<String, Expression>
        get() {
            return callParameters.subList(0, min(target?.parameters?.size ?: 0, callParameters.size))
                .mapIndexed { index, parameter -> parameter to target?.parameters?.get(index)?.name?.value }
                .filter { it.second != null }
                .associateBy({ it.second!! }, { it.first })
            //todo add default values as parameters
        }
    override val isConstant: ReasonedBoolean
        get() = ReasonedBoolean.False("Function call constant analysis not implemented yet")

    override fun calculateType(context: Map<String, TypeElement>, messages: CompilerMessages, typeHint: TypeElement?): TypeElement? {
        if (access.targetElement == null) {
            resolveTarget(context, messages)?.let { access.setResolvedTarget(it) }
        }
        val type = access.targetElement?.returnTypeElement?.value
        if (type != null) this.typeElement = type
        return type
    }

    fun resolveTarget(context: Map<String, TypeElement>, messages: CompilerMessages): FunctionElement? {
        var target: FunctionElement?
        val initialTypecalcMessages = CompilerMessages()
        val callSiteTypes = callParameterElements.map {
            (it.calculateType(context, initialTypecalcMessages) ?: Undefined).asType.locatedAt(it.location)
        }.toTypedArray()
        target = access.scope.resolveFunction(
            reference,
            callSiteTypes,
            messages
        )
        if (target != null && callSiteTypes.any { it.value is Type.Undefined } || initialTypecalcMessages.errors.isNotEmpty()) {
            //try again with typehints and hope it does not produce undefined / compiler messages
            callParameterElements.forEachIndexed { index, expression ->
                expression.calculateType(
                    context,
                    messages,
                    target?.parameters?.getOrNull(index)?.declaredTypeElement
                )
            }
        } else {
            messages.add(initialTypecalcMessages)
        }
        return target
    }

    override val type: Type get() = super.type
    //Undefined here is only relevant for void methods/methods with no return
    override var typeElement: TypeElement = Undefined
        private set;

    override val children: List<Element> get() = callParameters.toList()

    override fun toString(): String {
        return "${reference.value}(${callParameters.joinToString(", ")})"
    }
}