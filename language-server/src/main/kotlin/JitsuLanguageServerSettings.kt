import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.isReadable
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val SYSTEM_SETTINGS = Path(System.getProperty("user.home")) / ".jitsulsp"
@Serializable
data class JitsuLanguageServerSettings(
    val gradleCommand: String? = null,
    val allowGradle: Boolean? = null,
) {
    companion object {
        fun loadSystemSettings(): JitsuLanguageServerSettings {
            if(!SYSTEM_SETTINGS.isReadable()) return JitsuLanguageServerSettings()
            return Json.decodeFromString(SYSTEM_SETTINGS.readText())
        }
    }

    fun saveSystemSettings(onError: (Throwable?) -> Unit) {
        try {
            SYSTEM_SETTINGS.writeText(Json.encodeToString(this))
        } catch (e: Throwable) {
            onError(e)
        }
    }
}