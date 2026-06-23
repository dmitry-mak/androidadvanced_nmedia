package ru.netology.nmedia.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.auth.AppAuth
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(auth: AppAuth): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level =
                if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                auth.authState.value.takeIf {
                    !it.token.isNullOrEmpty()
                }?.let {
                    val newRequest = chain.request().newBuilder()
                        .addHeader("Authorization", requireNotNull(it.token))
                        .build()
                    return@addInterceptor chain.proceed(newRequest)
                }
                chain.proceed(chain.request())
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(client: OkHttpClient): AuthApiService {
        return Retrofit.Builder()
            .baseUrl("${BuildConfig.BASE_URL}/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePostApiService(client: OkHttpClient): PostApiService {
        return Retrofit.Builder()
            .baseUrl("${BuildConfig.BASE_URL}/api/slow/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(PostApiService::class.java)
    }
}