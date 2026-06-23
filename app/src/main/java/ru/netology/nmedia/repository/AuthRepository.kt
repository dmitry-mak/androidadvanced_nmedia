package ru.netology.nmedia.repository

import retrofit2.HttpException
import ru.netology.nmedia.api.AuthApiService
import javax.inject.Inject

// import ru.netology.nmedia.api.AuthApi

class AuthRepository @Inject constructor(
    private val apiService: AuthApiService
){
    suspend fun authenticate(login: String, password: String): AuthResponse {

//        val response = AuthApi.service.authenticate(login, password)
        val response = apiService.authenticate(login, password)
        if (!response.isSuccessful) throw HttpException(response)
        return response.body() ?: throw IllegalStateException("Empty body")
    }
}

data class AuthResponse(
    val id: Long,
    val token: String
)