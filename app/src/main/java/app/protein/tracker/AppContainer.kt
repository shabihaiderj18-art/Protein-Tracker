package app.protein.tracker

import android.content.Context
import app.protein.tracker.data.ProteinRepository
import app.protein.tracker.data.backup.BackupFiles
import app.protein.tracker.data.db.AppDatabase
import app.protein.tracker.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Creates the app's single instances of the database, settings and repository. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    /** For quick database writes that should finish even if the screen closes. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings = SettingsRepository(appContext)
    private val database = AppDatabase.build(appContext)
    val repository = ProteinRepository(database, settings)
    val backupFiles = BackupFiles(appContext)
}
