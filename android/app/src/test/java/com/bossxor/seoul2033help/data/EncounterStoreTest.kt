package com.bossxor.seoul2033help.data

import com.bossxor.seoul2033help.data.model.Choice
import com.bossxor.seoul2033help.data.model.Effect
import com.bossxor.seoul2033help.data.model.EncounterNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncounterStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun node(id: String, title: String) = EncounterNode(
        id = id, title = title, story = "기본",
        choices = listOf(Choice("지나친다", listOf(Effect.Text("지나감"))))
    )

    @Test
    fun cacheWinsOnSameId() {
        val cache = LocalCacheStore(tmp.newFile("local_encounters.json"))
        cache.save(listOf(node("a", "캐시제목")))
        val store = EncounterStore(listOf(node("a", "시드제목"), node("b", "시드B")), cache)
        assertEquals("캐시제목", store.get("a")!!.title)
        assertEquals("시드B", store.get("b")!!.title)
        assertEquals(2, store.all().size)
    }

    @Test
    fun searchMatchesChoiceTextAndSeesAppendedNodes() {
        val cache = LocalCacheStore(tmp.newFile("s.json"))
        val store = EncounterStore(listOf(node("a", "들개 무리"), node("b", "편의점")), cache)
        assertEquals(listOf("a"), store.search("들개").map { it.id })
        assertEquals(2, store.search("지나친다").size)
        // 제목 끝과 분류 앞이 이어 붙어 맞으면 안 된다
        assertTrue(store.search("무리기본").isEmpty())
        store.appendCache(node("c", "새 들개"))
        assertEquals(setOf("a", "c"), store.search("들개").map { it.id }.toSet())
    }

    @Test
    fun appendUpdatesMerge() {
        val cache = LocalCacheStore(tmp.newFile("c.json"))
        val store = EncounterStore(listOf(node("seed", "시드")), cache)
        store.appendCache(node("ai-1", "AI노드"))
        assertEquals(2, store.all().size)
        assertEquals("AI노드", store.get("ai-1")!!.title)
        assertTrue(cache.exportJson().contains("AI노드"))
    }
}
