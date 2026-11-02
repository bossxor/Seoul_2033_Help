package com.bossxor.seoul2033help

import android.app.Application
import com.bossxor.seoul2033help.data.EncounterRepository

class Seoul2033HelpApp : Application() {
    lateinit var repository: EncounterRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val raw = assets.open("encounters.json").bufferedReader().use { it.readText() }
        repository = EncounterRepository.fromJson(raw)
    }
}
