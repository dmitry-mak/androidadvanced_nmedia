package ru.netology.nmedia.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.MediaUpload
import java.io.IOException
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import java.io.File
import javax.inject.Inject

private val empty = Post(
    id = 0,
    authorId = 5L,
    author = "Netology",
    published = 0L,
    content = "",
    likes = 0,
    likedByMe = false,
    ownedByMe = false,
)

private val noPhoto = PhotoModel()

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PostViewModel @Inject constructor(
    private val repository: PostRepository,
    auth: AppAuth
) : ViewModel() {

    private val _state = MutableLiveData(FeedModelState())
    val state: LiveData<FeedModelState>
        get() = _state

    val data: Flow<PagingData<FeedItem>> = auth.authState
        .flatMapLatest { (myId, _) ->
            repository.posts.map { pagingData ->
                pagingData.map { post ->
                    if (post is Post) {
                        post.copy(ownedByMe = post.authorId == myId)
                    } else {
                        post
                    }
                }
            }
        }

    private val _singlePost = MutableLiveData<Post?>()
    val singlePost: LiveData<Post?>
        get() = _singlePost

    fun loadPostbyId(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.getById(id)
            }.onSuccess { post ->
                _singlePost.value = post
            }.onFailure { error ->
                _state.value = FeedModelState(error = true)
                handleError(error)
            }
        }
    }

    val newerCount: LiveData<Int> = repository.newerCount
        .asLiveData(Dispatchers.Default)

    val newerPolling = repository.getNewer(0L)
        .catch {
            _state.postValue(FeedModelState(error = true))
            emit(0)
        }.asLiveData(Dispatchers.Default)

    private val _actionError = MutableLiveData<String?>()
    val actionError: LiveData<String?>
        get() = _actionError

    val edited = MutableLiveData(empty)

    private val _photo = MutableLiveData<PhotoModel>(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    val draftPost = MutableLiveData("")

    private var lastRetryAction: (() -> Unit)? = null

//    init {
//        load()
//    }

    fun load() {
        _state.value = FeedModelState()
    }

    fun refresh() {
        _state.value = FeedModelState()
    }

    fun showNewer() {
        viewModelScope.launch {
            repository.showNewer()
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
                val post = current.copy(content = trimmed)

                when (val photo = _photo.value) {
                    noPhoto -> repository.saveAsync(post)
                    else -> photo?.file?.let { file ->
                        repository.saveWithAttachment(post, MediaUpload(file))
                    } ?: repository.saveAsync(post)
                }
            }.onSuccess {
                edited.value = empty
                clearDraftPost()
                _photo.value = noPhoto
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
        draftPost.value = text
    }

    fun clearDraftPost() {
        draftPost.value = ""
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
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
    }

    fun retryLastAction() {
        lastRetryAction?.invoke()
        lastRetryAction = null
    }
}