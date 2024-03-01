package com.quran.quranaudio.online.quran_module.frags.storageCleapup

import android.content.Context
import android.os.Bundle
import androidx.annotation.CallSuper
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.activities.ActivityStorageCleanup
import com.quran.quranaudio.online.quran_module.frags.BaseFragment
import com.quran.quranaudio.online.quran_module.views.BoldHeader

abstract class FragStorageCleanupBase : BaseFragment() {
    abstract fun getFragTitle(ctx: Context): String?

    protected fun activity(): ActivityStorageCleanup? = activity as? ActivityStorageCleanup

    @CallSuper
    open fun setupHeader(activity: ActivityStorageCleanup, header: BoldHeader) {
        header.setCallback { activity.onBackPressedDispatcher.onBackPressed() }
        header.setTitleText(getFragTitle(activity))
    }

    fun launchFrag(cls: Class<out FragStorageCleanupBase?>, args: Bundle? = null) {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.frags_container, cls, args, cls.simpleName)
            setReorderingAllowed(true)
            addToBackStack(cls.simpleName)
            commit()
        }
    }
}
