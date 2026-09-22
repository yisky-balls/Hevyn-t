package com.yifeng.commandcenter.history

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.util.UUID

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
