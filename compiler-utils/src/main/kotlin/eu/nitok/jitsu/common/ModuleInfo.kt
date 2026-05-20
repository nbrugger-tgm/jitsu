package eu.nitok.jitsu.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ModuleInfo(
    val name: String,
    val sourceRoot: Path,
) {
    fun save(file: Path) {
        val json = Schema(name, sourceRoot)
        file.writeText(Json.encodeToString(json))
    }
    @Serializable
    @SerialName("module-info")
    private data class Schema(
        val name: String,
        val sourceRoot: @Serializable(PathSerializer::class) Path?,
    )
    companion object {
        fun from(file: Path): ModuleInfo {
            val jsonModel: Schema = parse(file.readText())
            return ModuleInfo(jsonModel.name, jsonModel.sourceRoot?:file.parent)
        }
        private fun parse(json: String): Schema {
            return Json.decodeFromString(json)
        }
    }
}