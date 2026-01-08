package com.quran.quranaudio.quiz.base

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.quran.quranaudio.quiz.extension.SPTools
import com.quranaudio.quiz.quiz.QuestionTools
import com.blankj.utilcode.util.ThreadUtils
import com.quran.quranaudio.quiz.utils.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 优化后的 BaseApp
 * 
 * 🚀 性能优化策略：
 * 1. 消除 Firebase 重复初始化（单例检查）
 * 2. 严格主进程过滤（非主进程跳过所有初始化）
 * 3. Firebase 异步化（500ms 延迟 + 后台线程）
 * 4. Quiz 数据预加载异步化（1s 延迟 + IO 线程）
 * 5. 减少日志输出（仅关键日志）
 * 
 * 📊 目标：主线程阻塞时间 < 50ms
 * 
 * create by microspark 4/14/24
 * optimized 2026-01-06
 **/
open class BaseApp: MultiDexApplication() {
    companion object {
        var instance: Application? = null
        
        // 🔒 全局唯一化检查标志位
        @Volatile
        private var isBaseAppInitialized = false
        
        // 协程作用域（用于后台任务）
        private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    
    // Handler for delayed tasks
    private val mainHandler = Handler(Looper.getMainLooper())
    
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    override fun onCreate() {
        val startTime = System.currentTimeMillis()
        android.util.Log.d("PERFORMANCE", "========================================")
        android.util.Log.d("PERFORMANCE", "⚡ BaseApp.onCreate() START")
        android.util.Log.d("PERFORMANCE", "========================================")
        
        super.onCreate()
        
        // ============================================================
        // 🔒 全局唯一化检查：防止重复初始化
        // ============================================================
        
        if (isBaseAppInitialized) {
            android.util.Log.w("PERFORMANCE", "⚠️ BaseApp already initialized, skipping")
            return
        }
        
        // ============================================================
        // 🔍 严格主进程过滤
        // ============================================================
        
        val isMainProcess = isInMainProcess()
        android.util.Log.d("PERFORMANCE", "→ Process check: isMainProcess = $isMainProcess")
        
        if (!isMainProcess) {
            android.util.Log.d("PERFORMANCE", "⚠️ Running in CHILD process - skipping all initialization")
            android.util.Log.d("PERFORMANCE", "✅ BaseApp.onCreate() COMPLETED (child process) [${System.currentTimeMillis() - startTime}ms]")
            return
        }
        
        // ============================================================
        // 🟢 IMMEDIATE：主线程必须执行（< 50ms）
        // ============================================================
        
        // SPTools 初始化（轻量级，仅创建 SharedPreferences 引用）
        SPTools.init(this)
        
        // 语言配置（读取 SharedPreferences，很快）
        AppConfig.setLanguage()
        
        // 标记已初始化
        isBaseAppInitialized = true
        
        val immediateTime = System.currentTimeMillis() - startTime
        android.util.Log.d("PERFORMANCE", "✅ Immediate init completed [$immediateTime ms]")
        
        // ============================================================
        // 🟡 DELAYED：延迟执行（不阻塞主线程）
        // ============================================================
        
        scheduleDelayedInitialization()
        
        val totalTime = System.currentTimeMillis() - startTime
        android.util.Log.d("PERFORMANCE", "========================================")
        android.util.Log.d("PERFORMANCE", "✅ BaseApp.onCreate() COMPLETED in $totalTime ms (target: <50ms)")
        android.util.Log.d("PERFORMANCE", "========================================")
    }
    
    /**
     * 调度延迟初始化任务
     */
    private fun scheduleDelayedInitialization() {
        android.util.Log.d("PERFORMANCE", "🟡 [DELAY] Scheduling delayed tasks...")
        
        // 延迟 500ms：Firebase 初始化（后台线程）
        mainHandler.postDelayed({
            android.util.Log.d("PERFORMANCE", "→ [DELAY-500ms] Firebase initialization...")
            initFirebaseAsync()
        }, 500)
        
        // 延迟 1000ms：Quiz 数据预加载（IO 线程）
        mainHandler.postDelayed({
            android.util.Log.d("PERFORMANCE", "→ [DELAY-1s] Quiz data preloading...")
            initPlanAndQuizAsync()
        }, 1000)
        
        android.util.Log.d("PERFORMANCE", "✅ [DELAY] 2 delayed tasks scheduled (500ms, 1s)")
    }
    
    /**
     * 异步初始化 Firebase（后台线程）
     */
    private fun initFirebaseAsync() {
        appScope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                // 调用 Firebase 初始化（已有单例检查）
                FireBaseConfigManager.initCloud(this@BaseApp)
                
                val duration = System.currentTimeMillis() - startTime
                android.util.Log.d("PERFORMANCE", "✅ [DELAY-500ms] Firebase initialized [$duration ms]")
                
            } catch (e: Exception) {
                android.util.Log.e("PERFORMANCE", "❌ [DELAY-500ms] Firebase init failed", e)
            }
        }
    }
    
    /**
     * 异步初始化 Quiz 数据（IO 线程）
     */
    private fun initPlanAndQuizAsync() {
        ThreadUtils.executeByIo(object : ThreadUtils.SimpleTask<Boolean>() {
            override fun doInBackground(): Boolean {
                return try {
                    val startTime = System.currentTimeMillis()
                    
                    // 解压 Quiz 数据
                    QuestionTools.unZipBibleQuiz()
                    
                    val duration = System.currentTimeMillis() - startTime
                    android.util.Log.d("PERFORMANCE", "✅ [DELAY-1s] Quiz data preloaded [$duration ms]")
                    
                    true
                } catch (e: Exception) {
                    android.util.Log.e("PERFORMANCE", "❌ [DELAY-1s] Quiz data preload failed", e)
                    false
                }
            }

            override fun onSuccess(result: Boolean?) {
                // Success callback (optional)
            }

            override fun onDone() {
                super.onDone()
                // Cleanup (optional)
            }
        })
    }
    
    /**
     * 检查是否在主进程中运行
     * 
     * 🔍 优化：
     * - 使用 Process.myPid() 获取当前进程 PID
     * - 遍历 runningAppProcesses 查找匹配的进程名
     * - 比对进程名与包名，确保只有主 UI 进程返回 true
     * 
     * @return true 如果在主进程，false 如果在子进程（如 :error_activity）
     */
    private fun isInMainProcess(): Boolean {
        return try {
            val pid = Process.myPid()
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            
            if (activityManager == null) {
                android.util.Log.w("PERFORMANCE", "⚠️ ActivityManager is null, assuming main process")
                return true
            }
            
            val runningProcesses = activityManager.runningAppProcesses
            if (runningProcesses == null || runningProcesses.isEmpty()) {
                android.util.Log.w("PERFORMANCE", "⚠️ No running processes found, assuming main process")
                return true
            }
            
            val currentProcess = runningProcesses.find { it.pid == pid }
            val currentProcessName = currentProcess?.processName
            val mainProcessName = applicationContext.packageName
            
            val isMain = currentProcessName == mainProcessName
            
            if (!isMain) {
                android.util.Log.d("PERFORMANCE", "→ Child process detected: $currentProcessName (main: $mainProcessName)")
            }
            
            isMain
            
        } catch (e: Exception) {
            android.util.Log.e("PERFORMANCE", "❌ Failed to check process", e)
            true // 出错时默认为主进程
        }
    }
}
