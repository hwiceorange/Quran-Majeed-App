package com.quran.quranaudio.online.quran_module.frags

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.quran.quranaudio.online.databinding.FragmentQuranIndexBinding
import com.quran.quranaudio.online.quran_module.helpers.QuranIndexPageHelper

/**
 * Quran Index Fragment for bottom navigation
 * 复用 ActivityReaderIndexPage 的所有逻辑，避免代码重复
 */
class QuranIndexFragment : Fragment() {
    
    private var _binding: FragmentQuranIndexBinding? = null
    private val binding get() = _binding!!
    
    // 使用 Helper 来复用 ActivityReaderIndexPage 的逻辑
    private lateinit var pageHelper: QuranIndexPageHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuranIndexBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 初始化 Helper，传入 Fragment 上下文和 binding
        pageHelper = QuranIndexPageHelper(
            fragment = this,
            headerBinding = binding.header,
            viewPager = binding.viewPager,
            sortButton = binding.sort,
            hideBackButton = true  // Fragment 中隐藏返回按钮
        )
        
        // 初始化页面（复用 ActivityReaderIndexPage 的所有逻辑）
        pageHelper.init()

    }
    
    override fun onResume() {
        super.onResume()
        pageHelper.onResume()
    }

    /**
     * 接收子 Fragment 的回调注册（防止崩溃）
     * BaseFragReaderIndex 的子类（FragReaderIndexChapters 等）在 onAttach 时会调用此方法
     */
    fun addToCallbacks(callback: com.quran.quranaudio.online.quran_module.interfaceUtils.readerIndex.FragReaderIndexCallback) {
        pageHelper.addToCallbacks(callback)
        android.util.Log.d("QuranIndexFragment", "✅ Callback registered: ${callback.javaClass.simpleName}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pageHelper.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = QuranIndexFragment()
    }
}
