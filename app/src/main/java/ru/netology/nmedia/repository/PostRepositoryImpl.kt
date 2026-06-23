package ru.netology.nmedia.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import ru.netology.nmedia.api.PostApiService
//import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.map

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val dao: PostDao,
    private val apiService: PostApiService
) : PostRepository {

    //    override val posts = dao.getAll().map {
//        it.map {
//            it.toDto()
//        }
//    }
    override val posts: Flow<List<Post>> = dao.getAll()
        .map { entities ->
            entities.map(PostEntity::toDto)
        }.flowOn(Dispatchers.Default)


    override val newerCount: Flow<Int> =
        dao.getHiddenCount()
            .flowOn(Dispatchers.Default)


//    override suspend fun getAllDataAsync() {
//        val posts = PostApi.service.getAllData()
//        dao.clear()
//        dao.insert(posts.map(PostEntity::fromDto))
//    }

    override suspend fun getAllDataAsync() {
        val maxId = dao.getMaxId()
        val hiddenPostsId = dao.getHiddenPostsId().toSet()
//        val posts = PostApi.service.getAllData()
        val posts = apiService.getAllData()
        dao.clear()

        dao.insert(
            posts.map { post ->
                PostEntity.fromDto(
                    post = post,
                    hidden = post.id in hiddenPostsId || (maxId != null && post.id > maxId)
                )
            }
        )
    }

    override suspend fun showNewer() {
        dao.showHidden()
    }

    override fun getNewer(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L)

            val lastId = dao.getMaxId() ?: id
//            val response = PostApi.service.getNewer(lastId)
            val response = apiService.getNewer(lastId)

            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            val body = response.body().orEmpty()

            if (body.isNotEmpty()) {
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
//                PostApi.service.unlikeById(id)
                apiService.unlikeById(id)
            } else {
//                PostApi.service.likeById(id)
                apiService.likeById(id)
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
//        PostApi.service.deleteById(id)
        apiService.deleteById(id)
    }

    override suspend fun saveAsync(post: Post): Post {
//        val saved = PostApi.service.save(post)
        val saved = apiService.save(post)
        dao.insert(PostEntity.fromDto(saved))
        return saved
    }

    override suspend fun share(id: Long) {

    }

    override suspend fun saveWithAttachment(
        post: Post,
        upload: MediaUpload
    ): Post {
        val media = upload(upload)
        return saveAsync(
            post.copy(
                attachment = Attachment(
                    url = media.id,
                    description = null,
                    type = AttachmentType.IMAGE,
                )
            )
        )
    }

    override suspend fun upload(upload: MediaUpload): Media {

        val media = MultipartBody.Part.createFormData(
            "file", upload.file.name, upload.file.asRequestBody()
        )

//        val response = PostApi.service.uploadMedia(media)
        val response = apiService.uploadMedia(media)
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        return response.body() ?: throw HttpException(response)
    }


}