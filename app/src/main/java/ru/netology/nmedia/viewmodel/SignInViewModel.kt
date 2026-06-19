package ru.netology.nmedia.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.repository.AuthRepository
import java.io.IOException


data class SignInState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

class SignInViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _state = MutableLiveData(SignInState())
    val state: LiveData<SignInState>
        get() = _state

    fun authenticate(login: String, password: String) {
        val loginTrimmed = login.trim()
        val passwordTrimmed = password.trim()

        if (loginTrimmed.isBlank() || passwordTrimmed.isBlank()) {
            _state.value = SignInState(errorMessage = "Введите логин и пароль")
            return
        }

        _state.value = SignInState(loading = true)

        viewModelScope.launch {
            runCatching {
                repository.authenticate(loginTrimmed, passwordTrimmed)
            }.onSuccess { (id, token) ->
                AppAuth.getInstance().setAuth(id, token)
                _state.value = SignInState(success = true)
            }.onFailure { error ->
                val errorMessage = when (error) {
                    is HttpException -> when (error.code()) {
                        401 -> "Неверный логин или пароль"
                        else -> "Неизвестная ошибка: ${error.code()}"
                    }

                    is IOException -> "Ошибка сети"
                    else -> "Не удалось войти"
                }
                _state.value = SignInState(errorMessage = errorMessage)
            }
        }
    }
}