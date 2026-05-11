package eu.nitok.jitsu.parser.tokenization

import com.niton.jainparse.token.DefaultToken
import com.niton.jainparse.token.TokenStream
import java.net.URI

class FileTokenStream(
    val file: URI,
    private val tokens: TokenStream<DefaultToken>
): TokenStream<DefaultToken> by tokens