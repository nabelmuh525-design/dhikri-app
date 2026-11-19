package com.dhikri.islamic

import android.app.Application
import com.dhikri.islamic.data.local.DhikriDatabase
import com.dhikri.islamic.data.preferences.UserSettingsRepository
import com.dhikri.islamic.data.repository.DhikriRepository

class DhikriApplication : Application() {
    lateinit var database: DhikriDatabase
        private set
    lateinit var repository: DhikriRepository
        private set
    lateinit var settingsRepository: UserSettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = DhikriDatabase.getInstance(this)
        repository = DhikriRepository(this, database)
        settingsRepository = UserSettingsRepository(this)
    }
}
