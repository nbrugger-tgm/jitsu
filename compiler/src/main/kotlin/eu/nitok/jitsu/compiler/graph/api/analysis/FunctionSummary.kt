package eu.nitok.jitsu.compiler.graph.api.analysis

import eu.nitok.jitsu.common.ReasonedBoolean
import eu.nitok.jitsu.compiler.graph.api.Function

interface FunctionSummary {
    /** Whether the function has no observable effects beyond its return value. */
    val noSideEffects: ReasonedBoolean

    /** BORROW/MOVE mode for each parameter by name. */
    val parameterModes: Map<String, ParameterMode>

    /** Return value characteristics. Null for void functions. */
    val returnSummary: ReturnSummary?

    /** Names of functions called by this function (for serialization/display). */
    //TODO: maybe remove? This is sourceable in other ways such as accessesFromSelf or just graph-walking
    val callees: List<Function>

    val variableSummary: Map<String, VariableSummary>

    /** A function is pure if it is deterministic and has no side effects. */
    val pure: Boolean
}