package com.bossxor.seoul2033help.data

import com.bossxor.seoul2033help.data.model.Effect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EncounterRepositoryTest {
    private fun repo() = EncounterRepository.fromJson(
        javaClass.classLoader!!
            .getResourceAsStream("encounters.json")!!
            .bufferedReader().readText()
    )

    @Test
    fun loadsSampleSeed() {
        val repo = repo()
        assertTrue(repo.all().size >= 1000)
        assertNotNull(repo.get("wild-dogs"))
        // seed has wild-dogs + wild-dogs-pass
        val dogs = repo.search("들개").map { it.id }
        assertTrue(dogs.containsAll(listOf("wild-dogs", "wild-dogs-pass")))
        assertTrue(repo.search("나무 위로").any { it.id == "wild-dogs" })
    }

    @Test
    fun decodesPolymorphicEffects() {
        val effects = repo().get("wild-dogs")!!.choices[0].effects
        val chance = effects[0] as Effect.Chance
        assertTrue(chance.success[0] is Effect.Ability)
        assertTrue(chance.failure[0] is Effect.Resource)
    }
}
