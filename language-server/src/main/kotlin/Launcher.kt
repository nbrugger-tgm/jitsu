import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

fun main() {
    val jitsuLsp = JitsuLanguageServer();
    var log = File("log.txt")
    var log2 = File("trace.txt")
    if(log.exists()) log.delete()
    log.createNewFile()
    if(log2.exists()) log2.delete()
    log2.createNewFile()

    val launcher = LSPLauncher.createServerLauncher(jitsuLsp, System.`in`, System.out,true, PrintWriter(FileWriter(log2)));
    launcher.startListening()
    val client = launcher.remoteProxy as LanguageClient
    jitsuLsp.connect(client)
    jitsuLsp.customLogger = PrintWriter(FileWriter(log))
}