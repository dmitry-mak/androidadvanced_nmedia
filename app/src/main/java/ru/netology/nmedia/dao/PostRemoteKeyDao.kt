package ru.netology.nmedia.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.netology.nmedia.entity.PostRemoteKeyEntity

@Dao
interface PostRemoteKeyDao {

//    @Query("SELECT MAX(`key`) FROM PostRemoteKeyEntity")
//    suspend fun max(): Long?

    @Query("SELECT `key` FROM PostRemoteKeyEntity WHERE type = 'AFTER' LIMIT 1 ")
    suspend fun after(): Long?

    @Query("SELECT `key` FROM PostRemoteKeyEntity WHERE type = 'BEFORE' LIMIT 1 ")
    suspend fun before(): Long?


//    @Query("SELECT MIN(`key`) FROM PostRemoteKeyEntity")
//    suspend fun min(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(postRemoteKeyEntity: List<PostRemoteKeyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(postRemoteKeyEntity: PostRemoteKeyEntity)

    @Query("DELETE FROM PostRemoteKeyEntity")
    suspend fun clear()
}