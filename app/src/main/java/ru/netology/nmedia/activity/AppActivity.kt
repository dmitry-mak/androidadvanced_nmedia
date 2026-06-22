package ru.netology.nmedia.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.messaging.FirebaseMessaging
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.ActivityAppBinding
import ru.netology.nmedia.viewmodel.AuthViewModel

class AppActivity : AppCompatActivity() {

    private val viewModel by viewModels<AuthViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewModel.data.observe(this) {
            invalidateOptionsMenu()
        }

        requestNotificationPermission()

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {

                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.setGroupVisible(R.id.unauthenticated, !viewModel.authenticated)
                menu.setGroupVisible(R.id.authenticated, viewModel.authenticated)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                val navController = findNavController(R.id.main)
                return when (item.itemId) {
                    R.id.signin -> {
//                        AppAuth.getInstance().setAuth(5, "x-token")
                        navController.navigate(R.id.action_feedFragment_to_signInFragment)
                        true
                    }

                    R.id.signup -> {
                        AppAuth.getInstance().setAuth(5, "x-token")
                        true
                    }

                    R.id.signout -> {
                        if (navController.currentDestination?.id == R.id.newPostActivity) {
                            showSignOutConfirmation(navController)
                        } else {
                            AppAuth.getInstance().removeAuth()
                        }
                        true
                    }

                    else -> false
                }
            }
        })

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM Token", token)
        }
            .addOnFailureListener { e ->
                Log.w("FCM Token", "token error", e)
            }

//        AppAuth.getInstance().setAuth(5, "x-token")
    }

    //    запрос на проверку, разрешена ли отсылка уведомлений. Требования google, по документации
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        requestPermissions(arrayOf(permission), 1)
    }

    private fun showSignOutConfirmation(navController: NavController) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Sign out")
            .setMessage("Are you sure you want to quit")
            .setPositiveButton("Yes") { _, _ ->
                AppAuth.getInstance().removeAuth()
                navController.popBackStack()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}