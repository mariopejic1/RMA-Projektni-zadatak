package com.pejic.campmate.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.pejic.campmate.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class NotificationViewModel(
    private val context: Context
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val isProcessingNotifications = AtomicBoolean(false)

    init {
        createNotificationChannel()
        setupAuthStateListener()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "campsite_ratings",
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
            Log.d("Notification", "Notification channel created successfully")
        }
    }

    private fun setupAuthStateListener() {
        auth.addAuthStateListener { firebaseAuth ->
            val userId = firebaseAuth.currentUser?.uid
            if (userId != null && !isProcessingNotifications.get()) {
                Log.d("Notification", "User logged in: $userId, checking for pending notifications")
                sendPendingNotifications(userId)
            } else if (userId == null) {
                Log.d("Notification", "No user logged in")
            } else {
                Log.d("Notification", "Already processing notifications for user: $userId")
            }
        }
    }

    fun sendLocalNotification(
        context: Context,
        creatorId: String,
        raterFirstName: String,
        raterLastName: String,
        campsiteName: String,
        rating: Double
    ) {
        viewModelScope.launch {
            try {
                // Provjera postoji li već identična notifikacija
                val snapshot = db.collection("notifications")
                    .whereEqualTo("creatorId", creatorId)
                    .whereEqualTo("campsiteName", campsiteName)
                    .whereEqualTo("raterFirstName", raterFirstName)
                    .whereEqualTo("raterLastName", raterLastName)
                    .whereEqualTo("rating", rating)
                    .whereEqualTo("processed", false)
                    .get()
                    .await()

                if (snapshot.isEmpty) {
                    val notificationData = hashMapOf(
                        "creatorId" to creatorId,
                        "raterFirstName" to raterFirstName,
                        "raterLastName" to raterLastName,
                        "campsiteName" to campsiteName,
                        "rating" to rating,
                        "timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                        "processed" to false
                    )
                    db.collection("notifications")
                        .add(notificationData)
                        .await()
                    Log.d("Notification", "Notification queued for creator: $creatorId, campsite: $campsiteName, rating: $rating")
                } else {
                    Log.d("Notification", "Duplicate notification detected for creator: $creatorId, campsite: $campsiteName")
                }
            } catch (e: Exception) {
                Log.e("Notification", "Error queuing notification: ${e.message}", e)
            }
        }
    }

    private fun sendPendingNotifications(userId: String) {
        if (!isProcessingNotifications.compareAndSet(false, true)) {
            Log.d("Notification", "Already processing notifications, skipping")
            return
        }

        viewModelScope.launch {
            try {
                val snapshot = db.collection("notifications")
                    .whereEqualTo("creatorId", userId)
                    .whereEqualTo("processed", false)
                    .get()
                    .await()
                Log.d("Notification", "Found ${snapshot.size()} pending notifications for user: $userId")

                snapshot.documents.forEach { doc ->
                    try {
                        val creatorId = doc.getString("creatorId") ?: return@forEach
                        val raterFirstName = doc.getString("raterFirstName") ?: "Unknown"
                        val raterLastName = doc.getString("raterLastName") ?: "Unknown"
                        val campsiteName = doc.getString("campsiteName") ?: "Unknown"
                        val rating = doc.getDouble("rating") ?: 0.0

                        val message = context.getString(
                            R.string.notification_message,
                            "$raterFirstName $raterLastName",
                            campsiteName,
                            String.format("%.1f", rating)
                        )
                        val notification = NotificationCompat.Builder(context, "campsite_ratings")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle(context.getString(R.string.notification_title))
                            .setContentText(message)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .build()

                        notificationManager.notify(
                            System.currentTimeMillis().toInt(),
                            notification
                        )
                        Log.d("Notification", "Sent notification: $message")

                        db.collection("notifications")
                            .document(doc.id)
                            .update("processed", true)
                            .await()
                        Log.d("Notification", "Marked notification as processed: ${doc.id}")
                    } catch (e: Exception) {
                        Log.e("Notification", "Error sending notification for doc ${doc.id}: ${e.message}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("Notification", "Error fetching pending notifications: ${e.message}", e)
            } finally {
                isProcessingNotifications.set(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("Notification", "NotificationViewModel cleared")
    }
}
