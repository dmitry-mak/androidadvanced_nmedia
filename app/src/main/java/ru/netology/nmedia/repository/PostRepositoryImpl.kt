package ru.netology.nmedia.repository

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostApi
import ru.netology.nmedia.dto.Post

class PostRepositoryImpl : PostRepository {

    override fun getAllDataAsync(callback: PostRepository.PostCallback<List<Post>>) {
        PostApi.service.getAllData()
            .enqueue(object : Callback<List<Post>> {
                override fun onResponse(
                    call: Call<List<Post>>,
                    response: Response<List<Post>>
                ) {
                    if (!response.isSuccessful) {
                        val code = response.code()
                        val message = response.message()
                        val body = response.errorBody()?.string()
//                        callback.onError(Exception("error"))
                        callback.onError(ApiError(code, message, body))
                        return
                    }
//                    val body = response.body()
//                    if (body == null) {
//                        callback.onError(RuntimeException("body is null"))
//                        return
                    val body = response.body() ?: run {
                        callback.onError(RuntimeException("body is null"))
                        return
                    }
                    callback.onSuccess(body)
                }

                override fun onFailure(call: Call<List<Post>>, t: Throwable) {
//                    callback.onError(Exception("error"))
                    callback.onError(t)
                }
            })
    }

    override fun likeAsync(
        id: Long,
        isLiked: Boolean,
        callback: PostRepository.PostCallback<Post>
    ) {
        val call = if (isLiked) {
            PostApi.service.unlikeById(id)
        } else {
            PostApi.service.likeById(id)
        }
        call.enqueue(object : Callback<Post> {
            override fun onResponse(
                call: Call<Post>,
                response: Response<Post>
            ) {
                if (!response.isSuccessful) {
                    val errorCode = response.code()
                    val errorMessage = response.message()
                    val errorBody = response.errorBody()?.string()

                    callback.onError(ApiError(errorCode, errorMessage, errorBody))
                    return
                }
                val body = response.body()
                if (body == null) {
                    callback.onError(RuntimeException("body is null"))
                    return
                }
                callback.onSuccess(body)
            }

            override fun onFailure(
                call: Call<Post>,
                t: Throwable
            ) {
                callback.onError(t)
            }

        })
    }


    override fun removeByIdAsync(
        id: Long,
        callBack: PostRepository.PostCallback<Unit>
    ) {
        PostApi.service.deleteById(id)
            .enqueue(object : Callback<Unit> {
                override fun onResponse(
                    call: Call<Unit>,
                    response: Response<Unit>
                ) {
                    if (!response.isSuccessful) {
                        val code = response.code()
                        val message = response.message()
                        val body = response.errorBody()?.string()
                        callBack.onError(ApiError(code,message,body))
                        return
                    }
                    callBack.onSuccess(Unit)
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    callBack.onError(t)
                }
            })
    }

    override fun saveAsync(post: Post, callback: PostRepository.PostCallback<Post>) {
        PostApi.service.save(post)
            .enqueue(object : Callback<Post> {
                override fun onResponse(
                    call: Call<Post>,
                    response: Response<Post>
                ) {
                    if (!response.isSuccessful) {
                        val code = response.code()
                        val message = response.message()
                        val body = response.errorBody()?.string()
                        callback.onError(ApiError(code,message,body))
                        return
                    }
                    val body = response.body()
                    if (body == null) {
                        callback.onError(Exception("body is null"))
                        return
                    }
                    callback.onSuccess(body)
                }

                override fun onFailure(
                    call: Call<Post>,
                    t: Throwable
                ) {
                    callback.onError(t)
                }
            })
    }

    override fun share(id: Long) {
    }
}


data class ApiError(
    val code: Int,
    val httpMessage: String,
    val body: String? = null
) : Exception("HTTP $code: $httpMessage")
