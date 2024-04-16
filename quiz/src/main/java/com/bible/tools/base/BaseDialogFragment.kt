package com.bible.tools.base

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.quran.quranaudio.quiz.R
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {

    private var _binding: VB? = null
    protected val mBinding get() = _binding!!
    protected val mDisposables = CompositeDisposable()
    protected abstract fun getPageName(): String
    protected abstract fun getFromPageName(): String
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext(), R.style.Theme_AppCompat_Dialog_Loading_Fullscreen)
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = getViewBinding(inflater, container)
        return mBinding.root
    }

    abstract fun getViewBinding(inflater: LayoutInflater,
                                container: ViewGroup?): VB

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureViewAndClicks()
    }

    abstract fun ensureViewAndClicks()

    override fun onStart() {
        super.onStart()
//        val metrics = requireContext().resources.displayMetrics
//        val widthPixels = metrics.widthPixels
//        dialog?.window?.setLayout((widthPixels * 0.83).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    protected fun addDisposable(disposable: ()-> Disposable) {
        mDisposables.add(disposable.invoke())
    }

    @CallSuper
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposables.clear()
    }
}
