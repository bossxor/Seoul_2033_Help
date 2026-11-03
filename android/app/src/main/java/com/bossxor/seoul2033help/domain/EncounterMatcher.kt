package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.EncounterNode
import kotlin.math.ln

/**
 * OCR 줄 단위 매칭.
 *  - 흔한 선택지("지나간다" 등)는 가중을 낮추고, 드문 선택지는 높인다 (IDF).
 *  - 선택지 아래 푯말 줄에서 읽은 제목 앞부분/분류는 보너스로 더한다.
 */
class EncounterMatcher(
    nodes: List<EncounterNode>,
    private val missThreshold: Double = MISS_THRESHOLD,
    private val clearMargin: Double = CLEAR_MARGIN,
    private val maxCandidates: Int = MAX_CANDIDATES
) {
    private class Prepared(
        val node: EncounterNode,
        val title: String,
        val story: String,
        val choices: List<String>,
        val bigrams: List<Set<String>>
    )

    private val prepared = nodes.map { n ->
        val cs = n.choices.map { TextNormalizer.normalize(it.text) }.filter { it.isNotEmpty() }.distinct()
        Prepared(n, TextNormalizer.normalize(n.title), TextNormalizer.normalize(n.story), cs, cs.map(::bigrams))
    }
    /** 정규화된 선택지 문구 -> 그 문구를 가진 노드 수 */
    private val docFreq: Map<String, Int> = HashMap<String, Int>().also { m ->
        prepared.forEach { p -> p.choices.forEach { m[it] = (m[it] ?: 0) + 1 } }
    }

    fun match(query: String): MatchOutcome = match(query.lines(), null)

    fun match(lines: List<String>, footer: Footer? = null): MatchOutcome {
        val qs = lines.map(TextNormalizer::normalize).filter { it.length >= 2 }.distinct()
        val prefix = footer?.titlePrefix?.let(TextNormalizer::normalize)?.takeIf { it.length >= 2 }
        if (qs.isEmpty() && prefix == null) return MatchOutcome.Miss

        val n = prepared.size.toDouble()
        val weights = qs.map { ln(1.0 + n / (1.0 + (docFreq[it] ?: 0))) }
        val qBigrams = qs.map(::bigrams)
        val wSum = weights.sum()
        val tag = footer?.tag?.let(TextNormalizer::normalize)?.takeIf { it.isNotEmpty() }

        // 푯말에서 제목 앞부분을 읽었고 그 제목의 노드가 있으면, 그 인카운터의 화면들로 후보를 좁힌다.
        // (게임 화면 문구가 위키와 조금 달라도 엉뚱한 인카운터가 이기지 않게)
        val locked = prefix != null && prepared.any { it.title.startsWith(prefix) }
        val pool = if (locked) prepared.filter { it.title.startsWith(prefix!!) } else prepared

        val scored = pool.map { p ->
            var lineScore = 0.0
            if (qs.isNotEmpty()) {
                var acc = 0.0
                for (i in qs.indices) acc += weights[i] * best(qs[i], qBigrams[i], p)
                lineScore = acc / wSum
            }
            var bonus = 0.0
            if (prefix != null) {
                bonus += when {
                    p.title.startsWith(prefix) -> 0.4
                    p.title.contains(prefix) -> 0.15
                    else -> 0.0
                }
            }
            if (tag != null && storyMatches(p.story, tag)) bonus += 0.1
            ScoredNode(p.node, lineScore + bonus)
        }.sortedByDescending { it.score }

        val top = scored.first()
        // 인카운터는 맞는데 화면 문구를 못 맞춘 경우: Miss 대신 그 인카운터의 화면들을 보여 준다
        if (top.score < missThreshold) {
            return if (locked) MatchOutcome.Ambiguous(scored.take(LOCKED_CANDIDATES)) else MatchOutcome.Miss
        }
        val second = scored.getOrNull(1)
        return if (second != null && (top.score - second.score) < clearMargin) {
            MatchOutcome.Ambiguous(scored.take(if (locked) LOCKED_CANDIDATES else maxCandidates).filter { it.score >= missThreshold })
        } else {
            MatchOutcome.Clear(top.node, top.score)
        }
    }

    /** 게임의 분류 칩과 위키 스토리 이름이 다른 경우의 대응 (정규화된 이름). */
    private fun storyMatches(story: String, tag: String): Boolean =
        story.contains(tag) || (TAG_ALIASES[tag]?.any { story.contains(it) } ?: false)

    /** OCR 한 줄 q 와 노드의 선택지들 중 가장 비슷한 정도 (0..1) */
    private fun best(q: String, qb: Set<String>, p: Prepared): Double {
        var best = 0.0
        for (i in p.choices.indices) {
            val c = p.choices[i]
            val s = when {
                q == c -> 1.0
                // 긴 선택지가 두 줄로 잘려 읽힌 경우: 한 줄이 선택지의 일부면 높게 친다
                q.length >= 4 && c.contains(q) -> 0.9
                else -> dice(qb, p.bigrams[i])
            }
            if (s > best) best = s
            if (best == 1.0) break
        }
        return best
    }

    private fun bigrams(s: String): Set<String> =
        if (s.length < 2) setOf(s) else s.windowed(2).toSet()

    private fun dice(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val overlap = a.count { it in b }
        return 2.0 * overlap / (a.size + b.size)
    }

    companion object {
        const val MISS_THRESHOLD = 0.35
        const val CLEAR_MARGIN = 0.08
        const val MAX_CANDIDATES = 3
        const val LOCKED_CANDIDATES = 6
        private val TAG_ALIASES = mapOf("이야기" to listOf("레벨", "시즌패스", "주요스토리"))
    }
}
