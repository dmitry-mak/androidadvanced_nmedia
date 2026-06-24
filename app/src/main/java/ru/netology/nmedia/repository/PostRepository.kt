package ru.netology.nmedia.repository

import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post

interface PostRepository {

    val posts: Flow<List<Post>>

    val newerCount: Flow<Int>

    fun getNewer (id: Long): Flow<Int>

    suspend fun getAllDataAsync()

    suspend fun showNewer()

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

    suspend fun saveWithAttachment(
        post: Post,
        upload: MediaUpload
    ): Post

    suspend fun upload(upload: MediaUpload): Media

}