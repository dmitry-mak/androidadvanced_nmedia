package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator

import retrofit2.HttpException
import ru.netology.nmedia.api.PostApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: PostApiService,
    private val postDao: PostDao
) : RemoteMediator<Int, PostEntity>() {

//    override fun getRefreshKey(state: PagingState<Long, Post>): Long? = null

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
//        TODO("Not yet implemented")

//    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, Post> {
        try {
            val result = when (loadType) {
                LoadType.REFRESH -> {
                    apiService.getLatest(state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val id = state.lastItemOrNull()?.id ?: return MediatorResult.Success(false)
                    apiService.getBefore(id,state.config.pageSize)
                    //                    apiService.getBefore(id = params.key, count = params.loadSize)
                }

                LoadType.PREPEND -> {
                    val id = state.firstItemOrNull()?.id ?: return MediatorResult.Success(false)
                    apiService.getAfter(id, state.config.pageSize)

                }
//                    return LoadResult.Page(
//                    data = emptyList(), nextKey = null, prevKey = null
//                )
            }

            if (!result.isSuccessful) {
                throw HttpException(result)
            }


            val body = result.body() ?: throw HttpException(result)

//            val nextKey = if (body.isEmpty()) null else body.last().id

            postDao.insert(body.map (PostEntity::fromDto))

            return MediatorResult.Success(
                body.isEmpty()
            )
        }catch (e : Exception){
            return MediatorResult.Error(e)
        }
//            val data = result.body().orEmpty()
//            return LoadResult.Page(
//                data = data,
//                prevKey = params.key,
//                nextKey = data.lastOrNull()?.id
//            )
//        } catch (e: IOException) {
//            return LoadResult.Error(e)
//        } catch (e: Exception) {
//            return LoadResult.Error(e)
//        }
    }
}
