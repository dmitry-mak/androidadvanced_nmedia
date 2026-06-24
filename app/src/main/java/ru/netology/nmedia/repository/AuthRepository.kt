package ru.netology.nmedia.repository

import retrofit2.HttpException
import ru.netology.nmedia.api.AuthApiService
import javax.inject.Inject


class AuthRepository @Inject constructor(
    private val apiService: AuthApiService
){
    suspend fun authenticate(login: String, password: String): AuthResponse {

        val response = apiService.authenticate(login, password)
        if (!response.isSuccessful) throw HttpException(response)
        return response.body() ?: throw IllegalStateException("Empty body")
    }
}

data class AuthResponse(
    val id: Long,
    val token: String
)