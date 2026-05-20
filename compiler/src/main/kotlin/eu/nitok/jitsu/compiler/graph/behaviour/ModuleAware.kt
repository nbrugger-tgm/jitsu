package eu.nitok.jitsu.compiler.graph

interface ModuleAware {
    val module: JitsuModule
    fun setEnclosingModule(parent: JitsuModule)
}
