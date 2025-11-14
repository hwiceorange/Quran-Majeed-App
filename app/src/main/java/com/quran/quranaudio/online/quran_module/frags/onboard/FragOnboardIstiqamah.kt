package com.quran.quranaudio.online.quran_module.frags.onboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.quran.quranaudio.online.databinding.FragmentOnboardIstiqamahBinding

/**
 * 🕌 Istiqamah: Consistency in Worship 引导页
 * 
 * 功能：
 * 1. 展示坚持信仰的重要性
 * 2. 显示持续追踪带来的成长曲线
 * 3. 鼓励用户建立良好的习惯
 * 4. 点击Continue后导航到下一个引导页（通知权限）
 */
class FragOnboardIstiqamah : FragOnboardBase() {
    
    private var _binding: FragmentOnboardIstiqamahBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardIstiqamahBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("FragOnboardIstiqamah", "🕌 Istiqamah page displayed")
        
        setupContinueButton()
    }
    
    /**
     * 设置 Continue 按钮
     * 点击后导航到下一个引导页
     */
    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            android.util.Log.d("FragOnboardIstiqamah", "🚀 Continue button clicked")
            
            // 通知Activity导航到下一个页面
            val activity = activity as? com.quran.quranaudio.online.quran_module.activities.ActivityOnboarding
            activity?.navigateToNextPage()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

