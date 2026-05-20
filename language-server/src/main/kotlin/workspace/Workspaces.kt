package workspace

import kotlin.sequences.joinToString

class Workspaces {
    val diagnosticsId get() =
        workspaces.asSequence()
            .map { "${it.version}${it.modules.joinToString("") { it.sourceFiles.sumOf { it.version }.toString() }}" }
            .joinToString("-") { it }

    fun addAll(it: List<Workspace>) {
        this.workspaces.addAll(it)
    }

    fun remove(uri: String) {
        this.workspaces.removeIf { it.uri.toString() == uri }
    }

    fun getDocument(uri: String): SourceFile {
        //TODO should keep a Map<String, SourceFile>
        return workspaces.first { uri.startsWith(it.uri.toString()) }
            .modules.flatMap { it.sourceFiles }
            .first { it.uri.toString() == uri }
    }

    val workspaces: MutableList<Workspace> = mutableListOf()
}