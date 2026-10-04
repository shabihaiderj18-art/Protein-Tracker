package app.protein.tracker.data.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val BACKUP_FORMAT = "protein-backup"
const val BACKUP_VERSION = 1

@Serializable
data class BackupFile(
    /** Required, so a random JSON file is never mistaken for a backup. */
    val format: String,
    val version: Int = BACKUP_VERSION,
    val exportedAt: String = "",
    val settings: BackupSettings = BackupSettings(),
    val foods: List<BackupFood> = emptyList(),
    val entries: List<BackupEntry> = emptyList(),
    val activity: List<BackupActivity> = emptyList(),
)

@Serializable
data class BackupActivity(
    val date: String,
    val worked: Boolean,
    val workHours: Double,
    val gym: String,
    val gymMinutes: Int,
)

@Serializable
data class BackupSettings(
    val proteinTarget: Double = 110.0,
    val kcalTarget: Double = 2000.0,
    val bodyWeightKg: Double? = null,
    val dayStartMinutes: Int = 240,
    val theme: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val autoCalories: Boolean = false,
    val sex: String? = null,
    val ageYears: Int? = null,
    val heightCm: Double? = null,
    val jobType: String = "FIELD",
    val usualWorkHours: Double = 7.0,
    val goal: String = "MAINTAIN",
    val remindersOn: Boolean = false,
    val reminderTimes: List<Int> = listOf(780, 1080, 1260),
    val checkInReminderOn: Boolean = true,
    val checkInTime: Int = 540,
)

@Serializable
data class BackupFood(
    val id: Long,
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val baseUnit: String = "GRAM",
    val unitName: String? = null,
    val unitSize: Double? = null,
    val note: String? = null,
    val favorite: Boolean = false,
    val lastUsedAt: Long? = null,
    val useCount: Int = 0,
    val lastAmount: Double? = null,
    val lastInUnits: Boolean = false,
    val carbsPer100: Double? = null,
    val fatPer100: Double? = null,
    val fiberPer100: Double? = null,
)

@Serializable
data class BackupEntry(
    val id: Long,
    /** ISO date of the logical day, e.g. "2026-10-01". */
    val date: String,
    val loggedAt: Long,
    val meal: String,
    val foodId: Long? = null,
    val name: String,
    val amount: Double = 0.0,
    val inUnits: Boolean = false,
    val unitName: String? = null,
    val unitSize: Double? = null,
    val quantity: Double = 0.0,
    val baseUnit: String = "GRAM",
    val proteinPer100: Double? = null,
    val kcalPer100: Double? = null,
    val protein: Double,
    val kcal: Double,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val carbsPer100: Double? = null,
    val fatPer100: Double? = null,
    val fiberPer100: Double? = null,
)

class NotABackupException(message: String) : Exception(message)

object BackupCodec {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): BackupFile {
        val file = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (e: Exception) {
            throw NotABackupException("This file is not a Protein backup.")
        }
        if (file.format != BACKUP_FORMAT) throw NotABackupException("This file is not a Protein backup.")
        if (file.version > BACKUP_VERSION) {
            throw NotABackupException("This backup was made by a newer version of the app.")
        }
        return file
    }
}
