package com.pokesync.android.domain.model

data class RegisteredSave(
    val id: String,
    val absolutePath: String,
    val displayName: String,
    val emulator: String,
    val gameVersion: String?,
    val generation: Int?,
    val trainerName: String?,
    val addedAt: Long,
    val lastRefreshedAt: Long?,
    // last saveId returned from server (valid ~2hr, re-uploaded on next refresh)
    val lastSaveId: String?,
)
