package ru.netology.nmedia.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post


@Entity
class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String = "",
    val content: String,
    val published: Long,
    val likedByMe: Boolean = false,
    val likes: Int = 0,
    val attachment: String? = null,
    val attachmentDescription: String? = null,
    val attachmentType: AttachmentType? = null,
    val hidden: Boolean = false,
) {
    fun toDto() = Post(
        id = id,
        authorId = authorId,
        author = author,
        authorAvatar = authorAvatar,
        content = content,
        published = published,
        likes = likes,
        likedByMe = likedByMe,
        attachment = attachment?.let { url ->
            attachmentType?.let { type ->
                Attachment(
                    url = url,
                    description = attachmentDescription,
                    type = type,
                )
            }
        },
        ownedByMe = false
    )

    companion object {
        fun fromDto(post: Post, hidden: Boolean = false) = PostEntity(
            post.id,
            authorId = post.authorId,
            post.author,
            post.authorAvatar,
            post.content,
            post.published,
            post.likedByMe,
            post.likes,
            post.attachment?.url,
            attachmentDescription = post.attachment?.description,
            post.attachment?.type,
            hidden = hidden
        )
    }
}