package com.pokesync.android.domain.model

data class Pokemon(
    val id: String,
    val species: String,
    val nickname: String?,
    val level: Int,
    val isShiny: Boolean,
    val gender: String?,
    val nature: String?,
    val ability: String?,
    val moves: List<String>,
    val heldItem: String?,
    val generation: Int,
    val spriteUrl: String?,
)
