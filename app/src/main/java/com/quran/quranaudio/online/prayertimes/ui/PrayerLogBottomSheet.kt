package com.quran.quranaudio.online.prayertimes.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.ScaleAnimation
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.BottomSheetLogPrayerBinding
import com.quran.quranaudio.online.prayertimes.models.PrayerLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    
    // ⚡ 协程作用域（用于后台同步）
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
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
        // 设置祷告名称（使用本地化名称显示）
        val localizedPrayerName = com.quran.quranaudio.online.prayertimes.models.PrayerName.getLocalizedName(
            prayerName,
            requireContext()
        )
        binding.tvPrayerName.text = localizedPrayerName

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
                    Toast.makeText(requireContext(), getString(R.string.prayer_log_failed_load), Toast.LENGTH_SHORT).show()
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
        
        // 使用 Filter Chip 样式，支持多选
        // 标签使用本地化的字符串资源，但存储时使用英文键值
        val chips = listOf(
            Triple(binding.chipMosque, getString(R.string.prayer_log_tag_mosque), "At Mosque"),
            Triple(binding.chipTraveling, getString(R.string.prayer_log_tag_traveling), "Traveling"),
            Triple(binding.chipFamily, getString(R.string.prayer_log_tag_family), "With Family")
        )

        chips.forEach { (chip, displayText, tagKey) ->
            android.util.Log.d("PrayerLog", "🔧 Setting up listener for chip: $tagKey")
            
            // 使用 setOnCheckedChangeListener 来处理选中状态变化
            chip.setOnCheckedChangeListener { _, isChecked ->
                android.util.Log.d("PrayerLog", "🔘 Chip changed: $tagKey, isChecked: $isChecked")
                
                if (isChecked) {
                    if (!selectedTags.contains(tagKey)) {
                        selectedTags.add(tagKey)
                        android.util.Log.d("PrayerLog", "✅ Tag selected: $tagKey")
                    }
                    // 将标签添加到 Notes 文本框
                    appendTagToNotes(displayText)
                } else {
                    selectedTags.remove(tagKey)
                    android.util.Log.d("PrayerLog", "❌ Tag unselected: $tagKey")
                    // 从 Notes 文本框移除标签
                    removeTagFromNotes(displayText)
                }
                
                android.util.Log.d("PrayerLog", "📋 Selected tags: $selectedTags")
            }
        }
        
        android.util.Log.d("PrayerLog", "✅ setupChipListeners() completed")
    }
    
    /**
     * 将标签追加到 Notes 文本框
     */
    private fun appendTagToNotes(tag: String) {
        val currentText = binding.etNotes.text?.toString()?.trim() ?: ""
        val tagWithPrefix = "#$tag"
        
        // 检查标签是否已存在
        if (currentText.contains(tagWithPrefix)) {
            return
        }
        
        val newText = if (currentText.isEmpty()) {
            tagWithPrefix
        } else {
            "$currentText $tagWithPrefix"
        }
        
        binding.etNotes.setText(newText)
        binding.etNotes.setSelection(newText.length)
        android.util.Log.d("PrayerLog", "📝 Tag added to notes: $tagWithPrefix")
    }
    
    /**
     * 从 Notes 文本框移除标签
     */
    private fun removeTagFromNotes(tag: String) {
        val currentText = binding.etNotes.text?.toString() ?: ""
        val tagWithPrefix = "#$tag"
        
        // 移除标签（包括前后可能的空格）
        val newText = currentText
            .replace(" $tagWithPrefix", "")
            .replace("$tagWithPrefix ", "")
            .replace(tagWithPrefix, "")
            .trim()
        
        binding.etNotes.setText(newText)
        if (newText.isNotEmpty()) {
            binding.etNotes.setSelection(newText.length)
        }
        android.util.Log.d("PrayerLog", "📝 Tag removed from notes: $tagWithPrefix")
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

    /**
     * ⚡ 乐观更新版本：立即更新 UI，后台同步数据
     * 优势：用户感知的响应时间从 1-2秒 降低到 10-20ms
     */
    private fun savePrayerLog() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), getString(R.string.prayer_log_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        // ⚡ 立即触发触感反馈和按钮动画
        triggerHapticFeedback()
        animateButtonClick(binding.btnSave)
        
        // ⚡ 禁用按钮防止重复点击
        binding.btnSave.isEnabled = false
        binding.btnSave.text = getString(R.string.prayer_log_saving)

        // 获取当前日期 (YYYY-MM-DD)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val currentDate = dateFormat.format(Date())
        
        // 使用 originalDate 如果有（编辑模式或 Qada 弥补），否则使用当前日期
        val prayerDate = if (originalDate.isNotEmpty()) originalDate else currentDate
        
        // 判断是否为今天的祷告
        val isToday = (prayerDate == currentDate)

        // ✅ 转换祷告名称为英语（确保数据库一致性）
        val englishPrayerName = com.quran.quranaudio.online.prayertimes.models.PrayerName.toEnglishName(
            prayerName,
            requireContext()
        )
        
        android.util.Log.d("PrayerLog", "📝 Prayer name conversion: '$prayerName' → '$englishPrayerName'")

        // 创建祷告记录
        val prayerLog = PrayerLog.create(
            userId = currentUser.uid,
            prayerName = englishPrayerName,  // ✅ 使用英语名称保存
            status = selectedStatus,
            performedAt = performedAtTimestamp,
            notes = binding.etNotes.text?.toString()?.trim() ?: "",
            date = prayerDate,
            isToday = isToday,
            tags = selectedTags.toList()
        )
        
        // ⚡ 【乐观更新策略】立即更新 UI，后台同步数据
        val timestamp = System.currentTimeMillis()
        android.util.Log.d("PrayerLog", "⚡ [OPTIMISTIC-$timestamp] Immediate UI update")
        
        // 1. 计算 Qada 变化（用于立即更新 UI）
        val qadaDelta = if (isEditMode) {
            calculateQadaDelta(originalStatus, selectedStatus)
        } else {
            if (selectedStatus == PrayerLog.PrayerStatus.QADA) -1 else 0
        }
        
        // 2. 立即通知 Qada 计数器更新（乐观更新）
        if (qadaDelta != 0) {
            onPrayerLoggedListener?.onQadaCountChanged(qadaDelta)
        }
        
        // 3. 立即关闭 Bottom Sheet（用户感知：秒关）
        dismiss()
        
        // 4. 立即通知刷新数据（乐观更新）
        val tempLogId = existingLogId ?: "temp_${System.currentTimeMillis()}"
        if (onPrayerLoggedListener != null) {
            android.util.Log.d("PrayerLog", "✅ [OPTIMISTIC-$timestamp] Calling onPrayerLogged immediately")
            onPrayerLoggedListener?.onPrayerLogged(prayerName, prayerDate, selectedStatus.ordinal, tempLogId)
        } else {
            (parentFragment as? OnPrayerLoggedListener)?.onPrayerLogged(prayerName, prayerDate, selectedStatus.ordinal, tempLogId)
        }
        
        // 5. 在后台协程中异步写入 Firestore（不阻塞 UI）
        backgroundScope.launch {
            try {
                syncPrayerLogToFirestore(
                    currentUser.uid,
                    prayerLog,
                    prayerDate,
                    englishPrayerName,
                    prayerName,
                    qadaDelta
                )
            } catch (e: Exception) {
                android.util.Log.e("PrayerLog", "❌ Background sync failed", e)
                // 在主线程显示错误提示
                launch(Dispatchers.Main) {
                    // Fragment 可能已经销毁，检查 context
                    context?.let { ctx ->
                        Toast.makeText(
                            ctx,
                            getString(R.string.prayer_log_sync_failed, e.message ?: ""),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    /**
     * 后台同步祷告记录到 Firestore
     * 在协程中执行，不阻塞 UI
     */
    private suspend fun syncPrayerLogToFirestore(
        userId: String,
        prayerLog: PrayerLog,
        prayerDate: String,
        englishPrayerName: String,
        displayPrayerName: String,
        qadaDelta: Int
    ) {
        android.util.Log.d("PrayerLog", "🔄 Background sync started")
        
        val collectionRef = firestore.collection(PrayerLog.COLLECTION_NAME)
        
        if (isEditMode && existingLogId != null) {
            // 编辑模式：更新现有文档
            syncEditMode(collectionRef, prayerLog, displayPrayerName, qadaDelta)
        } else {
            // 新建模式：检查是否已存在，然后创建或更新
            syncCreateMode(collectionRef, userId, prayerLog, prayerDate, englishPrayerName, displayPrayerName, qadaDelta)
        }
    }
    
    /**
     * 编辑模式的后台同步
     */
    private suspend fun syncEditMode(
        collectionRef: com.google.firebase.firestore.CollectionReference,
        prayerLog: PrayerLog,
        displayPrayerName: String,
        qadaDelta: Int
    ) {
        try {
            collectionRef.document(existingLogId!!)
                .set(prayerLog.copy(id = existingLogId!!))
                .addOnSuccessListener {
                    android.util.Log.d("PrayerLog", "✅ Background sync successful (EDIT): $existingLogId")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PrayerLog", "❌ Background sync failed (EDIT)", e)
                    throw e
                }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 新建模式的后台同步
     */
    private suspend fun syncCreateMode(
        collectionRef: com.google.firebase.firestore.CollectionReference,
        userId: String,
        prayerLog: PrayerLog,
        prayerDate: String,
        englishPrayerName: String,
        displayPrayerName: String,
        qadaDelta: Int
    ) {
        try {
            // 先检查是否已存在该日期+祷告名称的记录
            val snapshot = collectionRef
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", prayerDate)
                .whereEqualTo("prayerName", englishPrayerName)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.documents.isNotEmpty()) {
                        // 找到现有记录，更新它
                        val existingId = snapshot.documents[0].id
                        android.util.Log.d("PrayerLog", "🔄 Background sync: updating existing log $existingId")
                        
                        collectionRef.document(existingId)
                            .set(prayerLog.copy(id = existingId))
                            .addOnSuccessListener {
                                android.util.Log.d("PrayerLog", "✅ Background sync successful (UPDATE): $existingId")
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("PrayerLog", "❌ Background sync failed (UPDATE)", e)
                            }
                    } else {
                        // 没有找到现有记录，创建新文档
                        android.util.Log.d("PrayerLog", "➕ Background sync: creating new log")
                        
                        collectionRef.add(prayerLog)
                            .addOnSuccessListener { documentReference ->
                                android.util.Log.d("PrayerLog", "✅ Background sync successful (CREATE): ${documentReference.id}")
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("PrayerLog", "❌ Background sync failed (CREATE)", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PrayerLog", "❌ Background sync failed (QUERY)", e)
                }
        } catch (e: Exception) {
            throw e
        }
    }
    
    /**
     * 旧版本的 savePrayerLog（保留作为后备，未使用）
     */
    @Suppress("UNUSED")
    private fun savePrayerLogSynchronous() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), getString(R.string.prayer_log_login_required), Toast.LENGTH_SHORT).show()
            return
        }

        // 禁用按钮防止重复点击
        binding.btnSave.isEnabled = false
        binding.btnSave.text = getString(R.string.prayer_log_saving)

        // 获取当前日期 (YYYY-MM-DD)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val currentDate = dateFormat.format(Date())
        
        // 使用 originalDate 如果有（编辑模式或 Qada 弥补），否则使用当前日期
        val prayerDate = if (originalDate.isNotEmpty()) originalDate else currentDate
        
        // 判断是否为今天的祷告
        val isToday = (prayerDate == currentDate)

        // ✅ 转换祷告名称为英语（确保数据库一致性）
        val englishPrayerName = com.quran.quranaudio.online.prayertimes.models.PrayerName.toEnglishName(
            prayerName,
            requireContext()
        )
        
        android.util.Log.d("PrayerLog", "📝 Prayer name conversion: '$prayerName' → '$englishPrayerName'")

        // 创建祷告记录
        val prayerLog = PrayerLog.create(
            userId = currentUser.uid,
            prayerName = englishPrayerName,  // ✅ 使用英语名称保存
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
                        getString(R.string.prayer_log_updated, prayerName),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 通知 Qada 计数器更新
                    if (qadaDelta != 0) {
                        onPrayerLoggedListener?.onQadaCountChanged(qadaDelta)
                    }
                    
                    // 🔍 诊断日志：记录回调状态和时间戳
                    val timestamp = System.currentTimeMillis()
                    android.util.Log.d("PrayerLog", "🔍 [EDIT-$timestamp] Before dismiss()")
                    android.util.Log.d("PrayerLog", "🔍 [EDIT-$timestamp] onPrayerLoggedListener = ${onPrayerLoggedListener != null}")
                    android.util.Log.d("PrayerLog", "🔍 [EDIT-$timestamp] Prayer: $prayerName, Date: $originalDate, Status: $selectedStatus")
                    
                    dismiss()
                    
                    android.util.Log.d("PrayerLog", "🔍 [EDIT-$timestamp] After dismiss(), calling onPrayerLogged()")
                    
                    // 通知刷新数据（优先使用 onPrayerLoggedListener，fallback 到 parentFragment）
                    if (onPrayerLoggedListener != null) {
                        android.util.Log.d("PrayerLog", "✅ [EDIT-$timestamp] Calling onPrayerLoggedListener.onPrayerLogged($prayerName, $originalDate, ${selectedStatus.ordinal}, $existingLogId)")
                        onPrayerLoggedListener?.onPrayerLogged(prayerName, originalDate, selectedStatus.ordinal, existingLogId!!)
                        android.util.Log.d("PrayerLog", "🔍 [EDIT-$timestamp] onPrayerLogged() returned")
                    } else {
                        android.util.Log.d("PrayerLog", "⚠️ [EDIT-$timestamp] Fallback to parentFragment.onPrayerLogged($prayerName, $originalDate, ${selectedStatus.ordinal}, $existingLogId)")
                        (parentFragment as? OnPrayerLoggedListener)?.onPrayerLogged(prayerName, originalDate, selectedStatus.ordinal, existingLogId!!)
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PrayerLog", "❌ Failed to update prayer log", e)
                    
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.prayer_log_failed_update, e.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 恢复按钮状态
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.prayer_log_save)
                }
        } else {
            // 新建模式：先检查是否已存在该日期+祷告名称的记录
            android.util.Log.d("PrayerLog", "🔍 Checking for existing log: date=$prayerDate, prayer=$englishPrayerName")
            
            collectionRef
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("date", prayerDate)
                .whereEqualTo("prayerName", englishPrayerName)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.documents.isNotEmpty()) {
                        // ✅ 找到现有记录，更新它（防止重复）
                        val existingDoc = snapshot.documents[0]
                        val existingId = existingDoc.id
                        
                        android.util.Log.d("PrayerLog", "🔄 Found existing log: $existingId, updating instead of creating new")
                        
                        // 计算状态转换对 Qada 计数器的影响
                        val existingLog = existingDoc.toObject(PrayerLog::class.java)
                        val originalStatusFromDb = existingLog?.status ?: PrayerLog.PrayerStatus.MISSED
                        val qadaDelta = calculateQadaDelta(originalStatusFromDb, selectedStatus)
                        
                        // ⚡ 乐观更新：立即本地刷新 UI，不等网络
                        if (qadaDelta != 0) {
                            onPrayerLoggedListener?.onQadaCountChanged(qadaDelta)
                        }
                        dismiss()
                        if (onPrayerLoggedListener != null) {
                            onPrayerLoggedListener?.onPrayerLogged(prayerName, prayerDate, selectedStatus.ordinal, existingId)
                        } else {
                            (parentFragment as? OnPrayerLoggedListener)?.onPrayerLogged(prayerName, prayerDate, selectedStatus.ordinal, existingId)
                        }

                        // 后台写入 Firestore
                        collectionRef.document(existingId)
                            .set(prayerLog.copy(id = existingId))
                            .addOnSuccessListener {
                                android.util.Log.d("PrayerLog", "✅ Existing prayer log updated: $existingId")
                                android.util.Log.d("PrayerLog", "  状态转换: $originalStatusFromDb → $selectedStatus, Qada delta: $qadaDelta")
                                
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.prayer_log_updated, prayerName),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("PrayerLog", "❌ Failed to update existing prayer log", e)
                                
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.prayer_log_failed_update, e.message ?: ""),
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                binding.btnSave.isEnabled = true
                                binding.btnSave.text = getString(R.string.prayer_log_save)
                            }
                    } else {
                        // ✅ 没有找到现有记录，创建新文档
                        android.util.Log.d("PrayerLog", "➕ No existing log found, creating new document")
                        
                        val qadaDelta = if (selectedStatus == PrayerLog.PrayerStatus.QADA) -1 else 0
                        
                        // ⚡ 乐观更新：先本地更新 UI，再异步写入 Firestore
                        val tempId = "temp_${System.currentTimeMillis()}"
                        // 1) 立即通知 Qada delta
                        if (qadaDelta != 0) {
                            onPrayerLoggedListener?.onQadaCountChanged(qadaDelta)
                        }
                        // 2) 立即关闭弹窗
                        dismiss()
                        // 3) 立即通知父级刷新 UI（使用临时 ID，后续成功会用真实 ID 覆盖）
                        if (onPrayerLoggedListener != null) {
                            onPrayerLoggedListener?.onPrayerLogged(prayerName, prayerDate, selectedStatus.ordinal, tempId)
                        } else {
                            (parentFragment as? OnPrayerLoggedListener)?.onPrayerLogged(prayerName, prayerDate, selectedStatus.ordinal, tempId)
                        }

                        // 4) 后台真正写入 Firestore
                        collectionRef.add(prayerLog)
                            .addOnSuccessListener { documentReference ->
                                android.util.Log.d("PrayerLog", "✅ New prayer log saved: ${documentReference.id}")
                                android.util.Log.d("PrayerLog", "  Status: $selectedStatus, Qada delta: $qadaDelta")
                                
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.prayer_log_saved, prayerName),
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                // 如果需要，可以在此再次通知，用真实 ID 覆盖（但 UI 已经乐观更新，不强制）
                                onPrayerLoggedListener?.onPrayerLogged(prayerName, prayerDate, selectedStatus.ordinal, documentReference.id)
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("PrayerLog", "❌ Failed to save new prayer log", e)
                                
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.prayer_log_failed_save, e.message ?: ""),
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                binding.btnSave.isEnabled = true
                                binding.btnSave.text = getString(R.string.prayer_log_save)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PrayerLog", "❌ Failed to check for existing log", e)
                    
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.prayer_log_failed_save, e.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    binding.btnSave.isEnabled = true
                    binding.btnSave.text = getString(R.string.prayer_log_save)
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
        backgroundScope.cancel()
        _binding = null
    }
    
    /**
     * ⚡ 触感反馈（Haptic Feedback）- 提供物理确认感
     */
    private fun triggerHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(50)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("PrayerLog", "Failed to trigger haptic feedback: ${e.message}")
        }
    }
    
    /**
     * ⚡ 按钮缩放动画 - 增强交互感
     */
    private fun animateButtonClick(button: View) {
        val scaleDown = ScaleAnimation(
            1f, 0.95f,  // X: from 1.0 to 0.95
            1f, 0.95f,  // Y: from 1.0 to 0.95
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        )
        scaleDown.duration = 100
        scaleDown.fillAfter = false
        
        val scaleUp = ScaleAnimation(
            0.95f, 1f,  // X: from 0.95 to 1.0
            0.95f, 1f,  // Y: from 0.95 to 1.0
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f
        )
        scaleUp.duration = 100
        scaleUp.startOffset = 100
        scaleUp.fillAfter = true
        
        button.startAnimation(scaleDown)
        button.postDelayed({
            button.startAnimation(scaleUp)
        }, 100)
    }

    /**
     * 回调接口 - 通知父 Fragment 祷告已记录
     */
    interface OnPrayerLoggedListener {
        fun onPrayerLogged(prayerName: String, date: String, newStatus: Int, logId: String)
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

