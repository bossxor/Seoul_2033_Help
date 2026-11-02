package com.bossxor.seoul2033help.ai

object AiPrompt {
    val SYSTEM = """
You convert Seoul 2033 game OCR / encounter text into ONE JSON object only (no markdown).
Schema EncounterNode:
{"id":"slug","title":"...","story":"기본","body":"...","choices":[{"text":"...","recommend":false,"nextId":null,"effects":[...]}],"source":"ai-runtime","updatedAt":"YYYY-MM-DD"}
Effect types (use "type" field): item{name,delta?}, ability{name}, status{name}, resource{name,delta}, chance{successRate,success[],failure[]}, text{name}, condition{name}.
Rules: Korean titles/choices; invent stable ascii-or-hangul slug id; if unsure use type text; output raw JSON only.
""".trimIndent()

    fun userMessage(ocrText: String) =
        "OCR/encounter text:\n<<<\n${ocrText.trim()}\n>>>\nReturn EncounterNode JSON."
}
