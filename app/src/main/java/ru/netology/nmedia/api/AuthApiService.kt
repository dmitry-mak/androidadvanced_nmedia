package ru.netology.nmedia.api

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.repository.AuthResponse

private val authRetrofit = Retrofit.Builder()
    .baseUrl("${BuildConfig.BASE_URL}/api/")
    .addConverterFactory(GsonConverterFactory.create())
    .client(OkHttpClient.Builder().build())
    .build()

interface AuthApiService {
    @FormUrlEncoded
    @POST("users/authentication")
    suspend fun authenticate(
        @Field("login") login: String,
        @Field("pass") password: String
    ): Response<AuthResponse>
}

object AuthApi{
    val service: AuthApiService by lazy { authRetrofit.create(AuthApiService::class.java) }
}