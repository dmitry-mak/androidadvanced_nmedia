package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String = "",
    val content: String,
    val published: Long,
//    @SerializedName("likes")
    val likes: Int = 0,
//    val sharesCount: Int = 0,
//    @SerializedName("likedByMe")
    val likedByMe: Boolean = false,
//    val video: String? = null
    val attachment: Attachment? = null,
    val ownedByMe: Boolean
)

data class Attachment(
    val url: String,
    val description: String?,
    val type: AttachmentType
)

enum class AttachmentType {
    IMAGE,
    VIDEO
}