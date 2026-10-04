package app.protein.tracker

import android.app.Application
import app.protein.tracker.work.Background
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProteinApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Background.createChannel(this)
        // Keep reminders and the daily backup in step with the settings.
        container.appScope.launch {
            container.settings.settings
                .map { it.copy(lastAutoBackupAt = null) }
                .distinctUntilChanged()
                .collect { runCatching { Background.apply(this@ProteinApp, it) } }
        }
    }
}
