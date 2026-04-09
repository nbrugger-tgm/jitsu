package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.Located
import eu.nitok.jitsu.compiler.graph.Function
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.Type
import eu.nitok.jitsu.common.Range

sealed interface FunctionSignatureMatch {
    data object NoMatch : FunctionSignatureMatch
    data class Suggestion(
        val function: Function,
        val typeError: List<TypeMissMatch>,
        val missing: List<Type.FunctionTypeSignature.Parameter>,
        val overflow: Int
    ): FunctionSignatureMatch

    data class Match(val function: Function) : FunctionSignatureMatch
}

data class TypeMissMatch(val paramDefinition: Located<String>, val parameterValueLocation:Range, val expected: Type, val reason: ReasonedBoolean.False)

private sealed interface SignatureMatch {
    data class FullMatch(
        val perciseMatches: Int,
        val assignableMatch: Int
    ) : SignatureMatch
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

private fun structuralyEqual(expected: Type, value: Type): Boolean {
    val expectedRaw = if(expected is Type.TypeReference) expected.resolvedCache else expected
    val valueRaw = if(value is Type.TypeReference) value.resolvedCache else value
    return expectedRaw == valueRaw
}
private fun Type.FunctionTypeSignature.matches(argumentTypes: Array<Located<Type>>): SignatureMatch {
    val missing = mutableListOf<Type.FunctionTypeSignature.Parameter>()
    val typeError = mutableListOf<TypeMissMatch>()
    var correctPercise = 0
    var correctAssignable = 0
    for (i in parameters.indices) {
        if (i >= argumentTypes.size) {
            missing.add(parameters[i])
            continue
        }
        val expected = parameters[i].type
        val actual = argumentTypes[i]
        val messages = CompilerMessages()
        val same = structuralyEqual(expected, actual.value)
        if(messages.errors.isNotEmpty()) {
            throw IllegalStateException("Error during resolvation!"+ messages.errors.map{it.toString()})
        }
        if(same) {
            correctPercise ++
        } else {
            val match = expected.acceptsInstanceOf(actual.value)
            if (match is ReasonedBoolean.False) {
                typeError.add(TypeMissMatch(parameters[i].name,actual.location, expected, match))
            } else {
                correctAssignable++
            }
        }
    }
    val overflow = argumentTypes.size - parameters.filter { !it.optional }.size
    return if (missing.isEmpty() && typeError.isEmpty() && overflow == 0) SignatureMatch.FullMatch(correctPercise, correctAssignable) else SignatureMatch.PartialMatch(
        missing,
        typeError,
        correctAssignable + correctPercise,
        overflow
    )
}

fun matchFunctionSignatures(functions: List<Function>, parameters: Array<Located<Type>>): FunctionSignatureMatch {
    if (functions.isEmpty()) return FunctionSignatureMatch.NoMatch
    if (functions.size == 1) {
        val first = functions.first()
        return when (val match = first.signature.matches(parameters)) {
            is SignatureMatch.FullMatch -> FunctionSignatureMatch.Match(first)
            is SignatureMatch.PartialMatch -> suggestion(first, match)
        }
    }
    var closestMatch: Pair<Function, SignatureMatch.PartialMatch>? = null
    var bestFullMatch: Pair<Function, SignatureMatch.FullMatch>? = null
    for (fu in functions) {
        when (val match = fu.signature.matches(parameters)) {
            is SignatureMatch.FullMatch -> {
                if(bestFullMatch == null) bestFullMatch = fu to match
                else if(bestFullMatch.second.perciseMatches < match.perciseMatches) bestFullMatch = fu to match
            }
            is SignatureMatch.PartialMatch -> {
                if (bestFullMatch == null && (closestMatch == null || match.betterThan(closestMatch.second))) {
                    closestMatch = fu to match
                }
            }
        }
    }
    return bestFullMatch?.let{ FunctionSignatureMatch.Match(it.first) }
        ?: closestMatch?.let { suggestion(it.first, it.second) }
        ?: FunctionSignatureMatch.NoMatch
}

private fun suggestion(function: Function, match: SignatureMatch.PartialMatch) = FunctionSignatureMatch.Suggestion(
    function, match.typeError, match.missing, match.overflow
)
