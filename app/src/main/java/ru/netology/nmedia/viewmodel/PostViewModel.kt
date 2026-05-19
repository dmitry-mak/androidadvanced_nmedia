package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import java.io.IOException
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.ApiError
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

    private val _actionError = MutableLiveData<String?>()
    val actionError: LiveData<String?> get() = _actionError

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
                _state.value = FeedModelState()
            }.onFailure { error ->
                _state.value = FeedModelState(error = true)
                handleError(error)
            }
//        repository.getAllDataAsync(object : PostRepository.PostCallback<List<Post>> {
//            override fun onSuccess(posts: List<Post>) {
//                _state.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
//            }
//
//            override fun onError(e: Throwable) {
//                _state.postValue(FeedModel(error = true))
//            }
//        })
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
            }.onFailure { error ->
                handleError(error)
            }
        }
//        repository.likeAsync(id, isLiked, object : PostRepository.PostCallback<Post> {
//            override fun onSuccess(post: Post) {
//                val currentPosts = _state.value?.posts ?: emptyList()
//                val updatedPosts = currentPosts.map { currentPost ->
//                    if (currentPost.id == post.id) post else currentPost
//                }
//                _state.postValue(
//                    FeedModel(
//                        posts = updatedPosts,
//                        empty = updatedPosts.isEmpty()
//                    )
//                )
//            }
//
//            override fun onError(e: Throwable) {
//                handleError(e)
//            }
//
//        })
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
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    fun save(text: String){
        val current = edited.value ?: return
        val trimmed = text.trim()

        if(trimmed.isBlank()){
            edited.value=empty
            clearDraftPost()
            return
        }
        lastRetryAction = {save(text)}

        viewModelScope.launch {
            runCatching {
                repository.saveAsync(current.copy(content = trimmed))
            }.onSuccess {
                edited.value =empty
                clearDraftPost()
            }.onFailure { error ->
                handleError(error)
            }
        }
    }

    /*
    fun save(text: String) {
        viewModelScope.launch {
            edited.value?.let {
                repository.saveAsync(it.copy(content = text.trim()))

            }
            edited.value = empty
        }
//        edited.value?.let { current ->
//            if (current.content != text) {
//                lastRetryAction = { save(text) }
//                repository.saveAsync(
//                    current.copy(content = text.trim()),
//                    object : PostRepository.PostCallback<Post> {
//                        override fun onSuccess(post: Post) {
//                            load()
//                            edited.postValue(empty)
//                            clearDraftPost()
//                        }
//
//                        override fun onError(e: Throwable) {
//                            handleError(e)
//                        }
//                    }
//                )
//            }
//        }
    }
     */
    fun edit(post: Post) {
        edited.value = post
    }

    fun cancelEditing() {
        edited.value = empty
    }

    fun setDraftPost(text: String) {
        draftPost.postValue(text)
    }

    fun clearDraftPost() {
        draftPost.postValue("")
    }

    fun handleError(e: Throwable) {
        val message = when (e) {
            is ApiError -> when (e.code) {
                in 300..309 -> "Ошибка редиректа"
                400 -> "Неверный формат"
                401 -> "Текст для ошибки 401"
                404 -> "Ресурс не найден"
                in 500..509 -> "Серверная ошибка - ${e.code}"
                else -> "Неизвестная ошибка- ${e.code} - ${e.message}"
            }

            is IOException -> "Отсутствует подключение к интернету"
            else -> "Ошибка. Попробуйте снова"
        }
        _actionError.postValue(message)
    }

    fun retryLastAction() {
        lastRetryAction?.invoke()
        lastRetryAction = null
    }
}