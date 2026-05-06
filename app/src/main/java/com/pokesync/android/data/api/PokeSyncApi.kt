package com.pokesync.android.data.api

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PokeSyncApi {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: LoginRequest): LoginResponse

    @GET("api/pokemon")
    suspend fun getVault(): List<PokemonDto>

    @Multipart
    @POST("api/saves/upload")
    suspend fun uploadSave(@Part file: MultipartBody.Part): UploadSaveResponse

    @GET("api/saves/{saveId}/pokemon")
    suspend fun getSavePokemon(@Path("saveId") saveId: String): List<PokemonDto>

    @POST("api/saves/{saveId}/transfer")
    suspend fun transferPokemon(
        @Path("saveId") saveId: String,
        @Body request: TransferRequest,
    ): UploadSaveResponse
}
