package com.bossxor.seoul2033help.domain

import com.bossxor.seoul2033help.data.model.EncounterNode

data class ScoredNode(val node: EncounterNode, val score: Double)

sealed class MatchOutcome {
    data class Clear(val node: EncounterNode, val score: Double) : MatchOutcome()
    data class Ambiguous(val candidates: List<ScoredNode>) : MatchOutcome()
    data object Miss : MatchOutcome()
}
