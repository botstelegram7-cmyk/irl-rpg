package com.irlrpg.app

import android.app.Application
import com.irlrpg.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IrlRpgApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // First-run defaults (starter categories + the six skill attributes).
        // Runs once, then a flag in SharedPreferences keeps startup instant.
        appScope.launch {
            runCatching { container.seeder.seedIfNeeded() }
        }
    }
}
