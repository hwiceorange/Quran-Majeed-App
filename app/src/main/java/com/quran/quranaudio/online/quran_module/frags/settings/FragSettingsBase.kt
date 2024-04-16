/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
package com.quran.quranaudio.online.quran_module.frags.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.CallSuper
import androidx.annotation.ColorInt
import androidx.annotation.LayoutRes
import androidx.core.widget.NestedScrollView
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.activities.readerSettings.Activity_Quran_Settings
import com.quran.quranaudio.online.quran_module.frags.BaseFragment
import com.quran.quranaudio.online.quran_module.utils.extensions.color
import com.quran.quranaudio.online.quran_module.views.BoldHeader

abstract class FragSettingsBase : BaseFragment() {
    protected fun activity(): Activity_Quran_Settings? = activity as? Activity_Quran_Settings

    protected open val shouldCreateScrollerView = false

    protected open fun getFragView(ctx: Context): View? = null

    abstract fun getFragTitle(ctx: Context): String?

    @get:LayoutRes
    abstract val layoutResource: Int

    @ColorInt
    open fun getPageBackgroundColor(ctx: Context): Int = ctx.color(R.color.colorBGPage)

    open fun getFinishingResult(ctx: Context): Bundle? = null

    @CallSuper
    open fun onNewArguments(args: Bundle) {
        arguments = args
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return if (shouldCreateScrollerView) {
            NestedScrollView(inflater.context).apply {
                id = R.id.scrollView
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                addView(
                    getFragView(inflater.context) ?: inflater.inflate(layoutResource, this, false)
                )
            }
        } else {
            getFragView(inflater.context) ?: inflater.inflate(layoutResource, container, false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(getPageBackgroundColor(view.context))

        if (shouldCreateScrollerView) {
            onViewReady(view.context, (view as ViewGroup).getChildAt(0), savedInstanceState)
        } else {
            onViewReady(view.context, view, savedInstanceState)
        }
    }

    abstract fun onViewReady(ctx: Context, view: View, savedInstanceState: Bundle?)

    @CallSuper
    open fun setupHeader(activity: Activity_Quran_Settings, header: BoldHeader) {
        header.setTitleText(getFragTitle(activity))
    }

    fun launchFrag(cls: Class<out FragSettingsBase?>, args: Bundle?) {
        parentFragmentManager.beginTransaction().apply {
            replace(R.id.frags_container, cls, args, cls.simpleName)
            setReorderingAllowed(true)
            addToBackStack(cls.simpleName)
            commit()
        }
    }
}
