package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.DiffMethods
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentSinglePostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class SinglePostFragment : Fragment() {

    companion object {
        const val POST_ID = "postId"
    }

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val binding = FragmentSinglePostBinding.inflate(inflater, container, false)
        val postId = requireArguments().getLong(POST_ID)

        viewModel.data.observe(viewLifecycleOwner) { state ->
            val post = state.posts.firstOrNull { it.id == postId }
            if (post == null) {
                findNavController().navigateUp()
                return@observe
            }
            bindPost(binding, post)
        }
        return binding.root
    }

    private fun bindPost(binding: FragmentSinglePostBinding, post: Post) {
        val b = binding.post

        b.author.text = post.author
        b.publishDay.text = DiffMethods.getCurrentDateFormatted(post.published)
        b.postContent.text = post.content

        b.likeIcon.isChecked = post.likedByMe
        b.likeIcon.text = DiffMethods.convertNumber(post.likes)
        b.likeIcon.setOnClickListener {
            viewModel.like(post.id, post.likedByMe)
        }

        b.shareIcon.setOnClickListener {
            viewModel.share(post.id)
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, post.content)
            }
            val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
            startActivity(chooser)
        }

        b.moreButton.setOnClickListener { anchor ->
            PopupMenu(anchor.context, anchor).apply {
                inflate(R.menu.options_post)
                setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.remove -> {
                            viewModel.removeById(post.id)
                            findNavController().navigateUp()
                            true
                        }

                        R.id.edit -> {
                            viewModel.edit(post)
                            findNavController().navigate(R.id.action_singlePostFragment_to_newPostActivity)
                            true
                        }

                        else -> false
                    }
                }
            }.show()
        }
    }
}