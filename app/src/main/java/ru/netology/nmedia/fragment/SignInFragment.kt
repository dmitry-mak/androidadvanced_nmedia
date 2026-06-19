package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.databinding.FragmentSignInBinding
import ru.netology.nmedia.viewmodel.SignInState
import ru.netology.nmedia.viewmodel.SignInViewModel

class SignInFragment : Fragment() {

    private lateinit var binding: FragmentSignInBinding
    private val viewModel: SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignInBinding.inflate(inflater, container, false)
        setupListeners()
        setupObservers()
        return binding.root
    }

    private fun setupListeners() =
        with(binding) {
            signInButton.setOnClickListener {
                viewModel.authenticate(
                    login = loginInput.text?.toString().orEmpty(),
                    password = passwordInput.text?.toString().orEmpty()
                )
            }
        }

    private fun setupObservers() =
        with(binding) {
            viewModel.state.observe(viewLifecycleOwner) { state ->
                progress.isVisible = state.loading
                signInButton.isEnabled = !state.loading

                if (state.success) {
                    findNavController().previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("signInCompleted", true)
                    findNavController().popBackStack()
                    return@observe
                }
                errorText.isVisible = !state.errorMessage.isNullOrBlank()
                errorText.text = state.errorMessage
            }
        }

}