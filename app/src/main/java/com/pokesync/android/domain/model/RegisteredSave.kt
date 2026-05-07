package com.pokesync.android.domain.model

data class RegisteredSave(
    val id: String,
    val absolutePath: String,
    val displayName: String,
    val emulator: String,
    val gameVersion: String?,
    val generation: Int?,
    val trainerName: String?,
    val boxCount: Int? = null,
    val addedAt: Long,
    val lastRefreshedAt: Long?,
    val lastSaveId: String?,
)
