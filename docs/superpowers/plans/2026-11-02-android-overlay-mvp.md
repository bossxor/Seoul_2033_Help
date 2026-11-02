# Seoul 2033 Help Android Overlay MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Android 앱에서 시드 인카운터를 검색·표시하고, 게임 위 오버레이로 OCR(크롭)·매칭·선택지/결과(확률·추천 테두리)·투명도·닫기=종료까지 동작하게 한다.

**Architecture:** `data/encounters.json` 시드를 앱 assets로 로드한다. 순수 Kotlin 도메인(정규화·fuzzy 매칭·Effect 포맷)은 JVM 단위 테스트한다. Compose 전체화면(검색/상세)과 `OverlayService`(SYSTEM_ALERT_WINDOW + MediaProjection + ML Kit OCR)가 같은 `EncounterRepository`를 쓴다. 런타임 AI·Web은 이 플랜 밖(후속 플랜).

**Tech Stack:** Kotlin 2.x, Jetpack Compose, Material3, kotlinx.serialization, DataStore Preferences, ML Kit Text Recognition (Korean), JUnit4/5 for unit tests, Android Gradle Plugin 8.x, minSdk 26, targetSdk 35.

**Spec:** `docs/superpowers/specs/2026-11-02-seoul2033-help-overlay-design.md`

## Global Constraints

- Android 우선. Web·런타임 AI·클라우드·조건 필터·추천 프리셋·비주얼 스킨 폴리시 = 이 플랜에서 구현하지 않음
- 오버레이는 읽기 위주. 재OCR: 수동 `인식` 항상 + 자동 주기 옵션(기본 OFF)
- 추천 ON 시 추천 선택지는 **다른 border 색** (구체 hex는 임시 값 OK, 스킨 후속)
- 닫기 = 오버레이+서비스+액티비티 정리 후 **앱 프로세스 종료**
- 시드 ∪ (나중 캐시) 조회 시 캐시 우선 — 캐시 구현은 후속; 이번엔 시드만
- 개인·비상업, 출처 표기 문자열은 설정/정보 화면에 한 줄 넣음
- YAGNI: 오버레이에서 선택지 탭으로 트리 진행 = 안 함

## Scope split

| 이 플랜 (MVP) | 후속 플랜 |
|---------------|-----------|
| 스키마·샘플 시드·매칭·포맷터 | 위키 대량 시드 파이프라인 |
| Android 검색/상세/오버레이/OCR | 런타임 AI + LocalCache export |
| | Web 검색 답지 |

## File structure

```
data/encounters.json                          # git 시드 (소스 오브 트루스)
android/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/assets/encounters.json         # data/ 복사본 (빌드 전 동기)
  app/src/main/java/com/bossxor/seoul2033help/
    Seoul2033HelpApp.kt
    MainActivity.kt
    data/model/Models.kt
    data/EncounterRepository.kt
    data/UserSettings.kt
    domain/TextNormalizer.kt
    domain/EncounterMatcher.kt
    domain/EffectFormatter.kt
    domain/MatchResult.kt
    ui/AppNav.kt
    ui/search/SearchScreen.kt
    ui/detail/NodeDetailScreen.kt
    ui/detail/ChoiceRow.kt
    ui/overlay/OverlayService.kt
    ui/overlay/OverlayController.kt
    ui/settings/SettingsScreen.kt
    ocr/OcrCropper.kt
    ocr/ScreenOcrEngine.kt
  app/src/test/java/com/bossxor/seoul2033help/
    domain/TextNormalizerTest.kt
    domain/EncounterMatcherTest.kt
    domain/EffectFormatterTest.kt
    data/EncounterRepositoryTest.kt
```

---

### Task 1: Encounter models + sample seed JSON

**Files:**
- Create: `data/encounters.json`
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/data/model/Models.kt`
- Create: `android/app/src/test/java/com/bossxor/seoul2033help/data/EncounterJsonTest.kt` (after gradle exists in Task 4; for Task 1 keep JSON + model file only, tests run from Task 4)
- Test: parse covered in Task 4

**Interfaces:**
- Consumes: nothing
- Produces:
  - `@Serializable data class EncounterNode(id, title, story, body?, choices, source?, updatedAt?)`
  - `@Serializable data class Choice(text, effects, nextId?, recommend: Boolean = false)`
  - `@Serializable sealed` / polymorphic `Effect` with types: `item|ability|status|resource|chance|text|condition`
  - `@Serializable data class EncounterSeedFile(version: Int, nodes: List<EncounterNode>)`

- [ ] **Step 1: Write sample seed with 2 linked nodes (들개 무리 style)**

Create `data/encounters.json`:

```json
{
  "version": 1,
  "nodes": [
    {
      "id": "wild-dogs",
      "title": "들개 무리",
      "story": "기본",
      "body": "들개 무리를 만났다.",
      "source": "namu:기본 인카운터",
      "updatedAt": "2026-11-02",
      "choices": [
        {
          "text": "나무 위로 올라간다",
          "recommend": true,
          "nextId": null,
          "effects": [
            {
              "type": "chance",
              "successRate": 0.5,
              "success": [{ "type": "ability", "name": "날렵함" }],
              "failure": [{ "type": "resource", "name": "체력", "delta": -1 }]
            }
          ]
        },
        {
          "text": "풀숲 속에 숨는다",
          "recommend": false,
          "nextId": null,
          "effects": [
            {
              "type": "chance",
              "successRate": 0.5,
              "success": [{ "type": "ability", "name": "은신술" }],
              "failure": [{ "type": "resource", "name": "체력", "delta": -1 }]
            }
          ]
        },
        {
          "text": "총으로 쏜다",
          "recommend": false,
          "nextId": null,
          "effects": [
            { "type": "condition", "name": "권총 보유" },
            {
              "type": "chance",
              "successRate": 0.5,
              "success": [{ "type": "ability", "name": "사격술" }],
              "failure": [{ "type": "resource", "name": "체력", "delta": -1 }]
            }
          ]
        },
        {
          "text": "지나친다",
          "recommend": false,
          "nextId": "wild-dogs-pass",
          "effects": [{ "type": "text", "name": "지나감" }]
        }
      ]
    },
    {
      "id": "wild-dogs-pass",
      "title": "들개 무리 (지나침)",
      "story": "기본",
      "body": "조용히 지나쳤다.",
      "choices": [],
      "source": "namu:기본 인카운터",
      "updatedAt": "2026-11-02"
    }
  ]
}
```

- [ ] **Step 2: Write Kotlin models**

Create `Models.kt`:

```kotlin
package com.bossxor.seoul2033help.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EncounterSeedFile(
    val version: Int,
    val nodes: List<EncounterNode>
)

@Serializable
data class EncounterNode(
    val id: String,
    val title: String,
    val story: String,
    val body: String? = null,
    val choices: List<Choice> = emptyList(),
    val source: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class Choice(
    val text: String,
    val effects: List<Effect> = emptyList(),
    val nextId: String? = null,
    val recommend: Boolean = false
)

@Serializable
sealed class Effect {
    @Serializable
    @SerialName("item")
    data class Item(val name: String, val delta: Int? = null) : Effect()

    @Serializable
    @SerialName("ability")
    data class Ability(val name: String) : Effect()

    @Serializable
    @SerialName("status")
    data class Status(val name: String) : Effect()

    @Serializable
    @SerialName("resource")
    data class Resource(val name: String, val delta: Int) : Effect()

    @Serializable
    @SerialName("chance")
    data class Chance(
        val successRate: Double,
        val success: List<Effect> = emptyList(),
        val failure: List<Effect> = emptyList()
    ) : Effect()

    @Serializable
    @SerialName("text")
    data class Text(val name: String) : Effect()

    @Serializable
    @SerialName("condition")
    data class Condition(val name: String) : Effect()
}
```

- [ ] **Step 3: Commit**

```bash
git add data/encounters.json android/app/src/main/java/com/bossxor/seoul2033help/data/model/Models.kt
git commit -m "feat: encounter seed schema and sample nodes"
```

Note: if `android/` paths do not exist yet, commit `data/encounters.json` + place `Models.kt` under a temp path `android/app/...` created as empty dirs, or commit models in Task 4 together. Prefer creating the directory tree now so the file path exists.

---

### Task 2: TextNormalizer + EncounterMatcher (TDD)

**Files:**
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/domain/TextNormalizer.kt`
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/domain/MatchResult.kt`
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/domain/EncounterMatcher.kt`
- Test: `android/app/src/test/java/com/bossxor/seoul2033help/domain/TextNormalizerTest.kt`
- Test: `android/app/src/test/java/com/bossxor/seoul2033help/domain/EncounterMatcherTest.kt`

**Interfaces:**
- Consumes: `EncounterNode`, `Choice` from Task 1
- Produces:
  - `object TextNormalizer { fun normalize(s: String): String }`
  - `data class ScoredNode(val node: EncounterNode, val score: Double)`
  - `sealed class MatchOutcome { data class Clear(val node, val score); data class Ambiguous(val candidates: List<ScoredNode>); data object Miss }`
  - `class EncounterMatcher(nodes: List<EncounterNode>) { fun match(query: String): MatchOutcome }`
  - Constants: `MISS_THRESHOLD = 0.35`, `CLEAR_MARGIN = 0.08`, `MAX_CANDIDATES = 3`

- [ ] **Step 1: Write failing TextNormalizer tests**

```kotlin
package com.bossxor.seoul2033help.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {
    @Test
    fun stripsSpacesAndPunctuation() {
        assertEquals("나무위로올라간다", TextNormalizer.normalize("나무 위로 올라간다!"))
    }

    @Test
    fun lowercasesAsciiOnlyKeepsHangul() {
        assertEquals("총으로쏜다", TextNormalizer.normalize("총으로 쏜다"))
    }
}
```

- [ ] **Step 2: Run tests — expect fail**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests com.bossxor.seoul2033help.domain.TextNormalizerTest`

Expected: FAIL (class missing) — if gradle not ready, complete Task 4 scaffold first then return. **Order note:** If Android project does not exist, do Task 4 Steps 1–3 (gradle scaffold only), then resume Task 2.

- [ ] **Step 3: Implement TextNormalizer**

```kotlin
package com.bossxor.seoul2033help.domain

object TextNormalizer {
    private val keep = Regex("[\\p{L}\\p{N}]+")

    fun normalize(s: String): String =
        keep.findAll(s.lowercase())
            .joinToString("") { it.value }
}
```

- [ ] **Step 4: Write EncounterMatcher tests**

```kotlin
package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.Choice
import com.bossxor.seoul2033help.data.model.EncounterNode
import org.junit.Assert.*
import org.junit.Test

class EncounterMatcherTest {
    private val dogs = EncounterNode(
        id = "wild-dogs",
        title = "들개 무리",
        story = "기본",
        choices = listOf(
            Choice("나무 위로 올라간다"),
            Choice("풀숲 속에 숨는다"),
            Choice("지나친다")
        )
    )
    private val other = EncounterNode(
        id = "library",
        title = "전쟁 전에 짓다 만 도서관",
        story = "기본",
        choices = listOf(Choice("책을 고른다"), Choice("지나친다"))
    )
    private val matcher = EncounterMatcher(listOf(dogs, other))

    @Test
    fun clearMatchOnDistinctChoice() {
        val r = matcher.match("나무 위로 올라간다")
        assertTrue(r is MatchOutcome.Clear)
        assertEquals("wild-dogs", (r as MatchOutcome.Clear).node.id)
    }

    @Test
    fun ambiguousWhenSharedChoice() {
        val r = matcher.match("지나친다")
        assertTrue(r is MatchOutcome.Ambiguous)
        assertTrue((r as MatchOutcome.Ambiguous).candidates.size >= 2)
    }

    @Test
    fun missOnGarbage() {
        assertEquals(MatchOutcome.Miss, matcher.match("asdfqwerzxcv"))
    }
}
```

- [ ] **Step 5: Implement MatchResult + EncounterMatcher**

```kotlin
package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.EncounterNode
import kotlin.math.max

data class ScoredNode(val node: EncounterNode, val score: Double)

sealed class MatchOutcome {
    data class Clear(val node: EncounterNode, val score: Double) : MatchOutcome()
    data class Ambiguous(val candidates: List<ScoredNode>) : MatchOutcome()
    data object Miss : MatchOutcome()
}

class EncounterMatcher(
    private val nodes: List<EncounterNode>,
    private val missThreshold: Double = 0.35,
    private val clearMargin: Double = 0.08,
    private val maxCandidates: Int = 3
) {
    fun match(query: String): MatchOutcome {
        val q = TextNormalizer.normalize(query)
        if (q.isEmpty()) return MatchOutcome.Miss

        val scored = nodes.map { node ->
            val title = TextNormalizer.normalize(node.title)
            val choiceScores = node.choices.map { c ->
                similarity(q, TextNormalizer.normalize(c.text))
            }
            val bestChoice = choiceScores.maxOrNull() ?: 0.0
            val titleScore = similarity(q, title)
            ScoredNode(node, max(bestChoice, titleScore * 0.9))
        }.sortedByDescending { it.score }

        val top = scored.firstOrNull() ?: return MatchOutcome.Miss
        if (top.score < missThreshold) return MatchOutcome.Miss

        val second = scored.getOrNull(1)
        val ambiguous = second != null && (top.score - second.score) < clearMargin
        return if (ambiguous) {
            MatchOutcome.Ambiguous(scored.take(maxCandidates).filter { it.score >= missThreshold })
        } else {
            MatchOutcome.Clear(top.node, top.score)
        }
    }

    /** Dice coefficient on bigrams; 0..1 */
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        if (a in b || b in a) return 0.85.coerceAtMost(1.0)
        fun bigrams(s: String): List<String> =
            if (s.length < 2) listOf(s) else s.windowed(2)
        val ba = bigrams(a)
        val bb = bigrams(b)
        val overlap = ba.count { it in bb }
        return (2.0 * overlap) / (ba.size + bb.size)
    }
}
```

- [ ] **Step 6: Run unit tests — expect PASS**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests "com.bossxor.seoul2033help.domain.*"`

Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add android/app/src/main/java/com/bossxor/seoul2033help/domain \
        android/app/src/test/java/com/bossxor/seoul2033help/domain
git commit -m "feat: normalize text and fuzzy encounter matcher"
```

---

### Task 3: EffectFormatter (TDD)

**Files:**
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/domain/EffectFormatter.kt`
- Test: `android/app/src/test/java/com/bossxor/seoul2033help/domain/EffectFormatterTest.kt`

**Interfaces:**
- Consumes: `Effect`
- Produces: `object EffectFormatter { fun format(effect: Effect): String; fun formatAll(effects: List<Effect>): String }`
- Chance must include percent, e.g. `44% 성공: … / 실패: …`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.Effect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EffectFormatterTest {
    @Test
    fun formatsChanceWithPercent() {
        val e = Effect.Chance(
            successRate = 0.44,
            success = listOf(Effect.Ability("날렵함")),
            failure = listOf(Effect.Resource("체력", -1))
        )
        val s = EffectFormatter.format(e)
        assertTrue(s.contains("44%"))
        assertTrue(s.contains("날렵함"))
        assertTrue(s.contains("체력"))
    }

    @Test
    fun formatsItem() {
        assertEquals("아이템: 상비약", EffectFormatter.format(Effect.Item("상비약")))
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

Run: `cd android && ./gradlew :app:testDebugUnitTest --tests com.bossxor.seoul2033help.domain.EffectFormatterTest`

- [ ] **Step 3: Implement**

```kotlin
package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.Effect
import kotlin.math.roundToInt

object EffectFormatter {
    fun formatAll(effects: List<Effect>): String =
        effects.joinToString(" · ") { format(it) }

    fun format(effect: Effect): String = when (effect) {
        is Effect.Item -> "아이템: ${effect.name}" + (effect.delta?.let { " ($it)" } ?: "")
        is Effect.Ability -> "능력: ${effect.name}"
        is Effect.Status -> "상태: ${effect.name}"
        is Effect.Resource -> "${effect.name}: ${effect.delta}"
        is Effect.Text -> effect.name
        is Effect.Condition -> "조건: ${effect.name}"
        is Effect.Chance -> {
            val pct = (effect.successRate * 100).roundToInt()
            val ok = formatAll(effect.success).ifEmpty { "-" }
            val bad = formatAll(effect.failure).ifEmpty { "-" }
            "${pct}% 성공: $ok / 실패: $bad"
        }
    }
}
```

- [ ] **Step 4: Run — expect PASS**

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main/java/com/bossxor/seoul2033help/domain/EffectFormatter.kt \
        android/app/src/test/java/com/bossxor/seoul2033help/domain/EffectFormatterTest.kt
git commit -m "feat: format encounter effects including chance"
```

---

### Task 4: Android project scaffold + EncounterRepository

**Files:**
- Create: `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle.properties`, `android/app/build.gradle.kts`, wrapper
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/Seoul2033HelpApp.kt`
- Create: `android/app/src/main/java/com/bossxor/seoul2033help/data/EncounterRepository.kt`
- Create: `android/app/src/main/assets/encounters.json` (copy from `data/encounters.json`)
- Test: `android/app/src/test/java/com/bossxor/seoul2033help/data/EncounterRepositoryTest.kt`

**Interfaces:**
- Consumes: `EncounterSeedFile`, models
- Produces:
  - `class EncounterRepository { fun loadFromJson(json: String): EncounterSeedFile; fun get(id: String): EncounterNode?; fun search(query: String): List<EncounterNode>; fun all(): List<EncounterNode> }`
  - Application class loads assets once into repository

- [ ] **Step 1: Create Android app module**

Use Android Studio "Empty Activity" or CLI. Package: `com.bossxor.seoul2033help`. minSdk 26. Enable Compose, kotlinx.serialization plugin.

`app/build.gradle.kts` dependencies (minimum):

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.bossxor.seoul2033help"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.bossxor.seoul2033help"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")
    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 2: Copy seed to assets**

```bash
cp data/encounters.json android/app/src/main/assets/encounters.json
```

- [ ] **Step 3: Implement EncounterRepository**

```kotlin
package com.bossxor.seoul2033help.data

import com.bossxor.seoul2033help.data.model.EncounterNode
import com.bossxor.seoul2033help.data.model.EncounterSeedFile
import com.bossxor.seoul2033help.domain.TextNormalizer
import kotlinx.serialization.json.Json

class EncounterRepository(
    private val seed: EncounterSeedFile
) {
    private val byId = seed.nodes.associateBy { it.id }

    fun all(): List<EncounterNode> = seed.nodes

    fun get(id: String): EncounterNode? = byId[id]

    fun search(query: String): List<EncounterNode> {
        val q = TextNormalizer.normalize(query)
        if (q.isEmpty()) return seed.nodes
        return seed.nodes.filter { node ->
            TextNormalizer.normalize(node.title).contains(q) ||
                TextNormalizer.normalize(node.story).contains(q) ||
                node.choices.any { TextNormalizer.normalize(it.text).contains(q) }
        }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

        fun fromJson(raw: String): EncounterRepository =
            EncounterRepository(json.decodeFromString(EncounterSeedFile.serializer(), raw))
    }
}
```

Polymorphic Effect decoding: ensure `Json { classDiscriminator = "type" }` matches `@SerialName` on Effect subclasses. If decode fails in test, switch to explicit `Json { serializersModule = ... }` — fix until `fromJson` loads sample seed.

- [ ] **Step 4: Unit test repository from classpath resource**

Put a copy of JSON under `android/app/src/test/resources/encounters.json` and:

```kotlin
package com.bossxor.seoul2033help.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class EncounterRepositoryTest {
    @Test
    fun loadsSampleSeed() {
        val raw = javaClass.classLoader!!
            .getResourceAsStream("encounters.json")!!
            .bufferedReader().readText()
        val repo = EncounterRepository.fromJson(raw)
        assertEquals(2, repo.all().size)
        assertNotNull(repo.get("wild-dogs"))
        assertEquals(1, repo.search("들개").size)
    }
}
```

- [ ] **Step 5: Run tests PASS, commit**

```bash
cd android && ./gradlew :app:testDebugUnitTest
git add android data/encounters.json
git commit -m "feat: android scaffold and encounter repository"
```

---

### Task 5: Fullscreen search + node detail (recommend border)

**Files:**
- Create: `MainActivity.kt`, `ui/AppNav.kt`, `ui/search/SearchScreen.kt`, `ui/detail/NodeDetailScreen.kt`, `ui/detail/ChoiceRow.kt`
- Modify: `AndroidManifest.xml` (MAIN activity)
- Modify: `Seoul2033HelpApp.kt` hold `EncounterRepository`

**Interfaces:**
- Consumes: `EncounterRepository`, `EffectFormatter`
- Produces: Nav routes `search`, `detail/{id}`, `settings`
- `ChoiceRow(choice, recommendHighlight: Boolean, borderColorRecommend, borderColorNormal)`

- [ ] **Step 1: Wire Application + MainActivity empty Compose host loading assets**

```kotlin
// Seoul2033HelpApp.kt
class Seoul2033HelpApp : Application() {
    lateinit var repository: EncounterRepository
        private set
    override fun onCreate() {
        super.onCreate()
        val raw = assets.open("encounters.json").bufferedReader().readText()
        repository = EncounterRepository.fromJson(raw)
    }
}
```

- [ ] **Step 2: Implement ChoiceRow with border**

```kotlin
@Composable
fun ChoiceRow(choice: Choice, showRecommend: Boolean) {
    val border = when {
        showRecommend && choice.recommend -> Color(0xFF2E7D32) // temp; skin later
        else -> Color(0xFF9E9E9E)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(choice.text, style = MaterialTheme.typography.titleMedium)
        if (choice.effects.isNotEmpty()) {
            Text(
                EffectFormatter.formatAll(choice.effects),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

- [ ] **Step 3: SearchScreen + NodeDetailScreen + Nav**

- Search: `OutlinedTextField` + `LazyColumn` of titles → navigate detail
- Detail: show `story` / `title` / `body`, all choices via `ChoiceRow`, `recommend` toggle (local remember + later DataStore)
- Footer Text: `출처: 나무위키 등 · 개인 비상업 용도`

- [ ] **Step 4: Manual run on emulator**

Run: `cd android && ./gradlew :app:installDebug`

Expected: search `들개` → open node → see chance text with `%` → recommend ON shows green border on first choice

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main
git commit -m "feat: fullscreen search and encounter detail with recommend borders"
```

---

### Task 6: UserSettings (opacity, auto OCR, crop, recommend)

**Files:**
- Create: `data/UserSettings.kt`
- Create: `ui/settings/SettingsScreen.kt`
- Modify: Nav to include settings

**Interfaces:**
- Consumes: DataStore
- Produces:
  - `data class UserSettings(overlayAlpha: Float = 0.85f, autoOcr: Boolean = false, autoOcrIntervalSec: Int = 3, cropBottomFraction: Float = 0.45f, showRecommend: Boolean = true)`
  - `class SettingsRepository(context) { val settings: Flow<UserSettings>; suspend fun update(...) }`
  - Clamp: `overlayAlpha in 0.4f..0.95f`, `cropBottomFraction in 0.25f..0.7f`

- [ ] **Step 1: Implement DataStore settings**

```kotlin
data class UserSettings(
    val overlayAlpha: Float = 0.85f,
    val autoOcr: Boolean = false,
    val autoOcrIntervalSec: Int = 3,
    val cropBottomFraction: Float = 0.45f,
    val showRecommend: Boolean = true
)
```

Persist with float/boolean/int preferences keys. Expose `Flow<UserSettings>`.

- [ ] **Step 2: Settings UI sliders/switches**

Labels: 투명도, 자동 OCR, 자동 간격(초), OCR 하단 비율, 추천 강조, 오버레이 시작 버튼(다음 태스크 연결)

- [ ] **Step 3: Commit**

```bash
git add android/app/src/main/java/com/bossxor/seoul2033help/data/UserSettings.kt \
        android/app/src/main/java/com/bossxor/seoul2033help/ui/settings
git commit -m "feat: persist overlay opacity crop and ocr settings"
```

---

### Task 7: Overlay service — panel, opacity, quit app

**Files:**
- Create: `ui/overlay/OverlayService.kt`, `ui/overlay/OverlayController.kt`
- Modify: `AndroidManifest.xml` — service, `SYSTEM_ALERT_WINDOW`, foreground service types
- Modify: Settings / Main — start overlay + request permission

**Interfaces:**
- Consumes: `UserSettings`, `EncounterRepository`, `MatchOutcome`
- Produces:
  - `OverlayService` shows ComposeView / WindowManager panel
  - Buttons: 인식, 검색(open MainActivity), 추천 토글, 투명도 슬라이더, 닫기
  - `fun quitApp()` stops service, finishes activities, `Process.killProcess(Process.myPid())` after cleanup
  - State: Idle / Matched(node, score) / Ambiguous(list) / Miss / OcrError

- [ ] **Step 1: Add manifest permissions + service**

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".ui.overlay.OverlayService"
    android:exported="false"
    android:foregroundServiceType="mediaProjection" />
```

- [ ] **Step 2: Implement OverlayService window**

Use `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, flag `FLAG_NOT_FOCUSABLE` optional — for sliders may need focusable when interacting.

Panel content (ComposeView):
- Title area: story + title OR candidate list OR `정보 없음`
- Choices with `ChoiceRow`
- Confidence text when Clear: `신뢰도 ${(score*100).toInt()}%`
- Controls as spec

- [ ] **Step 3: Implement quit**

```kotlin
fun quitApp() {
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
    // send broadcast or use Activity list
    Process.killProcess(Process.myPid())
}
```

Also `exitProcess(0)` after kill is redundant; ensure MediaProjection stopped first (Task 8).

- [ ] **Step 4: Manual test**

Grant overlay permission → start service → change opacity → press 닫기 → app process gone (recents cleared / not in foreground).

- [ ] **Step 5: Commit**

```bash
git add android/app/src/main
git commit -m "feat: overlay panel with opacity and full quit"
```

---

### Task 8: OCR crop + MediaProjection + match wire-up

**Files:**
- Create: `ocr/OcrCropper.kt`, `ocr/ScreenOcrEngine.kt`
- Modify: `OverlayService.kt` — 인식 button
- Modify: MainActivity or permission helper for MediaProjection consent Intent

**Interfaces:**
- Consumes: bitmap full screen, `cropBottomFraction`
- Produces:
  - `object OcrCropper { fun cropBottom(full: Bitmap, bottomFraction: Float): Bitmap }`
  - `class ScreenOcrEngine { suspend fun recognize(bitmap: Bitmap): String }` using ML Kit Korean
  - Overlay `onRecognize()`: capture → crop → OCR → `EncounterMatcher.match` → update UI state

- [ ] **Step 1: OcrCropper + unit test with synthetic bitmap size**

```kotlin
object OcrCropper {
    fun cropBottom(full: Bitmap, bottomFraction: Float): Bitmap {
        val f = bottomFraction.coerceIn(0.25f, 0.7f)
        val h = (full.height * f).toInt().coerceAtLeast(1)
        val y = (full.height - h).coerceAtLeast(0)
        return Bitmap.createBitmap(full, 0, y, full.width, h)
    }
}
```

Test: 1000px height, fraction 0.4 → result height 400.

- [ ] **Step 2: ScreenOcrEngine with ML Kit**

```kotlin
class ScreenOcrEngine {
    private val client = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    suspend fun recognize(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        client.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it.text) {} }
            .addOnFailureListener { cont.resume("") {} }
    }
}
```

- [ ] **Step 3: MediaProjection capture pipeline**

One-shot capture on 인식: acquire VirtualDisplay → ImageReader → Bitmap → release. Handle user denial → show `권한 필요` / `인식 실패`.

Empty OCR text → `OcrError` / `인식 실패` + suggest search.

- [ ] **Step 4: Wire matcher**

```kotlin
val outcome = EncounterMatcher(repo.all()).match(ocrText)
when (outcome) {
    is MatchOutcome.Clear -> state = Matched(outcome.node, outcome.score)
    is MatchOutcome.Ambiguous -> state = PickCandidate(outcome.candidates)
    MatchOutcome.Miss -> state = Miss
}
```

Candidate UI: list of titles; tap selects node for detail.

- [ ] **Step 5: Device smoke test + commit**

```bash
git add android/app/src/main/java/com/bossxor/seoul2033help/ocr \
        android/app/src/main/java/com/bossxor/seoul2033help/ui/overlay \
        android/app/src/test
git commit -m "feat: cropped OCR matching into overlay"
```

---

### Task 9: Auto OCR interval (default OFF)

**Files:**
- Modify: `OverlayService.kt`, `UserSettings.kt`

**Interfaces:**
- When `autoOcr == true`, schedule coroutine every `autoOcrIntervalSec` calling same path as 인식
- Manual 인식 always cancels/debounce and runs immediately
- Default `autoOcr = false`

- [ ] **Step 1: Implement ticker in service scope**

```kotlin
settings.collect { s ->
    autoJob?.cancel()
    if (s.autoOcr) {
        autoJob = scope.launch {
            while (isActive) {
                delay(s.autoOcrIntervalSec * 1000L)
                runRecognize()
            }
        }
    }
}
```

- [ ] **Step 2: Manual test on/off + commit**

```bash
git commit -am "feat: optional auto OCR interval on overlay"
```

---

### Task 10: README + seed sync note + MVP verification checklist

**Files:**
- Create: `README.md`
- Create: `scripts/sync-seed.ps1` (copy `data/encounters.json` → assets + test resources)

- [ ] **Step 1: README**

Contents: purpose, how to open `android/` in Android Studio, sync seed script, permissions list, link to spec, note Web/AI later, attribution line for namuwiki.

- [ ] **Step 2: sync-seed.ps1**

```powershell
Copy-Item -Force data/encounters.json android/app/src/main/assets/encounters.json
Copy-Item -Force data/encounters.json android/app/src/test/resources/encounters.json
```

- [ ] **Step 3: Run full unit tests**

`cd android && ./gradlew :app:testDebugUnitTest` → PASS

- [ ] **Step 4: Manual MVP checklist**

- [ ] Search finds sample node
- [ ] Chance shows percent
- [ ] Recommend border differs
- [ ] Overlay opacity persists
- [ ] OCR crop setting changes capture region (log bitmap height)
- [ ] Ambiguous path selectable
- [ ] Miss shows `정보 없음`
- [ ] Close kills app

- [ ] **Step 5: Commit + push**

```bash
git add README.md scripts/sync-seed.ps1
git commit -m "docs: README and seed sync for android MVP"
git push
```

---

## Out of scope (next plans)

1. **`2026-11-XX-runtime-ai-cache.md`** — 미스 시 AI 구조화, LocalCache, export
2. **`2026-11-XX-web-search.md`** — 같은 `data/encounters.json` 검색 UI
3. **Seed bulk** — 나무위키 → JSON 파이프라인
4. **Visual skin** — UI 스킬

## Spec coverage self-check

| Spec requirement | Task |
|------------------|------|
| Seed schema + sample | 1 |
| Fuzzy match + confidence/candidates | 2, 8 |
| Effect types + chance display | 1, 3, 5 |
| Fullscreen search/tree display | 5 |
| Overlay read-only + OCR sync | 7, 8 |
| OCR crop | 8 |
| Manual + auto OCR | 8, 9 |
| Recommend border | 5, 7 |
| Opacity | 6, 7 |
| Close = quit app | 7 |
| No condition filter / no Web / no AI | excluded |
| Attribution | 5, 10 |

## Placeholder scan

None intentional. Task 4 polymorphic JSON: if default discriminator fails, fix serializers in that task until sample seed decodes — do not leave broken decode.
