package eu.nitok.jitsu.compiler.graph.api

import eu.nitok.jitsu.common.locating.HasLocation
import eu.nitok.jitsu.common.locating.Located

interface Import : HasLocation, Element {
    val name: Located<String>
    val target: JitsuModule?
}