import org.eclipse.lsp4j.launch.LSPLauncher
import java.io.*
import java.net.ServerSocket
import java.util.concurrent.Future
import kotlin.system.exitProcess

const val PORT = 5007;
var log = File("log.txt")
private lateinit var customLogger: PrintStream;
fun main(vararg args: String) {
    val trace = File("trace.txt")
    if (log.exists()) log.delete()
    log.createNewFile()
    if (trace.exists()) trace.delete()
    trace.createNewFile()

    if (args.isEmpty() || args[0] == "--tcp") {
        customLogger = System.out;
        val server = ServerSocket(PORT)
        if(args.isEmpty()) customLogger.println("Starting in TCP server mode. To start as stdio use '--stdio'")
        customLogger.println("Waiting for connections on 0.0.0.0:${PORT}")
        while (true) {
            val connection = server.accept();
            customLogger.println("Language client connected")
            startServer(connection.getInputStream(), connection.getOutputStream(), trace) {
                customLogger.println("Server runs in TCP mode. In TCP mode exit closes the TCP connection to the server rather than stopping the server process")
                connection.close()
            }
        }
    } else if (args[0] == "--stdio") {
        customLogger = PrintStream(log);
        startServer(System.`in`, System.out, trace) {
            exitProcess(0);
        }
    } else {
        println("${args[0]} is not a valid run mode. The run modes are '--stdio' and '--tcp'")
    }
}

private var clientID = 0L;
private fun startServer(`in`: InputStream, out: OutputStream, trace: File,onExit: () -> Unit) {
    clientID++
    class TCPProcess(var listener: Future<Void>?)
    val process = TCPProcess(null)
    val jitsuLsp = JitsuLanguageServer(customLogger, clientID) { process.listener?.cancel(true); onExit() }
    val launcher = LSPLauncher.createServerLauncher(jitsuLsp, `in`, out, true, PrintWriter(FileWriter(trace)));
    val client = launcher.remoteProxy
    jitsuLsp.connect(client)
    process.listener = launcher.startListening()
}