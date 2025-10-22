package com.quran.quranaudio.online.quests.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * User's current learning position in the Quran.
 * 
 * Tracks the exact position where the user left off, enabling them to
 * continue from the correct page/verse/juz on their next session.
 * 
 * Firestore path: users/{userId}/learning_state/current
 */
data class UserLearningState(
    /** Current Surah number (1-114) */
    @PropertyName("current_surah")
    val currentSurah: Int = 1,
    
    /** Current Ayah number within the Surah */
    @PropertyName("current_ayah")
    val currentAyah: Int = 1,
    
    /** Current Mushaf page number (1-604) */
    @PropertyName("current_page")
    val currentPage: Int = 1,
    
    /** Current Juz' number (1-30) */
    @PropertyName("current_juz")
    val currentJuz: Int = 1,
    
    /** Total verses read so far */
    @PropertyName("total_verses_read")
    val totalVersesRead: Int = 0,
    
    /** Total pages read so far */
    @PropertyName("total_pages_read")
    val totalPagesRead: Int = 0,
    
    /** Last reading session timestamp */
    @PropertyName("last_read_at")
    val lastReadAt: Timestamp? = null,
    
    /** When this state was last updated */
    @PropertyName("updated_at")
    val updatedAt: Timestamp = Timestamp.now()
) {
    companion object {
        /** Firestore collection path */
        const val COLLECTION_PATH = "learning_state"
        const val DOCUMENT_ID = "current"
    }
}

