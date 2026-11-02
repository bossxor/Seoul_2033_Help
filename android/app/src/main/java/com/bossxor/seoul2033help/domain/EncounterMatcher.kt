package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.EncounterNode
import kotlin.math.max

class EncounterMatcher(
    private val nodes: List<EncounterNode>,
    private val missThreshold: Double = MISS_THRESHOLD,
    private val clearMargin: Double = CLEAR_MARGIN,
    private val maxCandidates: Int = MAX_CANDIDATES
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

    companion object {
        const val MISS_THRESHOLD = 0.35
        const val CLEAR_MARGIN = 0.08
        const val MAX_CANDIDATES = 3
    }
}
