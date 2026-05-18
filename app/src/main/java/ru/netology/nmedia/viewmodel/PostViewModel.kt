package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.IOException
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
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

    private val repository: PostRepository = PostRepositoryImpl()

    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel> get() = _data

    private val _actionError = MutableLiveData<String?>()
    val actionError: LiveData<String?> get() = _actionError

    val edited = MutableLiveData(empty)

    val draftPost = MutableLiveData("")

    private var lastRetryAction: (() -> Unit)? = null

    init {
        load()
    }

    fun load() {
        _data.postValue(FeedModel(loading = true))
        repository.getAllDataAsync(object : PostRepository.PostCallback<List<Post>> {
            override fun onSuccess(posts: List<Post>) {
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            }

            override fun onError(e: Throwable) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }



    fun like(id: Long, isLiked: Boolean) {
        lastRetryAction = { like(id, isLiked) }
        repository.likeAsync(id, isLiked, object : PostRepository.PostCallback<Post> {
            override fun onSuccess(post: Post) {
                val currentPosts = _data.value?.posts ?: emptyList()
                val updatedPosts = currentPosts.map { currentPost ->
                    if (currentPost.id == post.id) post else currentPost
                }
                _data.postValue(
                    FeedModel(
                        posts = updatedPosts,
                        empty = updatedPosts.isEmpty()
                    )
                )
            }

            override fun onError(e: Throwable) {
                handleError(e)
            }

        })
    }

    fun share(id: Long) {
        repository.share(id)
    }

    fun removeById(id: Long) {
        lastRetryAction = { removeById(id) }
        repository.removeByIdAsync(id, object : PostRepository.PostCallback<Unit> {
            override fun onSuccess(result: Unit) {
                load()
            }

            override fun onError(e: Throwable) {
            handleError(e)
            }
        })
    }

    fun save(text: String) {
        edited.value?.let { current ->
            if (current.content != text) {
                lastRetryAction = { save(text) }
                repository.saveAsync(
                    current.copy(content = text.trim()),
                    object : PostRepository.PostCallback<Post> {
                        override fun onSuccess(post: Post) {
                            load()
                            edited.postValue(empty)
                            clearDraftPost()
                        }

                        override fun onError(e: Throwable) {
                        handleError(e)
                        }
                    }
                )
            }
        }
    }

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
                in 300 ..309 -> "Ошибка редиректа"
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