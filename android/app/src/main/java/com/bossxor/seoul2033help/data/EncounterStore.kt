package com.bossxor.seoul2033help.data

import com.bossxor.seoul2033help.data.model.EncounterNode
import com.bossxor.seoul2033help.data.model.EncounterSeedFile
import com.bossxor.seoul2033help.domain.TextNormalizer
import kotlinx.serialization.json.Json

/**
 * Seed ∪ LocalCache. Same id → cache wins.
 */
class EncounterStore(
    seedNodes: List<EncounterNode>,
    private val cache: LocalCacheStore
) {
    private val seed = seedNodes.associateBy { it.id }
    @Volatile private var snap = Snapshot(merge(seed, cache.load()))

    /** 병합 결과와, 처음 검색할 때 한 번만 만드는 정규화 검색 문자열. */
    private class Snapshot(val nodes: Map<String, EncounterNode>) {
        val haystack: List<Pair<EncounterNode, String>> by lazy {
            nodes.values.map { n ->
                // 정규화 결과엔 줄바꿈이 없어서, 줄바꿈으로 이어 붙여도 경계를 넘어 맞지 않는다
                n to buildString {
                    append(TextNormalizer.normalize(n.title)).append('\n')
                    append(TextNormalizer.normalize(n.story))
                    n.choices.forEach { append('\n').append(TextNormalizer.normalize(it.text)) }
                }
            }
        }
    }

    fun all(): List<EncounterNode> = snap.nodes.values.toList()

    fun get(id: String): EncounterNode? = snap.nodes[id]

    fun search(query: String): List<EncounterNode> {
        val q = TextNormalizer.normalize(query)
        val s = snap
        if (q.isEmpty()) return s.nodes.values.toList()
        return s.haystack.mapNotNull { (n, h) -> n.takeIf { h.contains(q) } }
    }

    fun appendCache(node: EncounterNode): EncounterNode {
        cache.append(node)
        snap = Snapshot(merge(seed, cache.load()))
        return node
    }

    fun exportCacheJson(): String = cache.exportJson()

    fun reloadCache() {
        snap = Snapshot(merge(seed, cache.load()))
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

        fun fromSeedJson(raw: String, cache: LocalCacheStore): EncounterStore {
            val seed = json.decodeFromString(EncounterSeedFile.serializer(), raw)
            return EncounterStore(seed.nodes, cache)
        }

        private fun merge(
            seed: Map<String, EncounterNode>,
            cacheNodes: List<EncounterNode>
        ): Map<String, EncounterNode> {
            val out = seed.toMutableMap()
            for (n in cacheNodes) out[n.id] = n
            return out
        }
    }
}
