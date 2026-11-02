# Runtime AI Cache Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** On overlay match miss, optionally call an OpenAI-compatible API to structure OCR text into an EncounterNode, append to device LocalCache (cache wins on id), and allow export.

**Architecture:** EncounterStore = Seed ∪ LocalCache. AiDecoder uses HttpURLConnection Chat Completions. Manual `AI 해독` on Miss only when API key set.

**Tech Stack:** Kotlin, kotlinx.serialization, DataStore, HttpURLConnection (no OkHttp).

## Global Constraints

- Same EncounterNode / Effect schema as seed (`type` discriminator).
- No cloud sync of cache; key stays on device.
- Empty API key = AI disabled.
- Default base `https://api.openai.com/v1`, model `gpt-4o-mini`.

---

### Task 1: LocalCacheStore + EncounterStore

**Files:**
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/data/LocalCacheStore.kt`
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/data/EncounterStore.kt`
- Test: `android/app/src/test/java/com/bossxor/seoul2033help/data/EncounterStoreTest.kt`

- [ ] Implement file-backed cache + merge (cache id wins)
- [ ] Unit tests without Android Context (in-memory path via interface or temp file)
- [ ] Commit

### Task 2: AiDecoder

**Files:**
- Create: `android/.../ai/AiPrompt.kt`, `AiDecoder.kt`
- Test: `AiDecoderTest.kt`

- [ ] Strip fences, decode EncounterNode
- [ ] HTTP chat completions
- [ ] Commit

### Task 3: Settings + Manifest

- [ ] UserSettings ai fields + SettingsScreen + INTERNET
- [ ] Export share button
- [ ] Commit

### Task 4: Overlay Miss + wire

- [ ] `Miss(ocrText)`, AI button, loading, append
- [ ] App uses EncounterStore
- [ ] Commit

### Task 5: README + verify + push

- [ ] Docs, tests, assemble, push main
