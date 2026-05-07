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
    @Json(name = "userId") val userId: String,
    val username: String,
)

@JsonClass(generateAdapter = true)
data class AndroidPokemonDto(
    val id: String,
    val speciesId: Int,
    val speciesName: String,
    val nickname: String,
    val isNicknamed: Boolean,
    val level: Int,
    val isShiny: Boolean,
    val isEgg: Boolean,
    val gender: Int,
    val nature: String,
    val ball: String,
    val generation: Int,
    val box: Int,
    val slot: Int,
    val move1: Int,
    val move2: Int,
    val move3: Int,
    val move4: Int,
    val type1: String = "",
    val type2: String? = null,
    val statHp: Int = 0,
    val statAtk: Int = 0,
    val statDef: Int = 0,
    val statSpa: Int = 0,
    val statSpd: Int = 0,
    val statSpe: Int = 0,
    val move1Name: String = "",
    val move2Name: String = "",
    val move3Name: String = "",
    val move4Name: String = "",
    val move1Type: String = "",
    val move2Type: String = "",
    val move3Type: String = "",
    val move4Type: String = "",
)

@JsonClass(generateAdapter = true)
data class AndroidSaveResponse(
    val saveId: String,
    val gameVersion: String,
    val generation: Int,
    val trainerName: String,
    val boxCount: Int,
    val boxSlotCount: Int = 30,
    val pokemonCount: Int,
    val pokemon: List<AndroidPokemonDto>,
)

@JsonClass(generateAdapter = true)
data class VaultImportRequest(
    val saveId: String,
)
