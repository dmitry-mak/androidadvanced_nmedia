package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.db.AppDb
import java.io.IOException
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl

private val empty = Post(
    id = 0,
    author = "Netology",
    published = 0L,
    content = "",
    likes = 0,
    likedByMe = false
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository = PostRepositoryImpl(
        AppDb.getInstance(application).postDao
    )
    private val _state = MutableLiveData(FeedModelState())
    val state: LiveData<FeedModelState>
        get() = _state

    val data: LiveData<FeedModel> = repository.posts.map {
        FeedModel(posts = it, empty = it.isEmpty())
    }
        .catch { it.printStackTrace() }
        .asLiveData(Dispatchers.Default)

    val newerCount: LiveData<Int> = data.switchMap {
        repository.getNewer(it.posts.firstOrNull()?.id ?: 0L)
            .catch { error ->
//                _state.value = FeedModelState(error = true)
                _state.postValue(FeedModelState(error = true))
                handleError(error)
            }
            .asLiveData(Dispatchers.Default)
    }
    private val _actionError = MutableLiveData<String?>()
    val actionError: LiveData<String?>
        get() = _actionError

    val edited = MutableLiveData(empty)

    val draftPost = MutableLiveData("")

    private var lastRetryAction: (() -> Unit)? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = FeedModelState(isLoading = true)
            runCatching {
                repository.getAllDataAsync()
            }
                .onSuccess {
                    _state.value = FeedModelState()
                }.onFailure { error ->
                    _state.value = FeedModelState(error = true)
                    handleError(error)
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = FeedModelState(refreshing = true)

            runCatching {
                repository.getAllDataAsync()
            }.onSuccess {
                _state.value = FeedModelState()
            }.onFailure { error ->
                _state.value = FeedModelState(error = true)
                handleError(error)
            }
        }
    }

    fun like(id: Long, isLiked: Boolean) {
        lastRetryAction = { like(id, isLiked) }

        viewModelScope.launch {
            runCatching {
                repository.likeAsync(id, isLiked)
            }.onSuccess {
                lastRetryAction = null
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun share(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.share(id)
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun removeById(id: Long) {
        lastRetryAction = { removeById(id) }

        viewModelScope.launch {
            runCatching {
                repository.removeByIdAsync(id)
            }.onSuccess {
                lastRetryAction = null
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun save(text: String) {
        val current = edited.value ?: return
        val trimmed = text.trim()

        if (trimmed.isBlank()) {
            edited.value = empty
            clearDraftPost()
            return
        }
        lastRetryAction = { save(text) }

        viewModelScope.launch {
            runCatching {
                repository.saveAsync(current.copy(content = trimmed))
            }.onSuccess {
                edited.value = empty
                clearDraftPost()
                _state.value = FeedModelState()
                lastRetryAction = null
            }.onFailure { error ->
                _state.value = FeedModelState(error = true)
                handleError(error)
            }
        }
    }

    fun edit(post: Post) {
        edited.value = post
        draftPost.value = post.content
    }

    fun cancelEditing() {
        edited.value = empty
        clearDraftPost()
    }

    fun setDraftPost(text: String) {
//        draftPost.postValue(text)
        draftPost.value = text
    }

    fun clearDraftPost() {
//        draftPost.postValue("")
        draftPost.value = ""
    }

    fun handleError(error: Throwable) {
        val message = when (error) {
            is HttpException -> when (error.code()) {
                in 300..309 -> "Ошибка редиректа"
                400 -> "Неверный формат"
                401 -> "Текст для ошибки 401"
                404 -> "Ресурс не найден"
                in 500..509 -> "Серверная ошибка - ${error.code()}"
                else -> "Неизвестная ошибка- ${error.code()} - ${error.message()}"
            }

            is IOException -> "Отсутствует подключение к интернету"
            else -> "Ошибка. Попробуйте снова"
        }
        _actionError.postValue(message)
//        _actionError.value = message
    }

    fun retryLastAction() {
        lastRetryAction?.invoke()
        lastRetryAction = null
    }
}