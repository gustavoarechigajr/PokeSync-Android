package com.pokesync.android.data.repository

import android.net.Uri
import com.pokesync.android.data.api.AndroidPokemonDto
import com.pokesync.android.data.api.PokeSyncApi
import com.pokesync.android.data.local.SaveFileRepository
import com.pokesync.android.domain.model.Pokemon
import com.pokesync.android.domain.model.PokemonMove
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
    val boxCount: Int,
    val pokemon: List<Pokemon>,
)

@Singleton
class PokemonRepository @Inject constructor(
    private val api: PokeSyncApi,
    private val saveFileRepository: SaveFileRepository,
) {
    suspend fun uploadSaveFromPath(path: String): UploadResult {
        val bytes = File(path).readBytes()
        return upload(bytes, File(path).name)
    }

    suspend fun uploadSaveFromUri(uri: Uri): UploadResult {
        val bytes = saveFileRepository.readBytes(uri)
        val name = saveFileRepository.resolveSaveFile(uri)?.displayName ?: "save.bin"
        return upload(bytes, name)
    }

    suspend fun getSavePokemon(saveId: String): List<Pokemon> =
        api.getSavePokemon(saveId).map { it.toDomain() }

    suspend fun getVault(): List<Pokemon> =
        api.getVault().map { it.toDomain() }

    suspend fun importToVault(saveId: String, replace: Boolean = false): List<Pokemon> =
        api.importToVault(saveId, replace).map { it.toDomain() }

    suspend fun addSingleToVault(saveId: String, box: Int, slot: Int): List<Pokemon> =
        api.addSingleToVault(saveId, box, slot).map { it.toDomain() }

    suspend fun removeFromVault(id: String) =
        api.removeFromVault(id)

    suspend fun moveVaultPokemon(id: String, box: Int, slot: Int) =
        api.moveVaultPokemon(id, box, slot)

    /**
     * Exports a vault Pokémon into a cached save file at the specified box/slot.
     * @return the modified save file bytes, ready to write back to device storage via SAF.
     */
    suspend fun exportToSave(
        vaultPokemonId: String,
        saveId: String,
        targetBox: Int,
        targetSlot: Int,
    ): ByteArray = api.exportVaultPokemonToSave(vaultPokemonId, saveId, targetBox, targetSlot).bytes()

    /**
     * Pre-flight compatibility check before exportToSave. Errors block; warnings
     * are informational and the user can override.
     */
    suspend fun validateExport(vaultPokemonId: String, saveId: String) =
        api.validateExport(vaultPokemonId, saveId)

    private suspend fun upload(bytes: ByteArray, filename: String): UploadResult {
        val requestBody = bytes.toRequestBody("application/octet-stream".toMediaType())
        val part = MultipartBody.Part.createFormData("file", filename, requestBody)
        val response = api.uploadSave(part)
        return UploadResult(
            saveId = response.saveId,
            gameVersion = response.gameVersion,
            generation = response.generation,
            trainerName = response.trainerName,
            boxCount = response.boxCount,
            pokemon = response.pokemon.map { it.toDomain() },
        )
    }

    private fun AndroidPokemonDto.toDomain() = Pokemon(
        id = id,
        speciesId = speciesId,
        species = speciesName,
        nickname = nickname.takeIf { isNicknamed },
        level = level,
        isShiny = isShiny,
        gender = when (gender) { 0 -> "M"; 1 -> "F"; else -> null },
        nature = nature,
        ball = ball,
        generation = generation,
        box = box,
        slot = slot,
        spriteUrl = if (isShiny)
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/$speciesId.png"
        else
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$speciesId.png",
        type1 = type1,
        type2 = type2,
        statHp = statHp,
        statAtk = statAtk,
        statDef = statDef,
        statSpa = statSpa,
        statSpd = statSpd,
        statSpe = statSpe,
        moves = listOf(
            move1Name to move1Type,
            move2Name to move2Type,
            move3Name to move3Type,
            move4Name to move4Type,
        ).filter { it.first.isNotBlank() }.map { (name, type) -> PokemonMove(name, type) },
    )
}
