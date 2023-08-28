package eu.nitok.jitsu.playground

import com.niton.parser.exceptions.ParsingException
import eu.nitok.jitsu.compiler.parser.parse
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.title
import kotlinx.html.unsafe

fun main() {

    embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                call.respondHtml {
                    head {
                        title("Jitsu Playground")
                        unsafe { +"<script src=\"https://unpkg.com/htmx.org@1.9.5\" integrity=\"sha384-xcuj3WpfgjlKF+FXhSQFQ0ZNr39ln+hwjN3npfM9VBnUskLolQAcN80McRIVOPuO\" crossorigin=\"anonymous\"></script>" }
                    }
                    body {
                        unsafe { +mainPage() }
                    }
                }
            }
            post("/parse") {
                val code = call.receiveParameters()["code"]!!
                println("Parsing: $code")
                val ast = try {
                    "<pre>${
                        parse(code).format()
                            .replace("<","&lt")
                            .replace(">","&gt")
                            .replace("\n", "<br/>")
                    }</pre>"
                } catch (e: ParsingException) {
                    val error = e.mostProminentDeepException;
                    error.printStackTrace()
                    """Error: <pre>${
                        error.markInText(code, 2)
                        .replace("<","&lt")
                        .replace(">","&gt")
                    }</pre>
                    <h2>Detailed</h2>
                    Stack strace:
                    <pre>${error.stackTraceToString()}</pre>
                    """
                } catch (e: Exception) {
                    e.printStackTrace()
                    "Error: ${e.message}".replace("\n", "<br/>")
                }
                call.respond(ast)
            }
        }
    }.start(wait = true)
}

fun mainPage(): String {
    return """
        <h1>Jitsu Playground</h1>
        <p>Enter your code here:</p>
        <h2>Examples:</h2>
        <pre>
            fn main() {
                var a : String;
                var b = 1;
                fn c() {
                    return x + 1;
                }
                return c();
            }
        </pre>
        <pre>
        const a = {
           var cfg = loadConfig();
           var url = url_from(cfg);
           yield mysql_connect(url);
        };
        const answer = db.query(42);
        </pre>
        <pre>
        var a : Result;
        const b = switch (a) {
            case Ok ok-> 1;
            case Err(code) -> panic();
        }
        </pre>
        <textarea name='code' hx-post="/parse" hx-target='#ast' hx-trigger='change, click, load, type' hx-swap='innerHTML' rows="20" cols='80'></textarea>
        <div id='ast'></div>
    """
}
