package com.quran.quranaudio.online.quran_module.activities

import android.os.Bundle
import android.view.View
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.ActivityOnboardBinding
import com.quran.quranaudio.online.quran_module.frags.onboard.FragOnboardLanguage
import com.quran.quranaudio.online.quran_module.frags.onboard.FragOnboardRecitation
import com.quran.quranaudio.online.quran_module.frags.onboard.FragOnboardTranslations
import com.quran.quranaudio.online.quran_module.utils.gesture.HoverPushOpacityEffect
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppActions.setRequireOnboarding
import com.quran.quranaudio.online.quran_module.utils.simplified.SimpleTabSelectorListener

class ActivityOnboarding : com.quran.quranaudio.online.quran_module.activities.base.BaseActivity() {
    private lateinit var binding: ActivityOnboardBinding
    private lateinit var titles: Array<String>
    private lateinit var descs: Array<String>

    override fun shouldInflateAsynchronously() = true

    override fun getLayoutResource() = R.layout.activity_onboard

    private val lastPageIndex get() = titles.size - 1
    private var currentPageIndex = 0
    
    companion object {
        const val KEY_START_PAGE = "start_page_index"
        const val KEY_LANGUAGE_CHANGED = "language_changed"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("currentPageIndex", currentPageIndex)
        
        // 🔧 如果是语言切换触发的重启，将页面索引也保存到Bundle中
        // 这样可以确保在Activity重建时正确恢复
        val isLanguageChanged = intent?.getBooleanExtra(KEY_LANGUAGE_CHANGED, false) ?: false
        val startPage = intent?.getIntExtra(KEY_START_PAGE, 0) ?: 0
        if (isLanguageChanged) {
            android.util.Log.d("ActivityOnboarding", "💾 Saving language change state to bundle: startPage=$startPage")
            outState.putBoolean(KEY_LANGUAGE_CHANGED, true)
            outState.putInt(KEY_START_PAGE, startPage)
        }
        
        super.onSaveInstanceState(outState)
    }

    override fun preActivityInflate(savedInstanceState: Bundle?) {
        // 🔧 检查是否是语言切换触发的重启
        // 优先从savedInstanceState检查（因为recreate()会保存状态到Bundle）
        val isLanguageChangedFromBundle = savedInstanceState?.getBoolean(KEY_LANGUAGE_CHANGED, false) ?: false
        val startPageFromBundle = savedInstanceState?.getInt(KEY_START_PAGE, -1) ?: -1
        
        // 同时检查intent extras作为备用
        val isLanguageChangedFromIntent = intent?.getBooleanExtra(KEY_LANGUAGE_CHANGED, false) ?: false
        val startPageFromIntent = intent?.getIntExtra(KEY_START_PAGE, 0) ?: 0
        
        // 合并两个来源的信息
        val isLanguageChanged = isLanguageChangedFromBundle || isLanguageChangedFromIntent
        val startPage = if (startPageFromBundle >= 0) startPageFromBundle else startPageFromIntent
        
        // 🎯 语言切换时，优先使用指定的页面索引
        // 否则使用 savedInstanceState 恢复状态（例如屏幕旋转）
        currentPageIndex = if (isLanguageChanged) {
            startPage
        } else {
            savedInstanceState?.getInt("currentPageIndex", 0) ?: 0
        }
        
        super.preActivityInflate(savedInstanceState)
    }

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        binding = ActivityOnboardBinding.bind(activityView)

        prepare()
        navigate(currentPageIndex)

        for (button in arrayOf(binding.previous, binding.next)) {
            button.setOnTouchListener(HoverPushOpacityEffect())
        }

        binding.skip.setOnClickListener { takeOff() }
        binding.previous.setOnClickListener {
            if (currentPageIndex == 0) {
                return@setOnClickListener
            }

            navigate(--currentPageIndex)
        }
        binding.next.setOnClickListener {
            if (currentPageIndex == lastPageIndex) {
                takeOff()
                return@setOnClickListener
            }

            navigate(++currentPageIndex)
        }
        
        // 🌐 隐藏所有导航元素，让每个页面自己控制导航
        hideNavigationElements()
    }
    
    /**
     * 隐藏顶部和底部的导航元素
     * 各个Fragment通过自己的Continue按钮控制导航流程
     */
    private fun hideNavigationElements() {
        binding.skip.visibility = View.GONE
        binding.title.visibility = View.GONE
        binding.desc.visibility = View.GONE
        binding.previous.visibility = View.GONE
        binding.next.visibility = View.GONE
        binding.pagerIndicator.visibility = View.GONE
        
        android.util.Log.d("ActivityOnboarding", "🎯 Navigation elements hidden - using fragment-level navigation")
    }

    private fun prepare() {
        titles = strArray(R.array.arrOnboardingTitles)
        descs = strArray(R.array.arrOnboardingDescs)

        for (title in titles) {
            binding.pagerIndicator.addTab(binding.pagerIndicator.newTab())
        }

        binding.pagerIndicator.addOnTabSelectedListener(object : SimpleTabSelectorListener() {
            override fun onTabSelected(tab: TabLayout.Tab) {
                navigate(tab.position)
            }
        })

        initViewPager(binding.board)
    }

    private fun initViewPager(viewPager: ViewPager2) {
        val adapter = com.quran.quranaudio.online.quran_module.adapters.utility.ViewPagerAdapter2(
            this
        ).apply {
            // 🌐 新用户首次启动流程：
            // 语言选择 -> 古兰经版本选择 -> Istiqamah引导页 -> 通知权限 -> 7天试用 -> 订阅页
            arrayOf(
                FragOnboardLanguage(),
                com.quran.quranaudio.online.quran_module.frags.onboard.FragOnboardQuranVersion(),
                com.quran.quranaudio.online.quran_module.frags.onboard.FragOnboardIstiqamah(),
                com.quran.quranaudio.online.quran_module.frags.onboard.FragOnboardNotification(),
                com.quran.quranaudio.online.quran_module.frags.onboard.FragOnboardTrial()
            ).forEachIndexed { index, frag ->
                addFragment(frag, if (index < titles.size) titles[index] else "")
            }
        }

        viewPager.let {
            it.adapter = adapter
            // 🔧 设置为 ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT (不预加载)
            // 避免在语言选择前就创建所有Fragment，确保后续Fragment使用用户选择的语言
            it.offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
            it.getChildAt(0).overScrollMode = View.OVER_SCROLL_NEVER
            it.isUserInputEnabled = false
            
            // 🚨 最终解决方案：使用 post 将 setCurrentItem 推迟到布局完成后执行
            // 避免被系统 FragmentManager 的状态恢复所覆盖
            it.post {
                // 确保在所有 Activity/Fragment/View 状态恢复完成后，手动设置到目标页
                android.util.Log.d("ActivityOnboarding", "🔧 [POSTED] Setting ViewPager2 currentItem to: $currentPageIndex")
                
                // 确保设置的是正确的值，防止索引超出范围
                val finalIndex = currentPageIndex.coerceIn(0, adapter.itemCount - 1)
                it.setCurrentItem(finalIndex, false)  // false = 不使用动画，直接跳转
                
                android.util.Log.d("ActivityOnboarding", "✅ [POSTED] ViewPager2 currentItem set complete to: $finalIndex")
            }
        }
    }
    
    /**
     * 导航到下一页（公开方法，供子Fragment调用）
     */
    fun navigateToNextPage() {
        if (currentPageIndex < lastPageIndex) {
            navigate(++currentPageIndex)
        }
    }
    
    /**
     * 🌐 语言切换后重新创建Activity并跳转到下一页
     * 这样可以确保后续的Fragment使用新选择的语言创建
     */
    fun recreateWithLanguageChange(nextPageIndex: Int) {
        android.util.Log.d("ActivityOnboarding", "🔄 Recreating activity with language change, jumping to page: $nextPageIndex")
        
        // 保存要跳转的页面索引
        intent.putExtra(KEY_START_PAGE, nextPageIndex)
        intent.putExtra(KEY_LANGUAGE_CHANGED, true)
        
        // 重新创建Activity
        recreate()
    }

    private fun navigate(index: Int) {
        if (index < 0 || index > lastPageIndex) {
            return
        }

        currentPageIndex = index

        binding.let {
            it.previous.visibility = if (index == 0) View.GONE else View.VISIBLE
            it.next.setText(
                if (index == lastPageIndex) R.string.strLabelStart else R.string.strLabelNext
            )
            it.pagerIndicator.selectTab(it.pagerIndicator.getTabAt(index))

            it.title.text = titles[index]
            it.desc.text = descs[index]
            it.board.currentItem = index
        }
    }

    private fun takeOff() {
        setRequireOnboarding(this, false)

        launchMainActivity()
        finish()
    }
}
