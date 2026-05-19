package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import ru.netology.nmedia.dto.Post

interface PostRepository {

    val posts: LiveData<List<Post>>

    //    suspend fun getAllDataAsync():List<Post>
    suspend fun getAllDataAsync()
    suspend fun likeAsync(
        id: Long,
        isLiked: Boolean,
    ): Post

    suspend fun removeByIdAsync(
        id: Long,
    )

    suspend fun saveAsync(
        post: Post,
    ): Post

    suspend fun share(id: Long)

}