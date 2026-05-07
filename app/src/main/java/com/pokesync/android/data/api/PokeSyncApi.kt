package com.pokesync.android.data.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface PokeSyncApi {

    // Auth — no JWT required
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: LoginRequest): LoginResponse

    // Save file API — JWT required
    @Multipart
    @POST("api/android/saves/upload")
    suspend fun uploadSave(@Part file: MultipartBody.Part): AndroidSaveResponse

    @GET("api/android/saves/{saveId}/pokemon")
    suspend fun getSavePokemon(@Path("saveId") saveId: String): List<AndroidPokemonDto>

    // Vault (permanent bank) API — JWT required
    @GET("api/android/vault")
    suspend fun getVault(): List<AndroidPokemonDto>

    @POST("api/android/vault/import/{saveId}")
    suspend fun importToVault(
        @Path("saveId") saveId: String,
        @Query("replace") replace: Boolean = false,
    ): List<AndroidPokemonDto>

    @POST("api/android/vault/add/{saveId}")
    suspend fun addSingleToVault(
        @Path("saveId") saveId: String,
        @Query("box") box: Int,
        @Query("slot") slot: Int,
    ): List<AndroidPokemonDto>

    @DELETE("api/android/vault/{id}")
    suspend fun removeFromVault(@Path("id") id: String)

    @PUT("api/android/vault/{id}/move")
    suspend fun moveVaultPokemon(
        @Path("id") id: String,
        @Query("box") box: Int,
        @Query("slot") slot: Int,
    )

    /// Exports a vault Pokémon into a cached save and returns the modified save file.
    @Streaming
    @POST("api/android/vault/{vaultId}/export")
    suspend fun exportVaultPokemonToSave(
        @Path("vaultId") vaultId: String,
        @Query("saveId") saveId: String,
        @Query("box") box: Int,
        @Query("slot") slot: Int,
    ): ResponseBody

    /// Pre-flight check: returns blocking errors and informational warnings
    /// without modifying any state. Call before exportVaultPokemonToSave.
    @POST("api/android/vault/{vaultId}/validate-export")
    suspend fun validateExport(
        @Path("vaultId") vaultId: String,
        @Query("saveId") saveId: String,
    ): TransferValidationResponse
}

/// Mirrors C# TransferValidationDTO. canTransfer == errors.isEmpty().
data class TransferValidationResponse(
    val canTransfer: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val outputFormat: String?,
)
