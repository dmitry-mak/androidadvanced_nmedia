package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.AuthViewModel
import ru.netology.nmedia.viewmodel.PostViewModel

@AndroidEntryPoint
class FeedFragment : Fragment() {

    private lateinit var binding: FragmentFeedBinding
    private lateinit var adapter: PostAdapter

    private val viewModel: PostViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    private var scrollToTopAfterUpdate = false
    private var authDialog: AlertDialog? = null
    private var pendingAction: PendingAction? = null

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
                if (authViewModel.authenticated) {
                    viewModel.like(post.id, post.likedByMe)
                } else {
                    pendingAction = PendingAction.Like(post.id, post.likedByMe)
                    showAuthRequiredDialod()
                }
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

            override fun onImageClick(imageUrl: String) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_imageFullscreenFragment,
                    Bundle().apply {
                        putString(ImageFullscreenFragment.IMAGE_URL, imageUrl)
                    }
                )
            }
        })
        binding.list.adapter = adapter

        binding.retryButton.setOnClickListener {
//            viewModel.load()
            adapter.retry()
        }
    }

//    private fun setupObservers() {
//        viewModel.data.observe(viewLifecycleOwner) { model ->
//            adapter.submitList(model.posts) {
//                if (scrollToTopAfterUpdate) {
//                    binding.list.smoothScrollToPosition(0)
//                    scrollToTopAfterUpdate = false
//                }
//            }
//            binding.empty.isVisible = model.empty
//        }
//        viewModel.state.observe(viewLifecycleOwner) { state ->
//            binding.errorGroup.isVisible = state.error
//            binding.progress.isVisible = state.isLoading
//            binding.swipeRefresh.isRefreshing = state.refreshing
//        }
//        viewModel.actionError.observe(viewLifecycleOwner) { message ->
//            message?.let {
//                Snackbar
//                    .make(binding.root, it, Snackbar.LENGTH_LONG)
//                    .setAction("Повторить") {
//                        viewModel.retryLastAction()
//                    }
//                    .show()
//            }
//        }
//        viewModel.newerCount.observe(viewLifecycleOwner) { count ->
//            binding.newerPosts.isVisible = count > 0
//            binding.newerPosts.text = getString(R.string.newer_posts_notification, count)
////            println(it)
//        }
//        viewModel.newerPolling.observe(viewLifecycleOwner) {
//
//        }
//        findNavController().currentBackStackEntry
//            ?.savedStateHandle
//            ?.getLiveData<Boolean>("loginSuccess")
//            ?.observe(viewLifecycleOwner) { completedSignIn ->
//                if (completedSignIn == true) {
//                    handlePendingAction()
//                    findNavController().currentBackStackEntry
//                        ?.savedStateHandle
//                        ?.set("loginSuccess", false)
//                }
//            }
//    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest { pagingData ->
                    adapter.submitData(pagingData)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { state ->
                    binding.swipeRefresh.isRefreshing =
                        state.refresh is LoadState.Loading ||
                                state.prepend is LoadState.Loading ||
                                state.append is LoadState.Loading
                    binding.errorGroup.isVisible = state.refresh is LoadState.Error
                    binding.empty.isVisible =
                        state.refresh is LoadState.NotLoading && adapter.itemCount == 0

                    if (scrollToTopAfterUpdate && state.refresh is LoadState.NotLoading) {
                        binding.list.smoothScrollToPosition(0)
                        scrollToTopAfterUpdate = false
                    }
                }
            }
        }
        viewModel.state.observe(viewLifecycleOwner) { state ->

            if (state.error) binding.errorGroup.isVisible = true
        }

        viewModel.actionError.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar
                    .make(binding.root, it, Snackbar.LENGTH_LONG)
                    .setAction("Retry") {
                        viewModel.retryLastAction()
                    }
                    .show()
            }
        }

        viewModel.newerCount.observe(viewLifecycleOwner) { count ->
            binding.newerPosts.isVisible = count > 0
            binding.newerPosts.text = getString(R.string.newer_posts_notification, count)
        }

        viewModel.newerPolling.observe(viewLifecycleOwner) {
        }

        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("loginSuccess")
            ?.observe(viewLifecycleOwner) { completedSignIn ->
                if (completedSignIn == true) {
                    handlePendingAction()
                    findNavController().currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("loginSuccess", false)
                }

            }
    }

    private fun setupListeners() {
        binding.add.setOnClickListener {
            if (authViewModel.authenticated) {
                findNavController().navigate(R.id.action_feedFragment_to_newPostActivity)
            } else {
                pendingAction = PendingAction.AddPost
                showAuthRequiredDialod()
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
//            viewModel.refresh()
            adapter.refresh()
        }

        binding.newerPosts.setOnClickListener {
            scrollToTopAfterUpdate = true
            viewModel.showNewer()
//            binding.list.smoothScrollToPosition(0)
            adapter.refresh()
        }
    }

    private fun showAuthRequiredDialod() {
        if (authDialog?.isShowing == true) return
        authDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Operation for signed-in users only")
            .setMessage("Please sign-in to continue")
            .setPositiveButton("Sign-in") { _, _ ->
                findNavController().navigate(R.id.action_feedFragment_to_signInFragment)
            }
            .setNegativeButton("Cancel", null)
            .setOnDismissListener { authDialog = null }
            .show()
    }

    private fun handlePendingAction() {
        when (val action = pendingAction) {
            PendingAction.AddPost -> findNavController()
                .navigate(R.id.action_feedFragment_to_newPostActivity)

            is PendingAction.Like -> viewModel.like(action.postId, action.likedByMe)
            null -> Unit
        }
        pendingAction = null
    }

    override fun onDestroyView() {
        authDialog?.dismiss()
        authDialog = null
        super.onDestroyView()
    }

    private sealed interface PendingAction {
        data object AddPost : PendingAction
        data class Like(val postId: Long, val likedByMe: Boolean) : PendingAction
    }
}