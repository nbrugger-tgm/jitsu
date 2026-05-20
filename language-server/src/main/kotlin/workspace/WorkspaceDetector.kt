package workspace

import eu.nitok.jitsu.common.ModuleInfo
import JitsuLanguageServerSettings
import helpers.askUser
import helpers.joinAll
import helpers.showMessage
import isWindows
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.MessageType.Warning
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.services.LanguageClient
import java.io.IOException
import java.net.URI
import java.nio.file.*
import java.nio.file.Files.walk
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.*
import kotlin.streams.asSequence

private const val GRADLE_MODULE_TASK = "createJitsuModuleInfo"

object WorkspaceDetector {

    fun loadWorkspaces(
        folders: List<WorkspaceFolder>,
        client: LanguageClient,
        settings: JitsuLanguageServerSettings
    ): CompletableFuture<out List<Workspace>> {
        return folders.map { folder ->
            loadWorkspace(URI(folder.uri), client, settings)
        }.joinAll()
    }

    fun loadWorkspace(
        uri: URI,
        client: LanguageClient,
        settings: JitsuLanguageServerSettings
    ): CompletableFuture<Workspace> {
        return CompletableFuture.supplyAsync {
            val path = getReadablePath(uri, client) ?: return@supplyAsync Workspace(
                uri = uri,
                modules = mutableListOf(),
            )
            generateGradleModuleFiles(path, client, settings)
            discoverWorkspaceModules(path, client)
        }
    }

    private fun generateGradleModuleFiles(
        path: Path,
        client: LanguageClient,
        settings: JitsuLanguageServerSettings
    ): CompletableFuture<*> {
        val allGradleFiles = walk(path).asSequence()
            .filter { it.name.startsWith("build.gradle") || it.name.startsWith("settings.gradle") }.toSet()
        if (allGradleFiles.isEmpty()) return CompletableFuture.completedFuture(Unit)
        return hasGradlePermission(settings, client).thenCompose {
            if(!it) return@thenCompose CompletableFuture.completedFuture(null)
            val gradleRoots = allGradleFiles.filter { it.name.startsWith("settings.gradle") }.map { it.parent }
            val gradlew = findGradlew(path)
            val gradleProcesses = CompletableFuture.allOf(*gradleRoots.map { gradleRoot ->
                runGradle(gradlew, listOf(GRADLE_MODULE_TASK), gradleRoot)
            }.toTypedArray())
            gradleProcesses
        }
    }

    private fun runGradle(
        gradlew: Path?,
        tasks: List<String>,
        gradleRoot: Path
    ): CompletableFuture<Process> {
        return ProcessBuilder()
            .command(listOf(gradlew?.absolutePathString() ?: "gradle", *tasks.toTypedArray()))
            .directory(gradleRoot.toFile())
            .inheritIO()
            .start()
            .onExit()
    }

    private fun hasGradlePermission(
        settings: JitsuLanguageServerSettings,
        client: LanguageClient
    ): CompletableFuture<Boolean> {
        val allowed = settings.allowGradle
        return if (allowed == null) {
            client.requestGradlePermission()
        } else {
            CompletableFuture.completedFuture(allowed)
        }
    }

    private fun findGradlew(path: Path): Path? {
        val gradlewName = if (isWindows()) "gradlew.bat" else "gradlew"
        return walk(path).asSequence().firstOrNull { it.name == gradlewName }.let {
            if (it != null) null
            else {
                var dir = path
                while (dir.parent != null) {
                    dir = dir.parent
                    val gradlew = dir.resolve(gradlewName)
                    if (gradlew.exists()) return@let gradlew
                }
                null
            }
        }
    }

    private val sourceSetRegex = Regex("src/\\w+/jitsu")

    private fun discoverWorkspaceModules(
        path: Path,
        client: LanguageClient
    ): Workspace {
        val (explicitModules, nonModularizedFiles) = loadExplicitlyDefinedModules(path)

        if (nonModularizedFiles.isEmpty() && explicitModules.isNotEmpty()) return Workspace(
            uri = path.toUri(),
            modules = explicitModules
        )
        if (explicitModules.isNotEmpty()) client.showMessage(
            Warning,
            "Some files (${nonModularizedFiles.first()}, ...) are missing a module-info.json",
        ) else client.showMessage(
            Warning,
            "No module definitions found for $path. Consider creating module-info.json file(s). The LSP will stil work",
        )

        val conventionModulePaths = loadConventionModules(nonModularizedFiles)

        nonModularizedFiles.removeAll(conventionModulePaths.values.flatten())

        val conventionModules = conventionModulePaths.entries.map { (modulePathStr, sources) ->
            val modulePath = Path.of(modulePathStr)
            val sourceSetName = modulePath.parent.name.let {
                if (it == "main") null else it
            }
            //               jitsu      main   src    <module>
            val moduleName = modulePath.parent.parent.parent.name
            WorkspaceModule(
                name = sourceSetName?.let { "$moduleName.$sourceSetName" } ?: moduleName,
                sourceRoot = modulePath.toUri(),
                files = sources.map { toSourceFile(it) }.toMutableList()
            )
        }

        val arbitraryModule = if (nonModularizedFiles.isNotEmpty()) createUndefinedModule(path, nonModularizedFiles)
        else null

        val allModules = (explicitModules + conventionModules).let {
            if (arbitraryModule != null) it + arbitraryModule else it
        }
        return Workspace(
            uri = path.toUri(),
            modules = allModules.toMutableList()
        )
    }

    /**
     * Tries to create modules by the "src/<sourceset>/jitsu" pattern
     *
     * @return a map where keys are paths to sourcesets and values the sourcefiles within
     */
    private fun loadConventionModules(nonModularizedFiles: List<Path>): Map<String, List<Path>> {
        val conventionModulePaths =
            nonModularizedFiles.map { it to sourceSetRegex.find(it.parent.absolutePathString()) }
                .filter { it.second != null }
                .groupBy({ it.first.absolutePathString().substring(0, it.second!!.range.last + 1) }, { it.first })
        return conventionModulePaths
    }

    /**
     * Explicitly defined modules are one that have an associated module-info.json file. This file can be generated by grade
     * @return first: explicit modules, second: jitsu source files not associated with any module
     */
    private fun loadExplicitlyDefinedModules(path: Path): Pair<MutableList<WorkspaceModule>, MutableList<Path>> {
        val moduleInfos = walk(path).asSequence()
            .filter { it.name.split(".").first() == "module-info" && it.extension == "json" }
            .map { ModuleInfo.from(it) }
            .toList()
        val moduleStack = Stack<Pair<Path, WorkspaceModule>>()
        val explicitModules = mutableListOf<WorkspaceModule>()
        val nonModularizedFiles = mutableListOf<Path>()
        Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (file.extension == "jit") {
                    if (moduleStack.empty()) nonModularizedFiles.add(file)
                    else moduleStack.peek()?.second?.addFile(toSourceFile(file))
                }
                return FileVisitResult.CONTINUE
            }

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val module = moduleInfos.firstOrNull { it.sourceRoot.isSameFileAs(dir) }
                if (module != null) {
                    moduleStack.push(
                        dir to WorkspaceModule(
                            name = module.name,
                            sourceRoot = module.sourceRoot.toUri(),
                        )
                    )
                }
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (moduleStack.isNotEmpty() && moduleStack.peek().first.isSameFileAs(dir)) explicitModules.add(
                    moduleStack.pop().second
                )
                return FileVisitResult.CONTINUE
            }
        })
        return Pair(explicitModules, nonModularizedFiles)
    }

    private fun createUndefinedModule(
        path: Path,
        nonModularizedFiles: MutableList<Path>
    ): WorkspaceModule = WorkspaceModule(
        name = path.name,
        sourceRoot = path.toUri(),
        files = nonModularizedFiles.map {
            toSourceFile(it)
        }.toMutableList()
    )

    private fun toSourceFile(path: Path): SourceFile = SourceFile(
        uri = path.toUri(),
        content = path.readText()
    )

    private fun getReadablePath(uri: URI, client: LanguageClient): Path? = try {
        val path = Paths.get(uri)
        val readable = path.isReadable()
        if (!path.exists()) {
            client.showMessage(
                MessageParams(
                    MessageType.Error,
                    "Cannot read workspace! Does jitsu run remotely?",
                )
            )
            null
        } else if (!readable) {
            client.showMessage(
                MessageParams(
                    MessageType.Error,
                    "Cannot read workspace! Permissions might be wrong",
                )
            )
            null
        } else path
    } catch (e: Throwable) {
        val ex = when (e) {
            is IOException, is IllegalArgumentException, is FileSystemNotFoundException -> e
            else -> throw e
        }
        client.showMessage(
            MessageParams(
                MessageType.Error,
                "Cannot read workspace! Does jitsu run remotely? Error: $ex",
            )
        )
        null
    }
}

private fun LanguageClient.requestGradlePermission(): CompletableFuture<Boolean> {
    val YES_REMEMBER = "Yes, remember choice"
    val YES = "Yes"
    val NO = "No"
    val result = askUser(
        MessageType.Info,
        "Gradle files found in project. Do you want 'gradle :$GRADLE_MODULE_TASK' to be executed?",
        listOf(YES, YES_REMEMBER, NO)
    )
    return result.thenApply {
        when (it) {
            YES -> true
            NO -> false
            YES_REMEMBER -> {
                val sysSettings = JitsuLanguageServerSettings.loadSystemSettings()
                val newSettings = sysSettings.copy(allowGradle = true)
                newSettings.saveSystemSettings {
                    showMessage(Warning, "Saving preference 'allowGradle' failed: $it")
                }
                true
            }

            else -> throw IllegalStateException("Unexpected result: $result")
        }
    }
}

