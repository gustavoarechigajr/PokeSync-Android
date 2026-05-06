package com.pokesync.android.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.pokesync.android.domain.model.SaveFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaveFileRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun resolveSaveFile(uri: Uri): SaveFile? {
        val cursor = context.contentResolver.query(uri, null, null, null, null) ?: return null
        return cursor.use {
            if (!it.moveToFirst()) return null
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            val name = if (nameIndex >= 0) it.getString(nameIndex) else uri.lastPathSegment ?: "unknown"
            val size = if (sizeIndex >= 0) it.getLong(sizeIndex) else 0L
            SaveFile(
                uri = uri,
                displayName = name,
                gameTitle = null,
                generation = null,
                sizeBytes = size,
            )
        }
    }

    fun readBytes(uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Cannot open $uri")
}
