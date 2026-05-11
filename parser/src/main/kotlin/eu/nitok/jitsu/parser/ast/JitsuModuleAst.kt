package eu.nitok.jitsu.parser.ast

import eu.nitok.jitsu.common.Walkable

data class JitsuModuleAst(val name: String, val files: List<SourceFileNode>, val modules: List<JitsuModuleAst>) : Walkable<AstNode> {
    val allModules: Sequence<JitsuModuleAst> get() = modules.asSequence().flatMap { it.allModules } + this
    override val children: List<AstNode> by lazy { allModules.flatMap { it.files }.toList() }
}