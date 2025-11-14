package com.quran.quranaudio.online.prayertimes.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.BottomSheetLogPrayerBinding
import com.quran.quranaudio.online.prayertimes.models.PrayerLog
import java.text.SimpleDateFormat
import java.util.*

/**
 * 祷告记录 Bottom Sheet
 * 用于快速记录用户的祷告完成情况
 */
class PrayerLogBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetLogPrayerBinding? = null
    private val binding get() = _binding!!

    private lateinit var prayerName: String
    private var selectedStatus: PrayerLog.PrayerStatus = PrayerLog.PrayerStatus.ADA
    private var performedAtTimestamp: Timestamp = Timestamp.now()
    private var loggedAtTimestamp: Timestamp = Timestamp.now()
    private var existingLogId: String? = null // 编辑模式时使用
    private var isEditMode: Boolean = false
    private var selectedTags: MutableList<String> = mutableListOf() // 选中的标签
    private var originalDate: String = "" // 祷告原始日期
    private var originalStatus: PrayerLog.PrayerStatus? = null // 编辑模式：保存原始状态用于状态转换

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    
    // Callback for Qada counter updates (var automatically generates setter for Java)
    var onPrayerLoggedListener: OnPrayerLoggedListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prayerName = arguments?.getString(ARG_PRAYER_NAME) ?: "Unknown"
        existingLogId = arguments?.getString(ARG_EXISTING_LOG_ID)
        isEditMode = existingLogId != null
        
        // 如果有初始状态（用于 Missed -> Qada 转换）
        arguments?.getString(ARG_INITIAL_STATUS)?.let {
            try {
                selectedStatus = PrayerLog.PrayerStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                selectedStatus = PrayerLog.PrayerStatus.ADA
            }
        }
        
        // 如果有原始日期（用于 Qada 弥补场景）
        arguments?.getString(ARG_ORIGINAL_DATE)?.let {
            originalDate = it
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetLogPrayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        // 设置祷告名称
        binding.tvPrayerName.text = prayerName

        if (isEditMode && existingLogId != null) {
            // 编辑模式：加载现有数据
            loadExistingLog()
        } else {
            // 新建模式：使用预设状态
            // - Pending -> Ada' (准时完成，推荐)
            // - Missed -> Qada' (鼓励弥补)
            // 注意：selectedStatus 已经在 onCreate 中通过 ARG_INITIAL_STATUS 设置
            selectStatus(selectedStatus)
            performedAtTimestamp = Timestamp.now()
            loggedAtTimestamp = Timestamp.now()
            updatePerformedAtDisplay()
            updateRecordedAtDisplay()
        }
    }
    
    private fun loadExistingLog() {
        if (existingLogId == null) return
        
        firestore.collection(PrayerLog.COLLECTION_NAME)
            .document(existingLogId!!)
            .get()
            .addOnSuccessListener { document ->
                // ✅ 安全检查：确保 binding 不为 null（View 可能已被销毁）
                if (_binding == null) {
                    android.util.Log.w("PrayerLog", "⚠️ Binding is null, view destroyed before data loaded")
                    return@addOnSuccessListener
                }
                
                val log = document.toObject(PrayerLog::class.java)
                if (log != null) {
                    // 保存原始状态（用于状态转换判断）
                    originalStatus = log.status
                    originalDate = log.date
                    
                    // 填充现有数据
                    selectedStatus = log.status
                    selectStatus(log.status)
                    
                    if (log.performedAt != null) {
                        performedAtTimestamp = log.performedAt
                    }
                    if (log.loggedAt != null) {
                        loggedAtTimestamp = log.loggedAt
                    }
                    
                    binding.etNotes.setText(log.notes)
                    
                    // 加载标签
                    loadExistingTags(log.tags)
                    
                    updatePerformedAtDisplay()
                    updateRecordedAtDisplay()
                    
                    android.util.Log.d("PrayerLog", "✅ Loaded existing log: ${log.id}, status: ${log.status}, date: ${log.date}")
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("PrayerLog", "❌ Error loading existing log", e)
                // ✅ 安全检查
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Failed to load prayer log", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupListeners() {
        // Status 按钮点击事件
        binding.btnStatusAda.setOnClickListener {
            selectStatus(PrayerLog.PrayerStatus.ADA)
        }

        binding.btnStatusQada.setOnClickListener {
            selectStatus(PrayerLog.PrayerStatus.QADA)
        }

        binding.btnStatusMissed.setOnClickListener {
            selectStatus(PrayerLog.PrayerStatus.MISSED)
        }

        // Prayed At 时间选择
        binding.containerPrayedAt.setOnClickListener {
            showTimePickerDialog()
        }

        // 常用标签点击事件
        setupChipListeners()

        // Cancel 按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Save 按钮
        binding.btnSave.setOnClickListener {
            savePrayerLog()
        }
    }

    private fun selectStatus(status: PrayerLog.PrayerStatus) {
        selectedStatus = status

        // 重置所有按钮状态
        binding.btnStatusAda.isSelected = (status == PrayerLog.PrayerStatus.ADA)
        binding.btnStatusQada.isSelected = (status == PrayerLog.PrayerStatus.QADA)
        binding.btnStatusMissed.isSelected = (status == PrayerLog.PrayerStatus.MISSED)
        
        // Missed 状态：隐藏/禁用 Prayed At 时间选择
        // 因为错过意味着没有实际祷告时间
        if (status == PrayerLog.PrayerStatus.MISSED) {
            binding.containerPrayedAt.visibility = android.view.View.GONE
            binding.iconTime.visibility = android.view.View.GONE
            binding.tvPrayedAtLabel.visibility = android.view.View.GONE
        } else {
            binding.containerPrayedAt.visibility = android.view.View.VISIBLE
            binding.iconTime.visibility = android.view.View.VISIBLE
            binding.tvPrayedAtLabel.visibility = android.view.View.VISIBLE
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = performedAtTimestamp.toDate().time

        // 使用自定义主题（绿色 #429971，圆角）
        TimePickerDialog(
            requireContext(),
            R.style.PrayerTimePickerTheme,
            { _, hourOfDay, minute ->
                // 更新时间
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                performedAtTimestamp = Timestamp(calendar.time)
                updatePerformedAtDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // 使用 12 小时制
        ).show()
    }

    private fun updatePerformedAtDisplay() {
        val dateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        binding.tvPrayedAtTime.text = dateFormat.format(performedAtTimestamp.toDate())
    }

    private fun updateRecordedAtDisplay() {
        val userTimeZone = TimeZone.getDefault()
        val dateFormat = SimpleDateFormat("h:mm a (zzz)", Locale.getDefault())
        dateFormat.timeZone = userTimeZone
        binding.tvRecordedAtTime.text = dateFormat.format(loggedAtTimestamp.toDate())
    }

    private fun setupChipListeners() {
        android.util.Log.d("PrayerLog", "📍 setupChipListeners() called")
        
        // 🔧 关键修复：禁用 ChipGroup 的自动选择管理，改为手动管理
        // 这样可以避免全局样式（singleSelection=true）的干扰
        
        // 🔧 修复：确保 ChipGroup 不拦截点击事件
        binding.chipGroupTags.isFocusable = false
        binding.chipGroupTags.isClickable = false
        
        val chips = listOf(
            binding.chipMosque to "At Mosque",
            binding.chipTraveling to "Traveling",
            binding.chipFamily to "With Family"
        )

        chips.forEach { (chip, tag) ->
            android.util.Log.d("PrayerLog", "🔧 Setting up listener for chip: $tag")
            android.util.Log.d("PrayerLog", "  clickable=${chip.isClickable}, focusable=${chip.isFocusable}, checkable=${chip.isCheckable}")
            
            // 确保 chip 可以点击
            chip.isClickable = true
            chip.isFocusable = true
            chip.isCheckable = true
            
            // 使用 setOnClickListener 而不是 setOnCheckedChangeListener
            // 因为 ChipGroup 的 singleSelection 模式会干扰 setOnCheckedChangeListener
            chip.setOnClickListener {
                android.util.Log.d("PrayerLog", "🔘 Chip clicked: $tag, current checked: ${chip.isChecked}")
                
                // 手动切换选中状态
                if (chip.isChecked) {
                    // 当前是选中状态，点击后取消选中
                    chip.isChecked = false
                    selectedTags.remove(tag)
                    android.util.Log.d("PrayerLog", "❌ Tag unselected: $tag")
                } else {
                    // 当前是未选中状态，点击后选中
                    chip.isChecked = true
                    if (!selectedTags.contains(tag)) {
                        selectedTags.add(tag)
                    }
                    android.util.Log.d("PrayerLog", "✅ Tag selected: $tag")
                }
                
                android.util.Log.d("PrayerLog", "📋 Selected tags: $selectedTags")
            }
            
            // 🔧 添加触摸事件监听用于调试
            chip.setOnTouchListener { v, event ->
                android.util.Log.d("PrayerLog", "👆 Chip touched: $tag, action=${event.action}")
                false // 返回 false 让事件继续传递
            }
        }
        
        android.util.Log.d("PrayerLog", "✅ setupChipListeners() completed")
    }

    /**
     * 切换标签选中状态（已弃用，改用 setOnCheckedChangeListener）
     */
    @Deprecated("Use setOnCheckedChangeListener instead")
    private fun toggleTag(chip: Chip, tag: String) {
        if (selectedTags.contains(tag)) {
            // 取消选中
            selectedTags.remove(tag)
            chip.isChecked = false
        } else {
            // 选中
            selectedTags.add(tag)
            chip.isChecked = true
        }
    }
    
    /**
     * 加载现有标签（编辑模式）
     */
    private fun loadExistingTags(tags: List<String>) {
        selectedTags.clear()
        selectedTags.addAll(tags)
        
        // 更新 UI
        val chipMap = mapOf(
            "At Mosque" to binding.chipMosque,
            "Traveling" to binding.chipTraveling,
            "With Family" to binding.chipFamily
        )
        
        tags.forEach { tag ->
            chipMap[tag]?.isChecked = true
        }
    }

    private fun savePrayerLog() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please login to save prayer logs", Toast.LENGTH_SHORT).show()
            return
        }

        // 禁用按钮防止重复点击
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."

        // 获取当前日期 (YYYY-MM-DD)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val currentDate = dateFormat.format(Date())
        
        // 使用 originalDate 如果有（编辑模式或 Qada 弥补），否则使用当前日期
        val prayerDate = if (originalDate.isNotEmpty()) originalDate else currentDate
        
        // 判断是否为今天的祷告
        val isToday = (prayerDate == currentDate)

        // 创建祷告记录
        val prayerLog = PrayerLog.create(
            userId = currentUser.uid,
            prayerName = prayerName,
            status = selectedStatus,
            performedAt = performedAtTimestamp,
            notes = binding.etNotes.text?.toString()?.trim() ?: "",
            date = prayerDate,
            isToday = isToday,
            tags = selectedTags.toList()
        )

        // 保存到 Firestore
        val collectionRef = firestore.collection(PrayerLog.COLLECTION_NAME)
        
        if (isEditMode && existingLogId != null) {
            // 计算状态转换对 Qada 计数器的影响
            val qadaDelta = calculateQadaDelta(originalStatus, selectedStatus)
            
            // 更新现有文档
            collectionRef.document(existingLogId!!)
                .set(prayerLog.copy(id = existingLogId!!))
                .addOnSuccessListener {
                    android.util.Log.d("PrayerLog", "✅ Prayer log updated: $existingLogId")
                    android.util.Log.d("PrayerLog", "  状态转换: $originalStatus → $selectedStatus, Qada delta: $qadaDelta")
                    
                    Toast.makeText(
                        requireContext(),
                        "✅ $prayerName prayer updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 通知 Qada 计数器更新
                    if (qadaDelta != 0) {
                        onPrayerLoggedListener?.onQadaCountChanged(qadaDelta)
                    }
                    
                    dismiss()
                    
                    // 通知刷新数据（优先使用 onPrayerLoggedListener，fallback 到 parentFragment）
                    if (onPrayerLoggedListener != null) {
                        onPrayerLoggedListener?.onPrayerLogged(prayerName)
                    } else {
                        (parentFragment as? OnPrayerLoggedListener)?.onPrayerLogged(prayerName)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PrayerLog", "❌ Failed to update prayer log", e)
                    
                    Toast.makeText(
                        requireContext(),
                        "Failed to update: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 恢复按钮状态
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                }
        } else {
            // 新建模式：如果是 Qada 弥补，计数器 -1
            val qadaDelta = if (selectedStatus == PrayerLog.PrayerStatus.QADA) -1 else 0
            
            // 创建新文档
            collectionRef.add(prayerLog)
                .addOnSuccessListener { documentReference ->
                    android.util.Log.d("PrayerLog", "✅ Prayer log saved: ${documentReference.id}")
                    android.util.Log.d("PrayerLog", "  Status: $selectedStatus, Qada delta: $qadaDelta")
                    
                    Toast.makeText(
                        requireContext(),
                        "✅ $prayerName prayer logged successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 通知 Qada 计数器更新
                    if (qadaDelta != 0) {
                        onPrayerLoggedListener?.onQadaCountChanged(qadaDelta)
                    }
                    
                    dismiss()
                    
                    // 通知刷新数据（优先使用 onPrayerLoggedListener，fallback 到 parentFragment）
                    if (onPrayerLoggedListener != null) {
                        onPrayerLoggedListener?.onPrayerLogged(prayerName)
                    } else {
                        (parentFragment as? OnPrayerLoggedListener)?.onPrayerLogged(prayerName)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PrayerLog", "❌ Failed to save prayer log", e)
                    
                    Toast.makeText(
                        requireContext(),
                        "Failed to save: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 恢复按钮状态
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = "Save"
                }
        }
    }

    /**
     * 计算状态转换对 Qada 计数器的影响
     * 
     * 规则:
     * - Qada' → Missed: +1 (已弥补变为错过)
     * - Missed → Ada'/Qada': -1 (错过变为完成)
     * - Ada' → Missed: +1 (准时变为错过)
     * - Ada' → Qada': 0 (状态转换，但都是完成状态)
     * - Qada' → Ada': 0 (状态转换，但都是完成状态)
     */
    private fun calculateQadaDelta(oldStatus: PrayerLog.PrayerStatus?, newStatus: PrayerLog.PrayerStatus): Int {
        if (oldStatus == null) return 0
        
        return when {
            // Qada' → Missed: 增加待弥补
            oldStatus == PrayerLog.PrayerStatus.QADA && newStatus == PrayerLog.PrayerStatus.MISSED -> 1
            
            // Missed → Ada'/Qada': 减少待弥补
            oldStatus == PrayerLog.PrayerStatus.MISSED && 
            (newStatus == PrayerLog.PrayerStatus.ADA || newStatus == PrayerLog.PrayerStatus.QADA) -> -1
            
            // Ada' → Missed: 增加待弥补
            oldStatus == PrayerLog.PrayerStatus.ADA && newStatus == PrayerLog.PrayerStatus.MISSED -> 1
            
            // 其他转换: 不影响计数器
            else -> 0
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 回调接口 - 通知父 Fragment 祷告已记录
     */
    interface OnPrayerLoggedListener {
        fun onPrayerLogged(prayerName: String)
        fun onQadaCountChanged(delta: Int) // 新增：Qada 计数器变化
    }

    companion object {
        private const val ARG_PRAYER_NAME = "prayer_name"
        private const val ARG_EXISTING_LOG_ID = "existing_log_id"
        private const val ARG_INITIAL_STATUS = "initial_status"
        private const val ARG_ORIGINAL_DATE = "original_date"

        /**
         * 创建 Bottom Sheet 实例（新建模式）
         * @param prayerName 祷告名称（Fajr, Dhuhr, Asr, Maghrib, Isha）
         * @param initialStatus 初始状态（可选，用于 Missed -> Qada 转换）
         * @param originalDate 原始日期（可选，用于 Qada 弥补场景）
         */
        fun newInstance(
            prayerName: String, 
            initialStatus: PrayerLog.PrayerStatus? = null,
            originalDate: String? = null
        ): PrayerLogBottomSheet {
            return PrayerLogBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRAYER_NAME, prayerName)
                    initialStatus?.let { putString(ARG_INITIAL_STATUS, it.name) }
                    originalDate?.let { putString(ARG_ORIGINAL_DATE, it) }
                }
            }
        }
        
        /**
         * 创建 Bottom Sheet 实例（编辑模式）
         * @param prayerName 祷告名称
         * @param existingLogId 现有记录的 ID
         */
        fun newInstanceForEdit(prayerName: String, existingLogId: String): PrayerLogBottomSheet {
            return PrayerLogBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_PRAYER_NAME, prayerName)
                    putString(ARG_EXISTING_LOG_ID, existingLogId)
                }
            }
        }
    }
}

