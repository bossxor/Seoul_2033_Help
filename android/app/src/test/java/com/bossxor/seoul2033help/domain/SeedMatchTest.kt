package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.EncounterRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 실제 시드 + 실기기에서 읽은 문구로 확인하는 회귀 테스트. */
class SeedMatchTest {
    private val matcher by lazy {
        val raw = javaClass.classLoader!!.getResourceAsStream("encounters.json")!!.bufferedReader().readText()
        EncounterMatcher(EncounterRepository.fromJson(raw).all())
    }

    private val lines = listOf("지나간다.", "연기 속으로 들어가보자.")

    @Test
    fun footerPicksRainbowDryCleaning() {
        val r = matcher.match(lines, Footer.parse("이야기 무지개… - 169 -"))
        assertTrue(r.toString(), r is MatchOutcome.Clear)
        assertEquals("무지개 드라이크리닝", (r as MatchOutcome.Clear).node.title)
    }

    @Test
    fun rareChoiceAloneNarrowsDown() {
        // 푯말 줄을 못 읽어도 드문 선택지 덕에 후보 안에 들어와야 한다
        val titles = when (val r = matcher.match(lines)) {
            is MatchOutcome.Clear -> listOf(r.node.title)
            is MatchOutcome.Ambiguous -> r.candidates.map { it.node.title }
            MatchOutcome.Miss -> emptyList()
        }
        assertTrue(titles.toString(), "무지개 드라이크리닝" in titles)
    }

    @Test
    fun firstScreenShowsOnlyItsOwnChoices() {
        val r = matcher.match(lines, Footer.parse("이야기 무지개… - 169 -")) as MatchOutcome.Clear
        assertEquals(listOf("지나간다", "연기속으로 들어가보자"), r.node.choices.map { it.text })
        assertTrue(r.node.choices.last().nextId != null)
    }

    @Test
    fun secondScreenIsMatchedByItsOwnChoices() {
        val r = matcher.match(
            listOf("저 그냥 가볼게요", "좋아, 뭔데?", "맨 입으로?"),
            Footer.parse("이야기 무지개… - 170 -")
        )
        assertTrue(r.toString(), r is MatchOutcome.Clear)
        assertEquals("무지개 드라이크리닝 · 2", (r as MatchOutcome.Clear).node.title)
    }

    @Test
    fun unknownScreenOfKnownEncounterStaysInThatEncounter() {
        // 게임 170쪽: 위키 원문에 없는 문구여도 푯말(무지개…)로 그 인카운터의 화면들이 나와야 한다
        val r = matcher.match(listOf("엇. 죄송해요."), Footer.parse("이야기 무지개… - 170 -"))
        val titles = when (r) {
            is MatchOutcome.Clear -> listOf(r.node.title)
            is MatchOutcome.Ambiguous -> r.candidates.map { it.node.title }
            MatchOutcome.Miss -> emptyList()
        }
        assertTrue(titles.toString(), titles.isNotEmpty() && titles.all { it.startsWith("무지개") })
    }
}
