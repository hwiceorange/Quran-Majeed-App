package com.quran.quranaudio.online.quran_module.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quran.quranaudio.online.features.Helper.LastSurahAndAyahHelper
import com.quran.quranaudio.online.quran_module.data.LastReadRecord

/**
 * Repository for managing Last Read records
 * 
 * Provides functions to:
 * - Load last read position from Firestore (with local fallback)
 * - Save reading progress
 */
class LastReadRepository(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    companion object {
        private const val TAG = "LastReadRepository"
    }
    
    /**
     * 加载上次阅读位置
     * 优先从 Firestore 读取，如果用户未登录或读取失败，则从本地 SharedPreferences 读取
     */
    fun loadLastReadPosition(callback: (LastReadRecord?) -> Unit) {
        val user = auth.currentUser
        
        if (user == null) {
            Log.w(TAG, "User not logged in, loading from local storage")
            loadFromLocalStorage(callback)
            return
        }
        
        firestore.collection("users")
            .document(user.uid)
            .collection(LastReadRecord.COLLECTION_PATH)
            .document(LastReadRecord.DOCUMENT_ID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val record = document.toObject(LastReadRecord::class.java)
                        if (record != null && record.hasValidRecord()) {
                            Log.d(TAG, "Loaded from Firestore: Surah ${record.lastReadSurah}, Ayah ${record.lastReadAyah}")
                            callback(record)
                        } else {
                            Log.w(TAG, "Invalid Firestore record, loading from local storage")
                            loadFromLocalStorage(callback)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing Firestore record", e)
                        loadFromLocalStorage(callback)
                    }
                } else {
                    Log.w(TAG, "No Firestore record found, loading from local storage")
                    loadFromLocalStorage(callback)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load from Firestore", e)
                loadFromLocalStorage(callback)
            }
    }
    
    /**
     * 从本地 SharedPreferences 加载阅读位置
     */
    private fun loadFromLocalStorage(callback: (LastReadRecord?) -> Unit) {
        try {
            val lastSurah = LastSurahAndAyahHelper.getLastSurah(context)
            val lastAyah = LastSurahAndAyahHelper.getLastAyah(context)
            
            if (lastSurah > 0 && lastAyah > 0) {
                val record = LastReadRecord(
                    lastReadSurah = lastSurah,
                    lastReadAyah = lastAyah,
                    lastReadJuz = 0,
                    lastReadMode = LastReadRecord.MODE_SURAH, // 本地存储默认为 SURAH 模式
                    lastReadTimestamp = null
                )
                Log.d(TAG, "Loaded from local storage: Surah $lastSurah, Ayah $lastAyah")
                callback(record)
            } else {
                Log.w(TAG, "No valid local storage record found")
                callback(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from local storage", e)
            callback(null)
        }
    }
    
    /**
     * 检查用户是否登录
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    /**
     * 获取当前用户 ID
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}

