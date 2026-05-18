package ru.netology.nmedia.adapter

import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ru.netology.nmedia.DiffMethods
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.R
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.Post


interface OnInteractionListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
    fun onOpen(post: Post)
}

class PostAdapter(
    private val onInteractionListener: OnInteractionListener
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(
        holder: PostViewHolder,
        position: Int
    ) {
        val post = getItem(position)
        holder.bind(post)
    }

}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {

    private val BASE_URL = "http://10.0.2.2:9999/"
    fun bind(post: Post) {
        binding.apply {
            author.text = post.author
            publishDay.text = DiffMethods.getCurrentDateFormatted(post.published)
            postContent.text = post.content

            val avatarUrl = post.authorAvatar?.takeIf {
                it.isNotBlank()
            }?.let { "${BASE_URL}avatars/$it" }

            Glide.with(avatar.context)
                .load(avatarUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.netology_48dp)
                        .error(R.drawable.netology_48dp)
                        .fallback(R.drawable.netology_48dp)
                        .circleCrop()
                        .timeout(10000)
                )
                .into(avatar)

            val attachment =post.attachment
            if(attachment?.type == AttachmentType.IMAGE) {
                attachmentImage.visibility = View.VISIBLE

                val attachmentUrl = "${BASE_URL}images/${attachment.url}"
                Glide.with(attachmentImage.context)
                    .load(attachmentUrl)
                    .apply (
                        RequestOptions()
                            .timeout(10000)
                    )
                    .into(attachmentImage)
            }else{
                attachmentImage.visibility= View.GONE
                Glide.with(attachmentImage.context).clear(attachmentImage)
            }

            binding.root.setOnClickListener { onInteractionListener.onOpen(post) }
            postContent.setOnClickListener { onInteractionListener.onOpen(post) }

            likeIcon.isChecked = post.likedByMe
            likeIcon.text = DiffMethods.convertNumber(post.likes)
            likeIcon.setOnClickListener {
                onInteractionListener.onLike(post)
            }
            shareIcon.setOnClickListener { onInteractionListener.onShare(post) }
            moreButton.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }}}}

        object PostDiffCallback : DiffUtil.ItemCallback<Post>() {

            override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: Post,
                newItem: Post
            ): Boolean {
                return oldItem == newItem
            }
        }