package com.quran.quranaudio.online.feedback

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.quran.quranaudio.online.R

/**
 * 用户反馈底部弹窗
 * 
 * 三阶段交互：
 * 1. 选择情绪（😍 好用 / 😐 一般 / 😡 难用）
 * 2. 选择标签（根据情绪显示不同的预设标签）
 * 3. 输入评论（可选）
 * 
 * 用于诊断 9.4% 留存率和 52秒时长问题
 */
class FeedbackBottomSheetDialog : BottomSheetDialogFragment() {
    
    // UI 组件
    private lateinit var tvTitle: TextView
    private lateinit var layoutEmotionSelection: LinearLayout
    private lateinit var layoutTagSelection: LinearLayout
    private lateinit var layoutCommentInput: LinearLayout
    private lateinit var cardLove: MaterialCardView
    private lateinit var cardNeutral: MaterialCardView
    private lateinit var cardHate: MaterialCardView
    private lateinit var tvTagPrompt: TextView
    private lateinit var chipGroupTags: ChipGroup
    private lateinit var etComment: TextInputEditText
    private lateinit var btnSubmit: MaterialButton
    private lateinit var btnBack: MaterialButton
    
    // 当前选择的情绪
    private var selectedEmotion: FeedbackEmotion? = null
    
    // 当前选择的标签
    private val selectedTags = mutableListOf<String>()
    
    // 当前阶段（1: 情绪, 2: 标签, 3: 评论）
    private var currentStage = 1
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use Material Components default theme
        return BottomSheetDialog(requireContext(), com.google.android.material.R.style.Theme_Design_BottomSheetDialog)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_feedback_sheet, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupListeners()
        
        // 初始化为第一阶段
        showStage(1)
    }
    
    /**
     * 初始化视图
     */
    private fun initViews(view: View) {
        tvTitle = view.findViewById(R.id.tv_title)
        layoutEmotionSelection = view.findViewById(R.id.layout_emotion_selection)
        layoutTagSelection = view.findViewById(R.id.layout_tag_selection)
        layoutCommentInput = view.findViewById(R.id.layout_comment_input)
        cardLove = view.findViewById(R.id.card_love)
        cardNeutral = view.findViewById(R.id.card_neutral)
        cardHate = view.findViewById(R.id.card_hate)
        tvTagPrompt = view.findViewById(R.id.tv_tag_prompt)
        chipGroupTags = view.findViewById(R.id.chip_group_tags)
        etComment = view.findViewById(R.id.et_comment)
        btnSubmit = view.findViewById(R.id.btn_submit)
        btnBack = view.findViewById(R.id.btn_back)
    }
    
    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // Emoji 卡片点击
        cardLove.setOnClickListener { onEmotionSelected(FeedbackEmotion.LOVE) }
        cardNeutral.setOnClickListener { onEmotionSelected(FeedbackEmotion.NEUTRAL) }
        cardHate.setOnClickListener { onEmotionSelected(FeedbackEmotion.HATE) }
        
        // 提交按钮
        btnSubmit.setOnClickListener { onSubmit() }
        
        // 返回按钮
        btnBack.setOnClickListener { onBackPressed() }
    }
    
    /**
     * 情绪选择
     */
    private fun onEmotionSelected(emotion: FeedbackEmotion) {
        selectedEmotion = emotion
        
        // 高亮选中的卡片
        highlightSelectedEmotion(emotion)
        
        // 延迟 300ms 切换到标签选择
        view?.postDelayed({
            showStage(2)
        }, 300)
    }
    
    /**
     * 高亮选中的情绪卡片
     */
    private fun highlightSelectedEmotion(emotion: FeedbackEmotion) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.feedback_card_selected)
        val defaultColor = ContextCompat.getColor(requireContext(), R.color.feedback_card_bg)
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.feedback_primary)
        
        cardLove.setCardBackgroundColor(if (emotion == FeedbackEmotion.LOVE) selectedColor else defaultColor)
        cardNeutral.setCardBackgroundColor(if (emotion == FeedbackEmotion.NEUTRAL) selectedColor else defaultColor)
        cardHate.setCardBackgroundColor(if (emotion == FeedbackEmotion.HATE) selectedColor else defaultColor)
        
        cardLove.strokeWidth = if (emotion == FeedbackEmotion.LOVE) 4 else 0
        cardNeutral.strokeWidth = if (emotion == FeedbackEmotion.NEUTRAL) 4 else 0
        cardHate.strokeWidth = if (emotion == FeedbackEmotion.HATE) 4 else 0
        
        cardLove.strokeColor = primaryColor
        cardNeutral.strokeColor = primaryColor
        cardHate.strokeColor = primaryColor
    }
    
    /**
     * 显示指定阶段
     */
    private fun showStage(stage: Int) {
        currentStage = stage
        
        when (stage) {
            1 -> {
                // 阶段1：情绪选择
                tvTitle.text = "您觉得这个应用怎么样？"
                layoutEmotionSelection.visibility = View.VISIBLE
                layoutTagSelection.visibility = View.GONE
                layoutCommentInput.visibility = View.GONE
                btnBack.visibility = View.GONE
            }
            2 -> {
                // 阶段2：标签选择
                tvTitle.text = "感谢您的反馈！"
                layoutEmotionSelection.visibility = View.GONE
                layoutTagSelection.visibility = View.VISIBLE
                layoutCommentInput.visibility = View.GONE
                btnBack.visibility = View.VISIBLE
                
                // 加载标签
                loadTagsForEmotion(selectedEmotion!!)
                
                // 延迟 500ms 显示评论输入框
                view?.postDelayed({
                    showStage(3)
                }, 500)
            }
            3 -> {
                // 阶段3：评论输入
                layoutCommentInput.visibility = View.VISIBLE
            }
        }
    }
    
    /**
     * 加载标签
     */
    private fun loadTagsForEmotion(emotion: FeedbackEmotion) {
        chipGroupTags.removeAllViews()
        
        val tags = FeedbackTags.getTagsForEmotion(emotion)
        
        for (tag in tags) {
            val chip = Chip(requireContext()).apply {
                text = tag
                isCheckable = true
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.feedback_chip_bg)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.feedback_text_primary))
                
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedTags.add(tag)
                        chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.feedback_chip_selected)
                        setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    } else {
                        selectedTags.remove(tag)
                        chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.feedback_chip_bg)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.feedback_text_primary))
                    }
                }
            }
            chipGroupTags.addView(chip)
        }
    }
    
    /**
     * 返回按钮
     */
    private fun onBackPressed() {
        if (currentStage > 1) {
            showStage(currentStage - 1)
        } else {
            dismiss()
        }
    }
    
    /**
     * 提交反馈
     */
    private fun onSubmit() {
        val emotion = selectedEmotion ?: return
        val comment = etComment.text?.toString()?.trim()
        
        // 显示加载状态
        btnSubmit.isEnabled = false
        btnSubmit.text = "提交中..."
        
        // 提交到 Firebase
        FeedbackManager.getInstance().submitFeedback(
            context = requireContext(),
            emotion = emotion,
            selectedTags = selectedTags.toList(),
            comment = if (comment.isNullOrEmpty()) null else comment,
            onSuccess = {
                // 成功
                dismiss()
                Toast.makeText(
                    requireContext(),
                    "感谢您的直言不讳！您的建议已提交至开发者桌面。",
                    Toast.LENGTH_LONG
                ).show()
            },
            onFailure = { exception ->
                // 失败
                btnSubmit.isEnabled = true
                btnSubmit.text = "提交反馈"
                Toast.makeText(
                    requireContext(),
                    "提交失败，请检查网络连接后重试",
                    Toast.LENGTH_SHORT
                ).show()
                android.util.Log.e("FeedbackDialog", "Submit failed", exception)
            }
        )
    }
    
    companion object {
        const val TAG = "FeedbackBottomSheet"
        
        fun newInstance(): FeedbackBottomSheetDialog {
            return FeedbackBottomSheetDialog()
        }
    }
}

