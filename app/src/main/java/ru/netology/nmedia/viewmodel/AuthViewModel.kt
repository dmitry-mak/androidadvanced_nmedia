package ru.netology.nmedia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    auth: AppAuth
): ViewModel(){

    val data= auth.authState.asLiveData()

    val authenticated: Boolean
        get()= !data.value?.token.isNullOrEmpty()
}