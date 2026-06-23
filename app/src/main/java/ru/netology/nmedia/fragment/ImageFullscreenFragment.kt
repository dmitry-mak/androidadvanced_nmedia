package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentImageFullscreenBinding

@AndroidEntryPoint
class ImageFullscreenFragment : Fragment() {

    companion object {
        const val IMAGE_URL = "imageUrl"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentImageFullscreenBinding.inflate(
            inflater,
            container,
            false
        )

        val imageUrl = requireArguments().getString(IMAGE_URL).orEmpty()

        Glide.with(binding.fullscreenAttachmentImage.context)
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .error(R.drawable.netology_48dp)
                    .timeout(10_000)
            )
            .into(binding.fullscreenAttachmentImage)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.root.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }
}