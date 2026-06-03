package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel

class FeedFragment : Fragment() {

    private lateinit var binding: FragmentFeedBinding
    private lateinit var adapter: PostAdapter

    private val viewModel: PostViewModel by activityViewModels()

    private var scrollToTopAfterUpdate = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedBinding.inflate(inflater, container, false)
        setupAdapter()
        setupObservers()
        setupListeners()
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    private fun setupAdapter() {
        adapter = PostAdapter(object : OnInteractionListener {
            override fun onLike(post: Post) {
                viewModel.like(post.id, post.likedByMe)
            }

            override fun onShare(post: Post) {
                viewModel.share(post.id)
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, post.content)
                }
                val chooser = Intent.createChooser(intent, getString(R.string.chooser_share_post))
                startActivity(chooser)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
            }

            override fun onEdit(post: Post) {
                viewModel.edit(post)
                findNavController().navigate(R.id.action_feedFragment_to_newPostActivity)

            }

            override fun onOpen(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_singlePostFragment,
                    Bundle().apply { putLong(SinglePostFragment.POST_ID, post.id) }
                )
            }
        })
        binding.list.adapter = adapter

        binding.retryButton.setOnClickListener {
            viewModel.load()
        }
    }

    private fun setupObservers() {
        viewModel.data.observe(viewLifecycleOwner) { model ->
            adapter.submitList(model.posts) {
                if (scrollToTopAfterUpdate) {
                    binding.list.smoothScrollToPosition(0)
                    scrollToTopAfterUpdate = false
                }
            }
            binding.empty.isVisible = model.empty
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->
            binding.errorGroup.isVisible = state.error
            binding.progress.isVisible = state.isLoading
            binding.swipeRefresh.isRefreshing = state.refreshing
        }
        viewModel.actionError.observe(viewLifecycleOwner) { message ->
            message?.let {
                Snackbar
                    .make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Повторить") {
                        viewModel.retryLastAction()
                    }
                    .show()
            }
        }
        viewModel.newerCount.observe(viewLifecycleOwner) { count ->
            binding.newerPosts.isVisible = count > 0
            binding.newerPosts.text = getString(R.string.newer_posts_notification, count)
//            println(it)
        }
        viewModel.newerPolling.observe(viewLifecycleOwner) {

        }
    }

    private fun setupListeners() {
        binding.add.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostActivity)
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.newerPosts.setOnClickListener {
            scrollToTopAfterUpdate = true
            viewModel.showNewer()
//            binding.list.smoothScrollToPosition(0)
        }
    }
}