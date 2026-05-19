package ru.netology.nmedia.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import kotlin.collections.map

class PostRepositoryImpl(
    private val dao: PostDao
) : PostRepository {
    override val posts: LiveData<List<Post>> = dao.getAll().map {
        it.map(PostEntity::toDto)
    }

    override suspend fun getAllDataAsync() {
        val posts = PostApi.service.getAllData()
        dao.insert(posts.map(PostEntity::fromDto))
    }

    override suspend fun likeAsync(
        id: Long,
        isLiked: Boolean
    ): Post {
        dao.likeById(id)

        try {
            val post = if (isLiked) {
                PostApi.service.unlikeById(id)
            } else {
                PostApi.service.likeById(id)
            }

            dao.insert(PostEntity.fromDto(post))
            return post
        } catch (
            e: Exception
        ) {
            dao.likeById(id)
            throw e
        }
    }

    override suspend fun removeByIdAsync(id: Long) {
        dao.removeById(id)
        PostApi.service.deleteById(id)
    }

    override suspend fun saveAsync(post: Post): Post {
        val saved = PostApi.service.save(post)
        dao.insert(PostEntity.fromDto(saved))
        return saved
    }

    override suspend fun share(id: Long) {

    }
}