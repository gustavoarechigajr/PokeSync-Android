package com.pokesync.android.domain.model

data class PokemonMove(val name: String, val type: String)

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
    val type1: String = "",
    val type2: String? = null,
    val statHp: Int = 0,
    val statAtk: Int = 0,
    val statDef: Int = 0,
    val statSpa: Int = 0,
    val statSpd: Int = 0,
    val statSpe: Int = 0,
    val moves: List<PokemonMove> = emptyList(),
    val heldItemId: Int = 0,
    val heldItemName: String? = null,
) {
    val displayName: String get() = if (!nickname.isNullOrBlank() && nickname != species) nickname else species
    val genderSymbol: String? get() = when (gender) { "M" -> "♂"; "F" -> "♀"; else -> null }
    val typeString: String get() = if (!type2.isNullOrBlank()) "$type1 / $type2" else type1
    val hasStats: Boolean get() = statHp > 0
}
