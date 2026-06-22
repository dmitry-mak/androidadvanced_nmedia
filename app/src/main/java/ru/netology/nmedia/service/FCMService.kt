package ru.netology.nmedia.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import ru.netology.nmedia.R

class FCMService : FirebaseMessagingService() {

    private val channelId = "server"

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_remote_name)
            val descriptionText = getString(R.string.channel_remote_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        println(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        println(Gson().toJson(message))

        message.data["action"]?.let { actionFromServer ->
            val action = try {
                Action.valueOf(actionFromServer)
            } catch (e: IllegalArgumentException) {
                return
            }
            when (action) {
                Action.LIKE -> handleLike(
                    Gson().fromJson(
                        message.data["content"],
                        Like::class.java
                    )
                )
                Action.NEW_POST -> handleNewPost(
                    Gson().fromJson(
                        message.data["content"],
                        NewPost::class.java
                    )
                )
            }
        }
    }

    fun handleLike(like: Like) {
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentText(
                getString(
                    R.string.notification_user_liked,
                    like.userName,
                    like.postAuthor
                )
            )
            .build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(1, notification)
        }
    }

    fun handleNewPost(newPost: NewPost) {
        val fullPostText = newPost.postContent
        val previewText = fullPostText
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .let { postText -> if (postText.length <= 60) postText else postText.take(30) + "..." }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_new_post, newPost.userName))
//            .setContentText(newPost.postContent)
            .setContentText(previewText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullPostText))
            .build()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(2, notification)
        }

    }
}

enum class Action {
    LIKE,
    NEW_POST
}

data class Like(
    val userId: Int,
    val userName: String,
    val postId: Int,
    val postAuthor: String
)

data class NewPost(
    val userId: Int,
    val userName: String,
    val postId: Int,
    val postContent: String
)
