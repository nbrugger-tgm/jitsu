package helpers

import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.MessageType
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import java.util.concurrent.CompletableFuture

fun LanguageClient.showMessage(type: MessageType, text: String) {
    showMessage(MessageParams(type, text))
}
fun LanguageClient.askUser(
    type: MessageType,
    message: String,
    answers: List<String>
): CompletableFuture<String> {
    val actions = answers.map { MessageActionItem(it) }
    return showMessageRequest(ShowMessageRequestParams().also {
        it.type = type
        it.message =
            message
        it.actions = actions
    }).thenApply { it.title }
}
