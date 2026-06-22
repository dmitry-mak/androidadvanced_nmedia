package ru.netology.nmedia.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentNewPostBinding
import ru.netology.nmedia.viewmodel.PostViewModel

class NewPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentNewPostBinding.inflate(
            inflater,
            container,
            false
        )

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            val text = binding.content.text?.toString().orEmpty()
            val isCreatingNew = (viewModel.edited.value?.id ?: 0L) == 0L
            if (isCreatingNew) {
                viewModel.setDraftPost(text)
            }
            findNavController().navigateUp()
        }

        viewModel.edited.observe(viewLifecycleOwner) { post ->
            if (post.id != 0L) {
                binding.content.setText(post.content)
                binding.content.setSelection(binding.content.text?.length ?: 0)
            } else {
                val draftText = viewModel.draftPost.value.orEmpty()
                val initialText =
                    requireActivity().intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val textToShow = when {
                    draftText.isNotBlank() -> draftText
                    initialText.isNotBlank() -> initialText
                    else -> ""
                }
                if (textToShow.isNotBlank()) {
                    binding.content.setText(textToShow)
                    binding.content.setSelection(binding.content.text?.length ?: 0)
                    requireActivity().intent.removeExtra(Intent.EXTRA_TEXT)
                } else {
                    binding.content.setText("")
                }
            }
        }

        binding.save.setOnClickListener {
            val text = binding.content.text.toString().trim()
            if (text.isBlank()) {

                Snackbar.make(
                    binding.root,
                    R.string.empty_notificaton,
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            viewModel.save(text)
            findNavController().navigateUp()
        }

        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                viewModel.cancelEditing()
            }
        })

        val chooosePhoto =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                if (resultCode == Activity.RESULT_OK) {
                    val fileUri = data?.data!!

                    viewModel.changePhoto(fileUri, fileUri.toFile())
                } else if (resultCode == ImagePicker.RESULT_ERROR) {
                    Toast.makeText(requireContext(), ImagePicker.getError(data), Toast.LENGTH_SHORT)
                        .show()
                }
            }

//        viewModel.photo.observe(viewLifecycleOwner) {
//            binding.photo.setImageURI(it.uri)
//            binding.removePhoto.isVisible = it.uri != null
//        }
//
//        binding.photoContainer.visibility= View.VISIBLE
//        binding.photo.setImageURI()

        viewModel.photo.observe(viewLifecycleOwner){
            if (it.uri == null){
                binding.photoContainer.visibility = View.GONE
                binding.photo.setImageURI(null)
                return@observe
            }
            binding.photoContainer.visibility = View.VISIBLE
            binding.photo.setImageURI(it.uri)
        }

        binding.removePhoto.setOnClickListener {
            viewModel.changePhoto(null, null)
        }

        activity?.addMenuProvider(
            provider = object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_new_post, menu)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    return when (item.itemId) {
                        R.id.save -> {
                            val text = binding.content.text.toString().trim()
                            if (text.isBlank()) {
                                Snackbar.make(
                                    binding.root,
                                    R.string.empty_notificaton,
                                    Snackbar.LENGTH_LONG
                                ).show()
                                false
                            } else {
                                viewModel.save(text)
                                findNavController().navigateUp()
                                true
                            }
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner
        )

        binding.pickPhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .galleryOnly()
                .galleryMimeTypes(arrayOf("image/png", "image/jpg"))
                .createIntent(chooosePhoto::launch)
        }

        binding.takePhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(2048)
                .cameraOnly()
//                .galleryMimeTypes(arrayOf("image/png", "image/jpg"))
                .createIntent(chooosePhoto::launch)
        }

        return binding.root
    }
}