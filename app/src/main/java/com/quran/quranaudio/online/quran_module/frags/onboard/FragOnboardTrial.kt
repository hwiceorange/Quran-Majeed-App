package com.quran.quranaudio.online.quran_module.frags.onboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.quran.quranaudio.online.databinding.FragmentOnboardTrialBinding
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants
import com.quran.quranaudio.online.subscription.SubscriptionActivity

/**
 * 🎁 7天免费试用引导页
 * 
 * 功能：
 * 1. 展示7天免费试用优惠
 * 2. 激励用户体验完整功能
 * 3. 点击"Try for Free"按钮进入订阅页
 * 4. 订阅成功后进入主页
 */
class FragOnboardTrial : FragOnboardBase() {
    
    private var _binding: FragmentOnboardTrialBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardTrialBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("FragOnboardTrial", "🎁 7-day trial page displayed")
        
        setupTryFreeButton()
    }
    
    /**
     * 设置 Try for Free 按钮
     * 点击后进入订阅页面
     */
    private fun setupTryFreeButton() {
        binding.btnTryFree.setOnClickListener {
            android.util.Log.d("FragOnboardTrial", "🚀 Try for Free button clicked")
            
            // 标记引导流程完成
            markOnboardingComplete()
            
            // 进入订阅页面
            navigateToSubscription()
        }
    }
    
    /**
     * 标记引导完成
     */
    private fun markOnboardingComplete() {
        val prefs = requireContext().getSharedPreferences(
            PreferencesConstants.LOCATION,
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit().putBoolean(PreferencesConstants.FIRST_LAUNCH, false).commit()
        
        android.util.Log.d("FragOnboardTrial", "✅ Onboarding marked as complete")
    }
    
    /**
     * 导航到订阅页面
     */
    private fun navigateToSubscription() {
        android.util.Log.d("FragOnboardTrial", "💳 Navigating to Subscription page")
        
        val intent = Intent(requireContext(), SubscriptionActivity::class.java)
        intent.putExtra("from_onboarding", true) // 标记来自引导流程
        intent.putExtra(SubscriptionActivity.EXTRA_SOURCE, "onboarding")
        startActivity(intent)
        
        // 结束 ActivityOnboarding
        activity?.finish()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

