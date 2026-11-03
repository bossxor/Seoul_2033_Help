package com.bossxor.seoul2033help.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EncounterSeedFile(
    val version: Int,
    val nodes: List<EncounterNode>
)

@Serializable
data class EncounterNode(
    val id: String,
    val title: String,
    val story: String,
    val body: String? = null,
    val choices: List<Choice> = emptyList(),
    val source: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class Choice(
    val text: String,
    val effects: List<Effect> = emptyList(),
    val nextId: String? = null,
    val recommend: Boolean = false
)

@Serializable
sealed class Effect {
    @Serializable
    @SerialName("item")
    data class Item(val name: String, val delta: Int? = null) : Effect()

    @Serializable
    @SerialName("ability")
    data class Ability(val name: String) : Effect()

    @Serializable
    @SerialName("status")
    data class Status(val name: String, val delta: Int = 1) : Effect()

    @Serializable
    @SerialName("resource")
    data class Resource(val name: String, val delta: Int) : Effect()

    @Serializable
    @SerialName("chance")
    data class Chance(
        val successRate: Double,
        val success: List<Effect> = emptyList(),
        val failure: List<Effect> = emptyList()
    ) : Effect()

    @Serializable
    @SerialName("text")
    data class Text(val name: String) : Effect()

    @Serializable
    @SerialName("condition")
    data class Condition(val name: String) : Effect()
}
