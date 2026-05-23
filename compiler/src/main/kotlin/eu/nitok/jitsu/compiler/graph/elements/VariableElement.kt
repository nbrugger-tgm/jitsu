package eu.nitok.jitsu.compiler.graph.elements

import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.compiler.graph.api.Expression
import eu.nitok.jitsu.compiler.graph.api.Type
import eu.nitok.jitsu.compiler.graph.api.Variable
import eu.nitok.jitsu.compiler.graph.behaviour.ModuleAware
import eu.nitok.jitsu.compiler.graph.elements.types.TypeElement
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface VariableElement: ModuleAware, AccessibleElement {
    val asVariable: Variable get() = when(this) {
        is FunctionElement.Parameter -> this
        is VariableDeclaration -> this
    }
    val initialValueElement: ExpressionElement?
    val declaredTypeElement: TypeElement?

    val name: Located<String>
    val reassignable: Boolean

    val declaredType: Type? get() = declaredTypeElement?.asType
    val initialValue: Expression? get() = initialValueElement?.asExpression
}