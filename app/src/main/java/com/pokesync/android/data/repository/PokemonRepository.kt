package com.pokesync.android.data.repository

import android.net.Uri
import com.pokesync.android.data.api.AndroidPokemonDto
import com.pokesync.android.data.api.AndroidSaveResponse
import com.pokesync.android.data.api.PokeSyncApi
import com.pokesync.android.data.local.SaveFileRepository
import com.pokesync.android.domain.model.Pokemon
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PokemonRepository @Inject constructor(
    private val api: PokeSyncApi,
    private val saveFileRepository: SaveFileRepository,
) {
    suspend fun uploadSave(uri: Uri): Pair<String, List<Pokemon>> {
        val bytes = saveFileRepository.readBytes(uri)
        val requestBody = bytes.toRequestBody("application/octet-stream".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "save.bin", requestBody)
        val response = api.uploadSave(part)
        return response.saveId to response.pokemon.map { it.toDomain() }
    }

    suspend fun getSavePokemon(saveId: String): List<Pokemon> =
        api.getSavePokemon(saveId).map { it.toDomain() }

    private fun AndroidPokemonDto.toDomain() = Pokemon(
        id = id,
        species = speciesName,
        nickname = nickname.takeIf { isNicknamed },
        level = level,
        isShiny = isShiny,
        gender = when (gender) { 0 -> "M"; 1 -> "F"; else -> null },
        nature = nature,
        ability = null,
        moves = listOfNotNull(
            move1.takeIf { it != 0 }?.toString(),
            move2.takeIf { it != 0 }?.toString(),
            move3.takeIf { it != 0 }?.toString(),
            move4.takeIf { it != 0 }?.toString(),
        ),
        heldItem = null,
        generation = generation,
        spriteUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$speciesId.png",
    )
}
