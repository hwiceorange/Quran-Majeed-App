package com.quran.quranaudio.online.account

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Deletes all app-owned cloud data before deleting the authenticated identity. */
object AccountDeletionManager {
    interface Callback {
        fun onSuccess()
        fun onError(message: String)
    }

    private val userSubcollections = listOf(
        "learningPlan",
        "dailyProgress",
        "streakStats",
        "salahRecords",
        "learningState",
        "learning_state",
        "qadaConfig",
        "unlocked_content"
    )

    suspend fun deleteCurrentAccount(context: Context) {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser ?: error("No account is currently signed in")
        val uid = user.uid
        val firestore = FirebaseFirestore.getInstance()

        // Authentication is deliberately deleted last: Firestore security rules need the UID
        // while the user's documents are being removed.
        val userDocument = firestore.collection("users").document(uid)
        userSubcollections.forEach { collection ->
            deleteCollection(userDocument.collection(collection))
        }
        deleteQueryCollection(firestore.collection("prayer_logs").whereEqualTo("userId", uid))
        deleteQueryCollection(firestore.collection("feedback_submissions").whereEqualTo("userId", uid))
        userDocument.delete().await()
        user.delete().await()

        // Remove identity-bound local entitlement/session caches without touching downloaded
        // Quran content, prayer settings, bookmarks, or other offline user functionality.
        context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
        context.getSharedPreferences("google_auth_prefs", Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    @JvmStatic
    fun deleteCurrentAccount(context: Context, callback: Callback) {
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                deleteCurrentAccount(context.applicationContext)
                callback.onSuccess()
            } catch (error: Exception) {
                callback.onError(error.localizedMessage ?: "Unable to delete account")
            }
        }
    }

    private suspend fun deleteCollection(collection: CollectionReference) {
        while (true) {
            val snapshot = collection.limit(200).get().await()
            if (snapshot.isEmpty) return
            val batch = FirebaseFirestore.getInstance().batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }

    private suspend fun deleteQueryCollection(query: com.google.firebase.firestore.Query) {
        while (true) {
            val snapshot = query.limit(200).get().await()
            if (snapshot.isEmpty) return
            val batch = FirebaseFirestore.getInstance().batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }
}
