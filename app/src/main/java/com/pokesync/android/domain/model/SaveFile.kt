package com.pokesync.android.domain.model

import android.net.Uri

data class SaveFile(
    val uri: Uri,
    val displayName: String,
    val gameTitle: String?,
    val generation: Int?,
    val sizeBytes: Long,
)
