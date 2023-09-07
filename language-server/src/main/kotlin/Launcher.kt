import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import java.io.*
import java.net.ServerSocket
const val PORT = 5007;
var log = File("log.txt")
lateinit var customLogger: PrintStream;
fun main(vararg args: String) {
    val log2 = File("trace.txt")
    if (log.exists()) log.delete()
    log.createNewFile()
    if (log2.exists()) log2.delete()
    log2.createNewFile()

    if (args.isEmpty() || args[0] == "stdio") {
        customLogger = PrintStream(log);
        startServer(System.`in`, System.out, log2);
    } else if (args[0] == "tcp") {
        customLogger = System.out;
        val ss = ServerSocket(PORT)
        while (true){
            println("Waiting for connection on 0.0.0.0:${PORT}")
            val connection = ss.accept();
            println("Language client connected")
            startServer(connection.getInputStream(), connection.getOutputStream(), log2);
        }
    } else {
        println("${args[0]} is not a valid run mode. The run modes are 'stdio' and 'tcp'")
    }
}

private fun startServer(`in`: InputStream, out: OutputStream, log2: File) {
    val jitsuLsp = JitsuLanguageServer();
    val launcher = LSPLauncher.createServerLauncher(jitsuLsp, `in`, out, true, PrintWriter(FileWriter(log2)));
    launcher.startListening()
    val client = launcher.remoteProxy as LanguageClient
    jitsuLsp.connect(client)
}