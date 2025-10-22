package com.quran.quranaudio.online.quests.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.quran.quranaudio.online.quests.data.SalahName
import com.quran.quranaudio.online.quests.data.SalahRecord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Repository for managing Salah (Prayer) records in Firestore.
 * 
 * Firestore Path: users/{userId}/salahRecords/{YYYY-MM-DD}
 * 
 * Features:
 * - Real-time observation of today's prayer record
 * - Toggle prayer completion status
 * - Automatic document creation for new days
 */
class SalahRepository {
    
    companion object {
        private const val TAG = "SalahRepository"
        private const val USERS_COLLECTION = "users"
        private const val SALAH_RECORDS_COLLECTION = "salahRecords"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Gets the current authenticated user's ID.
     * @throws IllegalStateException if user is not logged in
     */
    private fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not logged in. Salah records require authentication.")
    }
    
    /**
     * Returns the Firestore path for today's salah record.
     * Path: users/{userId}/salahRecords/{YYYY-MM-DD}
     */
    private fun getTodaySalahRecordPath(date: LocalDate = LocalDate.now()): String {
        val userId = getUserId()
        val dateId = date.toString() // YYYY-MM-DD
        return "$USERS_COLLECTION/$userId/$SALAH_RECORDS_COLLECTION/$dateId"
    }
    
    /**
     * Observes today's salah record in real-time.
     * Creates a new document if it doesn't exist.
     * 
     * @return Flow<SalahRecord?> emitting null if not logged in or error occurs
     */
    fun observeTodaySalahRecord(): Flow<SalahRecord?> = callbackFlow {
        // Check if user is logged in before setting up listener
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not logged in, cannot observe salah records")
            trySend(null)
            close()
            return@callbackFlow
        }
        
        val userId = currentUser.uid
        val dateId = LocalDate.now().toString()
        val path = "$USERS_COLLECTION/$userId/$SALAH_RECORDS_COLLECTION/$dateId"
        val docRef = firestore.document(path)
        
        Log.d(TAG, "Observing salah record at: $path")
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing salah record", error)
                trySend(null)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val record = snapshot.toObject(SalahRecord::class.java)
                Log.d(TAG, "Salah record updated: ${record?.getTotalCompleted() ?: 0}/5 completed")
                trySend(record)
            } else {
                // Document doesn't exist, create initial record
                Log.d(TAG, "Salah record doesn't exist, creating new record")
                val initialRecord = SalahRecord(
                    userId = userId,
                    dateId = dateId,
                    fajr = false,
                    dhuhr = false,
                    asr = false,
                    maghrib = false,
                    isha = false,
                    lastUpdatedUtc = Timestamp.now(),
                    createdAt = Timestamp.now()
                )
                trySend(initialRecord)
            }
        }
        
        // ✅ Only ONE awaitClose call - this is critical!
        awaitClose { 
            Log.d(TAG, "Removing salah record listener")
            listener.remove() 
        }
    }
    
    /**
     * Gets today's salah record (one-time read).
     * Returns a default record if it doesn't exist.
     * 
     * @return SalahRecord or a new default record
     */
    suspend fun getTodaySalahRecord(): SalahRecord {
        return try {
            val userId = getUserId()
            val dateId = LocalDate.now().toString()
            val path = getTodaySalahRecordPath()
            val snapshot = firestore.document(path).get().await()
            
            if (snapshot.exists()) {
                snapshot.toObject(SalahRecord::class.java) ?: createDefaultRecord(userId, dateId)
            } else {
                createDefaultRecord(userId, dateId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's salah record", e)
            createDefaultRecord(getUserId(), LocalDate.now().toString())
        }
    }
    
    /**
     * Toggles the completion status of a specific prayer.
     * 
     * @param salahName The prayer to toggle (FAJR, DHUHR, ASR, MAGHRIB, ISHA)
     */
    suspend fun toggleSalahStatus(salahName: SalahName) {
        try {
            val userId = getUserId()
            val dateId = LocalDate.now().toString()
            val path = getTodaySalahRecordPath()
            val docRef = firestore.document(path)
            
            Log.d(TAG, "Toggling salah status: ${salahName.name}")
            
            // Get current status
            val snapshot = docRef.get().await()
            val currentRecord = if (snapshot.exists()) {
                snapshot.toObject(SalahRecord::class.java) ?: createDefaultRecord(userId, dateId)
            } else {
                createDefaultRecord(userId, dateId)
            }
            
            // Toggle the specific prayer
            val updatedRecord = when (salahName) {
                SalahName.FAJR -> currentRecord.copy(fajr = !currentRecord.fajr)
                SalahName.DHUHR -> currentRecord.copy(dhuhr = !currentRecord.dhuhr)
                SalahName.ASR -> currentRecord.copy(asr = !currentRecord.asr)
                SalahName.MAGHRIB -> currentRecord.copy(maghrib = !currentRecord.maghrib)
                SalahName.ISHA -> currentRecord.copy(isha = !currentRecord.isha)
            }.copy(
                userId = userId,
                dateId = dateId,
                lastUpdatedUtc = Timestamp.now()
            )
            
            // Save to Firestore
            docRef.set(updatedRecord, SetOptions.merge()).await()
            
            Log.d(TAG, "✅ ${salahName.name} toggled to ${when (salahName) {
                SalahName.FAJR -> updatedRecord.fajr
                SalahName.DHUHR -> updatedRecord.dhuhr
                SalahName.ASR -> updatedRecord.asr
                SalahName.MAGHRIB -> updatedRecord.maghrib
                SalahName.ISHA -> updatedRecord.isha
            }}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling salah status for ${salahName.name}", e)
            throw e
        }
    }
    
    /**
     * Sets the completion status of a specific prayer.
     * 
     * @param salahName The prayer to update
     * @param isCompleted The new completion status
     */
    suspend fun setSalahStatus(salahName: SalahName, isCompleted: Boolean) {
        try {
            val userId = getUserId()
            val dateId = LocalDate.now().toString()
            val path = getTodaySalahRecordPath()
            val docRef = firestore.document(path)
            
            Log.d(TAG, "Setting ${salahName.name} to $isCompleted")
            
            // Get current record or create new one
            val snapshot = docRef.get().await()
            val currentRecord = if (snapshot.exists()) {
                snapshot.toObject(SalahRecord::class.java) ?: createDefaultRecord(userId, dateId)
            } else {
                createDefaultRecord(userId, dateId)
            }
            
            // Update the specific prayer
            val updatedRecord = when (salahName) {
                SalahName.FAJR -> currentRecord.copy(fajr = isCompleted)
                SalahName.DHUHR -> currentRecord.copy(dhuhr = isCompleted)
                SalahName.ASR -> currentRecord.copy(asr = isCompleted)
                SalahName.MAGHRIB -> currentRecord.copy(maghrib = isCompleted)
                SalahName.ISHA -> currentRecord.copy(isha = isCompleted)
            }.copy(
                userId = userId,
                dateId = dateId,
                lastUpdatedUtc = Timestamp.now()
            )
            
            // Save to Firestore
            docRef.set(updatedRecord, SetOptions.merge()).await()
            
            Log.d(TAG, "✅ ${salahName.name} set to $isCompleted. Total completed: ${updatedRecord.getTotalCompleted()}/5")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting salah status for ${salahName.name}", e)
            throw e
        }
    }
    
    /**
     * Creates a default (empty) salah record.
     */
    private fun createDefaultRecord(userId: String, dateId: String): SalahRecord {
        return SalahRecord(
            userId = userId,
            dateId = dateId,
            fajr = false,
            dhuhr = false,
            asr = false,
            maghrib = false,
            isha = false,
            lastUpdatedUtc = Timestamp.now(),
            createdAt = Timestamp.now()
        )
    }
}


