package eu.nitok.jitsu.compiler.analysis

import eu.nitok.jitsu.common.CompilerMessages
import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.common.locating.Located
import eu.nitok.jitsu.common.locating.Location
import eu.nitok.jitsu.compiler.graph.api.Function
import eu.nitok.jitsu.compiler.graph.api.Type

sealed interface FunctionSignatureMatch<out T: Function> {
    data object NoMatch : FunctionSignatureMatch<Nothing>
    data class Suggestion<out T: Function>(
        val function: T,
        val typeError: List<TypeMissMatch>,
        val missing: List<Type.FunctionTypeSignature.Parameter>,
        val overflow: Int
    ): FunctionSignatureMatch<T>

    data class Match<out T: Function>(val function: T) : FunctionSignatureMatch<T>
}

data class TypeMissMatch(val paramDefinition: Located<String>, val parameterValueLocation:Location, val expected: Type, val actual: Type, val reason: ReasonedBoolean.False)

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

/**
 * Structural equality indicated that the types are the same and at runtime could be assigned without any conversion
 */
private fun structuralyEqual(expected: Type, value: Type): Boolean {
    val expectedRaw = expected.rawType
    val valueRaw = value.rawType
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
                typeError.add(TypeMissMatch(parameters[i].name,actual.location, expected, actual.value, match))
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

fun <T: Function> matchFunctionSignatures(functions: List<T>, parameters: Array<Located<Type>>): FunctionSignatureMatch<T> {
    if (functions.isEmpty()) return FunctionSignatureMatch.NoMatch
    if (functions.size == 1) {
        val first = functions.first()
        return when (val match = first.signature.matches(parameters)) {
            is SignatureMatch.FullMatch -> FunctionSignatureMatch.Match(first)
            is SignatureMatch.PartialMatch -> suggestion(first, match)
        }
    }
    var closestMatch: Pair<T, SignatureMatch.PartialMatch>? = null
    var bestFullMatch: Pair<T, SignatureMatch.FullMatch>? = null
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

private fun <T: Function> suggestion(function: T, match: SignatureMatch.PartialMatch) = FunctionSignatureMatch.Suggestion(
    function, match.typeError, match.missing, match.overflow
)
