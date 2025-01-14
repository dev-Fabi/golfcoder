package org.golfcoder.tokenizer

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import org.golfcoder.Sysinfo
import org.golfcoder.httpClient
import java.net.ConnectException

class TreeSitterTokenizer(private val language: String, override val tokenizerVersion: Int) : Tokenizer {

    @Serializable
    private class TreeSitterRequest(val language: String, val code: String)

    @Serializable
    private class TreeSitterResponse(val tokens: List<Token>) {
        @Serializable
        class Token(
            val startRow: Int,
            val startColumn: Int,
            val endRow: Int,
            val endColumn: Int,
            val type: String,
            val text: String,
        )
    }

    override suspend fun tokenize(input: String): List<Tokenizer.Token> {
        val response = try {
            httpClient.post("http://localhost:8031/tokenize") {
                contentType(ContentType.Application.Json)
                setBody(TreeSitterRequest(language, input))
            }
        } catch (e: ConnectException) {
            if (Sysinfo.isLocal) { // Improve local development UX
                throw Exception("tree-sitter-server not running, start it via `npm start`.", e)
            }
            throw e
        }

        val body = response.body<TreeSitterResponse>()

        return body.tokens.map { treeSitterToken ->
            Tokenizer.Token(
                Tokenizer.Token.Position(treeSitterToken.startRow, treeSitterToken.startColumn)..
                        Tokenizer.Token.Position(treeSitterToken.endRow, treeSitterToken.endColumn),
                treeSitterToken.text, // Some whitespaces (and comments?) might be missing. We could combine them using start/end position in case we add a visual feedback for the user.
                treeSitterToken.type.lowercase().let { type ->
                    when {
                        "error" in type -> throw Exception("Syntax error on line ${treeSitterToken.startRow}:${treeSitterToken.startColumn}:\n${treeSitterToken.text}")
                        "comment" in type -> Tokenizer.Token.Type.COMMENT
                        "string" in type -> Tokenizer.Token.Type.STRING // e.g. Python reports "string_content", Javascript "string_fragment".
                        "character" in type -> Tokenizer.Token.Type.STRING // Kotlin reports "character_literal" for e.g. 'a'.
                        else -> Tokenizer.Token.Type.CODE_TOKEN
                    }
                }
            )
        }
    }
}