package com.pokesync.android.data.api

import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface PokeSyncApi {

    // Auth — no JWT required
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: LoginRequest): LoginResponse

    // Android save API — JWT required
    @Multipart
    @POST("api/android/saves/upload")
    suspend fun uploadSave(@Part file: MultipartBody.Part): AndroidSaveResponse

    @GET("api/android/saves/{saveId}/pokemon")
    suspend fun getSavePokemon(@Path("saveId") saveId: String): List<AndroidPokemonDto>
}
