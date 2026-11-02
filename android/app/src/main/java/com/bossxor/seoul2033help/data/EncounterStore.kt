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
    @Volatile private var merged: Map<String, EncounterNode> = merge(seed, cache.load())

    fun all(): List<EncounterNode> = merged.values.toList()

    fun get(id: String): EncounterNode? = merged[id]

    fun search(query: String): List<EncounterNode> {
        val q = TextNormalizer.normalize(query)
        val nodes = all()
        if (q.isEmpty()) return nodes
        return nodes.filter { node ->
            TextNormalizer.normalize(node.title).contains(q) ||
                TextNormalizer.normalize(node.story).contains(q) ||
                node.choices.any { TextNormalizer.normalize(it.text).contains(q) }
        }
    }

    fun appendCache(node: EncounterNode): EncounterNode {
        cache.append(node)
        merged = merge(seed, cache.load())
        return node
    }

    fun exportCacheJson(): String = cache.exportJson()

    fun reloadCache() {
        merged = merge(seed, cache.load())
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
