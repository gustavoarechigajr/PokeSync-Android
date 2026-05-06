package com.pokesync.android.data.repository

import android.net.Uri
import com.pokesync.android.data.api.PokeSyncApi
import com.pokesync.android.data.api.PokemonDto
import com.pokesync.android.data.api.TransferRequest
import com.pokesync.android.data.local.SaveFileRepository
import com.pokesync.android.domain.model.Pokemon
import com.pokesync.android.domain.model.SaveFile
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
    suspend fun getVault(): List<Pokemon> = api.getVault().map { it.toDomain() }

    suspend fun uploadSave(uri: Uri): Pair<String, List<Pokemon>> {
        val bytes = saveFileRepository.readBytes(uri)
        val requestBody = bytes.toRequestBody("application/octet-stream".toMediaType())
        val part = MultipartBody.Part.createFormData("file", "save.bin", requestBody)
        val response = api.uploadSave(part)
        return response.saveId to response.pokemon.map { it.toDomain() }
    }

    suspend fun getSavePokemon(saveId: String): List<Pokemon> =
        api.getSavePokemon(saveId).map { it.toDomain() }

    suspend fun transferPokemon(pokemonIds: List<String>, targetSaveId: String): List<Pokemon> {
        val response = api.transferPokemon(targetSaveId, TransferRequest(pokemonIds, targetSaveId))
        return response.pokemon.map { it.toDomain() }
    }

    private fun PokemonDto.toDomain() = Pokemon(
        id = id,
        species = species,
        nickname = nickname,
        level = level,
        isShiny = isShiny,
        gender = gender,
        nature = nature,
        ability = ability,
        moves = moves,
        heldItem = heldItem,
        generation = generation,
        spriteUrl = spriteUrl,
    )
}
