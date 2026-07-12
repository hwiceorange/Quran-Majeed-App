package com.quran.quranaudio.online.quran_module.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.result.ActivityResult
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton
import com.peacedesign.android.utils.DrawableUtils
import com.peacedesign.android.utils.WindowUtils
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.model.UnlockedContent
import com.quran.quranaudio.online.repository.UnlockedContentRepository
import com.quran.quranaudio.online.subscription.SubscriptionHelper
import com.quran.quranaudio.online.ui.dialog.RewardedAdLoadingDialog
import com.quran.quranaudio.online.quran_module.api.JsonHelper
import com.quran.quranaudio.online.quran_module.api.RetrofitInstance
import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirInfoModel
import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirModel
import com.quran.quranaudio.online.quran_module.activities.readerSettings.Activity_Quran_Settings
import com.quran.quranaudio.online.databinding.ActivityTafsirBinding
import com.quran.quranaudio.online.databinding.LytTafsirHeaderBinding
import com.quran.quranaudio.online.databinding.LytTafsirTextSizeBinding
import com.quran.quranaudio.online.quran_module.utils.Log
import com.quran.quranaudio.online.quran_module.utils.extensions.disableView
import com.quran.quranaudio.online.quran_module.utils.extensions.drawable
import com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager
import com.quran.quranaudio.online.quran_module.utils.receivers.NetworkStateReceiver
import com.quran.quranaudio.online.quran_module.utils.simplified.SimpleSeekbarChangeListener
import com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirJsInterface
import com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirLanguageMapper
import com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirWebViewClient
import com.quran.quranaudio.online.quran_module.utils.univ.Codes
import com.quran.quranaudio.online.quran_module.utils.univ.Keys
import com.quran.quranaudio.online.quran_module.utils.univ.ResUtils
import com.quran.quranaudio.online.quran_module.widgets.PageAlert
import com.quran.quranaudio.online.quran_module.widgets.bottomSheet.PeaceBottomSheet
import com.quranaudio.common.ad.AdConfig
import com.quranaudio.common.ad.AdFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.*

class ActivityTafsir : com.quran.quranaudio.online.quran_module.activities.ReaderPossessingActivity() {
    private lateinit var binding: ActivityTafsirBinding
    private lateinit var fileUtils: com.quran.quranaudio.online.quran_module.utils.univ.FileUtils
    private lateinit var pageAlert: PageAlert
    private lateinit var jsInterface: TafsirJsInterface
    private lateinit var tafsirInfoModel: TafsirInfoModel
    
    // 解锁内容相关
    private lateinit var unlockedContentRepository: UnlockedContentRepository
    private var isContentUnlocked = false
    private var isAdLoaded = false
    private var adLoadingDialog: RewardedAdLoadingDialog? = null

    var tafsirKey: String? = null
    var chapterNo = 0
    var verseNo = 0

    override fun getLayoutResource(): Int {
        return R.layout.activity_tafsir
    }

    override fun shouldInflateAsynchronously(): Boolean {
        return false
    }

    override fun preReaderReady(activityView: View, intent: Intent, savedInstanceState: Bundle?) {
        fileUtils = com.quran.quranaudio.online.quran_module.utils.univ.FileUtils.newInstance(this)
        binding = ActivityTafsirBinding.bind(activityView)
        pageAlert = PageAlert(this)
        jsInterface = TafsirJsInterface(this)
        unlockedContentRepository = UnlockedContentRepository.getInstance()
        initThis()
        initLockOverlay()
    }

    private fun initThis() {
        binding.let {
            it.loader.visibility = View.VISIBLE
            it.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
            it.settings.setOnClickListener {
                val intent = Intent(this, com.quran.quranaudio.online.quran_module.activities.readerSettings.Activity_Quran_Settings::class.java).apply {
                    putExtra(com.quran.quranaudio.online.quran_module.activities.readerSettings.Activity_Quran_Settings.KEY_SETTINGS_DESTINATION, com.quran.quranaudio.online.quran_module.activities.readerSettings.Activity_Quran_Settings.SETTINGS_TAFSIR)
                }
                startActivity4Result(intent, null)
            }
            it.fontSize.setOnClickListener { showFontSizeDialog() }

            ViewCompat.setOnApplyWindowInsetsListener(it.appBar) { view, insets ->
                val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
                view.setPadding(view.paddingLeft, topInset, view.paddingRight, view.paddingBottom)
                insets
            }
        }
    }

    private fun showFontSizeDialog() {
        // Check if activity is still valid before showing dialog
        if (isFinishing || isDestroyed) {
            return
        }
        
        val binding = LytTafsirTextSizeBinding.inflate(layoutInflater)

        PeaceBottomSheet().apply {
            params.apply {
                headerTitleResource = R.string.titleReaderTextSizeTafsir
                contentView = binding.root
            }
        }.show(supportFragmentManager, "TafsirFontSize")

        binding.seekBar.max = com.quran.quranaudio.online.quran_module.utils.reader.ReaderTextSizeUtils.getMaxProgress()

        setProgressAndTextTransl(
            com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTextSizeMultTafsir(this),
            binding.seekBar,
            binding.progressText
        )

        binding.seekBar.setOnSeekBarChangeListener(object : SimpleSeekbarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val nProgress = com.quran.quranaudio.online.quran_module.utils.reader.ReaderTextSizeUtils.normalizeProgress(progress)
                val text = "$nProgress%"
                binding.progressText.text = text
                demonstrateTextSize(nProgress.toFloat())
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.setSavedTextSizeMultTafsir(
                    seekBar.context,
                    com.quran.quranaudio.online.quran_module.utils.reader.ReaderTextSizeUtils.calculateMultiplier(
                        com.quran.quranaudio.online.quran_module.utils.reader.ReaderTextSizeUtils.normalizeProgress(seekBar.progress))
                )
            }
        })
    }

    private fun demonstrateTextSize(progress: Float) {
        binding.webView.loadUrl("javascript:changeFontSize($progress)")
    }

    private fun setProgressAndTextTransl(multiplier: Float, seekBar: SeekBar, progressText: TextView) {
        seekBar.progress = com.quran.quranaudio.online.quran_module.utils.reader.ReaderTextSizeUtils.calculateProgress(multiplier)

        val text = "${com.quran.quranaudio.online.quran_module.utils.reader.ReaderTextSizeUtils.calculateProgressText(multiplier)}%"
        progressText.text = text
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initContent(intent)
    }

    override fun onReaderReady(intent: Intent, savedInstanceState: Bundle?) {
        (supportFragmentManager.findFragmentByTag("TafsirFontSize") as? PeaceBottomSheet)?.dismiss()

        initWebView()

        // 首次使用时强制下载Tafsirs资源
        val forceDownload = TafsirManager.getModels() == null
        
        TafsirManager.prepare(this, forceDownload) {
            android.util.Log.d("ActivityTafsir", "✅ TafsirManager prepared, models available: ${TafsirManager.getModels() != null}")
            initContent(intent)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        binding.webView.let {
            it.setBackgroundColor(Color.TRANSPARENT)
            it.settings.apply {
                javaScriptEnabled = true
            }
            it.addJavascriptInterface(jsInterface, "TafsirJSInterface")
            it.overScrollMode = View.OVER_SCROLL_NEVER
            it.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d("[" + consoleMessage.lineNumber() + "]" + consoleMessage.message())
                    return true
                }
            }
            it.webViewClient = object : TafsirWebViewClient(this) {
                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    binding.loader.visibility = View.GONE
                    binding.tafsirHeader.btnPrevVerse.visibility = View.VISIBLE
                    binding.tafsirHeader.btnNextVerse.visibility = View.VISIBLE
                    loadTafsirNativeAd()
                }
            }
        }
    }

    // 🔥 优化：缓存 HTML 模板，避免每次都从 assets 读取
    private var cachedBoilerPlateHTML: String? = null
    
    private fun getBoilerPlateHTML(): String {
        if (cachedBoilerPlateHTML != null) {
            android.util.Log.d("ActivityTafsir", "✅ 使用缓存的 HTML 模板")
            return cachedBoilerPlateHTML!!
        }
        
        val html = ResUtils.readAssetsTextFile(this, "tafsir/tafsir_page.html")
        cachedBoilerPlateHTML = html
        android.util.Log.d("ActivityTafsir", "📄 HTML 模板已缓存")
        return html
    }

    private fun resolveDarkMode(): String {
        return if (WindowUtils.isNightMode(this)) "dark" else "light"
    }

    private fun resolveTextDirection(): String {
        val directionFromLocale = TextUtils.getLayoutDirectionFromLocale(Locale(tafsirInfoModel.langCode))
        return if (directionFromLocale == View.LAYOUT_DIRECTION_RTL) "rtl" else "ltr"
    }

    private fun initContent(intent: Intent) {
        val chapterNo = intent.getIntExtra(Keys.READER_KEY_CHAPTER_NO, -1)
        val verseNo = intent.getIntExtra(Keys.READER_KEY_VERSE_NO, -1)

        if (chapterNo < 1 || verseNo < 1) {
            fail("Invalid params", false)
            return
        }

        android.util.Log.d("ActivityTafsir", "🔍 Initializing Tafsir for Surah:$chapterNo, Ayah:$verseNo")
        
        // 🔧 强制确保 TafsirManager 已准备好
        // 这对于从答题模块等外部入口跳转的场景很重要
        val models = TafsirManager.getModels()
        if (models == null || models.isEmpty()) {
            android.util.Log.w("ActivityTafsir", "⚠️ TafsirManager not ready, preparing now...")
            binding.loader.visibility = View.VISIBLE
            
            TafsirManager.prepare(this, false) {
                android.util.Log.d("ActivityTafsir", "✅ TafsirManager prepared, continuing initialization...")
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        initContentAfterPrepare(intent, chapterNo, verseNo)
                    }
                }
            }
            return
        }
        
        initContentAfterPrepare(intent, chapterNo, verseNo)
    }
    
    /**
     * 在 TafsirManager 准备完成后初始化内容
     */
    private fun initContentAfterPrepare(intent: Intent, chapterNo: Int, verseNo: Int) {
        var key = intent.getStringExtra("tafsirKey") ?: com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTafsirKey(this)
        
        if (key == null) {
            key = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirUtils.getPreferredTafsirKey(this)
        }

        if (key == null) {
            android.util.Log.e("ActivityTafsir", "❌ Tafsir key is null")
            showTafsirSetupDialog()
            return
        }

        val model = TafsirManager.getModel(key)

        if (model == null) {
            android.util.Log.e("ActivityTafsir", "❌ Tafsir model not found for key: $key")
            android.util.Log.e("ActivityTafsir", "❌ Available models: ${TafsirManager.getModels()}")
            showTafsirSetupDialog()
            return
        }

        android.util.Log.d("ActivityTafsir", "✅ Using Tafsir: ${model.name} (${model.langName})")
        
        this.tafsirInfoModel = model
        this.tafsirKey = key
        this.chapterNo = chapterNo
        this.verseNo = verseNo

        initTafsirHeader(binding.tafsirHeader)
        loadContent()
    }

    private fun initTafsirHeader(header: LytTafsirHeaderBinding) {
        val chapter = mQuranRef.get().getChapter(chapterNo)

        setupTafsirTitle(header, chapter)

        val isRTL = bool(R.bool.isRTL)

        header.textPrevTafsir.setDrawables(getStartPointingArrow(this, isRTL), null, null, null)
        header.textNextTafsir.setDrawables(null, null, getEndPointingArrow(this, isRTL), null)

        header.btnPrevVerse.visibility = View.GONE
        header.btnNextVerse.visibility = View.GONE

        val prevVerseName = if (verseNo == 1) "" else getString(R.string.strLabelVerseNo, verseNo - 1)
        val hasPrevVerseName = prevVerseName.isNotEmpty()
        header.btnPrevVerse.disableView(!hasPrevVerseName)
        header.btnPrevVerse.setOnClickListener { jsInterface.previousTafsir() }
        header.prevVerseName.text = if (hasPrevVerseName) prevVerseName else ""
        header.prevVerseName.visibility = if (hasPrevVerseName) View.VISIBLE else View.GONE

        val nextVerseName = if (verseNo == chapter.verseCount) "" else getString(R.string.strLabelVerseNo, verseNo + 1)
        val hasNextVerseName = nextVerseName.isNotEmpty()
        header.btnNextVerse.disableView(!hasNextVerseName)
        header.btnNextVerse.setOnClickListener { jsInterface.nextTafsir() }
        header.nextVerseName.text = if (hasNextVerseName) nextVerseName else ""
        header.nextVerseName.visibility = if (hasNextVerseName) View.VISIBLE else View.GONE
    }

    private fun getStartPointingArrow(context: Context, isRTL: Boolean): Drawable? {
        val arrowLeft = context.drawable(R.drawable.dr_icon_arrow_left)
        return if (!isRTL) arrowLeft else DrawableUtils.rotate(context, arrowLeft, 180f)
    }

    private fun getEndPointingArrow(context: Context, isRTL: Boolean): Drawable? {
        val arrowLeft = context.drawable(R.drawable.dr_icon_arrow_left)
        return if (isRTL) arrowLeft else DrawableUtils.rotate(context, arrowLeft, 180f)
    }

    private fun setupTafsirTitle(header: LytTafsirHeaderBinding, chapter: com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Chapter) {
        val chapterInfo = SpannableString(
            getString(
                R.string.strLabelVerseWithChapNameWithBar, chapter.name, verseNo
            )
        )

        chapterInfo.setSpan(
            ForegroundColorSpan(color(R.color.colorText2)),
            0,
            chapterInfo.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        chapterInfo.setSpan(
            AbsoluteSizeSpan(dimen(R.dimen.dmnCommonSize2)),
            0,
            chapterInfo.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        header.tafsirTitle.text = TextUtils.concat(tafsirInfoModel.name, "\n", chapterInfo)
    }

    private fun loadContent() {
        pageAlert.remove()
        binding.loader.visibility = View.VISIBLE
        
        val loadStartTime = System.currentTimeMillis()
        android.util.Log.d("ActivityTafsir", "⏱️ [性能] loadContent 开始: $chapterNo:$verseNo")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 🔥 优化：使用三级缓存（内存 → 文件 → 网络）
                val tafsir = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirCacheManager.getTafsir(
                    context = this@ActivityTafsir,
                    tafsirKey = tafsirKey,
                    chapterNo = chapterNo,
                    verseNo = verseNo
                )
                
                if (tafsir != null) {
                    // ✅ 缓存命中，立即渲染
                    val elapsed = System.currentTimeMillis() - loadStartTime
                    android.util.Log.d("ActivityTafsir", "✅ [性能] Tafsir 从缓存加载成功 (${elapsed}ms)")
                    renderData(tafsir)
                    return@launch
                }
                
                // 🔥 缓存不存在，检查网络
                if (!NetworkStateReceiver.isNetworkConnected(this@ActivityTafsir)) {
                    runOnUiThread { 
                        if (!isFinishing && !isDestroyed) {
                            noInternet() 
                        }
                    }
                    return@launch
                }
                
                // 🔥 从网络加载并缓存
                val result = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirCacheManager.loadAndCacheTafsir(
                    context = this@ActivityTafsir,
                    tafsirKey = tafsirKey!!,
                    chapterNo = chapterNo,
                    verseNo = verseNo
                )
                
                result.onSuccess { loadedTafsir ->
                    val elapsed = System.currentTimeMillis() - loadStartTime
                    android.util.Log.d("ActivityTafsir", "✅ [性能] Tafsir 从网络加载成功 (${elapsed}ms)")
                    renderData(loadedTafsir)
                }.onFailure { e ->
                    val slug = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirUtils.getTafsirSlugFromKey(tafsirKey)
                    val apiSource = if (slug.startsWith("id-")) "custom server (dochubai.com)" else "Quran.com"
                    android.util.Log.e("ActivityTafsir", "❌ Failed to load tafsir from $apiSource: ${e.message}")
                    Log.saveError(e, "ActivityTafsir")
                    fail("Failed to load tafsir.", true)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ActivityTafsir", "❌ Unexpected error: ${e.message}")
                fail("Failed to load tafsir.", true)
            }
        }
    }

    private fun renderData(tafsir: TafsirModel) {
        val renderStartTime = System.currentTimeMillis()
        android.util.Log.d("ActivityTafsir", "⏱️ [性能] renderData 开始")
        
        val map = mapOf(
            "{{THEME}}" to resolveDarkMode(),
            "{{CONTENT}}" to tafsir.text,
            "{{DIR}}" to resolveTextDirection(),
            "{{FONT_SIZE}}" to (com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTextSizeMultTafsir(this) * 100).toString()
        )

        val pattern = Regex(pattern = map.keys.joinToString("|") { Regex.escape(it) })
        val html = pattern.replace(getBoilerPlateHTML()) { match -> map[match.value].orEmpty() }
        
        val replaceElapsed = System.currentTimeMillis() - renderStartTime
        android.util.Log.d("ActivityTafsir", "⏱️ [性能] HTML 替换完成 (${replaceElapsed}ms)")

        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                binding.webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "utf-8", null)
                
                val totalElapsed = System.currentTimeMillis() - renderStartTime
                android.util.Log.d("ActivityTafsir", "⏱️ [性能] renderData 完成 (${totalElapsed}ms)")
                
                // 内容加载完成后，检查解锁状态
                checkUnlockStatus()
            }
        }
    }

    /**
     * 显示Tafsir设置引导对话框
     * 提供自动下载和手动设置两个选项
     */
    private fun showTafsirSetupDialog() {
        runOnUiThread {
            // Check if activity is still valid before showing dialog
            if (isFinishing || isDestroyed) {
                return@runOnUiThread
            }
            
            binding.loader.visibility = View.GONE
            
            // 获取当前语言
            val userLanguage = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(this)
            val systemLanguage = java.util.Locale.getDefault().language
            val targetLanguage = if (!userLanguage.isNullOrEmpty()) userLanguage else systemLanguage
            
            val languageName = when(targetLanguage) {
                "ar" -> "Arabic"
                "en" -> "English"
                "ur" -> "Urdu"
                "id" -> "Indonesian"  // 统一使用 "id" 表示印尼语
                else -> "English"
            }
            
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tafsir Not Available")
                .setMessage("No Tafsir is currently selected. Would you like to:\n\n1. Auto-download Tafsir for your language ($languageName)\n2. Go to Settings to choose from all available Tafsirs")
                .setPositiveButton("Auto Download") { dialog, _ ->
                    dialog.dismiss()
                    autoDownloadDefaultTafsir()
                }
                .setNegativeButton("Go to Settings") { dialog, _ ->
                    dialog.dismiss()
                    openTafsirSettings()
                }
                .setNeutralButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }
    
    /**
     * 自动下载默认Tafsir并重新加载
     * 智能匹配用户设置的应用语言或系统语言
     */
    private fun autoDownloadDefaultTafsir() {
        binding.loader.visibility = View.VISIBLE
        
        TafsirManager.prepare(this, true) {
            android.util.Log.d("ActivityTafsir", "✅ Auto-downloaded tafsirs")
            
            // 获取用户设置的语言或系统语言
            val userLanguage = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(this)
            val systemLanguage = java.util.Locale.getDefault().language
            val targetLanguage = if (!userLanguage.isNullOrEmpty()) userLanguage else systemLanguage
            
            android.util.Log.d("ActivityTafsir", "🌍 User language: $userLanguage, System language: $systemLanguage, Target: $targetLanguage")
            
            val tafsirModels = TafsirManager.getModels()
            val selectedKey = TafsirLanguageMapper.pickBestTafsirKey(targetLanguage, tafsirModels)
            android.util.Log.d(
                "ActivityTafsir",
                "📖 Preferred tafsir for '$targetLanguage': ${selectedKey ?: "not found"}"
            )
            
            if (selectedKey != null) {
                com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.setSavedTafsirKey(this, selectedKey)
                android.util.Log.d("ActivityTafsir", "✅ Saved tafsir key: $selectedKey")
                
                // 重新加载内容
                initContent(intent)
            } else {
                fail("Failed to download tafsirs. Please check internet connection.", true)
            }
        }
    }
    
    /**
     * 打开Settings的Tafsirs页面
     */
    private fun openTafsirSettings() {
        try {
            val intent = Intent(this, Activity_Quran_Settings::class.java).apply {
                putExtra(Activity_Quran_Settings.KEY_SETTINGS_DESTINATION, Activity_Quran_Settings.SETTINGS_TAFSIR)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            android.util.Log.e("ActivityTafsir", "❌ Failed to open settings", e)
            finish()
        }
    }

    override fun getStatusBarBG(): Int {
        return Color.WHITE
    }

    override fun isStatusBarLight(): Boolean {
        return true
    }
    
    private fun fail(msg: String, showRetry: Boolean) {
        android.util.Log.e("ActivityTafsir", "❌ fail() called: $msg, showRetry=$showRetry")
        binding.loader.visibility = View.GONE

        pageAlert.let {
            it.setMessage(msg, null)
            if (showRetry) {
                it.setActionButton(R.string.strLabelRetry) { 
                    android.util.Log.d("ActivityTafsir", "🔄 Retry clicked, reloading...")
                    initContent(intent)
                }
            } else {
                it.setActionButton(null, null)
            }
            it.show(binding.container)
        }
    }

    private fun noInternet() {
        pageAlert.let {
            it.setupForNoInternet { loadContent() }
            it.show(binding.container)
        }
    }

    override fun onActivityResult2(result: ActivityResult?) {
        super.onActivityResult2(result)

        if (result?.resultCode == Codes.SETTINGS_LAUNCHER_RESULT_CODE) {
            tafsirKey = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTafsirKey(this)
            loadContent()
        }
    }

    fun scrollToTop() {
        binding.webView.scrollTo(0, 0)
        binding.appBar.setExpanded(true)
    }
    
    // ==================== 内容解锁功能 ====================
    
    /**
     * 初始化锁定覆盖层
     */
    private fun initLockOverlay() {
        val lockOverlay = binding.contentLockOverlay.root as? ConstraintLayout ?: return
        
        val btnWatchAd = lockOverlay.findViewById<MaterialButton>(R.id.btnWatchAd)
        val btnSubscribe = lockOverlay.findViewById<MaterialButton>(R.id.btnSubscribe)
        
        btnWatchAd?.setOnClickListener {
            android.util.Log.d("ActivityTafsir", "🎬 Watch Ad button clicked")
            showRewardedAd()
        }
        
        btnSubscribe?.setOnClickListener {
            android.util.Log.d("ActivityTafsir", "💳 Subscribe button clicked")
            goToSubscriptionPage()
        }
    }
    
    /**
     * 检查内容解锁状态并更新UI
     */
    private fun checkUnlockStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 方式1: 检查用户是否订阅
                val isSubscribed = SubscriptionHelper.isUserSubscribed(this@ActivityTafsir)
                
                // 方式2: 检查该经文是否通过广告解锁
                val isUnlockedByAd = unlockedContentRepository.isContentUnlocked(chapterNo, verseNo)
                
                isContentUnlocked = isSubscribed || isUnlockedByAd
                
                android.util.Log.d("ActivityTafsir", "📊 Unlock Status Check:")
                android.util.Log.d("ActivityTafsir", "  - Subscribed: $isSubscribed")
                android.util.Log.d("ActivityTafsir", "  - Unlocked by Ad: $isUnlockedByAd")
                android.util.Log.d("ActivityTafsir", "  - Final Status: $isContentUnlocked")
                
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        updateLockOverlayVisibility()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivityTafsir", "❌ Error checking unlock status", e)
                isContentUnlocked = false
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        updateLockOverlayVisibility()
                    }
                }
            }
        }
    }
    
    /**
     * 更新锁定覆盖层的可见性
     */
    private fun updateLockOverlayVisibility() {
        val lockOverlay = binding.contentLockOverlay.root
        val webView = binding.webView
        
        if (isContentUnlocked) {
            // 已解锁：隐藏覆盖层，显示完整内容，恢复滚动
            lockOverlay.visibility = View.GONE
            binding.fontSize.visibility = View.VISIBLE // 解锁后恢复字号按钮
            webView.setOnTouchListener(null) // 恢复触摸事件
            webView.setOnScrollChangeListener(null) // 移除滚动监听
            android.util.Log.d("ActivityTafsir", "✅ Content unlocked, hiding overlay and enabling scroll")
        } else {
            // 未解锁：显示覆盖层（50%遮罩效果），限制滚动
            lockOverlay.visibility = View.VISIBLE
            // 内容锁定时隐藏字号 FAB：锁定内容无法调整字号，且会浮在解锁卡片上造成视觉混乱
            binding.fontSize.visibility = View.GONE
            
            // 限制WebView滚动到最多50%的内容高度
            webView.setOnScrollChangeListener { v, _, scrollY, _, _ ->
                val webViewHeight = v.height
                val contentHeight = (v as android.webkit.WebView).contentHeight * v.scale
                val maxScrollY = (contentHeight * 0.5f).toInt() - webViewHeight
                
                if (scrollY > maxScrollY && maxScrollY > 0) {
                    // 如果滚动超过50%，强制回滚到50%位置
                    v.scrollTo(0, maxScrollY)
                    android.util.Log.d("ActivityTafsir", "🚫 Scroll limited to 50%")
                }
            }
            
            android.util.Log.d("ActivityTafsir", "🔒 Content locked, showing overlay and limiting scroll to 50%")
            
            // 预加载激励广告
            preloadRewardedAd()
        }
    }
    
    /**
     * 预加载激励广告
     * ⚠️ 注意：只预加载，不自动播放（只有用户点击解锁按钮时才播放）
     */
    private var isLoadingAd: Boolean = false
    private var isUserRequestedAd: Boolean = false  // 标记是否是用户主动请求广告
    private val adRetryHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val adRetryRunnable = Runnable {
        if (!isAdLoaded && !isLoadingAd) {
            android.util.Log.d("ActivityTafsir", "🔄 Retrying rewarded ad preload after failure/close")
            preloadRewardedAd()
        }
    }
    
    private fun preloadRewardedAd() {
        if (isAdLoaded || isLoadingAd) {
            android.util.Log.d("ActivityTafsir", "✅ Ad already loaded or loading")
            return
        }
        
        android.util.Log.d("ActivityTafsir", "📡 Preloading rewarded ad...")
        isLoadingAd = true
        adRetryHandler.removeCallbacks(adRetryRunnable)
        
        AdFactory.loadRewardAd(this, AdConfig.AD_TAFSIR_REWARD, object : com.quranaudio.common.ad.AdLoadCallback {
            override fun onAdLoaded(adItem: com.quranaudio.common.ad.model.AdItem?) {
                isAdLoaded = true
                isLoadingAd = false
                android.util.Log.d("ActivityTafsir", "✅ Rewarded ad loaded successfully")
                
                // ⚠️ 【关键修复】只有在用户主动点击解锁按钮时才自动播放
                // 避免在滚动到底部或退出页面时意外播放广告
                if (isUserRequestedAd && adLoadingDialog != null && adLoadingDialog!!.isShowing) {
                    android.util.Log.d("ActivityTafsir", "✅ User requested ad, showing immediately")
                    adLoadingDialog?.onAdReadyToShow()
                } else {
                    android.util.Log.d("ActivityTafsir", "ℹ️ Ad loaded but not user-requested, just caching")
                }
            }
            
            override fun onAdFailedToLoad(adPosition: String?) {
                isAdLoaded = false
                isLoadingAd = false
                android.util.Log.e("ActivityTafsir", "❌ Rewarded ad failed to load")
                
                // 如果Loading对话框还在显示，显示错误提示
                if (isUserRequestedAd && adLoadingDialog != null) {
                    adLoadingDialog?.showAdNotReadyError()
                }
                
                // 安排重试，防止缓存池为空
                adRetryHandler.postDelayed(adRetryRunnable, 5000)
            }
        })
    }
    
    /**
     * 显示激励广告
     * ⚠️ 只有在用户点击解锁按钮时调用
     */
    private fun showRewardedAd() {
        // 标记为用户主动请求
        isUserRequestedAd = true
        
        if (isAdLoaded) {
            // 广告已加载，直接播放
            android.util.Log.d("ActivityTafsir", "▶️ Showing loaded ad immediately")
            playRewardedAd()
        } else {
            // 广告未加载，显示Loading对话框
            android.util.Log.d("ActivityTafsir", "⏳ Ad not loaded, showing loading dialog")
            showAdLoadingDialog()
        }
    }
    
    /**
     * 显示广告加载对话框
     */
    private fun showAdLoadingDialog() {
        // Check if activity is still valid before showing dialog
        if (isFinishing || isDestroyed) {
            return
        }
        
        adLoadingDialog = RewardedAdLoadingDialog(
            context = this,
            onAdReady = {
                // 广告准备好，播放广告
                android.util.Log.d("ActivityTafsir", "✅ Ad ready from dialog callback")
                playRewardedAd()
            },
            onRetry = {
                // 用户点击重试
                android.util.Log.d("ActivityTafsir", "🔄 User clicked retry")
                isAdLoaded = false
                preloadRewardedAd()
            },
            onDismiss = {
                // 用户关闭对话框
                android.util.Log.d("ActivityTafsir", "❌ User dismissed loading dialog")
                adLoadingDialog = null
                isUserRequestedAd = false  // 重置用户请求标记
            }
        )
        
        adLoadingDialog?.show()
        
        // 如果广告还没开始加载，现在开始加载
        if (!isAdLoaded) {
            preloadRewardedAd()
        }
    }
    
    /**
     * 播放激励广告
     */
    private fun playRewardedAd() {
        AdFactory.showRewardAd(
            this,
            AdConfig.AD_TAFSIR_REWARD,
            "tafsir_unlock",
            object : com.quranaudio.common.ad.AdShowCallback {
                override fun onAdImpression(p0: com.quranaudio.common.ad.model.AdItem?) {
                    android.util.Log.d("ActivityTafsir", "👁️ Ad impression")
                }
                
                override fun onAdClicked(p0: com.quranaudio.common.ad.model.AdItem?) {
                    android.util.Log.d("ActivityTafsir", "👆 Ad clicked")
                }
                
                override fun onUserEarnedReward(
                    p0: com.quranaudio.common.ad.model.AdItem?,
                    p1: com.quranaudio.common.ad.model.RewardItem?
                ) {
                    android.util.Log.d("ActivityTafsir", "🎉 User earned reward!")
                    
                    // 用户观看完广告，解锁内容
                    unlockContentByAd()
                }
                
                override fun onAdClosed(p0: com.quranaudio.common.ad.model.AdItem?) {
                    android.util.Log.d("ActivityTafsir", "🚪 Ad closed")
                    
                    // 重置广告加载状态和用户请求标记
                    isAdLoaded = false
                    isUserRequestedAd = false
                    
                    // 预加载下一条广告（但不自动播放）
                    preloadRewardedAd()
                }
                
                override fun onShow(p0: com.quranaudio.common.ad.model.AdItem?) {
                    android.util.Log.d("ActivityTafsir", "▶️ Ad showing")
                }
                
                override fun onShowFail() {
                    android.util.Log.e("ActivityTafsir", "❌ Ad show failed")
                    Toast.makeText(
                        this@ActivityTafsir,
                        R.string.ad_not_ready_message,
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 重置状态并重新加载
                    isAdLoaded = false
                    preloadRewardedAd()
                }
            }
        )
    }
    
    /**
     * 通过观看广告解锁内容
     */
    private fun unlockContentByAd() {
        android.util.Log.d("ActivityTafsir", "🎬 Starting unlock process by ad...")
        android.util.Log.d("ActivityTafsir", "  - chapterNo: $chapterNo")
        android.util.Log.d("ActivityTafsir", "  - verseNo: $verseNo")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = unlockedContentRepository.unlockContent(
                    chapterNo,
                    verseNo,
                    UnlockedContent.UnlockMethod.REWARDED_AD
                )
                
                android.util.Log.d("ActivityTafsir", "📝 Firestore save result: $success")
                
                if (success) {
                    android.util.Log.d("ActivityTafsir", "✅ Content unlocked successfully in Firestore")
                    
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            // 更新本地状态
                            isContentUnlocked = true
                            
                            // 立即更新UI
                            updateLockOverlayVisibility()
                            
                            // 显示成功提示
                            Toast.makeText(
                                this@ActivityTafsir,
                                R.string.unlock_success_message,
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            android.util.Log.d("ActivityTafsir", "✅ UI updated, overlay should be hidden now")
                        }
                    }
                } else {
                    android.util.Log.e("ActivityTafsir", "❌ Failed to unlock content in Firestore")
                    
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(
                                this@ActivityTafsir,
                                "Failed to unlock content. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ActivityTafsir", "❌ Exception during unlock", e)
                android.util.Log.e("ActivityTafsir", "  Exception message: ${e.message}")
                android.util.Log.e("ActivityTafsir", "  Stack trace: ${e.stackTraceToString()}")
                
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        Toast.makeText(
                            this@ActivityTafsir,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
    
    /**
     * 跳转到订阅页面
     */
    private fun goToSubscriptionPage() {
        android.util.Log.d("ActivityTafsir", "🛒 Navigating to subscription page")
        SubscriptionHelper.launchSubscriptionPage(this, "tafsir_unlock")
    }
    
    /**
     * 在onResume中检查订阅状态
     */
    override fun onResume() {
        super.onResume()
        
        // 检查解锁状态（可能用户刚订阅回来）
        checkUnlockStatus()
    }
    
    /**
     * ⚠️ 暂停时清理对话框状态，避免意外播放广告
     */
    override fun onPause() {
        super.onPause()
        
        // 关闭广告加载对话框
        if (adLoadingDialog != null && adLoadingDialog!!.isShowing) {
            android.util.Log.d("ActivityTafsir", "⏸️ onPause: Dismissing ad loading dialog")
            adLoadingDialog?.dismiss()
        }
        adLoadingDialog = null
        
        // 重置用户请求标记
        isUserRequestedAd = false
        
        // 取消广告重试任务
        adRetryHandler.removeCallbacks(adRetryRunnable)
    }
    
    /**
     * 清理资源
     */
    private fun loadTafsirNativeAd() {
        val container = binding.tafsirNativeAdContainer
        com.quranaudio.common.ad.NativeAdHelper.displayNativeAdWithAutoLoad(
            this,
            container,
            com.quran.quranaudio.quiz.R.layout.layout_ad_native_small_wrapper
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // 清理广告相关资源
        adLoadingDialog?.dismiss()
        adLoadingDialog = null
        adRetryHandler.removeCallbacks(adRetryRunnable)
        isUserRequestedAd = false
    }
}