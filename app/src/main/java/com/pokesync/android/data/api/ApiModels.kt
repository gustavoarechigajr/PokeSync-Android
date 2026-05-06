package com.pokesync.android.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String,
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    @Json(name = "user_id") val userId: String,
    val username: String,
)

@JsonClass(generateAdapter = true)
data class PokemonDto(
    val id: String,
    val species: String,
    val nickname: String?,
    val level: Int,
    @Json(name = "is_shiny") val isShiny: Boolean,
    val gender: String?,
    val nature: String?,
    val ability: String?,
    val moves: List<String>,
    @Json(name = "held_item") val heldItem: String?,
    val generation: Int,
    @Json(name = "sprite_url") val spriteUrl: String?,
)

@JsonClass(generateAdapter = true)
data class UploadSaveResponse(
    @Json(name = "save_id") val saveId: String,
    val pokemon: List<PokemonDto>,
    @Json(name = "game_title") val gameTitle: String?,
    val generation: Int?,
)

@JsonClass(generateAdapter = true)
data class TransferRequest(
    @Json(name = "pokemon_ids") val pokemonIds: List<String>,
    @Json(name = "target_save_id") val targetSaveId: String,
)
