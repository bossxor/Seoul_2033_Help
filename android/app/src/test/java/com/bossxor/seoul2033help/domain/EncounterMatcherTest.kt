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
