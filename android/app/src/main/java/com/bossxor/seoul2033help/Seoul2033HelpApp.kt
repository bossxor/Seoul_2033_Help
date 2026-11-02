package com.bossxor.seoul2033help

import android.app.Application
import com.bossxor.seoul2033help.data.EncounterStore
import com.bossxor.seoul2033help.data.LocalCacheStore

class Seoul2033HelpApp : Application() {
    lateinit var store: EncounterStore
        private set

    override fun onCreate() {
        super.onCreate()
        val raw = assets.open("encounters.json").bufferedReader().use { it.readText() }
        store = EncounterStore.fromSeedJson(raw, LocalCacheStore.inFilesDir(filesDir))
    }
}
