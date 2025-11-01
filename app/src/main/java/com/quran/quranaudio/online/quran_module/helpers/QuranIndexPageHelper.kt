package com.quran.quranaudio.online.quran_module.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.LytFavChaptersTabBtnBinding
import com.quran.quranaudio.online.databinding.LytReaderIndexHeaderBinding
import com.quran.quranaudio.online.databinding.LytReaderIndexTabBinding
import com.quran.quranaudio.online.quran_module.adapters.utility.ViewPagerAdapter2
import com.quran.quranaudio.online.quran_module.data.LastReadRecord
import com.quran.quranaudio.online.quran_module.frags.readerindex.FragReaderIndexChapters
import com.quran.quranaudio.online.quran_module.frags.readerindex.FragReaderIndexFavChapters
import com.quran.quranaudio.online.quran_module.frags.readerindex.FragReaderIndexJuz
import com.quran.quranaudio.online.quran_module.interfaceUtils.readerIndex.FragReaderIndexCallback
import com.quran.quranaudio.online.quran_module.repository.LastReadRepository
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs
import com.quran.quranaudio.online.quran_module.utils.simplified.SimpleTabSelectorListener
import com.quran.quranaudio.online.quran_module.viewModels.FavChaptersViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Quran Index Page Helper
 * 复用 ActivityReaderIndexPage 的所有逻辑，供 Activity 和 Fragment 共同使用
 */
class QuranIndexPageHelper(
    private val fragment: Fragment,
    private val headerBinding: LytReaderIndexHeaderBinding,
    private val viewPager: ViewPager2,
    private val sortButton: FloatingActionButton,
    private val hideBackButton: Boolean = false
) {
    private val fragCallbacks: MutableList<FragReaderIndexCallback> = ArrayList()
    private lateinit var lastReadRepository: LastReadRepository
    private lateinit var favChaptersModel: FavChaptersViewModel
    
    // Last Read Card components
    private var lastReadTitle: TextView? = null
    private var lastReadCard: View? = null
    private var tvLastReadInfo: TextView? = null
    private var tvVerseNumber: TextView? = null
    private var btnContinue: TextView? = null
    
    private val context: Context get() = fragment.requireContext()

    fun init() {
        // Initialize ViewModels and Repository
        favChaptersModel = ViewModelProvider(fragment.requireActivity())[FavChaptersViewModel::class.java]
        favChaptersModel.refreshFavChapters(context)
        lastReadRepository = LastReadRepository(context)
        
        // Initialize all components
        initHeader()
        initViewPager()
        initTabs()
        initSort()
        initLastReadCard()
        loadLastReadPosition()
    }

    private fun initHeader() {
        headerBinding.let {
            // 根据参数决定是否隐藏返回按钮
            if (hideBackButton) {
                it.back.visibility = View.GONE
            } else {
                it.back.setOnClickListener { fragment.requireActivity().finish() }
            }
            
            it.search.setOnClickListener { 
                context.startActivity(Intent(context, com.quran.quranaudio.online.quran_module.activities.ActivityQuran_Search::class.java)) 
            }

            it.qBookmark?.setOnClickListener { 
                context.startActivity(Intent(context, com.quran.quranaudio.online.quran_module.activities.Activity_Quran_Bookmark::class.java)) 
            }
        }
    }

    private fun initViewPager() {
        val adapter = ViewPagerAdapter2(
            fragment.requireActivity()
        ).apply {
            addFragment(FragReaderIndexChapters.newInstance(), context.getString(R.string.strTitleReaderChapters))
            addFragment(FragReaderIndexJuz.newInstance(), context.getString(R.string.strTitleReaderJuz))
            addFragment(FragReaderIndexFavChapters(), "")
        }

        viewPager.let {
            it.adapter = adapter
            it.offscreenPageLimit = adapter.itemCount
            it.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
            it.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    sortButton.visibility = if (position == 2) View.GONE else View.GONE
                }
            })
        }
    }

    private fun initTabs() {
        headerBinding.let {
            it.readerTabLayout.setTabSetupCallback { vp: ViewPager2, tab: TabLayout.Tab, position: Int ->
                val inflater = LayoutInflater.from(context)
                if (position == 2) {
                    tab.customView = LytFavChaptersTabBtnBinding.inflate(inflater).root
                } else {
                    tab.customView = LytReaderIndexTabBinding.inflate(inflater).apply {
                        tabTitle.text = (vp.adapter as? ViewPagerAdapter2)?.getPageTitle(position)
                    }.root
                }
            }
            it.readerTabLayout.populateFromViewPager(viewPager)
            it.readerTabLayout.addOnTabSelectedListener(object : SimpleTabSelectorListener() {
                override fun onTabReselected(tab: TabLayout.Tab) {
                    scrollToTop()
                }
            })
        }
    }

    private fun initSort() {
        sortButton.visibility = View.VISIBLE
        sortButton.setOnClickListener { sort() }
    }

    private fun scrollToTop() {
        if (fragCallbacks.size > viewPager.currentItem) {
            fragCallbacks[viewPager.currentItem].scrollToTop(true)
        }
    }

    private fun sort() {
        if (fragCallbacks.size > viewPager.currentItem) {
            fragCallbacks[viewPager.currentItem].sort(context)
        }
    }

    fun addToCallbacks(callback: FragReaderIndexCallback) {
        fragCallbacks.add(callback)
    }
    
    /**
     * 初始化 Last Read 卡片（完全复用 BaseFragReaderIndex 的逻辑）
     */
    private fun initLastReadCard() {
        try {
            lastReadTitle = headerBinding.root.findViewById(R.id.tv_last_read_title)
            lastReadCard = headerBinding.root.findViewById(R.id.last_read_card)
            tvLastReadInfo = lastReadCard?.findViewById(R.id.tv_last_read_info)
            tvVerseNumber = lastReadCard?.findViewById(R.id.tv_verse_number)
            btnContinue = lastReadCard?.findViewById(R.id.btn_continue)
            
            lastReadTitle?.visibility = View.GONE
            lastReadCard?.visibility = View.GONE
            
            Log.d("QuranIndexPageHelper", "Last Read Card initialized")
        } catch (e: Exception) {
            Log.e("QuranIndexPageHelper", "Error initializing Last Read Card", e)
        }
    }
    
    /**
     * 加载上次阅读位置（完全复用 BaseFragReaderIndex 的逻辑）
     */
    private fun loadLastReadPosition() {
        try {
            lastReadRepository.loadLastReadPosition { record ->
                fragment.lifecycleScope.launch(Dispatchers.Main) {
                    if (record != null && record.hasValidRecord()) {
                        updateLastReadCard(record)
                    } else {
                        lastReadTitle?.visibility = View.GONE
                        lastReadCard?.visibility = View.GONE
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("QuranIndexPageHelper", "Error loading last read position", e)
        }
    }
    
    /**
     * 更新 Last Read 卡片（完全复用 BaseFragReaderIndex 的逻辑）
     */
    private fun updateLastReadCard(record: LastReadRecord) {
        try {
            val surahNumber = record.lastReadSurah
            val ayahNumber = record.lastReadAyah
            val readingMode = record.getReadingMode()
            
            com.quran.quranaudio.online.quran_module.components.quran.QuranMeta.prepareInstance(context, object : com.quran.quranaudio.online.quran_module.interfaceUtils.OnResultReadyCallback<com.quran.quranaudio.online.quran_module.components.quran.QuranMeta> {
                override fun onReady(quranMeta: com.quran.quranaudio.online.quran_module.components.quran.QuranMeta) {
                    fragment.lifecycleScope.launch(Dispatchers.Main) {
                        val language = SPAppConfigs.getLocale(context)
                        val normalizedLang = com.quran.quranaudio.online.quran_module.components.quran.QuranMeta.normalizeLanguageCode(language)
                        val surahName = quranMeta.getChapterName(context, surahNumber, normalizedLang)
                        
                        tvLastReadInfo?.text = context.getString(R.string.surah_format, surahNumber, surahName)
                        tvVerseNumber?.text = context.getString(R.string.verse_no_format, ayahNumber)
                        
                        btnContinue?.setOnClickListener {
                            launchReaderByMode(record, readingMode)
                        }
                        
                        lastReadCard?.setOnClickListener {
                            launchReaderByMode(record, readingMode)
                        }
                        
                        lastReadTitle?.visibility = View.VISIBLE
                        lastReadCard?.visibility = View.VISIBLE
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("QuranIndexPageHelper", "Error updating Last Read Card", e)
        }
    }
    
    /**
     * 根据阅读模式启动阅读器（完全复用 BaseFragReaderIndex 的逻辑）
     */
    private fun launchReaderByMode(record: LastReadRecord, mode: String) {
        when (mode) {
            LastReadRecord.MODE_SURAH, LastReadRecord.MODE_VERSES -> {
                ReaderFactory.startVerse(context, record.lastReadSurah, record.lastReadAyah)
            }
            LastReadRecord.MODE_JUZ -> {
                if (record.lastReadJuz > 0) {
                    ReaderFactory.startJuz(context, record.lastReadJuz)
                } else {
                    ReaderFactory.startVerse(context, record.lastReadSurah, record.lastReadAyah)
                }
            }
            else -> {
                ReaderFactory.startVerse(context, record.lastReadSurah, record.lastReadAyah)
            }
        }
    }

    fun onResume() {
        loadLastReadPosition()
    }

    fun onDestroyView() {
        fragCallbacks.clear()
    }
}

