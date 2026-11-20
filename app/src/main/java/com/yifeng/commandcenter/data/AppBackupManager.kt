package com.yifeng.commandcenter.data

import android.content.Context
import android.net.Uri
import com.yifeng.commandcenter.history.WorkoutHistoryDatabase
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object AppBackupManager {
    private val ALLOWED_PREFIXES = listOf(
        "databases/",
        "shared_prefs/",
        "files/workout-photos/",
    )

    fun exportBackupToUri(context: Context, destinationUri: Uri): Result<Int> = runCatching {
        val appContext = context.applicationContext
        val dataDir = appContext.dataDir

        // Flush WAL into main SQLite files before reading
        runCatching {
            RoutineLibraryDatabase.getInstance(appContext).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)").close()
        }
        runCatching {
            WorkoutHistoryDatabase.getInstance(appContext).openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)").close()
        }

        val filesToPack = mutableListOf<Pair<String, File>>()

        File(dataDir, "databases").listFiles()?.forEach { file ->
            if (file.isFile && (file.name.startsWith("routine_library.db") || file.name.startsWith("workout_history.db"))) {
                filesToPack.add("databases/${file.name}" to file)
            }
        }
        File(dataDir, "shared_prefs").listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".xml")) {
                filesToPack.add("shared_prefs/${file.name}" to file)
            }
        }
        File(appContext.filesDir, "workout-photos").listFiles()?.forEach { file ->
            if (file.isFile) {
                filesToPack.add("files/workout-photos/${file.name}" to file)
            }
        }

        val outputStream = appContext.contentResolver.openOutputStream(destinationUri)
            ?: error("Could not open destination file for writing.")

        ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
            val buffer = ByteArray(16 * 1024)
            for ((entryName, file) in filesToPack) {
                zipOut.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        zipOut.write(buffer, 0, read)
                    }
                }
                zipOut.closeEntry()
            }
        }

        filesToPack.size
    }

    fun importBackupFromUri(context: Context, sourceUri: Uri): Result<Int> = runCatching {
        val appContext = context.applicationContext
        val dataDir = appContext.dataDir
        val canonicalDataDir = dataDir.canonicalPath

        // Pass 1: validate archive contains recognized Hevy't backup files
        var validEntryCount = 0
        val probeStream = appContext.contentResolver.openInputStream(sourceUri)
            ?: error("Could not open backup file.")
        ZipInputStream(BufferedInputStream(probeStream)).use { zipIn ->
            while (true) {
                val entry = zipIn.nextEntry ?: break
                if (!entry.isDirectory && isAllowedEntryName(entry.name)) {
                    validEntryCount++
                }
                zipIn.closeEntry()
            }
        }
        require(validEntryCount > 0) {
            "This file does not appear to be a valid Hevyn’t backup archive (.zip)."
        }

        // Clear existing Room DB files (including stale WAL/SHM) before extracting
        val dbDir = File(dataDir, "databases")
        dbDir.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.startsWith("routine_library.db") || file.name.startsWith("workout_history.db"))) {
                file.delete()
            }
        }

        // Pass 2: extract allowed entries safely
        var restoredCount = 0
        val inputStream = appContext.contentResolver.openInputStream(sourceUri)
            ?: error("Could not reopen backup file.")
        ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val entry = zipIn.nextEntry ?: break
                val name = entry.name
                if (!entry.isDirectory && isAllowedEntryName(name)) {
                    val targetFile = File(dataDir, name)
                    val canonicalTarget = targetFile.canonicalPath
                    if (!canonicalTarget.startsWith("$canonicalDataDir${File.separator}")) {
                        error("Unsafe path in backup archive: $name")
                    }
                    targetFile.parentFile?.mkdirs()
                    FileOutputStream(targetFile).use { out ->
                        while (true) {
                            val read = zipIn.read(buffer)
                            if (read <= 0) break
                            out.write(buffer, 0, read)
                        }
                    }
                    restoredCount++
                }
                zipIn.closeEntry()
            }
        }

        restoredCount
    }

    internal fun isAllowedEntryName(rawName: String): Boolean {
        if (rawName.isBlank() || rawName.startsWith("/") || rawName.contains("..")) return false
        return ALLOWED_PREFIXES.any { rawName.startsWith(it) }
    }
}
