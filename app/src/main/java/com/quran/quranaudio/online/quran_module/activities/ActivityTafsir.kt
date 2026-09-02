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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.peacedesign.android.utils.DrawableUtils
import com.peacedesign.android.utils.WindowUtils
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.model.UnlockedContent
import com.quran.quranaudio.online.repository.UnlockedContentRepository
import com.quran.quranaudio.online.rewards.RewardedValueCoordinator
import com.quran.quranaudio.online.subscription.SubscriptionHelper
import com.quran.quranaudio.online.tafsir.TafsirSessionAdFreeManager
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
import com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirCacheManager
import com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirWebViewClient
import com.quran.quranaudio.online.quran_module.utils.univ.Codes
import com.quran.quranaudio.online.quran_module.utils.univ.Keys
import com.quran.quranaudio.online.quran_module.utils.univ.ResUtils
import com.quran.quranaudio.online.quran_module.widgets.PageAlert
import com.quran.quranaudio.online.quran_module.widgets.bottomSheet.PeaceBottomSheet
import com.quranaudio.common.ad.AdConfig
import com.quranaudio.common.ad.RewardedAdFlowCoordinator
import com.quranaudio.common.ad.SubscriptionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private var contentLoadJob: Job? = null
    private var contentRequestId = 0L
    private var lastReadyLoggedRequestId = -1L
    private var currentContentLoadStartedAt = 0L
    private var currentContentSource = "unknown"
    private var currentManifestPrepareMs = 0L
    private var currentContentReadyMs = 0L
    private var currentRenderSubmitMs = 0L
    private var nativeAdRequestInFlight = false
    private var nativeAdDisplayed = false
    private var webPageReady = false
    private var tafsirAdFreeSubscriptionPending = false
    private var pausedAfterTafsirAdFreeSubscription = false

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
            it.tafsirAdFreeAction.setOnClickListener { onTafsirAdFreeActionClicked() }

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

        val manifestStartedAt = System.currentTimeMillis()
        TafsirManager.prepare(this, false) {
            currentManifestPrepareMs = System.currentTimeMillis() - manifestStartedAt
            android.util.Log.d(
                "ActivityTafsir",
                "✅ TafsirManager local-first prepare completed in ${currentManifestPrepareMs}ms, models available: ${TafsirManager.getModels() != null}"
            )
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
                    webPageReady = true
                    binding.loader.visibility = View.GONE
                    binding.tafsirHeader.btnPrevVerse.visibility = View.VISIBLE
                    binding.tafsirHeader.btnNextVerse.visibility = View.VISIBLE
                    logTafsirReadyIfNeeded()
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
        val requestedKey = intent.getStringExtra("tafsirKey")
            ?.takeIf { it.isNotBlank() }
            ?: com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader
                .getSavedTafsirKey(this)
                ?.takeIf { it.isNotBlank() }
        var key = requestedKey
        var model = key?.let(TafsirManager::getModel)

        if (model == null) {
            key = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirUtils
                .getPreferredTafsirKey(this)
            model = key?.let(TafsirManager::getModel)
            if (model != null) {
                com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader
                    .setSavedTafsirKey(this, model.key)
                android.util.Log.w(
                    "ActivityTafsir",
                    "Recovered missing/stale Tafsir selection '$requestedKey' with '${model.key}'"
                )
            }
        }

        if (key == null || model == null) {
            android.util.Log.e("ActivityTafsir", "❌ No valid Tafsir selection after local manifest prepare")
            android.util.Log.e("ActivityTafsir", "❌ Requested key: $requestedKey, available models: ${TafsirManager.getModels()}")
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

        // 步骤5：Tafsir 名后加下拉指示 ▾ 并让标题可点，提升"可切换/对照"的发现性
        header.tafsirTitle.text = TextUtils.concat(tafsirInfoModel.name, "  ▾", "\n", chapterInfo)
        header.tafsirTitle.setOnClickListener { showTafsirSwitcher() }
    }

    private fun loadContent() {
        pageAlert.remove()
        binding.loader.visibility = View.VISIBLE
        webPageReady = false

        val requestedKey = tafsirKey ?: run {
            fail("Failed to load tafsir.", true)
            return
        }
        val requestedChapter = chapterNo
        val requestedVerse = verseNo
        val requestId = ++contentRequestId
        currentContentLoadStartedAt = System.currentTimeMillis()
        currentContentSource = "unknown"
        currentContentReadyMs = 0L
        currentRenderSubmitMs = 0L
        contentLoadJob?.cancel()

        android.util.Log.d("ActivityTafsir", "⏱️ [性能] loadContent 开始: $requestedChapter:$requestedVerse, request=$requestId")
        contentLoadJob = lifecycleScope.launch {
            val result = TafsirCacheManager.getOrLoadTafsir(
                context = applicationContext,
                tafsirKey = requestedKey,
                chapterNo = requestedChapter,
                verseNo = requestedVerse
            )

            if (!isCurrentContentRequest(requestId, requestedKey, requestedChapter, requestedVerse)) {
                android.util.Log.d("ActivityTafsir", "Ignoring stale Tafsir result for request=$requestId")
                return@launch
            }

            result.onSuccess { loaded ->
                currentContentSource = loaded.source.name.lowercase(Locale.ROOT)
                currentContentReadyMs = System.currentTimeMillis() - currentContentLoadStartedAt
                android.util.Log.d(
                    "ActivityTafsir",
                    "✅ [性能] Tafsir ${currentContentSource} ready in ${currentContentReadyMs}ms"
                )
                renderData(loaded.tafsir)

                val verseCount = mQuranRef.get().getChapter(requestedChapter).verseCount
                TafsirCacheManager.prefetchAdjacent(
                    applicationContext,
                    requestedKey,
                    requestedChapter,
                    requestedVerse,
                    verseCount
                )
            }.onFailure { error ->
                Log.saveError(error, "ActivityTafsir")
                if (!NetworkStateReceiver.isNetworkConnected(this@ActivityTafsir)) {
                    noInternet()
                } else {
                    fail(getString(R.string.tafsir_load_failed), true)
                }
            }
        }
    }

    private fun isCurrentContentRequest(
        requestId: Long,
        requestedKey: String,
        requestedChapter: Int,
        requestedVerse: Int
    ): Boolean {
        return !isFinishing &&
            !isDestroyed &&
            requestId == contentRequestId &&
            requestedKey == tafsirKey &&
            requestedChapter == chapterNo &&
            requestedVerse == verseNo
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

        if (!isFinishing && !isDestroyed) {
            binding.webView.loadDataWithBaseURL(null, html, "text/html; charset=UTF-8", "utf-8", null)

            val totalElapsed = System.currentTimeMillis() - renderStartTime
            currentRenderSubmitMs = totalElapsed
            android.util.Log.d("ActivityTafsir", "⏱️ [性能] renderData 提交 WebView (${totalElapsed}ms)")

            // 内容加载完成后，检查解锁状态
            checkUnlockStatus()
        }
    }

    private fun logTafsirReadyIfNeeded() {
        if (lastReadyLoggedRequestId == contentRequestId || currentContentLoadStartedAt <= 0L) return
        lastReadyLoggedRequestId = contentRequestId
        val totalMs = System.currentTimeMillis() - currentContentLoadStartedAt
        android.util.Log.d(
            "ActivityTafsir",
            "✅ [性能] Tafsir first content visible: source=$currentContentSource, total=${totalMs}ms"
        )
        com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logEvent(
            "tafsir_load_performance",
            mapOf(
                "source" to currentContentSource,
                "manifest_ms" to currentManifestPrepareMs,
                "content_ms" to currentContentReadyMs,
                "network_ms" to if (currentContentSource == "network") currentContentReadyMs else 0L,
                "render_submit_ms" to currentRenderSubmitMs,
                "total_ms" to totalMs,
                "chapter" to chapterNo,
                "verse" to verseNo
            )
        )
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

    /**
     * 步骤3：就地快速切换 Tafsir(底部弹窗)。
     * 让"多 Tafsir 对照"从"跳整页设置(4步)"降到"点标题→选(1步)"，激活会员对照价值。
     * 复用 ADPTafsir(已带免费/会员角标 + 付费否决门)；免费/已订阅即时切换重载。
     */
    private fun showTafsirSwitcher() {
        try {
            val currentModel = tafsirInfoModel ?: return
            val allModels = com.quran.quranaudio.online.quran_module.utils.reader.tafsir
                .TafsirManager.getModels() ?: return
            val list = allModels[currentModel.langCode]?.toList() ?: return
            if (list.isEmpty()) return

            list.forEach { it.isChecked = (it.key == tafsirKey) }

            val sheet = com.google.android.material.bottomsheet.BottomSheetDialog(this)
            val recycler = androidx.recyclerview.widget.RecyclerView(this).apply {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@ActivityTafsir)
                setPadding(0, dimen(R.dimen.dmnCommonSize2), 0, dimen(R.dimen.dmnCommonSize2))
                clipToPadding = false
            }
            recycler.adapter = com.quran.quranaudio.online.quran_module.adapters.tafsir
                .ADPTafsir(list) { index ->
                    // 选中的是免费/已订阅那部(付费门已在 ADPTafsir 内否决高级未订阅)
                    val picked = list[index]
                    if (picked.key != tafsirKey) {
                        tafsirKey = picked.key
                        tafsirInfoModel = picked
                        initTafsirHeader(binding.tafsirHeader)
                        loadContent()
                    }
                    sheet.dismiss()
                }
            sheet.setContentView(recycler)
            sheet.show()
        } catch (e: Exception) {
            android.util.Log.w("ActivityTafsir", "showTafsirSwitcher failed", e)
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

        // 步骤4：逃生按钮 —— 一键切回该语言的免费权威注释，用户永不被"卡住"读不了
        val btnReadFree = lockOverlay.findViewById<android.widget.TextView>(R.id.btnReadFreeTafsir)
        btnReadFree?.setOnClickListener {
            val freeKey = com.quran.quranaudio.online.quran_module.utils.tafsir
                .TafsirLanguageMapper.resolvePreferredSlug(
                    tafsirInfoModel?.langCode
                        ?: com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(this))
            val freeModel = freeKey?.let {
                com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager.getModel(it)
            }
            if (freeModel != null) {
                com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.setSavedTafsirKey(this, freeModel.key)
                tafsirKey = freeModel.key
                tafsirInfoModel = freeModel
                initTafsirHeader(binding.tafsirHeader)
                loadContent()
            } else {
                showTafsirSwitcher()
            }
        }
    }
    
    /**
     * 检查内容解锁状态并更新UI
     */
    private fun checkUnlockStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 方式0: 当前查看的是否为"免费首选注释"(每语言至少一部权威 Tafsir 永久免费)
                // 只判断"当前看的这部"，不改用户选择，与现有默认/用户选择完全兼容
                val isFreeTafsir = com.quran.quranaudio.online.quran_module.utils.tafsir
                    .TafsirLanguageMapper.isFreeTafsir(tafsirKey)

                // 方式1: 检查用户是否订阅
                val isSubscribed = SubscriptionHelper.isUserSubscribed(this@ActivityTafsir)

                // 方式2: 检查该经文是否通过广告解锁
                val isUnlockedByAd = unlockedContentRepository.isContentUnlocked(chapterNo, verseNo)

                isContentUnlocked = isFreeTafsir || isSubscribed || isUnlockedByAd

                android.util.Log.d("ActivityTafsir", "📊 Unlock Status Check:")
                android.util.Log.d("ActivityTafsir", "  - Free Tafsir: $isFreeTafsir")
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
    private fun preloadRewardedAd() {
        RewardedAdFlowCoordinator.preload(this, AdConfig.AD_TAFSIR_REWARD)
    }
    
    /**
     * 显示激励广告
     * ⚠️ 只有在用户点击解锁按钮时调用
     */
    private fun showRewardedAd() {
        RewardedAdFlowCoordinator.request(
            this,
            AdConfig.AD_TAFSIR_REWARD,
            "tafsir_unlock",
            getString(R.string.unlock_full_content),
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
                }
                
                override fun onShow(p0: com.quranaudio.common.ad.model.AdItem?) {
                    android.util.Log.d("ActivityTafsir", "▶️ Ad showing")
                }
                
                override fun onShowFail() {
                    android.util.Log.d("ActivityTafsir", "Reward flow cancelled or unavailable")
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

    private fun onTafsirAdFreeActionClicked() {
        if (shouldSuppressTafsirNativeAd()) {
            hideTafsirNativeAd()
            return
        }

        if (TafsirSessionAdFreeManager.shouldOfferRewardedAlternative()) {
            com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logEvent(
                "tafsir_ad_free_entry",
                mapOf("action" to "reward_request")
            )
            RewardedValueCoordinator.request(
                activity = this,
                placement = AdConfig.AD_TAFSIR_SESSION_AD_FREE_REWARD,
                rewardDescription = getString(R.string.tafsir_ad_free_reward_description)
            ) {
                TafsirSessionAdFreeManager.activate()
                com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logEvent(
                    "tafsir_ad_free_entry",
                    mapOf("action" to "reward_earned")
                )
                hideTafsirNativeAd()
                Toast.makeText(this, R.string.tafsir_ad_free_reward_earned, Toast.LENGTH_SHORT).show()
            }
        } else {
            tafsirAdFreeSubscriptionPending = true
            pausedAfterTafsirAdFreeSubscription = false
            com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logEvent(
                "tafsir_ad_free_entry",
                mapOf("action" to "subscription_open")
            )
            SubscriptionHelper.launchSubscriptionPage(this, "tafsir_native_ad_vip")
        }
    }
    
    /**
     * 在onResume中检查订阅状态
     */
    override fun onResume() {
        super.onResume()

        if (tafsirAdFreeSubscriptionPending && pausedAfterTafsirAdFreeSubscription) {
            tafsirAdFreeSubscriptionPending = false
            pausedAfterTafsirAdFreeSubscription = false
            if (!SubscriptionChecker.shouldHideAds(this)) {
                TafsirSessionAdFreeManager.enableRewardedAlternative()
            }
        }

        refreshTafsirNativeAdState()

        // 检查解锁状态（可能用户刚订阅回来）
        if (tafsirKey != null && chapterNo > 0 && verseNo > 0) {
            checkUnlockStatus()
        }
    }
    
    /**
     * ⚠️ 暂停时清理对话框状态，避免意外播放广告
     */
    override fun onPause() {
        super.onPause()

        if (tafsirAdFreeSubscriptionPending) {
            pausedAfterTafsirAdFreeSubscription = true
        }

    }
    
    /**
     * 清理资源
     */
    private fun loadTafsirNativeAd() {
        if (shouldSuppressTafsirNativeAd()) {
            hideTafsirNativeAd()
            return
        }
        if (nativeAdDisplayed) {
            showTafsirAdFooter()
            return
        }
        if (nativeAdRequestInFlight) return

        nativeAdRequestInFlight = true
        val container = binding.tafsirNativeAdContainer
        com.quranaudio.common.ad.NativeAdHelper.displayNativeAdWithAutoLoad(
            this,
            container,
            com.quran.quranaudio.quiz.R.layout.layout_ad_native_small_wrapper
        ) { displayed ->
            nativeAdRequestInFlight = false
            if (isFinishing || isDestroyed || shouldSuppressTafsirNativeAd()) {
                hideTafsirNativeAd()
                return@displayNativeAdWithAutoLoad
            }

            nativeAdDisplayed = displayed
            if (displayed) {
                showTafsirAdFooter()
            } else {
                binding.tafsirAdFooter.visibility = View.GONE
            }
        }
    }

    private fun shouldSuppressTafsirNativeAd(): Boolean {
        return SubscriptionChecker.shouldHideAds(this) || TafsirSessionAdFreeManager.isActive()
    }

    private fun refreshTafsirNativeAdState() {
        if (!::binding.isInitialized) return
        if (shouldSuppressTafsirNativeAd()) {
            hideTafsirNativeAd()
        } else if (nativeAdDisplayed) {
            showTafsirAdFooter()
        } else if (webPageReady) {
            loadTafsirNativeAd()
        }
    }

    private fun showTafsirAdFooter() {
        if (shouldSuppressTafsirNativeAd() || !nativeAdDisplayed) {
            hideTafsirNativeAd()
            return
        }

        val rewardedAlternative = TafsirSessionAdFreeManager.shouldOfferRewardedAlternative()
        binding.tafsirAdFreeAction.apply {
            val label = getString(
                if (rewardedAlternative) R.string.tafsir_ad_free_reward else R.string.tafsir_ad_free_vip
            )
            text = label
            contentDescription = label
            setIconResource(
                if (rewardedAlternative) {
                    com.quran.quranaudio.quiz.R.drawable.ic_rewarded_video
                } else {
                    R.drawable.dr_icon_premium
                }
            )
            visibility = View.VISIBLE
        }
        binding.tafsirNativeAdContainer.visibility = View.VISIBLE
        binding.tafsirAdFooter.visibility = View.VISIBLE
    }

    private fun hideTafsirNativeAd() {
        if (!::binding.isInitialized) return
        nativeAdDisplayed = false
        nativeAdRequestInFlight = false
        binding.tafsirNativeAdContainer.removeAllViews()
        binding.tafsirNativeAdContainer.visibility = View.GONE
        binding.tafsirAdFooter.visibility = View.GONE
    }

    override fun onDestroy() {
        contentLoadJob?.cancel()
        contentLoadJob = null

        super.onDestroy()
    }
}
