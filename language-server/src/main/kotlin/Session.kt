data class Session(
    var initialized: Boolean = false,
    val fileService: JitsuFileService,
    val workspaceService: JitsuWorkspaceService
)
