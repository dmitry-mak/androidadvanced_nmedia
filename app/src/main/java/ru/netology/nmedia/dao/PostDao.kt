package ru.netology.nmedia.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.netology.nmedia.entity.PostEntity

@Dao
interface PostDao {
    @Query("SELECT * FROM PostEntity ORDER BY id DESC")
    fun getAll(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(posts: List<PostEntity>)

    @Query("UPDATE PostEntity SET content = :text WHERE id = :id")
    suspend fun updateContentById(id: Long, text: String)

    suspend fun save(post: PostEntity) =
        if (post.id == 0L) insert(post) else updateContentById(post.id, post.content)

    @Query("""
                UPDATE PostEntity SET
                likes = likes + CASE WHEN likedByMe = 1 THEN -1 ELSE 1 END,
                likedByMe = CASE WHEN likedByMe = 1 THEN 0 ELSE 1 END
                WHERE id = :id;
                """)
   suspend fun likeById(id: Long)

    @Query("DELETE FROM PostEntity WHERE id = :id")
   suspend fun removeById(id: Long)

}