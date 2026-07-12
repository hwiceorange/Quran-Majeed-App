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

        if (isGranted) {
            showRemindersEnabledConfirmation()
        }

        // 无论用户是否授权，都导航到下一页
        navigateToNextPage()
    }

    /**
     * 授权成功后的确认反馈。
     * 内容是准确的：五番祈祷通知已默认开启（PreferencesHelper.ensureDefaultPrayerNotificationTypes），
     * 进入主页获取位置后即完成当天闹钟调度。
     */
    private fun showRemindersEnabledConfirmation() {
        val ctx = context ?: return
        android.widget.Toast.makeText(
            ctx,
            com.quran.quranaudio.online.R.string.onboarding_prayer_reminders_enabled,
            android.widget.Toast.LENGTH_SHORT
        ).show()
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
        localizeTestimonialAvatar()
    }

    /**
     * 证言头像：名字首字母 + 彩色圆圈(Google Play 风格)。
     * 证言姓名/内容已按 locale 用真实 Google Play 评论本地化，头像首字母从姓名动态取，
     * 颜色按姓名哈希从调色板选取，视觉与 Google Play 评论一致、真实可信、无需照片。
     */
    private fun localizeTestimonialAvatar() {
        try {
            val avatar = binding.imgUserAvatar
            val name = getString(com.quran.quranaudio.online.R.string.onboard_notification_review_name).trim()
            if (name.isEmpty()) return

            // 首字母(兼容阿拉伯/孟加拉等非拉丁文字，取第一个字符)
            avatar.text = name.substring(0, 1).uppercase()

            // 颜色：按姓名哈希从 Google Play 风格调色板选取，保证同名同色、不同名多样
            val palette = intArrayOf(
                0xFF5E97F6.toInt(), 0xFF9CCC65.toInt(), 0xFFF06292.toInt(),
                0xFF7E57C2.toInt(), 0xFF4DB6AC.toInt(), 0xFFFF8A65.toInt(),
                0xFFA1887F.toInt(), 0xFF4FC3F7.toInt()
            )
            val color = palette[Math.abs(name.hashCode()) % palette.size]
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(color)
            }
            avatar.background = bg
        } catch (e: Exception) {
            android.util.Log.w("FragOnboardNotification", "localizeTestimonialAvatar failed", e)
        }
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

            // Android 12 及以下没有系统通知权限门控，用户明确拒绝时必须在业务层
            // 关闭祈祷通知，否则"说了不要却还收到通知"。
            // Android 13+ 交给系统权限控制：用户之后在系统设置里授权，提醒即自动生效。
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                context?.let {
                    com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper(it.applicationContext)
                        .disableAllPrayerNotificationTypes()
                }
            }

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
                    showRemindersEnabledConfirmation()
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
            // Android 12 及以下版本不需要请求通知权限，提醒默认即生效
            android.util.Log.d("FragOnboardNotification", "✅ No notification permission needed (Android < 13)")
            showRemindersEnabledConfirmation()
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

