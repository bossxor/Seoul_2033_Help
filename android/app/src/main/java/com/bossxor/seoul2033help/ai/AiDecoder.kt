package com.bossxor.seoul2033help.ai

import com.bossxor.seoul2033help.data.model.EncounterNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class AiDecoder(
    private val apiKey: String,
    private val baseUrl: String = DEFAULT_BASE,
    private val model: String = DEFAULT_MODEL,
    private val httpPost: (url: String, headers: Map<String, String>, body: String) -> String = ::defaultHttpPost
) {
    private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type"; isLenient = true }

    fun decodeOcr(ocrText: String): Result<EncounterNode> = runCatching {
        require(apiKey.isNotBlank()) { "API key empty" }
        require(ocrText.isNotBlank()) { "OCR text empty" }
        val root = buildJsonObject {
            put("model", model)
            put("temperature", 0.2)
            put(
                "messages",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("role", "system")
                            put("content", AiPrompt.SYSTEM)
                        },
                        buildJsonObject {
                            put("role", "user")
                            put("content", AiPrompt.userMessage(ocrText))
                        }
                    )
                )
            )
        }
        val endpoint = baseUrl.trimEnd('/') + "/chat/completions"
        val raw = httpPost(
            endpoint,
            mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json"
            ),
            root.toString()
        )
        val content = extractAssistantContent(raw)
        parseNode(content)
    }

    fun parseNode(assistantText: String): EncounterNode {
        val cleaned = stripFences(assistantText).trim()
        return json.decodeFromString(EncounterNode.serializer(), cleaned)
    }

    fun extractAssistantContent(responseBody: String): String {
        val obj = json.parseToJsonElement(responseBody).jsonObject
        val choices = obj["choices"]?.jsonArray ?: error("no choices")
        val msg = choices.first().jsonObject["message"]?.jsonObject
            ?: error("no message")
        return msg["content"]?.jsonPrimitive?.contentOrNull
            ?: error("no content")
    }

    companion object {
        const val DEFAULT_BASE = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "gpt-4o-mini"

        fun stripFences(s: String): String {
            var t = s.trim()
            val fence = Regex("^```(?:json)?\\s*", RegexOption.IGNORE_CASE)
            t = fence.replaceFirst(t, "")
            if (t.endsWith("```")) t = t.removeSuffix("```")
            return t.trim()
        }

        fun defaultHttpPost(url: String, headers: Map<String, String>, body: String): String {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 30_000
                readTimeout = 60_000
                doOutput = true
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
            if (code !in 200..299) error("HTTP $code: ${text.take(300)}")
            return text
        }
    }
}
