package com.yifeng.commandcenter.history

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WorkoutPhotoStore(
    private val filesDir: File,
) {
    constructor(context: Context) : this(context.applicationContext.filesDir)

    private val photoDirectory: File get() = File(filesDir, DIRECTORY)

    suspend fun copyFromPicker(
        contentResolver: ContentResolver,
        uri: Uri,
    ): String = withContext(Dispatchers.IO) {
        photoDirectory.mkdirs()
        check(photoDirectory.isDirectory) { "Unable to create the workout photo directory." }
        val relativePath = "$DIRECTORY/${UUID.randomUUID()}.image"
        val target = resolve(relativePath) ?: error("Invalid workout photo path.")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().buffered().use(input::copyTo)
            } ?: throw IOException("The selected image could not be opened.")
            if (target.length() == 0L) throw IOException("The selected image was empty.")
            relativePath
        } catch (error: Throwable) {
            target.delete()
            throw error
        }
    }

    fun resolve(relativePath: String): File? {
        val candidate = File(filesDir, relativePath)
        val root = runCatching { photoDirectory.canonicalFile }.getOrNull() ?: return null
        val resolved = runCatching { candidate.canonicalFile }.getOrNull() ?: return null
        return resolved.takeIf { it.parentFile == root }
    }

    fun existingFile(relativePath: String): File? =
        resolve(relativePath)?.takeIf(File::isFile)

    suspend fun delete(relativePaths: Iterable<String>) = withContext(Dispatchers.IO) {
        relativePaths.forEach { path -> resolve(path)?.delete() }
    }

    companion object {
        const val DIRECTORY = "workout-photos"
    }
}

class WorkoutPhotoManager(
    private val repository: WorkoutHistoryRepository,
    private val photoStore: WorkoutPhotoStore,
) {
    suspend fun replaceFromPicker(
        sessionId: String,
        contentResolver: ContentResolver,
        uri: Uri,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): WorkoutHistoryPhoto {
        val relativePath = photoStore.copyFromPicker(contentResolver, uri)
        val photo = WorkoutHistoryPhoto(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            relativePath = relativePath,
            position = 0,
            createdAtMillis = createdAtMillis,
        )
        return try {
            val replaced = repository.replaceVisiblePhoto(photo)
            photoStore.delete(replaced.filterNot { it == relativePath })
            photo
        } catch (error: Throwable) {
            photoStore.delete(listOf(relativePath))
            throw error
        }
    }

    suspend fun remove(photo: WorkoutHistoryPhoto) {
        val removed = repository.removeVisiblePhoto(photo.sessionId)
        photoStore.delete(removed)
    }

    suspend fun deleteSession(sessionId: String) {
        photoStore.delete(repository.deleteSession(sessionId))
    }

    fun existingFile(photo: WorkoutHistoryPhoto?): File? =
        photo?.let { photoStore.existingFile(it.relativePath) }
}