package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.compiler.ast.Located
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.compiler.graph.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.compiler.parser.Range

sealed interface FunctionSignatureMatch {
    data object NoMatch : FunctionSignatureMatch
    data class Suggestion(
        val function: Function,
        val typeError: List<TypeMissMatch>,
        val missing: List<Type.FunctionTypeSignature.Parameter>,
        val overflow: Int
    ) : FunctionSignatureMatch

    data class Match(val function: Function) : FunctionSignatureMatch
}

data class TypeMissMatch(val paramDefinition: Located<String>, val parameterValueLocation:Range, val expected: Type, val reason: ReasonedBoolean.False)

private sealed interface SignatureMatch {
    data object FullMatch : SignatureMatch
    data class PartialMatch(
        val missing: List<Type.FunctionTypeSignature.Parameter>,
        val typeError: List<TypeMissMatch>,
        val correct: Int,
        val overflow: Int
    ) : SignatureMatch {
        fun betterThan(closestMatch: PartialMatch): Boolean =
            correct > closestMatch.correct || (correct == closestMatch.correct && errorCount < closestMatch.errorCount)

        private val PartialMatch.errorCount: Int get() = missing.size + typeError.size
    }
}

private fun Type.FunctionTypeSignature.matches(argumentTypes: Array<Located<Type>>): SignatureMatch {
    val missing = mutableListOf<Type.FunctionTypeSignature.Parameter>()
    val typeError = mutableListOf<TypeMissMatch>()
    var correct = 0
    for (i in parameters.indices) {
        if (i >= argumentTypes.size) {
            missing.add(parameters[i])
            continue
        }
        val expected = parameters[i].type
        val actual = argumentTypes[i]
        val match = expected.acceptsInstanceOf(actual.value)
        if (match is ReasonedBoolean.False) {
            typeError.add(TypeMissMatch(parameters[i].name,actual.location, expected, match))
        } else {
            correct++
        }
    }
    val overflow = argumentTypes.size - parameters.filter { !it.optional }.size
    return if (missing.isEmpty() && typeError.isEmpty() && overflow == 0) SignatureMatch.FullMatch else SignatureMatch.PartialMatch(
        missing,
        typeError,
        correct,
        overflow
    )
}

fun matchFunctionSignatures(functions: List<Function>, parameters: Array<Located<Type>>): FunctionSignatureMatch {
    if (functions.isEmpty()) return FunctionSignatureMatch.NoMatch
    if (functions.size == 1) {
        val first = functions.first()
        return when (val match = first.signature.matches(parameters)) {
            SignatureMatch.FullMatch -> FunctionSignatureMatch.Match(first)
            is SignatureMatch.PartialMatch -> FunctionSignatureMatch.Suggestion(
                first,
                match.typeError,
                match.missing,
                match.overflow
            )
        }
    }
    var closestMatch: Pair<Function, SignatureMatch.PartialMatch>? = null
    for (fu in functions) {
        when (val match = fu.signature.matches(parameters)) {
            is SignatureMatch.FullMatch -> return FunctionSignatureMatch.Match(fu)
            is SignatureMatch.PartialMatch -> {
                if ((closestMatch == null || match.betterThan(closestMatch.second)) && match.correct > 0) {
                    closestMatch = fu to match
                }
            }
        }
    }
    return FunctionSignatureMatch.NoMatch
}


