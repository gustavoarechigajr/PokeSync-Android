package com.pokesync.android.data.api

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

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

    @DELETE("api/android/vault/{id}")
    suspend fun removeFromVault(@Path("id") id: String)

    @PUT("api/android/vault/{id}/move")
    suspend fun moveVaultPokemon(
        @Path("id") id: String,
        @Query("box") box: Int,
        @Query("slot") slot: Int,
    )
}
