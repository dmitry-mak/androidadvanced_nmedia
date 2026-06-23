package ru.netology.nmedia.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @ApplicationContext private val context: Context) {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val _authStateFlow = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState>
        get() = _authStateFlow

    init {
        val id = prefs.getLong(KEY_ID, 0)
        val token = prefs.getString(KEY_TOKEN, null)

        if (id != 0L && !token.isNullOrEmpty()) {
            _authStateFlow.value = AuthState(id, token)
        }
    }

    fun setAuth(id: Long, token: String) {
        _authStateFlow.value = AuthState(id, token)
        prefs.edit {
            putLong(KEY_ID, id)
            putString(KEY_TOKEN, token)
        }
    }

    fun removeAuth(){
        _authStateFlow.value= AuthState()
        prefs.edit {
            putLong(KEY_ID,0)
            putString(KEY_TOKEN,null)
        }
    }

    companion object {

        private const val KEY_ID = "id"
        private const val KEY_TOKEN = "token"
//        private var INSTANCE: AppAuth? = null

//        fun getInstance() = INSTANCE ?: throw RuntimeException("call init")
//
//        fun init(context: Context) {
//            INSTANCE = AppAuth(context)
//        }
    }
}

data class AuthState(
    val id: Long = 0,
    val token: String? = null
)