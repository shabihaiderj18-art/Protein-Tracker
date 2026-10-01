package app.protein.tracker.data.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Reads and writes files the user picked with the system file picker. */
class BackupFiles(private val context: Context) {
    suspend fun write(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("Cannot open file")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    suspend fun read(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
        stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }
}
