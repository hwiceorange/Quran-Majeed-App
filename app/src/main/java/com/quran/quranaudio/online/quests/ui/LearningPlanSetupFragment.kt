package com.quran.quranaudio.online.quests.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.material.slider.Slider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.FragmentLearningPlanSetupBinding
import com.quran.quranaudio.online.quests.data.ReadingGoalUnit
import com.quran.quranaudio.online.quests.data.UserQuestConfig
import com.quran.quranaudio.online.quests.repository.QuestRepository
import com.quran.quranaudio.online.quests.utils.QuranDataHelper
import com.quran.quranaudio.online.quests.viewmodel.LearningPlanSetupViewModel
import com.quran.quranaudio.online.Utils.GoogleAuthManager
import java.time.LocalDate

/**
 * Fragment for Learning Plan Setup.
 * 
 * Features:
 * - User selects daily reading pages (1-50)
 * - User selects recitation duration (15/30/45/60 minutes)
 * - User enables/disables Tasbih reminder
 * - Real-time challenge duration calculation
 * - Deferred authentication (login required before save)
 * - Save configuration to Firestore
 */
class LearningPlanSetupFragment : Fragment() {

    companion object {
        private const val TAG = "LearningPlanSetupFrag"
    }

    private var _binding: FragmentLearningPlanSetupBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LearningPlanSetupViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var googleAuthManager: GoogleAuthManager
    private lateinit var questRepository: QuestRepository

    // ActivityResultLauncher for Google Sign-In
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    // Recitation minutes options
    private val recitationValues = arrayOf(15, 30, 45, 60)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            Log.d(TAG, "📱 onCreate called")
            
            // Initialize Firebase components directly (no Dagger injection)
            auth = FirebaseAuth.getInstance()
            
            // Use application context instead of requireContext() for safer initialization
            val appContext = requireActivity().applicationContext
            googleAuthManager = GoogleAuthManager(appContext)
            
            // Create QuestRepository directly
            val firestore = FirebaseFirestore.getInstance()
            questRepository = QuestRepository(firestore)

            // Initialize Sign-In Launcher with error handling
            signInLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                try {
                    if (result.resultCode == Activity.RESULT_OK) {
                        handleSignInResult(result.data)
                    } else {
                        Log.w(TAG, "Sign-in canceled or failed")
                        if (isAdded && context != null) {
                            Toast.makeText(requireContext(), "Sign-in canceled", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error handling sign-in result", e)
                }
            }
            
            Log.d(TAG, "✅ onCreate completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fatal error in onCreate", e)
            // Attempt to navigate back gracefully
            activity?.runOnUiThread {
                try {
                    if (isAdded && activity != null) {
                        findNavController().popBackStack()
                        Toast.makeText(
                            requireContext(), 
                            "Failed to load setup page. Please try again.", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (navError: Exception) {
                    Log.e(TAG, "❌ Failed to navigate back", navError)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearningPlanSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            Log.d(TAG, "📱 onViewCreated called")
            
            // Setup toolbar back button with safe navigation
            binding.toolbar.setNavigationOnClickListener {
                try {
                    if (isAdded && activity != null) {
                        findNavController().popBackStack()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to navigate back", e)
                    activity?.onBackPressed()
                }
            }

            setupUI()
            observeViewModel()
            
            Log.d(TAG, "✅ onViewCreated completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fatal error in onViewCreated", e)
            // Show error dialog and navigate back
            activity?.runOnUiThread {
                if (isAdded && context != null) {
                    try {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Error")
                            .setMessage("Failed to initialize setup page: ${e.message}\n\nPlease try again.")
                            .setPositiveButton("OK") { _, _ ->
                                try {
                                    findNavController().popBackStack()
                                } catch (navError: Exception) {
                                    activity?.onBackPressed()
                                }
                            }
                            .setCancelable(false)
                            .show()
                    } catch (dialogError: Exception) {
                        Log.e(TAG, "❌ Failed to show error dialog", dialogError)
                        // Last resort: just go back
                        try {
                            findNavController().popBackStack()
                        } catch (navError: Exception) {
                            activity?.onBackPressed()
                        }
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 隐藏底部导航栏，让用户专注于配置流程
        hideBottomNavigation()
    }
    
    override fun onPause() {
        super.onPause()
        // 恢复底部导航栏
        showBottomNavigation()
    }
    
    /**
     * 隐藏底部导航栏
     * 注意：MainActivity 中的底部导航栏 ID 是 nav_view
     */
    private fun hideBottomNavigation() {
        try {
            activity?.findViewById<View>(R.id.nav_view)?.let { bottomNav ->
                bottomNav.visibility = View.GONE
                Log.d(TAG, "✅ Bottom navigation hidden successfully")
            } ?: run {
                Log.w(TAG, "⚠️ Bottom navigation view not found (R.id.nav_view)")
                // 尝试打印 Activity 的类名以便调试
                Log.d(TAG, "Current activity: ${activity?.javaClass?.simpleName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to hide bottom navigation", e)
        }
    }
    
    /**
     * 显示底部导航栏
     * 注意：MainActivity 中的底部导航栏 ID 是 nav_view
     */
    private fun showBottomNavigation() {
        try {
            activity?.findViewById<View>(R.id.nav_view)?.let { bottomNav ->
                bottomNav.visibility = View.VISIBLE
                Log.d(TAG, "✅ Bottom navigation shown successfully")
            } ?: run {
                Log.w(TAG, "⚠️ Bottom navigation view not found when trying to show")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to show bottom navigation", e)
        }
    }

    private fun setupUI() {
        try {
            Log.d(TAG, "🔧 Setting up UI...")
            
            // Check if fragment is still attached
            if (!isAdded || context == null) {
                Log.w(TAG, "⚠️ Fragment detached, skipping UI setup")
                return
            }
            
            // Initialize ViewModel with proper error handling
            try {
                viewModel = ViewModelProvider(
                    this,
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return LearningPlanSetupViewModel(questRepository) as T
                        }
                    }
                ).get(LearningPlanSetupViewModel::class.java)
                
                Log.d(TAG, "✅ ViewModel initialized successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize ViewModel", e)
                throw e
            }
        
        // ===== Setup Reading Unit Spinner =====
        val unitAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            arrayOf(
                getString(R.string.unit_pages),
                getString(R.string.unit_verses),
                getString(R.string.unit_juz)
            )
        )
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spReadingUnit.adapter = unitAdapter
        binding.spReadingUnit.setSelection(1) // Default: Verses (10 verses - 最小可管理单位)
        
        // Unit Spinner change listener
        binding.spReadingUnit.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedUnit = when (position) {
                    0 -> ReadingGoalUnit.PAGES
                    1 -> ReadingGoalUnit.VERSES
                    2 -> ReadingGoalUnit.JUZ
                    else -> ReadingGoalUnit.PAGES
                }
                viewModel.setReadingUnit(selectedUnit)
                Log.d(TAG, "User selected unit: ${selectedUnit.displayName}")
                
                // 立即重新计算挑战天数
                updateChallengeDays()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        // Setup Recitation Minutes Spinner
        val recitationOptions = arrayOf(
            getString(R.string.minutes_15),
            getString(R.string.minutes_30),
            getString(R.string.minutes_45),
            getString(R.string.minutes_60)
        )
        val recitationAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            recitationOptions
        )
        recitationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRecitationMinutes.adapter = recitationAdapter
        binding.spRecitationMinutes.setSelection(0) // Default: 15 minutes

        // Setup Reading Goal Slider
        binding.sbReadingGoal.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                binding.tvReadingPagesValue.text = value.toInt().toString()
                updateChallengeDays()
            }
        }

        // Setup Recitation Switch
        binding.swRecitationEnabled.setOnCheckedChangeListener { _, isChecked ->
            binding.spRecitationMinutes.isEnabled = isChecked
            updateChallengeDays()
        }

        // Setup Recitation Minutes Spinner change listener
        binding.spRecitationMinutes.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateChallengeDays()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // 🔥 Setup Tasbih Reminder Switch and Count RadioGroup
        // 初始状态：RadioGroup根据Tasbih开关状态启用/禁用
        binding.rgTasbihCount.isEnabled = binding.swTasbihReminder.isChecked
        updateTasbihCountRadioButtons(binding.swTasbihReminder.isChecked)
        
        binding.swTasbihReminder.setOnCheckedChangeListener { _, isChecked ->
            // 当开关变化时，启用或禁用RadioGroup
            binding.rgTasbihCount.isEnabled = isChecked
            updateTasbihCountRadioButtons(isChecked)
            Log.d(TAG, "Tasbih Reminder: ${if (isChecked) "Enabled" else "Disabled"}")
        }

        // Setup Save Button
        binding.btnSaveChallenge.setOnClickListener {
            onSaveButtonClicked()
        }

        // Initial challenge days calculation
        updateChallengeDays()
        
        Log.d(TAG, "✅ UI setup completed successfully")
        
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fatal error in setupUI", e)
            throw e // Re-throw to be caught by onViewCreated
        }
    }

    private fun observeViewModel() {
        try {
            Log.d(TAG, "🔧 Starting to observe ViewModel...")
            
            // 检查 Fragment 状态
            if (!isAdded || context == null) {
                Log.w(TAG, "⚠️ Fragment not attached, skipping ViewModel observation")
                return
            }
            
            // ===== 观察 Slider 范围变化 =====
            viewModel.sliderRange.observe(viewLifecycleOwner) { range ->
                if (!isAdded || context == null) return@observe
                
                Log.d(TAG, "Slider range updated: ${range.min}-${range.max}, default: ${range.defaultValue}")
                
                // 更新 Slider 范围
                binding.sbReadingGoal.valueFrom = range.min.toFloat()
                binding.sbReadingGoal.valueTo = range.max.toFloat()
                binding.sbReadingGoal.value = range.defaultValue.toFloat()
                
                // 更新范围标签
                binding.tvSliderMin.text = range.min.toString()
                binding.tvSliderMax.text = range.max.toString()
                
                // 更新显示
                binding.tvReadingPagesValue.text = range.defaultValue.toString()
            }
            
            // ===== 观察用户配置并回显到 UI =====
            // 使用 viewLifecycleOwner 而不是 lifecycleScope 更安全
            // 🔧 关键修复：添加空安全检查防止崩溃
            try {
                // 🚨 关键：确保 view 已创建且 viewLifecycleOwner 可用
                if (!isAdded || view == null) {
                    Log.w(TAG, "⚠️ View not ready, skipping config observation")
                    return
                }
                
                // 🔥 核心修复：获取 viewLifecycleOwner 并检查其状态
                val lifecycleOwner = try {
                    viewLifecycleOwner
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "❌ viewLifecycleOwner not available yet", e)
                    return
                }
                
                // 检查生命周期状态
                if (!lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.INITIALIZED)) {
                    Log.w(TAG, "⚠️ Lifecycle not initialized")
                    return
                }
                
                // 🔥 最关键修复：安全访问 lifecycleScope
                val scope = try {
                    lifecycleOwner.lifecycleScope
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Cannot access lifecycleScope", e)
                    null
                }
                
                if (scope == null) {
                    Log.w(TAG, "⚠️ lifecycleScope is null, aborting observation")
                    return
                }
                
                try {
                    scope.launch {
                        try {
                            viewModel.userConfig.collectLatest { config ->
                            if (!isAdded || context == null) {
                                Log.w(TAG, "Fragment detached, skipping config update")
                                return@collectLatest
                            }
                            
                            config?.let {
                                Log.d(TAG, "加载已保存的配置: 单位=${it.readingGoalUnit}, 目标=${it.dailyReadingGoal}, 朗诵=${it.recitationMinutes}分钟, Tasbih=${it.tasbihReminderEnabled}")
                                
                                // 1. 回显 Reading Unit (Spinner)
                                val unit = it.getReadingUnitEnum()
                                val unitPosition = when (unit) {
                                    ReadingGoalUnit.PAGES -> 0
                                    ReadingGoalUnit.VERSES -> 1
                                    ReadingGoalUnit.JUZ -> 2
                                }
                                binding.spReadingUnit.setSelection(unitPosition)
                                
                                // 2. 通知 ViewModel 更新单位（触发范围更新）
                                viewModel.setReadingUnit(unit)
                                
                                // 3. 回显 Daily Reading Goal (Slider)
                                binding.sbReadingGoal.value = it.dailyReadingGoal.toFloat()
                                binding.tvReadingPagesValue.text = it.dailyReadingGoal.toString()
                                
                                // 4. 回显 Recitation Enabled (Switch)
                                binding.swRecitationEnabled.isChecked = it.recitationEnabled
                                binding.spRecitationMinutes.isEnabled = it.recitationEnabled
                                
                                // 5. 回显 Recitation Minutes (Spinner)
                                val recitationIndex = recitationValues.indexOf(it.recitationMinutes)
                                if (recitationIndex >= 0) {
                                    binding.spRecitationMinutes.setSelection(recitationIndex)
                                }
                                
                                // 6. 回显 Dua Reminder (Switch)
                                binding.swDuaReminder.isChecked = it.duaReminderEnabled ?: false
                                
                                // 7. 回显 Tasbih Reminder (Switch)
                                binding.swTasbihReminder.isChecked = it.tasbihReminderEnabled
                                
                                // 8. 🔥 回显 Tasbih Count (RadioGroup)
                                when (it.tasbihCount) {
                                    33 -> binding.rbTasbih33.isChecked = true
                                    50 -> binding.rbTasbih50.isChecked = true
                                    100 -> binding.rbTasbih100.isChecked = true
                                    else -> binding.rbTasbih33.isChecked = true // Default to 33
                                }
                                // 更新RadioButton的启用状态
                                binding.rgTasbihCount.isEnabled = it.tasbihReminderEnabled
                                updateTasbihCountRadioButtons(it.tasbihReminderEnabled)
                                
                                // 9. 计算并显示挑战天数
                                viewModel.calculateChallengeDays(
                                    unit,
                                    it.dailyReadingGoal,
                                    it.recitationMinutes,
                                    it.recitationEnabled
                                )
                                
                                Log.d(TAG, "配置回显完成")
                            } ?: run {
                                Log.d(TAG, "未找到已保存的配置，使用默认值")
                            }
                        }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error in collectLatest", e)
                            if (isAdded && context != null) {
                                android.widget.Toast.makeText(requireContext(), "Failed to load configuration", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: IllegalStateException) {
                    // 协程启动失败（lifecycleScope 不可用）
                    Log.e(TAG, "❌ Failed to launch coroutine - lifecycleScope not available", e)
                } catch (e: Exception) {
                    // 任何其他协程启动异常
                    Log.e(TAG, "❌ Failed to launch coroutine - unexpected error", e)
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "❌ viewLifecycleOwner not available", e)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting up config observation", e)
            }
        
        // Observe challenge days
        viewModel.challengeDays.observe(viewLifecycleOwner) { days ->
            binding.tvChallengeDaysDisplay.text = getString(R.string.challenge_days_format, days)
            Log.d(TAG, "Challenge days updated: $days")
        }

        // Observe save status
        viewModel.saveStatus.observe(viewLifecycleOwner) { status ->
            Log.d(TAG, "Save status changed: $status")
            when (status) {
                is LearningPlanSetupViewModel.SaveStatus.Success -> {
                    Log.d(TAG, "收到 Success 状态，准备显示 Toast 并返回")
                    Toast.makeText(requireContext(), "Learning plan saved successfully! ✅", Toast.LENGTH_SHORT).show()
                    viewModel.resetSaveStatus()
                    
                    // 🎯 Show interstitial ad before returning to home
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            if (isAdded && view != null && activity != null) {
                                Log.d(TAG, "🎯 Attempting to show interstitial ad before navigation")
                                
                                // Show ad with callback (it will handle subscription check internally)
                                val adShown = com.quranaudio.common.ad.InterstitialAdManager.getInstance().showAdIfAvailable(requireActivity()) {
                                    // This callback is invoked when user dismisses the ad or ad fails to show
                                    Log.d(TAG, "✅ Ad closed by user, navigating back to home")
                                    navigateBackToHome()
                                }
                                
                                if (!adShown) {
                                    Log.d(TAG, "⚠️ No ad shown (subscribed or unavailable), navigating immediately")
                                    navigateBackToHome()
                                } else {
                                    Log.d(TAG, "✅ Interstitial ad shown, waiting for user to close it")
                                }
                            } else {
                                Log.w(TAG, "Fragment 已分离，无法导航")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "广告展示或导航失败", e)
                            navigateBackToHome()
                        }
                    }, 500) // 500ms 延迟，确保 Toast 显示
                }
                is LearningPlanSetupViewModel.SaveStatus.Error -> {
                    Log.e(TAG, "保存失败: ${status.message}")
                    Toast.makeText(requireContext(), "Error: ${status.message}", Toast.LENGTH_LONG).show()
                    viewModel.resetSaveStatus()
                }
                null -> {
                    Log.d(TAG, "Save status is null (初始状态或已重置)")
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "Loading state changed: $isLoading")
            binding.btnSaveChallenge.isEnabled = !isLoading
            binding.btnSaveChallenge.text = if (isLoading) 
                getString(R.string.saving) 
            else 
                getString(R.string.save_start_challenge)
        }
        
        Log.d(TAG, "✅ ViewModel observation setup completed")
        
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fatal error in observeViewModel", e)
            // 显示友好错误提示
            if (isAdded && context != null) {
                Toast.makeText(requireContext(), "Failed to initialize page. Please try again.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Updates the challenge days based on current UI selections
     */
    private fun updateChallengeDays() {
        val selectedUnit = viewModel.selectedUnit.value ?: ReadingGoalUnit.PAGES
        val dailyGoal = binding.sbReadingGoal.value.toInt()
        val recitationMinutes = recitationValues[binding.spRecitationMinutes.selectedItemPosition]
        val recitationEnabled = binding.swRecitationEnabled.isChecked

        viewModel.calculateChallengeDays(selectedUnit, dailyGoal, recitationMinutes, recitationEnabled)
    }

    /**
     * Handles Save button click.
     * Checks authentication status before saving.
     */
    private fun onSaveButtonClicked() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            // User not logged in - show login dialog
            showLoginRequiredDialog()
        } else {
            // User logged in - proceed to save
            saveConfiguration()
        }
    }

    /**
     * Shows a dialog prompting the user to log in
     */
    private fun showLoginRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Login Required")
            .setMessage("Please login with your Google account to save your learning plan and track your progress across devices.")
            .setPositiveButton("Login with Google") { dialog, _ ->
                dialog.dismiss()
                initiateGoogleSignIn()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    /**
     * Initiates Google Sign-In flow
     */
    private fun initiateGoogleSignIn() {
        try {
            val signInIntent = googleAuthManager.getSignInIntent()
            signInLauncher.launch(signInIntent)
            Log.d(TAG, "Google Sign-In intent launched")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Google Sign-In", e)
            Toast.makeText(requireContext(), "Failed to launch sign-in: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Handles the result from Google Sign-In
     */
    private fun handleSignInResult(data: Intent?) {
        // Authenticate with Firebase using GoogleAuthManager
        googleAuthManager.handleSignInResult(data, object : GoogleAuthManager.AuthCallback {
            override fun onSuccess(user: com.google.firebase.auth.FirebaseUser?) {
                Log.d(TAG, "Firebase authentication successful: ${user?.email}")
                Toast.makeText(requireContext(), "Login successful! ✅", Toast.LENGTH_SHORT).show()
                // Proceed to save configuration
                saveConfiguration()
            }

            override fun onFailure(error: String?) {
                Log.e(TAG, "Firebase authentication failed: $error")
                Toast.makeText(requireContext(), "Authentication failed: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * Collects UI values and saves configuration to Firestore
     */
    private fun saveConfiguration() {
        // Collect UI values
        val selectedUnit = viewModel.selectedUnit.value ?: ReadingGoalUnit.PAGES
        val dailyGoal = binding.sbReadingGoal.value.toInt()
        val recitationEnabled = binding.swRecitationEnabled.isChecked
        val recitationMinutes = recitationValues[binding.spRecitationMinutes.selectedItemPosition]
        val duaReminderEnabled = binding.swDuaReminder.isChecked
        val tasbihReminderEnabled = binding.swTasbihReminder.isChecked
        
        // 🔥 Get selected Tasbih count from RadioGroup
        val tasbihCount = when (binding.rgTasbihCount.checkedRadioButtonId) {
            R.id.rb_tasbih_33 -> 33
            R.id.rb_tasbih_50 -> 50
            R.id.rb_tasbih_100 -> 100
            else -> 33 // Default to 33
        }
        
        val challengeDays = viewModel.challengeDays.value ?: QuranDataHelper.TOTAL_PAGES
        
        // Construct UserQuestConfig (包含新字段)
        val config = UserQuestConfig(
            dailyReadingPages = dailyGoal, // 向后兼容
            readingGoalUnit = selectedUnit.name, // 新字段
            dailyReadingGoal = dailyGoal, // 新字段
            recitationEnabled = recitationEnabled,
            recitationMinutes = recitationMinutes,
            duaReminderEnabled = duaReminderEnabled,
            tasbihReminderEnabled = tasbihReminderEnabled,
            tasbihCount = tasbihCount, // 🔥 使用用户选择的值
            totalChallengeDays = challengeDays,
            startDate = LocalDate.now().toString(),
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        
        Log.d(TAG, "Saving configuration: unit=${selectedUnit.displayName}, goal=$dailyGoal, tasbihCount=$tasbihCount, days=$challengeDays")
        
        // Save via ViewModel
        viewModel.saveUserQuest(config)
    }
    
    /**
     * 🔥 辅助函数：根据Tasbih开关状态启用或禁用RadioButton
     */
    private fun updateTasbihCountRadioButtons(enabled: Boolean) {
        val alpha = if (enabled) 1.0f else 0.4f
        binding.rbTasbih33.isEnabled = enabled
        binding.rbTasbih33.alpha = alpha
        binding.rbTasbih50.isEnabled = enabled
        binding.rbTasbih50.alpha = alpha
        binding.rbTasbih100.isEnabled = enabled
        binding.rbTasbih100.alpha = alpha
    }
    
    /**
     * 🔥 Helper function: Navigate back to home screen
     */
    private fun navigateBackToHome() {
        try {
            if (isAdded && view != null) {
                findNavController().popBackStack()
                Log.d(TAG, "✅ Successfully navigated back to home")
            } else {
                Log.w(TAG, "⚠️ Fragment detached, cannot navigate")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Navigation failed", e)
            Toast.makeText(requireContext(), "Navigation failed, please go back manually", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

