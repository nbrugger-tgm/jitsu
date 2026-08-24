package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.Located

/**
 * @param P the [Accessible] parameterized object
 * @param S the specialized form of P that results from this access
 */
interface AccessWithParameters<out P,out S: Specialized<P>> : Access<P>
        where P : Accessible<P>, P : Parameterized {
    val typeParameters: List<Located<Type>>
    val specializedTarget: S?
}