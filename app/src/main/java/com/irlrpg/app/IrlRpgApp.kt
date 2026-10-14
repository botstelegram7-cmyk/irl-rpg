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

        appScope.launch {
            // First-run defaults (starter categories, skill attributes, skill tree).
            runCatching { container.seeder.seedIfNeeded() }

            // Reconcile achievements with existing history exactly once per install
            // upgrade. Two years of reading should not arrive as eleven pop-ups, so
            // the first pass records them silently; afterwards every unlock is live.
            runCatching {
                val alreadyPrimed = container.settings.achievementsPrimed
                if (alreadyPrimed) {
                    container.progressSync.sweep(announce = true)
                } else {
                    container.progressSync.prime()
                    container.settings.achievementsPrimed = true
                }
            }
        }
    }
}
