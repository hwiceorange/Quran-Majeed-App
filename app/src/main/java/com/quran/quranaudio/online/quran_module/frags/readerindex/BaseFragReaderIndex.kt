package com.quran.quranaudio.online.quran_module.frags.readerindex

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.cardview.widget.CardView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.SimpleItemAnimator
import com.google.android.material.button.MaterialButton
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.FragReaderIndexBinding
import com.quran.quranaudio.online.quran_module.activities.ActivityReaderIndexPage
import com.quran.quranaudio.online.quran_module.data.LastReadRecord
import com.quran.quranaudio.online.quran_module.frags.BaseFragment
import com.quran.quranaudio.online.quran_module.interfaceUtils.OnResultReadyCallback
import com.quran.quranaudio.online.quran_module.interfaceUtils.readerIndex.FragReaderIndexCallback
import com.quran.quranaudio.online.quran_module.repository.LastReadRepository
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs
import com.quran.quranaudio.online.quran_module.viewModels.FavChaptersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

abstract class BaseFragReaderIndex : BaseFragment(), FragReaderIndexCallback {
    lateinit var favChaptersModel: FavChaptersViewModel
    private val quranMetaRef = AtomicReference<com.quran.quranaudio.online.quran_module.components.quran.QuranMeta>()
    protected lateinit var binding: FragReaderIndexBinding
    private var isReversed = false
    
    // Last Read Card components
    private lateinit var lastReadRepository: LastReadRepository
    private var lastReadTitle: TextView? = null  // 外部标题
    private var lastReadCard: View? = null
    private var tvLastReadInfo: TextView? = null
    private var tvVerseNumber: TextView? = null
    private var btnContinue: TextView? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // 支持 Activity Host
        (context as? ActivityReaderIndexPage)?.addToCallbacks(this)
        
        // 支持 Fragment Host (QuranIndexFragment)
        (parentFragment as? com.quran.quranaudio.online.quran_module.frags.QuranIndexFragment)?.addToCallbacks(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.frag_reader_index, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favChaptersModel = ViewModelProvider(requireActivity())[FavChaptersViewModel::class.java]
        favChaptersModel.refreshFavChapters(view.context)
        binding = FragReaderIndexBinding.bind(view)
        
        // Initialize Last Read Repository
        lastReadRepository = LastReadRepository(view.context)
        
        // Initialize Last Read Card views
        initLastReadCard(view)

        // ViewUtils.setBounceOverScrollRV(mBinding.list);
        binding.loader.visibility = View.VISIBLE
        com.quran.quranaudio.online.quran_module.components.quran.QuranMeta.prepareInstance(view.context, object : OnResultReadyCallback<com.quran.quranaudio.online.quran_module.components.quran.QuranMeta> {
            override fun onReady(r: com.quran.quranaudio.online.quran_module.components.quran.QuranMeta) {
                quranMetaRef.set(r)

                binding.loader.visibility = View.VISIBLE

                lifecycleScope.launch(Dispatchers.Main) {
                    initList(binding.list, view.context)
                    
                    // Load Last Read position after QuranMeta is ready
                    loadLastReadPosition(view.context)

                    binding.loader.visibility = View.GONE
                }
            }
        })
    }

    @CallSuper
    protected open fun initList(list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2, ctx: Context) {
        val animator = list.itemAnimator
        if (animator is SimpleItemAnimator) {
            animator.supportsChangeAnimations = false
        }
    }

    override fun scrollToTop(smooth: Boolean) {
        binding.list.layoutManager?.let {
            if (smooth) binding.list.smoothScrollToPosition(0)
            else binding.list.scrollToPosition(0)
        }
    }

    override fun sort(ctx: Context) {
        binding.loader.visibility = View.VISIBLE
        lifecycleScope.launch {
            resetAdapter(binding.list, ctx, !isReversed)

            withContext(Dispatchers.Main) {
                binding.loader.visibility = View.GONE
            }
        }
    }

    @CallSuper
    protected open fun resetAdapter(list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2, ctx: Context, reverse: Boolean) {
        isReversed = reverse
    }

    val quranMeta: com.quran.quranaudio.online.quran_module.components.quran.QuranMeta get() = quranMetaRef.get()
    
    /**
     * 初始化 Last Read 卡片视图（从 Activity 的 Header 中获取）
     */
    private fun initLastReadCard(view: View) {
        try {
            // 从 Activity 的根视图查找 Last Read Card
            val activityRootView = activity?.window?.decorView?.findViewById<View>(android.R.id.content)
            
            if (activityRootView != null) {
                lastReadTitle = activityRootView.findViewById(R.id.tv_last_read_title)  // 外部标题
                lastReadCard = activityRootView.findViewById(R.id.last_read_card)
                tvLastReadInfo = activityRootView.findViewById(R.id.tv_last_read_info)
                tvVerseNumber = activityRootView.findViewById(R.id.tv_verse_number)
                btnContinue = activityRootView.findViewById(R.id.btn_continue)
                
                // 默认隐藏，等待数据加载后显示
                lastReadTitle?.visibility = View.GONE
                lastReadCard?.visibility = View.GONE
                
                Log.d("BaseFragReaderIndex", "Last Read Card views initialized from header")
            } else {
                Log.w("BaseFragReaderIndex", "Activity root view not found")
            }
        } catch (e: Exception) {
            Log.e("BaseFragReaderIndex", "Error initializing Last Read Card views", e)
        }
    }
    
    /**
     * 加载上次阅读位置并更新 UI
     */
    private fun loadLastReadPosition(context: Context) {
        try {
            lastReadRepository.loadLastReadPosition { record ->
                lifecycleScope.launch(Dispatchers.Main) {
                    if (record != null && record.hasValidRecord()) {
                        updateLastReadCard(context, record)
                    } else {
                        Log.d("BaseFragReaderIndex", "No valid last read record found")
                        lastReadTitle?.visibility = View.GONE
                        lastReadCard?.visibility = View.GONE
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("BaseFragReaderIndex", "Error loading last read position", e)
            lastReadTitle?.visibility = View.GONE
            lastReadCard?.visibility = View.GONE
        }
    }
    
    /**
     * 更新 Last Read 卡片显示内容
     */
    private fun updateLastReadCard(context: Context, record: LastReadRecord) {
        try {
            val quranMeta = quranMetaRef.get() ?: return
            
            val surahNumber = record.lastReadSurah
            val ayahNumber = record.lastReadAyah
            val readingMode = record.getReadingMode() // 获取阅读模式
            
            // Get localized chapter name
            val language = SPAppConfigs.getLocale(context)
            val normalizedLang = com.quran.quranaudio.online.quran_module.components.quran.QuranMeta.normalizeLanguageCode(language)
            val surahName = quranMeta.getChapterName(context, surahNumber, normalizedLang)
            
            // Update UI - 章节名称（加粗加大）
            tvLastReadInfo?.text = context.getString(R.string.surah_format, surahNumber, surahName)
            
            // Update UI - 节号（小字不加粗）
            tvVerseNumber?.text = context.getString(R.string.verse_no_format, ayahNumber)
            
            // Set click listener for "Continue →" button (根据阅读模式启动正确的阅读器)
            btnContinue?.setOnClickListener {
                Log.d("BaseFragReaderIndex", "Continue button clicked: Surah $surahNumber, Ayah $ayahNumber, Mode: $readingMode")
                launchReaderByMode(context, record, readingMode)
            }
            
            // Set click listener for the entire card (根据阅读模式启动正确的阅读器)
            lastReadCard?.setOnClickListener {
                Log.d("BaseFragReaderIndex", "Last Read Card clicked: Surah $surahNumber, Ayah $ayahNumber, Mode: $readingMode")
                launchReaderByMode(context, record, readingMode)
            }
            
            // Show the title and card
            lastReadTitle?.visibility = View.VISIBLE
            lastReadCard?.visibility = View.VISIBLE
            
            Log.d("BaseFragReaderIndex", "Last Read Card updated: Surah $surahNumber ($surahName), Ayah $ayahNumber, Mode: $readingMode")
        } catch (e: Exception) {
            Log.e("BaseFragReaderIndex", "Error updating Last Read Card", e)
            lastReadTitle?.visibility = View.GONE
            lastReadCard?.visibility = View.GONE
        }
    }
    
    /**
     * 根据阅读模式启动正确的阅读器
     * - SURAH 模式：启动章节阅读（ReaderFactory.startChapter）
     * - JUZ 模式：启动 Juz 阅读（ReaderFactory.startJuz）
     * - VERSES 模式或其他：启动单节阅读（ReaderFactory.startVerse）
     */
    private fun launchReaderByMode(context: Context, record: LastReadRecord, mode: String) {
        try {
            val quranMeta = quranMetaRef.get() ?: return
            
            when (mode) {
                LastReadRecord.MODE_SURAH -> {
                    // 章节模式：启动章节阅读（分页滚动），并滚动到指定的节号
                    Log.d("BaseFragReaderIndex", "Launching SURAH mode: Chapter ${record.lastReadSurah}, scrolling to Ayah ${record.lastReadAyah}")
                    val intent = ReaderFactory.prepareChapterIntent(record.lastReadSurah)
                    // 添加滚动位置（READER_KEY_PENDING_SCROLL 在 ReaderFactory 中已支持）
                    intent.putExtra(com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_PENDING_SCROLL, 
                        intArrayOf(record.lastReadSurah, record.lastReadAyah))
                    context.startActivity(intent.setClass(context, com.quran.quranaudio.online.quran_module.activities.ActivityReader::class.java))
                }
                LastReadRecord.MODE_JUZ -> {
                    // Juz 模式：启动 Juz 阅读，并滚动到指定的节号
                    val juzNo = if (record.lastReadJuz > 0) {
                        record.lastReadJuz
                    } else {
                        // 如果没有保存 Juz 编号，遍历所有 Juz 查找包含该节号的 Juz
                        var foundJuz = 1
                        for (j in 1..30) {
                            if (quranMeta.isVerseValid4Juz(j, record.lastReadSurah, record.lastReadAyah)) {
                                foundJuz = j
                                break
                            }
                        }
                        foundJuz
                    }
                    Log.d("BaseFragReaderIndex", "Launching JUZ mode: Juz $juzNo, scrolling to Chapter ${record.lastReadSurah}, Ayah ${record.lastReadAyah}")
                    val intent = ReaderFactory.prepareJuzIntent(juzNo)
                    intent.putExtra(com.quran.quranaudio.online.quran_module.utils.univ.Keys.READER_KEY_PENDING_SCROLL, 
                        intArrayOf(record.lastReadSurah, record.lastReadAyah))
                    context.startActivity(intent.setClass(context, com.quran.quranaudio.online.quran_module.activities.ActivityReader::class.java))
                }
                LastReadRecord.MODE_VERSES -> {
                    // 单节模式：启动单节阅读
                    Log.d("BaseFragReaderIndex", "Launching VERSES mode: Chapter ${record.lastReadSurah}, Ayah ${record.lastReadAyah}")
                    ReaderFactory.startVerse(context, record.lastReadSurah, record.lastReadAyah)
                }
                else -> {
                    // 默认：启动单节阅读（向后兼容）
                    Log.d("BaseFragReaderIndex", "Launching default mode: Chapter ${record.lastReadSurah}, Ayah ${record.lastReadAyah}")
                    ReaderFactory.startVerse(context, record.lastReadSurah, record.lastReadAyah)
                }
            }
        } catch (e: Exception) {
            Log.e("BaseFragReaderIndex", "Error launching reader by mode: $mode", e)
            // 回退到默认的单节阅读
            ReaderFactory.startVerse(context, record.lastReadSurah, record.lastReadAyah)
        }
    }
    
    /**
     * 在 Fragment 恢复时重新加载 Last Read 位置（解决用户离开经文页后回到目录页时更新不及时的问题）
     */
    override fun onResume() {
        super.onResume()
        // 重新加载 Last Read 位置，确保数据是最新的
        loadLastReadPosition(requireContext())
    }
}