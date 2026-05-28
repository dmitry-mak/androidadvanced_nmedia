package ru.netology.nmedia.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import kotlin.collections.map

class PostRepositoryImpl(
    private val dao: PostDao
) : PostRepository {

    override val posts = dao.getAll().map {
        it.map {
            it.toDto()
        }
    }
    override val newerCount: Flow<Int> =
        dao.getHiddenCount()
            .flowOn(Dispatchers.Default)


    override suspend fun getAllDataAsync() {
        val posts = PostApi.service.getAllData()
        dao.clear()
        dao.insert(posts.map(PostEntity::fromDto))
    }

    override suspend fun showNewer() {
        dao.showHidden()
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)

            val  lastId = dao.getMaxId() ?: id
            val response = PostApi.service.getNewer(lastId)

            if(!response.isSuccessful){
                throw HttpException(response)
            }

            val body = response.body().orEmpty()

            if (body.isNotEmpty()){
                dao.insert(body.map {
                    PostEntity.fromDto(it, hidden = true)
                })
            }
            emit(body.size)
        }
    }.flowOn(Dispatchers.Default)

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