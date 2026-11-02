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
