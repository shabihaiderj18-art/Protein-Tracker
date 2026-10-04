package app.protein.tracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.ZoneOffset

class BackupCodecTest {
    private val sample = BackupFile(
        format = BACKUP_FORMAT,
        exportedAt = "2026-10-01T10:00:00Z",
        settings = BackupSettings(proteinTarget = 120.0, bodyWeightKg = 72.5, dayStartMinutes = 0),
        foods = listOf(
            BackupFood(id = 1, name = "Egg", kcalPer100 = 143.0, proteinPer100 = 12.6, unitName = "egg", unitSize = 50.0),
        ),
        entries = listOf(
            BackupEntry(
                id = 7, date = "2026-10-01", loggedAt = 1_790_000_000_000L, meal = "MORNING", foodId = 1,
                name = "Egg", amount = 2.0, inUnits = true, unitName = "egg", unitSize = 50.0, quantity = 100.0,
                proteinPer100 = 12.6, kcalPer100 = 143.0, protein = 12.6, kcal = 143.0,
            ),
            BackupEntry(
                id = 8, date = "2026-10-01", loggedAt = 1_790_000_100_000L, meal = "NIGHT",
                name = "Shake, \"large\"", protein = 30.0, kcal = 250.0,
            ),
        ),
    )

    @Test
    fun roundTrip() {
        val text = BackupCodec.encode(sample)
        assertTrue(text.contains("\"format\": \"protein-backup\""))
        assertEquals(sample, BackupCodec.decode(text))
    }

    @Test
    fun ignoresUnknownFields() {
        val text = """{"format":"protein-backup","version":1,"somethingNew":true,"foods":[],"entries":[]}"""
        assertEquals(0, BackupCodec.decode(text).foods.size)
    }

    @Test
    fun rejectsOtherFiles() {
        for (text in listOf("hello", "{}", """{"format":"other"}""")) {
            try {
                BackupCodec.decode(text)
                fail("Expected failure for $text")
            } catch (e: NotABackupException) {
                // expected
            }
        }
    }

    @Test
    fun csvEscapesAndFormats() {
        val csv = CsvExport.build(sample.entries, ZoneOffset.UTC)
        val lines = csv.trimEnd().split("\r\n")
        assertEquals("date,time,meal,food,amount,unit,quantity,base_unit,protein_g,kcal,carbs_g,fat_g,fiber_g", lines[0])
        assertTrue(lines[1].startsWith("2026-10-01,"))
        assertTrue(lines[1].endsWith(",Morning,Egg,2,egg,100,g,12.6,143,0,0,0"))
        assertTrue(lines[2].contains(",Night,\"Shake, \"\"large\"\"\",,,,,30,250,0,0,0"))
    }
}
