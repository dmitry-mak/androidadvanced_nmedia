package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {

    fun getAllDataAsync(callback: PostCallback<List<Post>>)

    fun likeAsync(
        id: Long,
        isLiked: Boolean,
        callback: PostCallback<Post>
    )
    fun removeByIdAsync(
        id: Long,
        callBack: PostCallback<Unit>
    )
    fun saveAsync(
        post: Post,
        callback: PostCallback<Post>
    )
    fun share(id: Long)

    interface PostCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: Throwable)
    }
}