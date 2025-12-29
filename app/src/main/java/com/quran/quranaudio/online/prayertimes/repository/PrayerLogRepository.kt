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
     * 
     * @param todayPrayerTimes Optional DayPrayer for today to use actual prayer times
     */
    suspend fun getQadaSummary(todayPrayerTimes: com.quran.quranaudio.online.prayertimes.timings.DayPrayer? = null): QadaSummary {
        val userId = auth.currentUser?.uid ?: return QadaSummary(0, 0)

        return try {
            // 1. 获取 Qada 起始日期配置
            val startDateStr = getQadaStartDate()
            if (startDateStr == null) {
                Log.d(TAG, "No Qada start date configured, returning empty summary")
                return QadaSummary(0, 0)
            }
            
            val startDate = LocalDate.parse(startDateStr)
            val today = LocalDate.now()
            
            // ✅ 修改：使用 Qada Tracker 的计算规则，包括今天
            // 如果起始日期在今天之后，没有待弥补的祷告
            if (startDate.isAfter(today)) {
                Log.d(TAG, "Qada start date ($startDate) is after today ($today), no Qada needed")
                return QadaSummary(0, 0)
            }
            
            Log.d(TAG, "✅ 【统一计算规则】Computing Qada summary from $startDate to $today (including today's started prayers)")
            Log.d(TAG, "   📌 This calculation is shared between Salat page and Qada Tracker")
            
            // 2. 获取该时间段内的所有祷告记录（包括今天）
            // ✅ 按 loggedAt 降序排序，确保最新记录在前
            val snapshot = firestore.collection(PrayerLog.COLLECTION_NAME)
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDateStr)
                .whereLessThanOrEqualTo("date", today.toString())
                .orderBy("loggedAt", Query.Direction.DESCENDING)
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
                    
                    // ✅ 只保留最新的记录（由于已按loggedAt降序，第一次遇到的就是最新的）
                    if (!prayerRecords.containsKey(key)) {
                        prayerRecords[key] = status
                    } else {
                        Log.d(TAG, "   🔄 Duplicate ignored: $date-$prayerName (keeping latest: ${prayerRecords[key]})")
                    }
                } catch (e: IllegalArgumentException) {
                    Log.w(TAG, "Unknown prayer status: $statusValue", e)
                }
            }
            
            // 4. 计算总祷告数和统计
            // ✅ 修改：包括今天，所以计算范围到今天
            val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, today).toInt() + 1
            
            var missedCount = 0
            var qadaCount = 0
            var adaCount = 0
            var totalCountedPrayers = 0 // 实际计入的祷告数
            
            // 遍历所有日期和祷告
            val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
            var currentDate = startDate
            
            // 🔍 用于追踪哪些祷告被计为MISSED
            val missedPrayersList = mutableListOf<String>()
            
            while (!currentDate.isAfter(today)) {
                val dateStr = currentDate.toString()
                val isToday = currentDate.isEqual(today)
                
                if (isToday) {
                    Log.d(TAG, "🔍 Processing TODAY ($dateStr) prayers:")
                    Log.d(TAG, "   todayPrayerTimes: ${if (todayPrayerTimes != null) "✅ Available" else "❌ NULL"}")
                    if (todayPrayerTimes != null) {
                        Log.d(TAG, "   todayPrayerTimes.timings: ${if (todayPrayerTimes.timings != null) "✅ Available" else "❌ NULL"}")
                    }
                }
                
                for (prayerName in prayerNames) {
                    // ✅ 今天的祷告：只计入已开始的
                    if (isToday && !hasPrayerWindowStarted(prayerName, todayPrayerTimes)) {
                        Log.d(TAG, "   ⏭️ SKIPPED: $prayerName (window not started)")
                        continue // 跳过未开始的祷告
                    }
                    
                    totalCountedPrayers++
                    
                    val key = "$dateStr-$prayerName"
                    val status = prayerRecords[key]
                    
                    if (isToday) {
                        Log.d(TAG, "   ✅ COUNTED: $prayerName -> ${status ?: "NULL (→ MISSED)"}")
                    }
                    
                    when (status) {
                        PrayerLog.PrayerStatus.MISSED -> {
                            missedCount++
                            missedPrayersList.add("$dateStr-$prayerName [MISSED]")
                        }
                        PrayerLog.PrayerStatus.QADA -> qadaCount++
                        PrayerLog.PrayerStatus.ADA -> adaCount++
                        null -> {
                            missedCount++ // Pending 视为 Missed
                            missedPrayersList.add("$dateStr-$prayerName [NULL/PENDING]")
                        }
                    }
                }
                
                currentDate = currentDate.plusDays(1)
            }
            
            // 🔍 输出所有被计为MISSED的祷告
            if (missedPrayersList.isNotEmpty()) {
                Log.d("QadaDiagnosis", "🔍 ═══ MISSED/PENDING Prayers Details ═══")
                missedPrayersList.forEach { prayer ->
                    Log.d("QadaDiagnosis", "   ❌ $prayer")
                }
                Log.d("QadaDiagnosis", "   Total MISSED/PENDING: ${missedPrayersList.size}")
                Log.d("QadaDiagnosis", "═══════════════════════════════════════════════")
            }
            
            // Outstanding = Missed + Pending (即 missedCount)
            // Completed = Qada
            val outstanding = missedCount
            val completed = qadaCount
            
            val summary = QadaSummary(outstanding, completed)
            Log.d(TAG, """
                |Qada summary computed:
                |  Period: $startDate to $today ($totalDays days)
                |  Total Counted Prayers: $totalCountedPrayers (including today's started prayers)
                |  Missed/Pending: $missedCount
                |  Qada' (completed): $qadaCount
                |  Ada' (on-time): $adaCount
                |  Outstanding: ${summary.outstandingCount}
                |  Completed: ${summary.completedCount}
                |  Total Qada: ${summary.totalCount}
            """.trimMargin())
            
            // 🔍 统一计算规则的诊断日志
            Log.d("QadaDiagnosis", "═══════════════════════════════════════════════")
            Log.d("QadaDiagnosis", "📊 【统一计算规则】Qada Summary (Used by Both Salat & QadaTracker)")
            Log.d("QadaDiagnosis", "   ✅ Calculation Source: PrayerLogRepository.getQadaSummary()")
            Log.d("QadaDiagnosis", "   📅 Date Range: $startDate to $today")
            Log.d("QadaDiagnosis", "   📆 Total Days: $totalDays")
            Log.d("QadaDiagnosis", "   🔢 Total Counted Prayers: $totalCountedPrayers")
            Log.d("QadaDiagnosis", "   ❌ Outstanding (Missed/Pending): $outstanding")
            Log.d("QadaDiagnosis", "   ✅ Completed (Qada): $completed")
            Log.d("QadaDiagnosis", "   📈 Expected for full period: ${totalDays * 5} prayers")
            Log.d("QadaDiagnosis", "   ⏭️  Unstarted today: ${totalDays * 5 - totalCountedPrayers}")
            Log.d("QadaDiagnosis", "═══════════════════════════════════════════════")
            
            summary
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating Qada summary", e)
            QadaSummary(0, 0)
        }
    }

    /**
     * ✅ 统一的祷告窗口判断逻辑 (与 QadaTrackerActivity 完全一致)
     * Check if a prayer's window has started today
     * Uses actual prayer times when available, falls back to conservative estimates
     * @param prayerName The prayer to check
     * @param dayPrayer Optional DayPrayer with actual prayer times
     */
    fun hasPrayerWindowStarted(prayerName: String, dayPrayer: com.quran.quranaudio.online.prayertimes.timings.DayPrayer?): Boolean {
        // Try to use actual prayer times first
        if (dayPrayer != null && dayPrayer.timings != null) {
            try {
                val now = java.time.LocalDateTime.now()
                val nextPrayerTime = getNextPrayerTime(prayerName, dayPrayer)
                
                if (nextPrayerTime != null) {
                    val hasStarted = now.isAfter(nextPrayerTime) || now.isEqual(nextPrayerTime)
                    Log.d(TAG, "✅ Prayer: $prayerName, Next: ${nextPrayerTime.toLocalTime()}, Now: ${now.toLocalTime()}, Started: $hasStarted")
                    return hasStarted
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking prayer time for $prayerName, using fallback", e)
            }
        }
        
        // Fallback to conservative time estimates if actual times not available
        Log.d(TAG, "⚠️ Using fallback time check for $prayerName (no actual prayer times)")
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)

        return when (prayerName) {
            "Fajr" -> hour >= 11 // After typical Dhuhr time
            "Dhuhr" -> hour >= 15 // After typical Asr time  
            "Asr" -> hour >= 18 // After typical Maghrib time
            "Maghrib" -> hour >= 20 // After typical Isha time
            "Isha" -> hour >= 23 // Close to midnight
            else -> false
        }
    }
    
    /**
     * Get the NEXT prayer's time (used to determine if current prayer window has passed)
     * @return LocalDateTime of the next prayer, or null if not available
     */
    private fun getNextPrayerTime(prayerName: String, dayPrayer: com.quran.quranaudio.online.prayertimes.timings.DayPrayer): java.time.LocalDateTime? {
        val timings = dayPrayer.timings ?: return null
        
        return when (prayerName) {
            "Fajr" -> timings[com.quran.quranaudio.online.prayertimes.common.PrayerEnum.DHOHR]
            "Dhuhr" -> timings[com.quran.quranaudio.online.prayertimes.common.PrayerEnum.ASR]
            "Asr" -> timings[com.quran.quranaudio.online.prayertimes.common.PrayerEnum.MAGHRIB]
            "Maghrib" -> timings[com.quran.quranaudio.online.prayertimes.common.PrayerEnum.ICHA]
            "Isha" -> {
                // Isha is the last prayer, use 3 hours after Isha as boundary
                val isha = timings[com.quran.quranaudio.online.prayertimes.common.PrayerEnum.ICHA]
                isha?.plusHours(3)
            }
            else -> null
        }
    }
    
    /**
     * Java-compatible async method to get Qada summary
     * @param todayPrayerTimes Optional DayPrayer for today to use actual prayer times
     * @param callback Callback to receive the result
     */
    fun getQadaSummaryAsync(todayPrayerTimes: com.quran.quranaudio.online.prayertimes.timings.DayPrayer?, callback: QadaSummaryCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            val summary = try {
                getQadaSummary(todayPrayerTimes)
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
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()
            
            Log.d(TAG, "Found ${snapshot.documents.size} prayer logs in date range")
            
            // ✅ 先按 loggedAt 排序（最新的在前），然后去重
            val sortedDocs = snapshot.documents.sortedByDescending { doc ->
                doc.getTimestamp("loggedAt")?.toDate()?.time ?: 0L
            }
            
            for (doc in sortedDocs) {
                val log = doc.toObject(PrayerLog::class.java)
                if (log != null) {
                    val date = log.date
                    val prayerName = log.prayerName
                    val status = log.status
                    
                    // ✅ 注意：prayerName 可能是英语或本地化名称
                    // 向后兼容由调用者处理（QadaTrackerActivity会标准化查询）
                    
                    if (!result.containsKey(date)) {
                        result[date] = mutableMapOf()
                    }
                    
                    // ✅ 去重逻辑：如果同一天同一个祷告有多条记录，只保留第一条（最新的）
                    if (!result[date]!!.containsKey(prayerName)) {
                        result[date]!![prayerName] = status
                        Log.d(TAG, "  $date $prayerName -> $status")
                    } else {
                        Log.d(TAG, "  🔄 Duplicate log ignored: $date $prayerName (keeping the latest one)")
                    }
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
        
        // ✅ 【诊断日志】Repository 层查询信息
        Log.d(TAG, "════════════════════════════════════════════════════════")
        Log.d(TAG, "🔍 getPrayerLogsByDateRangeWithIds()")
        Log.d(TAG, "   🔐 Query Parameters:")
        Log.d(TAG, "      User ID: $userId")
        Log.d(TAG, "      Start Date: $startDate")
        Log.d(TAG, "      End Date: $endDate")
        Log.d(TAG, "      Collection: prayer_logs")
        
        if (userId == null) {
            Log.e(TAG, "❌ User not authenticated - returning empty map")
            Log.e(TAG, "   This means FirebaseAuth.getCurrentUser() returned null")
            Log.e(TAG, "   Check if anonymous/Google sign-in was successful")
            Log.d(TAG, "════════════════════════════════════════════════════════")
            return emptyMap()
        }
        
        return try {
            Log.d(TAG, "   → Querying Firestore...")
            
            val result = mutableMapOf<String, MutableMap<String, PrayerLogInfo>>()
            
            val snapshot = firestore.collection("prayer_logs")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .await()
            
            Log.d(TAG, "   ✅ Firestore query completed")
            Log.d(TAG, "   📦 Found ${snapshot.documents.size} documents")
            
            if (snapshot.documents.isEmpty()) {
                Log.w(TAG, "   ⚠️ NO DOCUMENTS FOUND!")
                Log.w(TAG, "   Possible reasons:")
                Log.w(TAG, "   1. No prayer logs exist for userId=$userId in date range [$startDate, $endDate]")
                Log.w(TAG, "   2. Prayer logs were saved with a different userId")
                Log.w(TAG, "   3. Date format in Firestore doesn't match query format")
                Log.w(TAG, "   4. Firestore rules are blocking the read")
                Log.d(TAG, "════════════════════════════════════════════════════════")
                return emptyMap()
            }
            
            // ✅ 详细打印每个文档
            Log.d(TAG, "   📄 Document Details:")
            snapshot.documents.forEachIndexed { index, doc ->
                Log.d(TAG, "   [$index] Document ID: ${doc.id}")
                Log.d(TAG, "        userId: ${doc.getString("userId")}")
                Log.d(TAG, "        date: ${doc.getString("date")}")
                Log.d(TAG, "        prayerName: ${doc.getString("prayerName")}")
                Log.d(TAG, "        status: ${doc.getString("status")}")
                Log.d(TAG, "        loggedAt: ${doc.getTimestamp("loggedAt")}")
            }
            
            // ✅ 先按 loggedAt 排序（最新的在前），然后去重
            val sortedDocs = snapshot.documents.sortedByDescending { doc ->
                doc.getTimestamp("loggedAt")?.toDate()?.time ?: 0L
            }
            
            for (doc in sortedDocs) {
                val log = doc.toObject(PrayerLog::class.java)
                if (log != null) {
                    val date = log.date
                    val prayerName = log.prayerName
                    
                    // 跳过无效记录
                    if (date.isEmpty() || prayerName.isEmpty()) {
                        Log.w(TAG, "  ⚠️ Skipping invalid log: doc.id=${doc.id}, date='$date', prayerName='$prayerName'")
                        continue
                    }
                    
                    val logInfo = PrayerLogInfo(doc.id, log.status)
                    
                    // ✅ 注意：prayerName 可能是英语或本地化名称
                    // 向后兼容由调用者处理（QadaTrackerActivity会标准化查询）
                    
                    if (!result.containsKey(date)) {
                        result[date] = mutableMapOf()
                    }
                    
                    // ✅ 去重逻辑：如果同一天同一个祷告有多条记录，只保留第一条（最新的）
                    if (!result[date]!!.containsKey(prayerName)) {
                        result[date]!![prayerName] = logInfo
                        Log.d(TAG, "  ✅ $date $prayerName -> ${log.status} (docId: ${doc.id})")
                    } else {
                        Log.d(TAG, "  🔄 Duplicate log ignored: $date $prayerName (keeping the latest one)")
                    }
                }
            }
            
            Log.d(TAG, "   📊 Processing Summary:")
            Log.d(TAG, "      Processed ${result.size} unique dates")
            Log.d(TAG, "      Total unique prayers: ${result.values.sumOf { it.size }}")
            Log.d(TAG, "════════════════════════════════════════════════════════")
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading prayer logs with IDs", e)
            Log.e(TAG, "   Exception: ${e.message}")
            Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
            Log.d(TAG, "════════════════════════════════════════════════════════")
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

