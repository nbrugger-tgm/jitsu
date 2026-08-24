package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.compiler.graph.api.Function.Signature

interface FunctionDefinition : Function, Accessible<FunctionDefinition>, Parameterized {

    override val signature: ParameterizedSignature

    interface ParameterizedSignature : Signature, Parameterized

}