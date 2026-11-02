package com.bossxor.seoul2033help.data

import com.bossxor.seoul2033help.data.model.EncounterNode
import com.bossxor.seoul2033help.data.model.EncounterSeedFile
import kotlinx.serialization.json.Json
import java.io.File

/** Device-local AI-decoded nodes. Same schema as seed. */
class LocalCacheStore(private val file: File) {
    private val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        prettyPrint = true
    }

    @Synchronized
    fun load(): List<EncounterNode> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(EncounterSeedFile.serializer(), file.readText()).nodes
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun save(nodes: List<EncounterNode>) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(EncounterSeedFile.serializer(), EncounterSeedFile(1, nodes)))
    }

    @Synchronized
    fun append(node: EncounterNode): List<EncounterNode> {
        val map = load().associateBy { it.id }.toMutableMap()
        map[node.id] = node
        val out = map.values.toList()
        save(out)
        return out
    }

    fun exportJson(): String {
        val nodes = load()
        return json.encodeToString(EncounterSeedFile.serializer(), EncounterSeedFile(1, nodes))
    }

    companion object {
        const val FILE_NAME = "local_encounters.json"
        fun inFilesDir(dir: File) = LocalCacheStore(File(dir, FILE_NAME))
    }
}
