package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import retrofit2.HttpException
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: PostApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb,
) : RemoteMediator<Int, PostEntity>() {


    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val result = when (loadType) {
                LoadType.REFRESH -> {
                    val maxId = postRemoteKeyDao.max()
                    if (maxId == null) {
                        apiService.getLatest(state.config.pageSize)
                    } else {
                        apiService.getAfter(maxId, state.config.pageSize)
                    }
//                    apiService.getLatest(state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val minId = postRemoteKeyDao.min() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getBefore(minId, state.config.pageSize)
                }

                LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            }

            if (!result.isSuccessful) {
                throw HttpException(result)
            }


            val body = result.body() ?: throw HttpException(result)

            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        if (body.isEmpty()) return@withTransaction

                        val wasEmpty = postRemoteKeyDao.max() == null
                        if (wasEmpty) {
                            postRemoteKeyDao.insert(
                                listOf(
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.AFTER,
                                        body.first().id
                                    ),
                                    PostRemoteKeyEntity(
                                        PostRemoteKeyEntity.KeyType.BEFORE,
                                        body.last().id
                                    )
                                )
                            )
                        } else {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER,
                                    body.first().id
                                )
                            )
                        }
                    }

                    LoadType.PREPEND -> {
                    }

                    LoadType.APPEND -> {
                        if (body.isEmpty()) return@withTransaction
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.BEFORE,
                                body.last().id
                            )
                        )
                    }
                }
                postDao.insert(body.map(PostEntity::fromDto))
            }

            val endOfPagination = loadType == LoadType.APPEND && body.isEmpty()
            return MediatorResult.Success(
                endOfPaginationReached = endOfPagination
            )
        } catch (e: Exception) {
            return MediatorResult.Error(e)
        }
    }
}
