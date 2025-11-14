package com.quran.quranaudio.online.prayertimes.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.quran.quranaudio.online.prayertimes.models.PrayerLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Repository for managing Prayer Logs
 */
class PrayerLogRepository {
    
    // Java-compatible callback interface
    interface PrayerLogCallback {
        fun onResult(log: PrayerLog?)
    }

    data class QadaSummary(
        val outstandingCount: Int,
        val completedCount: Int
    ) {
        val totalCount: Int
            get() = outstandingCount + completedCount
    }
    
    companion object {
        private const val TAG = "PrayerLogRepository"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Get today's prayer log status for a specific prayer
     * Returns null if no log exists (Pending state)
     */
    suspend fun getTodayPrayerLogStatus(prayerName: String): PrayerLog.PrayerStatus? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            val query = firestore.collection(PrayerLog.COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("prayerName", prayerName)
                .whereEqualTo("date", today)
                .orderBy("loggedAt", Query.Direction.DESCENDING)
                .limit(1)
            
            val snapshot = query.get().await()
            
            if (snapshot.isEmpty) {
                Log.d(TAG, "No log found for $prayerName today - Pending state")
                null // Pending state
            } else {
                val log = snapshot.documents[0].toObject(PrayerLog::class.java)
                Log.d(TAG, "Found log for $prayerName: ${log?.status}")
                log?.status
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting prayer log status for $prayerName", e)
            null
        }
    }
    
    /**
     * Get today's prayer log for a specific prayer
     * Returns null if no log exists
     */
    suspend fun getTodayPrayerLog(prayerName: String): PrayerLog? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            val query = firestore.collection(PrayerLog.COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("prayerName", prayerName)
                .whereEqualTo("date", today)
                .orderBy("loggedAt", Query.Direction.DESCENDING)
                .limit(1)
            
            val snapshot = query.get().await()
            
            if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents[0].toObject(PrayerLog::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting prayer log for $prayerName", e)
            null
        }
    }
    
    /**
     * Get all today's prayer logs
     */
    suspend fun getTodayPrayerLogs(): Map<String, PrayerLog> {
        return try {
            val userId = auth.currentUser?.uid ?: return emptyMap()
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            val query = firestore.collection(PrayerLog.COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .orderBy("loggedAt", Query.Direction.DESCENDING)
            
            val snapshot = query.get().await()
            
            val logs = mutableMapOf<String, PrayerLog>()
            
            // Group by prayerName and take the latest for each
            snapshot.documents.forEach { doc ->
                val log = doc.toObject(PrayerLog::class.java)
                if (log != null && !logs.containsKey(log.prayerName)) {
                    logs[log.prayerName] = log
                }
            }
            
            Log.d(TAG, "Retrieved ${logs.size} prayer logs for today")
            logs
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's prayer logs", e)
            emptyMap()
        }
    }
    
    /**
     * Java-compatible method to get today's prayer log for a specific prayer
     * Uses callback instead of suspend function
     */
    fun getTodayPrayerLogAsync(prayerName: String, callback: PrayerLogCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val log = getTodayPrayerLog(prayerName)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onResult(log)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in getTodayPrayerLogAsync", e)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onResult(null)
                }
            }
        }
    }
    
    /**
     * Java-compatible callback interface for getting all prayer logs
     */
    interface PrayerLogsCallback {
        fun onResult(logs: Map<String, PrayerLog>)
    }

    interface QadaSummaryCallback {
        fun onResult(summary: QadaSummary)
    }

    interface MarkMissedCallback {
        fun onComplete(created: Boolean)
        fun onError(e: Exception)
    }
    
    /**
     * Java-compatible method to get all today's prayer logs
     */
    fun getTodayPrayerLogsAsync(callback: PrayerLogsCallback) {
        Log.d(TAG, "📡 getTodayPrayerLogsAsync called")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔍 Querying prayer logs...")
                val logs = getTodayPrayerLogs()
                Log.d(TAG, "📥 Query returned ${logs.size} logs")
                
                // Log each entry
                logs.forEach { (key, value) ->
                    Log.d(TAG, "  📝 $key -> ${value.status} (${value.id})")
                }
                
                CoroutineScope(Dispatchers.Main).launch {
                    Log.d(TAG, "✅ Calling callback with ${logs.size} logs")
                    callback.onResult(logs)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in getTodayPrayerLogsAsync", e)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onResult(emptyMap())
                }
            }
        }
    }

    private suspend fun markPrayerAsMissedIfNeeded(prayerName: String, date: String): Boolean {
        val userId = auth.currentUser?.uid ?: return false

        val snapshot = firestore.collection(PrayerLog.COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .whereEqualTo("prayerName", prayerName)
            .whereEqualTo("date", date)
            .orderBy("loggedAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()

        if (!snapshot.isEmpty) {
            Log.d(TAG, "markPrayerAsMissedIfNeeded: existing log found for $prayerName on $date, skipping auto-miss.")
            return false
        }

        val isToday = date == LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val log = PrayerLog(
            userId = userId,
            prayerName = prayerName,
            status = PrayerLog.PrayerStatus.MISSED,
            performedAt = null,
            notes = "",
            date = date,
            isToday = isToday,
            tags = emptyList()
        )

        firestore.collection(PrayerLog.COLLECTION_NAME)
            .add(log)
            .await()

        Log.d(TAG, "markPrayerAsMissedIfNeeded: auto-marked $prayerName on $date as Missed.")
        return true
    }

    fun markPrayerAsMissedIfNeededAsync(
        prayerName: String,
        date: String,
        callback: MarkMissedCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val created = markPrayerAsMissedIfNeeded(prayerName, date)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onComplete(created)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error auto-marking $prayerName on $date as Missed", e)
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onError(e)
                }
            }
        }
    }

    /**
     * 获取用户的 Qada' 追溯起始日期配置
     * 如果不存在，返回 null
     */
    suspend fun getQadaStartDate(): String? {
        val userId = auth.currentUser?.uid ?: return null
        
        return try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("qadaConfig")
                .document("settings")
                .get()
                .await()
            
            if (snapshot.exists()) {
                snapshot.getString("qadaStartDate")
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Qada start date", e)
            null
        }
    }
    
    /**
     * Java-compatible callback interface for Qada start date retrieval
     */
    interface QadaStartDateCallback {
        fun onSuccess(startDate: String?)
        fun onError(e: Exception)
    }
    
    /**
     * Java-compatible async method to get Qada start date
     */
    fun getQadaStartDateAsync(callback: QadaStartDateCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startDate = getQadaStartDate()
                callback.onSuccess(startDate)
            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }
    
    /**
     * 设置用户的 Qada' 追溯起始日期
     */
    suspend fun setQadaStartDate(startDate: String) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
        
        try {
            val config = mapOf(
                "userId" to userId,
                "qadaStartDate" to startDate,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            firestore.collection("users")
                .document(userId)
                .collection("qadaConfig")
                .document("settings")
                .set(config)
                .await()
            
            Log.d(TAG, "Qada start date set to: $startDate")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting Qada start date", e)
            throw e
        }
    }

    /**
     * 计算 Qada' 汇总统计
     * 
     * 逻辑：
     * 1. 获取用户的 qadaStartDate
     * 2. 统计从 qadaStartDate 到昨天的所有祷告记录
     * 3. Outstanding (待弥补) = Missed + Pending
     * 4. Completed (已弥补) = Qada'
     * 
     * 注意：
     * - Pending 状态的祷告（没有记录）计入 Outstanding
     * - Ada' 状态的祷告不计入 Qada' 统计
     */
    suspend fun getQadaSummary(): QadaSummary {
        val userId = auth.currentUser?.uid ?: return QadaSummary(0, 0)

        return try {
            // 1. 获取 Qada 起始日期配置
            val startDateStr = getQadaStartDate()
            if (startDateStr == null) {
                Log.d(TAG, "No Qada start date configured, returning empty summary")
                return QadaSummary(0, 0)
            }
            
            val startDate = LocalDate.parse(startDateStr)
            val yesterday = LocalDate.now().minusDays(1)
            
            // 如果起始日期在今天或之后，没有待弥补的祷告
            if (startDate.isAfter(yesterday)) {
                Log.d(TAG, "Qada start date ($startDate) is after yesterday ($yesterday), no Qada needed")
                return QadaSummary(0, 0)
            }
            
            Log.d(TAG, "Computing Qada summary from $startDate to $yesterday")
            
            // 2. 获取该时间段内的所有祷告记录
            val snapshot = firestore.collection(PrayerLog.COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDateStr)
                .whereLessThan("date", LocalDate.now().toString())
                .get()
                .await()
            
            // 3. 按日期和祷告名称分组，统计每个祷告的最新状态
            val prayerRecords = mutableMapOf<String, PrayerLog.PrayerStatus?>()
            
            snapshot.documents.forEach { doc ->
                val date = doc.getString("date") ?: return@forEach
                val prayerName = doc.getString("prayerName") ?: return@forEach
                val statusValue = doc.getString("status") ?: return@forEach
                
                val key = "$date-$prayerName"
                
                try {
                    val status = PrayerLog.PrayerStatus.valueOf(statusValue.uppercase(Locale.ROOT))
                    
                    // 只保留最新的记录（通过 loggedAt 排序，但这里简化处理）
                    if (!prayerRecords.containsKey(key)) {
                        prayerRecords[key] = status
                    }
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Unknown prayer status: $statusValue", e)
                }
            }
            
            // 4. 计算总祷告数和统计
            val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, yesterday).toInt() + 1
            val totalPrayers = totalDays * 5 // 每天5次祷告
            
            var missedCount = 0
            var qadaCount = 0
            var adaCount = 0
            
            // 遍历所有日期和祷告
            val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
            var currentDate = startDate
            
            while (!currentDate.isAfter(yesterday)) {
                val dateStr = currentDate.toString()
                
                for (prayerName in prayerNames) {
                    val key = "$dateStr-$prayerName"
                    val status = prayerRecords[key]
                    
                    when (status) {
                        PrayerLog.PrayerStatus.MISSED -> missedCount++
                        PrayerLog.PrayerStatus.QADA -> qadaCount++
                        PrayerLog.PrayerStatus.ADA -> adaCount++
                        null -> missedCount++ // Pending 视为 Missed
                    }
                }
                
                currentDate = currentDate.plusDays(1)
            }
            
            // Outstanding = Missed + Pending (即 missedCount)
            // Completed = Qada
            val outstanding = missedCount
            val completed = qadaCount
            
            val summary = QadaSummary(outstanding, completed)
            Log.d(TAG, """
                |Qada summary computed:
                |  Period: $startDate to $yesterday ($totalDays days, $totalPrayers total prayers)
                |  Missed/Pending: $missedCount
                |  Qada' (completed): $qadaCount
                |  Ada' (on-time): $adaCount
                |  Outstanding: ${summary.outstandingCount}
                |  Completed: ${summary.completedCount}
                |  Total Qada: ${summary.totalCount}
            """.trimMargin())
            
            summary
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating Qada summary", e)
            QadaSummary(0, 0)
        }
    }

    fun getQadaSummaryAsync(callback: QadaSummaryCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val summary = try {
                getQadaSummary()
            } catch (e: Exception) {
                Log.e(TAG, "Error in getQadaSummaryAsync", e)
                QadaSummary(0, 0)
            }

            CoroutineScope(Dispatchers.Main).launch {
                callback.onResult(summary)
            }
        }
    }
    
    /**
     * 获取指定日期范围的祷告记录
     * 返回格式: Map<日期, Map<祷告名称, 祷告状态>>
     */
    /**
     * Data class to hold prayer log info with ID
     */
    data class PrayerLogInfo(
        val logId: String,
        val status: PrayerLog.PrayerStatus
    )
    
    suspend fun getPrayerLogsByDateRange(
        startDate: String,
        endDate: String
    ): Map<String, Map<String, PrayerLog.PrayerStatus>> {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not authenticated")
            return emptyMap()
        }
        
        return try {
            Log.d(TAG, "Loading prayer logs from $startDate to $endDate")
            
            val result = mutableMapOf<String, MutableMap<String, PrayerLog.PrayerStatus>>()
            
            val snapshot = firestore.collection("prayer_logs")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            Log.d(TAG, "Found ${snapshot.documents.size} prayer logs in date range")
            
            for (doc in snapshot.documents) {
                val log = doc.toObject(PrayerLog::class.java)
                if (log != null) {
                    val date = log.date
                    val prayerName = log.prayerName
                    val status = log.status
                    
                    if (!result.containsKey(date)) {
                        result[date] = mutableMapOf()
                    }
                    
                    // 如果同一天同一个祷告有多条记录，取最新的
                    result[date]!![prayerName] = status
                    
                    Log.d(TAG, "  $date $prayerName -> $status")
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading prayer logs by date range", e)
            emptyMap()
        }
    }
    
    /**
     * Get prayer logs by date range with log IDs
     * 返回格式: Map<日期, Map<祷告名称, PrayerLogInfo>>
     */
    suspend fun getPrayerLogsByDateRangeWithIds(
        startDate: String,
        endDate: String
    ): Map<String, Map<String, PrayerLogInfo>> {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "User not authenticated")
            return emptyMap()
        }
        
        return try {
            Log.d(TAG, "Loading prayer logs with IDs from $startDate to $endDate")
            
            val result = mutableMapOf<String, MutableMap<String, PrayerLogInfo>>()
            
            val snapshot = firestore.collection("prayer_logs")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()
            
            Log.d(TAG, "Found ${snapshot.documents.size} prayer logs in date range")
            
            for (doc in snapshot.documents) {
                val log = doc.toObject(PrayerLog::class.java)
                if (log != null && log.date.isNotEmpty() && log.prayerName.isNotEmpty()) {
                    val date = log.date
                    val prayerName = log.prayerName
                    val logInfo = PrayerLogInfo(doc.id, log.status)
                    
                    if (!result.containsKey(date)) {
                        result[date] = mutableMapOf()
                    }
                    result[date]!![prayerName] = logInfo
                }
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading prayer logs with IDs", e)
            emptyMap()
        }
    }
    
    /**
     * Java-compatible callback interface for date range query with IDs
     */
    interface DateRangeWithIdsCallback {
        fun onResult(data: @JvmSuppressWildcards Map<String, Map<String, PrayerLogInfo>>)
    }
    
    /**
     * Java-compatible async method for date range query with IDs
     */
    fun getPrayerLogsByDateRangeWithIdsAsync(
        startDate: String,
        endDate: String,
        callback: DateRangeWithIdsCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = getPrayerLogsByDateRangeWithIds(startDate, endDate)
            
            CoroutineScope(Dispatchers.Main).launch {
                callback.onResult(data)
            }
        }
    }
    
    /**
     * Java-compatible callback interface for date range query
     */
    interface DateRangeCallback {
        fun onResult(data: @JvmSuppressWildcards Map<String, Map<String, PrayerLog.PrayerStatus>>)
    }
    
    /**
     * Java-compatible async method for date range query
     */
    fun getPrayerLogsByDateRangeAsync(
        startDate: String,
        endDate: String,
        callback: DateRangeCallback
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = getPrayerLogsByDateRange(startDate, endDate)
            
            CoroutineScope(Dispatchers.Main).launch {
                callback.onResult(data)
            }
        }
    }
}

