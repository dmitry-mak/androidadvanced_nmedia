package ru.netology.nmedia.adapter

import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.DiffMethods
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.AttachmentType
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post


interface OnInteractionListener {
    fun onLike(post: Post)
    fun onShare(post: Post)
    fun onRemove(post: Post)
    fun onEdit(post: Post)
    fun onOpen(post: Post)
    fun onImageClick(imageUrl: String)
}


class PostAdapter(
    private val onInteractionListener: OnInteractionListener
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(PostDiffCallback) {

    companion object {
        private const val BASE_URL = "http://10.0.2.2:9999/"
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is Ad -> R.layout.card_ad
            is Post -> R.layout.card_post
            null -> error("unknown item type")
        }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            R.layout.card_post -> {
                val binding = CardPostBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                PostViewHolder(binding, onInteractionListener)
            }

            R.layout.card_ad -> {
                val binding = CardAdBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                AdViewHolder(binding)
            }

            else -> error("unknown view type: $viewType")
        }


    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = getItem(position)) {
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is Post -> (holder as? PostViewHolder)?.bind(item)
            null -> error("Unknown item type")
        }
    }


    class AdViewHolder(

        private val binding: CardAdBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(ad: Ad) {
//        binding.adImage.load("${BuildConfig.BASE_URL}/media/${ad.image}")
            Glide.with(binding.adImage.context)
                .load("${BASE_URL}media/${ad.image}")
                .into(binding.adImage)
        }
    }

    class PostViewHolder(
        private val binding: CardPostBinding,
        private val onInteractionListener: OnInteractionListener
    ) : RecyclerView.ViewHolder(binding.root) {

        //    private val BASE_URL = "http://10.0.2.2:9999/"
        fun bind(post: Post) {
            binding.apply {
                author.text = post.author
                publishDay.text = DiffMethods.getCurrentDateFormatted(post.published)
                postContent.text = post.content

                val avatarUrl = post.authorAvatar.takeIf {
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

                val attachment = post.attachment
                if (attachment?.type == AttachmentType.IMAGE) {
                    attachmentImage.visibility = View.VISIBLE

                    val attachmentUrl = "${BASE_URL}media/${attachment.url}"
                    Glide.with(attachmentImage.context)
                        .load(attachmentUrl)
                        .apply(
                            RequestOptions()
                                .timeout(10000)
                        )
                        .into(attachmentImage)

                    attachmentImage.setOnClickListener {
                        onInteractionListener.onImageClick(attachmentUrl)
                    }
                } else {
                    attachmentImage.visibility = View.GONE
                    attachmentImage.setOnClickListener(null)
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

                moreButton.isVisible = post.ownedByMe
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
                }
            }
        }
    }

    object PostDiffCallback : DiffUtil.ItemCallback<FeedItem>() {

        override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
            if (oldItem::class != newItem::class) {
                return false
            }

            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: FeedItem,
            newItem: FeedItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}