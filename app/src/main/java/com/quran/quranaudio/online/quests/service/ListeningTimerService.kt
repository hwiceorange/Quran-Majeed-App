package com.quran.quranaudio.online.quests.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quests.repository.QuestRepository
import com.quran.quranaudio.online.quests.constants.FirestoreConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Foreground Service 用于 Quran Listening 任务计时
 * 
 * 功能：
 * - 后台计时（支持锁屏收听）
 * - 显示持久通知
 * - 计时完成后标记任务并更新学习状态
 */
class ListeningTimerService : Service() {

    companion object {
        private const val TAG = "ListeningTimerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "quran_listening_timer"
        
        const val EXTRA_TARGET_MINUTES = "TARGET_MINUTES"
        const val EXTRA_SURAH = "SURAH"
        const val EXTRA_AYAH = "AYAH"
        const val EXTRA_PAGE = "PAGE"
        const val EXTRA_JUZ = "JUZ"
        
        const val ACTION_START_TIMER = "START_TIMER"
        const val ACTION_STOP_TIMER = "STOP_TIMER"
        const val ACTION_COMPLETE_TASK = "COMPLETE_TASK"
        
        /**
         * 启动计时器
         */
        fun startListeningTimer(
            context: Context,
            targetMinutes: Int,
            surah: Int,
            ayah: Int,
            page: Int = 1,
            juz: Int = 1
        ) {
            val intent = Intent(context, ListeningTimerService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_TARGET_MINUTES, targetMinutes)
                putExtra(EXTRA_SURAH, surah)
                putExtra(EXTRA_AYAH, ayah)
                putExtra(EXTRA_PAGE, page)
                putExtra(EXTRA_JUZ, juz)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 停止计时器并保存学习状态
         */
        fun stopListeningTimer(
            context: Context,
            surah: Int,
            ayah: Int,
            page: Int = 1,
            juz: Int = 1
        ) {
            val intent = Intent(context, ListeningTimerService::class.java).apply {
                action = ACTION_STOP_TIMER
                putExtra(EXTRA_SURAH, surah)
                putExtra(EXTRA_AYAH, ayah)
                putExtra(EXTRA_PAGE, page)
                putExtra(EXTRA_JUZ, juz)
            }
            context.startService(intent)
        }
        
        /**
         * 标记任务完成
         */
        fun completeTask(context: Context) {
            val intent = Intent(context, ListeningTimerService::class.java).apply {
                action = ACTION_COMPLETE_TASK
            }
            context.startService(intent)
        }
    }
    
    private var targetMinutes: Int = 0
    private var elapsedSeconds: Int = 0
    private var surah: Int = 1
    private var ayah: Int = 1
    private var page: Int = 1
    private var juz: Int = 1
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    
    private lateinit var questRepository: QuestRepository
    private lateinit var notificationManager: NotificationManager
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        val firestore = FirebaseFirestore.getInstance()
        questRepository = QuestRepository(firestore)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_TIMER -> {
                targetMinutes = intent.getIntExtra(EXTRA_TARGET_MINUTES, 15)
                surah = intent.getIntExtra(EXTRA_SURAH, 1)
                ayah = intent.getIntExtra(EXTRA_AYAH, 1)
                page = intent.getIntExtra(EXTRA_PAGE, 1)
                juz = intent.getIntExtra(EXTRA_JUZ, 1)
                
                startForeground(NOTIFICATION_ID, createNotification(0, targetMinutes))
                startTimer()
            }
            ACTION_STOP_TIMER -> {
                surah = intent.getIntExtra(EXTRA_SURAH, surah)
                ayah = intent.getIntExtra(EXTRA_AYAH, ayah)
                page = intent.getIntExtra(EXTRA_PAGE, page)
                juz = intent.getIntExtra(EXTRA_JUZ, juz)
                
                saveCurrentState()
                stopSelf()
            }
            ACTION_COMPLETE_TASK -> {
                markTaskComplete()
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        Log.d(TAG, "Service destroyed")
    }
    
    /**
     * 创建通知渠道 (Android O+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quran Listening Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示 Quran 收听计时器进度"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建前台通知
     */
    private fun createNotification(elapsedMinutes: Int, targetMinutes: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val remainingMinutes = targetMinutes - elapsedMinutes
        val progress = (elapsedMinutes * 100) / targetMinutes
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quran Listening")
            .setContentText("$remainingMinutes minutes remaining")
            .setSmallIcon(R.drawable.ic_quran)
            .setContentIntent(contentIntent)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    /**
     * 启动计时器
     */
    private fun startTimer() {
        timerJob?.cancel()
        elapsedSeconds = 0
        
        timerJob = serviceScope.launch {
            Log.d(TAG, "Timer started: target=$targetMinutes minutes")
            
            while (elapsedSeconds < targetMinutes * 60) {
                delay(1000) // 每秒更新
                elapsedSeconds++
                
                // 每分钟更新通知
                if (elapsedSeconds % 60 == 0) {
                    val elapsedMinutes = elapsedSeconds / 60
                    updateNotification(elapsedMinutes, targetMinutes)
                    Log.d(TAG, "Timer progress: $elapsedMinutes / $targetMinutes minutes")
                }
            }
            
            // 计时完成
            onTimerComplete()
        }
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification(elapsedMinutes: Int, targetMinutes: Int) {
        val notification = createNotification(elapsedMinutes, targetMinutes)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 计时器完成
     */
    private fun onTimerComplete() {
        Log.d(TAG, "Timer completed! Marking task as complete...")
        
        serviceScope.launch(Dispatchers.IO) {
            try {
                // 1. 标记任务完成
                questRepository.updateTaskCompletion(
                    FirestoreConstants.TaskIds.TASK_2_TAJWEED,
                    true
                )
                Log.d(TAG, "Task 2 (Listening) marked as complete")
                
                // 2. 保存学习状态
                saveCurrentState()
                
                // 3. 显示完成通知
                showCompletionNotification()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete task", e)
            } finally {
                stopSelf()
            }
        }
    }
    
    /**
     * 保存当前学习状态
     */
    private fun saveCurrentState() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                questRepository.saveUserLearningState(
                    surah = surah,
                    ayah = ayah,
                    page = page,
                    juz = juz
                )
                Log.d(TAG, "Learning state saved: Surah $surah, Ayah $ayah")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save learning state", e)
            }
        }
    }
    
    /**
     * 标记任务完成（手动触发）
     */
    private fun markTaskComplete() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                questRepository.updateTaskCompletion(
                    FirestoreConstants.TaskIds.TASK_2_TAJWEED,
                    true
                )
                Log.d(TAG, "Task 2 manually marked as complete")
                showCompletionNotification()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark task complete", e)
            }
        }
    }
    
    /**
     * 显示完成通知
     */
    private fun showCompletionNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quran Listening Complete!")
            .setContentText("You've completed your daily listening goal ✓")
            .setSmallIcon(R.drawable.ic_quran)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}

