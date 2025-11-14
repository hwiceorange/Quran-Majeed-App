package com.quran.quranaudio.online.quran_module.frags.onboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.quran.quranaudio.online.databinding.FragmentOnboardNotificationBinding

/**
 * 🔔 通知权限引导页
 * 
 * 功能：
 * 1. 展示通知权限的重要性和用途
 * 2. 模仿系统通知权限对话框样式
 * 3. 引导用户点击Allow按钮（小手指图标提示）
 * 4. 点击Allow后请求系统通知权限
 * 5. 无论用户选择什么，都导航到下一页（7天试用页）
 */
class FragOnboardNotification : FragOnboardBase() {
    
    private var _binding: FragmentOnboardNotificationBinding? = null
    private val binding get() = _binding!!
    
    // 通知权限请求launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        android.util.Log.d("FragOnboardNotification", if (isGranted) 
            "✅ Notification permission granted" 
        else 
            "❌ Notification permission denied"
        )
        
        // 无论用户是否授权，都导航到下一页
        navigateToNextPage()
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("FragOnboardNotification", "🔔 Notification permission page displayed")
        
        setupButtons()
        startHandPointerAnimation()
    }
    
    /**
     * 设置按钮点击事件
     */
    private fun setupButtons() {
        // Allow 按钮
        binding.btnAllow.setOnClickListener {
            android.util.Log.d("FragOnboardNotification", "🔔 Allow button clicked")
            requestNotificationPermission()
        }
        
        // Don't allow 按钮
        binding.btnDontAllow.setOnClickListener {
            android.util.Log.d("FragOnboardNotification", "❌ Don't allow button clicked")
            
            // 用户选择不允许，直接导航到下一页
            navigateToNextPage()
        }
    }
    
    /**
     * 请求通知权限
     */
    private fun requestNotificationPermission() {
        // Android 13+ 需要请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // 已经有权限
                    android.util.Log.d("FragOnboardNotification", "✅ Notification permission already granted")
                    navigateToNextPage()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 用户之前拒绝过，显示说明后再请求
                    android.util.Log.d("FragOnboardNotification", "⚠️ Should show rationale")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // 第一次请求权限
                    android.util.Log.d("FragOnboardNotification", "🔔 Launching permission request")
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 及以下版本不需要请求通知权限
            android.util.Log.d("FragOnboardNotification", "✅ No notification permission needed (Android < 13)")
            navigateToNextPage()
        }
    }
    
    /**
     * 启动小手指动画（提示用户点击Allow按钮）
     */
    private fun startHandPointerAnimation() {
        binding.iconHandPointer.apply {
            // 淡入淡出动画
            animate()
                .alpha(0.3f)
                .setDuration(800)
                .withEndAction {
                    animate()
                        .alpha(1f)
                        .setDuration(800)
                        .withEndAction {
                            // 循环动画
                            if (_binding != null) {
                                startHandPointerAnimation()
                            }
                        }
                        .start()
                }
                .start()
        }
    }
    
    /**
     * 导航到下一页（7天试用页）
     */
    private fun navigateToNextPage() {
        android.util.Log.d("FragOnboardNotification", "🚀 Navigating to next page (7-day trial)")
        
        // 通知Activity导航到下一个页面
        val activity = activity as? com.quran.quranaudio.online.quran_module.activities.ActivityOnboarding
        activity?.navigateToNextPage()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        
        // 停止动画
        binding.iconHandPointer.animate().cancel()
        
        _binding = null
    }
}

