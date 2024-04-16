package com.quran.quranaudio.online.features.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

abstract class BaseFragment<BINDING : ViewDataBinding, VM : BaseViewModel>() : Fragment() {

    protected abstract val layoutId: Int

    protected abstract val viewModel: VM

    protected lateinit var binding: BINDING

    protected abstract fun onReady(savedInstanceState: Bundle?)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            layoutId,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    //    observeNavigation()

        onReady(savedInstanceState)
    }


}