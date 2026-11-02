package com.bossxor.seoul2033help.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDecoderTest {
    private val sampleNodeJson = """
        {
          "id": "test-mushroom",
          "title": "특이한 버섯",
          "story": "기본",
          "body": "버섯이 있다",
          "choices": [
            {
              "text": "먹는다",
              "recommend": false,
              "nextId": null,
              "effects": [{ "type": "resource", "name": "멘탈", "delta": -1 }]
            }
          ],
          "source": "ai-runtime",
          "updatedAt": "2026-11-03"
        }
    """.trimIndent()

    @Test
    fun stripsFencesAndParses() {
        val decoder = AiDecoder(apiKey = "x", httpPost = { _, _, _ -> error("no http") })
        val node = decoder.parseNode("```json\n$sampleNodeJson\n```")
        assertEquals("test-mushroom", node.id)
        assertEquals("특이한 버섯", node.title)
        assertEquals(1, node.choices.size)
    }

    @Test
    fun extractContentFromChatResponse() {
        val decoder = AiDecoder(apiKey = "x", httpPost = { _, _, _ -> error("no") })
        val escaped = sampleNodeJson.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val resp = """{"choices":[{"message":{"role":"assistant","content":"$escaped"}}]}"""
        val content = decoder.extractAssistantContent(resp)
        val node = decoder.parseNode(content)
        assertEquals("test-mushroom", node.id)
    }

    @Test
    fun decodeOcrUsesHttpHook() {
        val escaped = sampleNodeJson.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val resp = """{"choices":[{"message":{"role":"assistant","content":"$escaped"}}]}"""
        val decoder = AiDecoder(apiKey = "sk-test", httpPost = { url, headers, body ->
            assertTrue(url.endsWith("/chat/completions"))
            assertTrue(headers["Authorization"]!!.startsWith("Bearer "))
            assertTrue(body.contains("OCR"))
            resp
        })
        val node = decoder.decodeOcr("특이한 버섯\n먹는다").getOrThrow()
        assertEquals("test-mushroom", node.id)
    }

    @Test
    fun emptyKeyFails() {
        val r = AiDecoder(apiKey = "", httpPost = { _, _, _ -> "" }).decodeOcr("x")
        assertTrue(r.isFailure)
    }
}
