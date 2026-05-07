package com.pokesync.android.domain.model

data class Pokemon(
    val id: String,
    val speciesId: Int,
    val species: String,
    val nickname: String?,
    val level: Int,
    val isShiny: Boolean,
    val gender: String?,
    val nature: String?,
    val ball: String?,
    val generation: Int,
    val box: Int,
    val slot: Int,
    val spriteUrl: String,
) {
    val displayName: String get() = if (!nickname.isNullOrBlank() && nickname != species) nickname else species
    val genderSymbol: String? get() = when (gender) { "M" -> "♂"; "F" -> "♀"; else -> null }
}
