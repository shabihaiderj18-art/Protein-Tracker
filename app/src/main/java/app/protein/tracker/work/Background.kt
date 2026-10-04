package app.protein.tracker.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.protein.tracker.MainActivity
import app.protein.tracker.ProteinApp
import app.protein.tracker.R
import app.protein.tracker.domain.DayClock
import app.protein.tracker.domain.Fmt
import app.protein.tracker.domain.UserSettings
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "progress"
private const val KIND = "kind"
private const val KIND_PROGRESS = "progress"
private const val KIND_CHECK_IN = "check_in"
private const val BACKUP_WORK = "auto-backup"
private const val BACKUP_NOW_WORK = "auto-backup-now"
private const val BACKUP_PREFIX = "protein-backup-"
private const val BACKUPS_TO_KEEP = 10

object Background {
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Progress reminders", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "How much protein and how many calories are still to go today"
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** (Re)schedules reminders and the daily backup to match the settings. */
    fun apply(context: Context, settings: UserSettings) {
        val work = WorkManager.getInstance(context)
        for (slot in 0 until 3) {
            val name = "reminder-$slot"
            val time = settings.reminderTimes.getOrNull(slot)
            if (settings.remindersOn && time != null) {
                work.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, daily<ReminderWorker>(time, KIND_PROGRESS))
            } else {
                work.cancelUniqueWork(name)
            }
        }
        if (settings.remindersOn && settings.checkInReminderOn && settings.autoCalories) {
            work.enqueueUniquePeriodicWork("check-in", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, daily<ReminderWorker>(settings.checkInTime, KIND_CHECK_IN))
        } else {
            work.cancelUniqueWork("check-in")
        }
        if (settings.autoBackupFolder != null) {
            // Backups run once a day around 2:30 AM.
            work.enqueueUniquePeriodicWork(BACKUP_WORK, ExistingPeriodicWorkPolicy.UPDATE, daily<BackupWorker>(150, "backup"))
        } else {
            work.cancelUniqueWork(BACKUP_WORK)
        }
    }

    fun backupNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(BACKUP_NOW_WORK, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<BackupWorker>().build())
    }

    private inline fun <reified W : CoroutineWorker> daily(minutesOfDay: Int, kind: String) =
        PeriodicWorkRequestBuilder<W>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntil(minutesOfDay), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KIND to kind))
            .build()

    private fun delayUntil(minutesOfDay: Int): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(LocalTime.of(minutesOfDay / 60, minutesOfDay % 60))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return ChronoUnit.MILLIS.between(now, next)
    }
}

/** Posts "still to go" progress, or the morning check-in question. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ProteinApp).container
        val settings = container.settings.settings.first()
        if (!settings.remindersOn) return Result.success()
        val today = DayClock.today(settings.dayStartMinutes)
        val activity = container.repository.observeActivity(today).first()

        if (inputData.getString(KIND) == KIND_CHECK_IN) {
            if (activity == null) {
                notify(1, "Plan your day", "Working today? Gym? Tap to set today's calorie target.")
            }
            return Result.success()
        }

        val entries = container.repository.observeDay(today).first()
        val protein = entries.sumOf { it.protein }
        val kcal = entries.sumOf { it.kcal }
        val kcalTarget = container.repository.kcalTargetFor(settings, activity)
        val proteinLeft = settings.proteinTarget - protein
        val kcalLeft = kcalTarget - kcal
        if (proteinLeft < 1 && kcalLeft < 10) return Result.success()

        val toGo = buildList {
            if (proteinLeft >= 1) add("${Fmt.protein(proteinLeft)} g protein")
            if (kcalLeft >= 10) add("${Fmt.kcal(kcalLeft)} kcal")
        }.joinToString(" and ")
        notify(
            2,
            "Still to go: $toGo",
            "So far today: ${Fmt.protein(protein)} g protein, ${Fmt.kcal(kcal)} kcal.",
        )
        return Result.success()
    }

    private fun notify(id: Int, title: String, text: String) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        val intent = Intent(applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val open = PendingIntent.getActivity(applicationContext, id, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notification)
        } catch (e: SecurityException) {
            // Notification permission was taken away; nothing to do.
        }
    }
}

/** Writes a JSON backup into the folder picked in Settings and keeps the newest ten. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as ProteinApp).container
        val folder = container.settings.settings.first().autoBackupFolder ?: return Result.success()
        return try {
            AutoBackup.write(applicationContext, Uri.parse(folder), container.repository.exportJson())
            container.settings.update { it.copy(lastAutoBackupAt = System.currentTimeMillis()) }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}

object AutoBackup {
    fun write(context: Context, treeUri: Uri, json: String) {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val name = "$BACKUP_PREFIX${LocalDate.now()}.json"

        // Replace today's file if it already exists, then drop the oldest beyond ten.
        val existing = listBackups(context, treeUri)
        existing.filter { it.second == name }.forEach { DocumentsContract.deleteDocument(resolver, it.first) }
        val file = DocumentsContract.createDocument(resolver, parent, "application/json", name)
            ?: error("Could not create the backup file")
        resolver.openOutputStream(file)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            ?: error("Could not open the backup file")
        listBackups(context, treeUri)
            .sortedByDescending { it.second }
            .drop(BACKUPS_TO_KEEP)
            .forEach { DocumentsContract.deleteDocument(resolver, it.first) }
    }

    private fun listBackups(context: Context, treeUri: Uri): List<Pair<Uri, String>> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val result = mutableListOf<Pair<Uri, String>>()
        context.contentResolver.query(children, columns, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1) ?: continue
                if (name.startsWith(BACKUP_PREFIX) && name.endsWith(".json")) {
                    result += DocumentsContract.buildDocumentUriUsingTree(treeUri, id) to name
                }
            }
        }
        return result
    }

    /** "primary:Download/Protein" → "Download/Protein" */
    fun folderLabel(treeUri: String): String =
        runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(treeUri)).substringAfter(':') }
            .getOrNull()?.ifBlank { "Phone storage" } ?: "Chosen folder"
}
