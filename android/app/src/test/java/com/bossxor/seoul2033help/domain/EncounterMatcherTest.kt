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

    @Test
    fun rareChoiceBeatsCommonOne() {
        // "지나간다"는 흔하고 "연기 속으로 들어가보자"는 드문 문구: 드문 쪽이 판가름한다
        val common = (1..20).map {
            EncounterNode("c$it", "흔한 노드 $it", "기본", choices = listOf(Choice("지나간다"), Choice("돌아간다 $it")))
        }
        val rainbow = EncounterNode(
            "rainbow", "무지개 드라이크리닝", "레벨",
            choices = listOf(Choice("지나간다"), Choice("연기 속으로 들어가보자"))
        )
        val m = EncounterMatcher(common + rainbow)
        val r = m.match(listOf("지나간다.", "연기 속으로 들어가보자."))
        assertTrue(r is MatchOutcome.Clear)
        assertEquals("rainbow", (r as MatchOutcome.Clear).node.id)
    }

    @Test
    fun footerTitlePrefixDecidesSharedChoices() {
        val a = EncounterNode("a", "무지개 드라이크리닝", "레벨", choices = listOf(Choice("지나간다")))
        val b = EncounterNode("b", "철물점", "기본", choices = listOf(Choice("지나간다")))
        val m = EncounterMatcher(listOf(a, b))
        assertTrue(m.match(listOf("지나간다")) is MatchOutcome.Ambiguous)
        val r = m.match(listOf("지나간다"), Footer("이야기", "무지개", 169))
        assertTrue(r is MatchOutcome.Clear)
        assertEquals("a", (r as MatchOutcome.Clear).node.id)
    }

    @Test
    fun storyTagAliasPicksLevelStory() {
        val lvl = EncounterNode("lvl", "비슷한 이야기", "레벨", choices = listOf(Choice("지나간다")))
        val basic = EncounterNode("basic", "비슷한 이야기", "기본", choices = listOf(Choice("지나간다")))
        val r = EncounterMatcher(listOf(basic, lvl)).match(listOf("지나간다"), Footer("이야기", null, null))
        assertTrue(r is MatchOutcome.Clear)
        assertEquals("lvl", (r as MatchOutcome.Clear).node.id)
    }

    @Test
    fun footerTitleLocksToThatEncounterEvenWhenChoiceTextDiffers() {
        val rainbow = (1..3).map {
            EncounterNode("r$it", "무지개 드라이크리닝 · $it", "레벨", choices = listOf(Choice("전혀 다른 문구 $it")))
        }
        val store = EncounterNode("s", "무너지지 않은 편의점", "기본", choices = listOf(Choice("죄송해요!")))
        val r = EncounterMatcher(rainbow + store).match(listOf("엇. 죄송해요."), Footer("이야기", "무지개", 170))
        // 편의점의 비슷한 문구가 아니라 무지개 인카운터의 화면들이 후보로 나온다
        val titles = when (r) {
            is MatchOutcome.Clear -> listOf(r.node.title)
            is MatchOutcome.Ambiguous -> r.candidates.map { it.node.title }
            MatchOutcome.Miss -> emptyList()
        }
        assertTrue(titles.toString(), titles.isNotEmpty() && titles.all { it.startsWith("무지개") })
    }
}
