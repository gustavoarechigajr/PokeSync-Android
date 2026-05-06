package com.pokesync.android.data.repository

import android.net.Uri
import com.pokesync.android.data.api.AndroidPokemonDto
import com.pokesync.android.data.api.PokeSyncApi
import com.pokesync.android.data.local.SaveFileRepository
import com.pokesync.android.domain.model.Pokemon
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class UploadResult(
    val saveId: String,
    val gameVersion: String,
    val generation: Int,
    val trainerName: String,
    val pokemon: List<Pokemon>,
)

@Singleton
class PokemonRepository @Inject constructor(
    private val api: PokeSyncApi,
    private val saveFileRepository: SaveFileRepository,
) {
    /** Upload from an absolute file path (MANAGE_EXTERNAL_STORAGE path). */
    suspend fun uploadSaveFromPath(path: String): UploadResult {
        val bytes = File(path).readBytes()
        return upload(bytes, File(path).name)
    }

    /** Upload from a SAF URI (fallback when full storage access is not granted). */
    suspend fun uploadSaveFromUri(uri: Uri): UploadResult {
        val bytes = saveFileRepository.readBytes(uri)
        val name = saveFileRepository.resolveSaveFile(uri)?.displayName ?: "save.bin"
        return upload(bytes, name)
    }

    suspend fun getSavePokemon(saveId: String): List<Pokemon> =
        api.getSavePokemon(saveId).map { it.toDomain() }

    private suspend fun upload(bytes: ByteArray, filename: String): UploadResult {
        val requestBody = bytes.toRequestBody("application/octet-stream".toMediaType())
        val part = MultipartBody.Part.createFormData("file", filename, requestBody)
        val response = api.uploadSave(part)
        return UploadResult(
            saveId = response.saveId,
            gameVersion = response.gameVersion,
            generation = response.generation,
            trainerName = response.trainerName,
            pokemon = response.pokemon.map { it.toDomain() },
        )
    }

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
