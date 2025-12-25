package com.quran.quranaudio.online.quran_module.frags.onboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.FragmentOnboardLanguageSelectionBinding
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants
import com.quran.quranaudio.online.prayertimes.ui.MainActivity
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs

/**
 * 🌐 语言选择引导页
 * 
 * 功能：
 * 1. 显示 7 种支持的语言选项
 * 2. 与 Settings 页面共用同一套语言数据和存储层 (SPAppConfigs)
 * 3. UI 严格按照截图设计实现
 * 4. 选中的语言会立即保存到 SharedPreferences
 */
class FragOnboardLanguage : FragOnboardBase() {
    
    private var _binding: FragmentOnboardLanguageSelectionBinding? = null
    private val binding get() = _binding!!
    
    // 🔗 共享数据层：与 Settings 页面使用相同的语言列表和代码
    private lateinit var languageNames: Array<String>
    private lateinit var languageCodes: Array<String>
    
    // 当前选中的语言代码
    private var selectedLanguageCode: String = SPAppConfigs.LOCALE_DEFAULT
    
    // 所有语言 Card 的映射 <languageCode, cardView>
    private val languageCards = mutableMapOf<String, LanguageCardViews>()
    
    private data class LanguageCardViews(
        val card: MaterialCardView,
        val checkIcon: View
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardLanguageSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        android.util.Log.d("FragOnboardLanguage", "═══════════════════════════════════════════════")
        android.util.Log.d("FragOnboardLanguage", "🎬 onViewCreated() START")
        
        super.onViewCreated(view, savedInstanceState)
        
        // 🎯 Firebase Analytics: 语言选择页展示（新用户引导的第一个流失点）
        com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(requireContext())
            .logWorkflowStep("language_selection_view")
        
        android.util.Log.d("FragOnboardLanguage", "✅ super.onViewCreated() completed")
        android.util.Log.d("FragOnboardLanguage", "🔍 Checking binding: ${_binding != null}")
        android.util.Log.d("FragOnboardLanguage", "🔍 Checking btnContinue: ${_binding?.btnContinue != null}")
        
        // 🔗 从共享资源加载语言列表（与 Settings 页面完全一致）
        languageNames = resources.getStringArray(R.array.app_language_names)
        languageCodes = resources.getStringArray(R.array.app_language_codes)
        
        android.util.Log.d("FragOnboardLanguage", "✅ Language arrays loaded: ${languageCodes.size} languages")
        
        // 🔗 从共享数据层读取当前已保存的语言
        selectedLanguageCode = SPAppConfigs.getLocale(requireContext())
        
        android.util.Log.d("FragOnboardLanguage", "🌐 Current saved language: $selectedLanguageCode")
        
        android.util.Log.d("FragOnboardLanguage", "🔧 Calling setupLanguageCards()...")
        setupLanguageCards()
        android.util.Log.d("FragOnboardLanguage", "✅ setupLanguageCards() completed")
        
        android.util.Log.d("FragOnboardLanguage", "🔧 Calling setupContinueButton()...")
        setupContinueButton()
        android.util.Log.d("FragOnboardLanguage", "✅ setupContinueButton() completed")
        
        // 🎯 Setup native ad at bottom
        android.util.Log.d("FragOnboardLanguage", "🔧 Calling setupNativeAd()...")
        setupNativeAd()
        android.util.Log.d("FragOnboardLanguage", "✅ setupNativeAd() completed")
        
        android.util.Log.d("FragOnboardLanguage", "🎬 onViewCreated() END")
        android.util.Log.d("FragOnboardLanguage", "═══════════════════════════════════════════════")
    }
    
    /**
     * 设置所有语言选择卡片
     */
    private fun setupLanguageCards() {
        // 映射：语言代码 -> Card ID 和 Check Icon ID
        // 🔗 使用与实际资源目录匹配的语言代码
        // 注意：Android 资源目录使用 values-in（不是 values-id）
        val cardMapping = mapOf(
            "en" to Pair(R.id.card_english, R.id.check_english),
            "id" to Pair(R.id.card_indonesian, R.id.check_indonesian),  // values-in (Android uses 'in' for resources)
            "ar" to Pair(R.id.card_arabic, R.id.check_arabic),          // values-ar
            "ur" to Pair(R.id.card_urdu, R.id.check_urdu),              // values-ur
            "ms" to Pair(R.id.card_malay, R.id.check_malay),            // values-ms
            "tr" to Pair(R.id.card_turkish, R.id.check_turkish),        // values-tr
            "bn" to Pair(R.id.card_bengali, R.id.check_bengali)         // values-bn
        )
        
        android.util.Log.d("FragOnboardLanguage", "🗺️ 语言卡片映射:")
        cardMapping.forEach { (code, ids) ->
            android.util.Log.d("FragOnboardLanguage", "   '$code' → cardId=${ids.first}, checkId=${ids.second}")
        }
        
        cardMapping.forEach { (code, ids) ->
            val card = binding.root.findViewById<MaterialCardView>(ids.first)
            val checkIcon = binding.root.findViewById<View>(ids.second)
            
            if (card != null && checkIcon != null) {
                languageCards[code] = LanguageCardViews(card, checkIcon)
                
                card.setOnClickListener {
                    android.util.Log.d("FragOnboardLanguage", "👆 卡片被点击: 语言代码='$code'")
                    selectLanguage(code)
                }
                android.util.Log.d("FragOnboardLanguage", "   ✅ 语言 '$code' 的卡片已设置点击事件")
            } else {
                android.util.Log.e("FragOnboardLanguage", "   ❌ 语言 '$code' 的卡片或图标未找到！card=$card, checkIcon=$checkIcon")
            }
        }
        
        // 初始化：显示当前选中的语言
        updateLanguageSelection(selectedLanguageCode)
    }
    
    /**
     * 选择语言
     * @param code 语言代码 (e.g., "en", "id", "ar")
     */
    private fun selectLanguage(code: String) {
        android.util.Log.d("FragOnboardLanguage", "═══════════════════════════════════════════════")
        android.util.Log.d("FragOnboardLanguage", "🔘 用户点击了语言卡片")
        android.util.Log.d("FragOnboardLanguage", "   接收到的语言代码: '$code'")
        
        selectedLanguageCode = code
        android.util.Log.d("FragOnboardLanguage", "   selectedLanguageCode 已设置为: '$selectedLanguageCode'")
        
        // 🎯 Firebase Analytics: 记录用户选择的语言（分析语言偏好）
        com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(requireContext())
            .logEvent("language_selected", mapOf("language_code" to code))
        
        // 🔗 关键：立即保存到共享数据层（与 Settings 页面使用相同的保存方法）
        android.util.Log.d("FragOnboardLanguage", "   正在调用 SPAppConfigs.setLocale()...")
        // 注意：这里只保存语言选择，不立即应用
        // 真正的语言切换会在用户点击Continue后，通过recreate()实现
        SPAppConfigs.setLocale(requireContext(), code)
        
        // 验证保存
        val savedLanguage = SPAppConfigs.getLocale(requireContext())
        android.util.Log.d("FragOnboardLanguage", "   保存后验证读取: '$savedLanguage'")
        
        if (savedLanguage == code) {
            android.util.Log.d("FragOnboardLanguage", "   ✅ 语言保存成功！")
        } else {
            android.util.Log.e("FragOnboardLanguage", "   ❌ 语言保存失败！保存的是 '$code'，但读取到 '$savedLanguage'")
        }
        
        // 更新 UI
        updateLanguageSelection(code)
        
        android.util.Log.d("FragOnboardLanguage", "═══════════════════════════════════════════════")
    }
    
    /**
     * 更新所有语言卡片的选中状态
     * 选中：白色背景 + 绿色边框 + 绿色文字 + 绿色对勾显示
     * 未选中：深绿色背景 + 无边框 + 白色文字 + 对勾隐藏
     */
    private fun updateLanguageSelection(selectedCode: String) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val unselectedBgColor = ContextCompat.getColor(requireContext(), R.color.language_card_unselected_bg)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.language_card_stroke_width)
        
        languageCards.forEach { (code, views) ->
            val isSelected = (code == selectedCode)
            
            // 获取卡片内的 TextView 和 ImageView
            val linearLayout = views.card.getChildAt(0) as? LinearLayout
            val textView = linearLayout?.getChildAt(0) as? TextView
            val imageView = linearLayout?.getChildAt(1) as? ImageView
            
            if (isSelected) {
                // ✅ 选中状态：白色背景 + 绿色边框 + 绿色文字 + 绿色对勾
                views.card.setCardBackgroundColor(whiteColor)
                views.card.strokeColor = primaryColor
                views.card.strokeWidth = strokeWidth
                textView?.setTextColor(primaryColor)
                imageView?.setColorFilter(primaryColor)
                views.checkIcon.visibility = View.VISIBLE
                
                android.util.Log.d("FragOnboardLanguage", "  ✓ Card $code: SELECTED (white bg, green border/text)")
            } else {
                // ⭕ 未选中状态：深绿色背景 + 无边框 + 白色文字 + 对勾隐藏
                views.card.setCardBackgroundColor(unselectedBgColor)
                views.card.strokeWidth = 0
                textView?.setTextColor(whiteColor)
                imageView?.setColorFilter(whiteColor)
                views.checkIcon.visibility = View.GONE
                
                android.util.Log.d("FragOnboardLanguage", "  ○ Card $code: UNSELECTED (dark green bg, white text)")
            }
        }
    }
    
    /**
     * 设置 Continue 按钮
     * 点击后保存语言并导航到古兰经版本选择页
     */
    private fun setupContinueButton() {
        android.util.Log.d("FragOnboardLanguage", "🔧 Setting up Continue button...")
        
        // 🔍 验证 binding 和按钮是否存在
        if (_binding == null) {
            android.util.Log.e("FragOnboardLanguage", "❌ ERROR: _binding is null!")
            return
        }
        
        val button = binding.btnContinue
        android.util.Log.d("FragOnboardLanguage", "🔍 Button reference: $button")
        android.util.Log.d("FragOnboardLanguage", "🔍 Button isClickable: ${button.isClickable}")
        android.util.Log.d("FragOnboardLanguage", "🔍 Button isEnabled: ${button.isEnabled}")
        android.util.Log.d("FragOnboardLanguage", "🔍 Button visibility: ${button.visibility} (0=VISIBLE, 4=INVISIBLE, 8=GONE)")
        
        button.setOnClickListener {
            android.util.Log.d("FragOnboardLanguage", "═══════════════════════════════════════════════")
            android.util.Log.d("FragOnboardLanguage", "🚀 Continue button clicked!")
            android.util.Log.d("FragOnboardLanguage", "   Current selected language: $selectedLanguageCode")

            // 1. 保存选中的语言
            SPAppConfigs.setLocale(requireContext(), selectedLanguageCode)
            android.util.Log.d("FragOnboardLanguage", "✅ Language saved to SPAppConfigs: $selectedLanguageCode")

            // 2. 验证保存
            val savedLang = SPAppConfigs.getLocale(requireContext())
            android.util.Log.d("FragOnboardLanguage", "🔍 Verification - saved language: $savedLang")

            // 3. 🔄 重新创建Activity并跳转到下一页
            android.util.Log.d("FragOnboardLanguage", "🔄 Getting activity reference...")
            val activity = activity
            android.util.Log.d("FragOnboardLanguage", "   Activity: $activity")
            android.util.Log.d("FragOnboardLanguage", "   Activity class: ${activity?.javaClass?.simpleName}")
            
            if (activity is com.quran.quranaudio.online.quran_module.activities.ActivityOnboarding) {
                android.util.Log.d("FragOnboardLanguage", "✅ Activity is ActivityOnboarding")
                
                // 🎯 Firebase Analytics: 用户完成语言选择并继续（关键转化点）
                com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(requireContext())
                    .logWorkflowStep("language_selection_complete")
                
                try {
                    android.util.Log.d("FragOnboardLanguage", "🔄 Attempting to recreate activity...")
                    activity.recreateWithLanguageChange(1)
                    android.util.Log.d("FragOnboardLanguage", "✅ recreateWithLanguageChange() called")
                } catch (e: Exception) {
                    android.util.Log.e("FragOnboardLanguage", "❌ Failed to recreate: ${e.message}", e)
                    android.util.Log.d("FragOnboardLanguage", "   Falling back to direct navigation...")
                    activity.navigateToNextPage()
        }
            } else {
                android.util.Log.e("FragOnboardLanguage", "❌ Activity is NOT ActivityOnboarding!")
                android.util.Log.e("FragOnboardLanguage", "   Activity class: ${activity?.javaClass?.name}")
            }
            
            android.util.Log.d("FragOnboardLanguage", "═══════════════════════════════════════════════")
        }
        
        android.util.Log.d("FragOnboardLanguage", "✅ Continue button setup complete")
    }
    
    /**
     * 🎯 Setup native ad at bottom of language list
     * Shows ad for unpaid users, loads dynamically if needed
     */
    private fun setupNativeAd() {
        android.util.Log.d("DIAGNOSE", "→→ setupNativeAd() called")
        
        val container = binding.nativeAdContainer
        android.util.Log.d("DIAGNOSE", "→→ Native ad container found: ${container != null}")
        
        try {
            // Check subscription status
            val isSubscribed = com.quranaudio.common.ad.SubscriptionChecker.isUserSubscribed(requireContext())
            android.util.Log.d("DIAGNOSE", "→→ User subscribed: $isSubscribed")
            
            if (isSubscribed) {
                android.util.Log.d("DIAGNOSE", "→→ User is subscribed, hiding ad container")
                container.visibility = View.GONE
                return
            }
            
            android.util.Log.d("DIAGNOSE", "→→ Calling NativeAdHelper.displayNativeAdWithAutoLoad()")
            android.util.Log.d("DIAGNOSE", "→→ Activity: ${requireActivity()}")
            android.util.Log.d("DIAGNOSE", "→→ Container: $container")
            android.util.Log.d("DIAGNOSE", "→→ Layout: R.layout.native_ad_onboarding")
            
            // Use new dynamic loading method
            com.quranaudio.common.ad.NativeAdHelper.displayNativeAdWithAutoLoad(
                requireActivity(),
                container,
                R.layout.native_ad_onboarding
            )
            
            // 🎯 Firebase Analytics: 原生广告展示在语言选择页（可能影响新用户转化）
            com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(requireContext())
                .logAdExposure("native", "onboarding_language")
            
            android.util.Log.d("DIAGNOSE", "✅ NativeAdHelper.displayNativeAdWithAutoLoad() returned")
        } catch (e: Exception) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ setupNativeAd() FAILED", e)
            android.util.Log.e("DIAGNOSE_ERROR", "❌ Exception type: ${e.javaClass.name}")
            android.util.Log.e("DIAGNOSE_ERROR", "❌ Exception message: ${e.message}")
            e.printStackTrace()
            container.visibility = View.GONE
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // 🔄 Refresh native ad when user returns (e.g., after clicking ad)
        try {
            android.util.Log.d("FragOnboardLanguage", "🔄 onResume: Refreshing native ad")
            setupNativeAd()
        } catch (e: Exception) {
            android.util.Log.e("FragOnboardLanguage", "❌ Failed to refresh ad on resume: ${e.message}", e)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
