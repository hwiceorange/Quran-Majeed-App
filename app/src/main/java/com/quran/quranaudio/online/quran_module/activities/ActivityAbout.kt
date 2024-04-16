package com.quran.quranaudio.online.quran_module.activities

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import com.peacedesign.android.utils.AppBridge
import com.peacedesign.android.utils.DrawableUtils
import com.quran.quranaudio.online.BuildConfig
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.api.ApiConfig
import com.quran.quranaudio.online.databinding.ActivityAboutBinding
import com.quran.quranaudio.online.databinding.LytReaderSettingsItemBinding
import com.quran.quranaudio.online.quran_module.utils.app.InfoUtils.openAbout
import com.quran.quranaudio.online.quran_module.utils.extensions.isRTL
import com.quran.quranaudio.online.quran_module.utils.extensions.updateMarginHorizontal
import com.quran.quranaudio.online.quran_module.utils.extensions.updateMarginVertical

class ActivityAbout : com.quran.quranaudio.online.quran_module.activities.base.BaseActivity() {
    override fun shouldInflateAsynchronously() = true

    override fun getLayoutResource() = R.layout.activity_about

    override fun onActivityInflated(activityView: View, savedInstanceState: Bundle?) {
        val binding = ActivityAboutBinding.bind(activityView)
        initHeader(binding.header)
        init(binding)
    }

    private fun initHeader(header: com.quran.quranaudio.online.quran_module.views.BoldHeader) {
        header.let {
            it.setCallback { onBackPressed() }
            it.setTitleText(R.string.strTitleAboutUs)
            it.setShowRightIcon(false)
            it.setShowSearchIcon(false)
            it.setBGColor(R.color.colorBGPage)
        }
    }

    private fun init(binding: ActivityAboutBinding) {
        setup(
            binding,
            LytReaderSettingsItemBinding.inflate(layoutInflater),
            R.drawable.logo,
            R.string.strTitleAppVersion,
            BuildConfig.VERSION_NAME,
            false
        )
        setup(
            binding,
            LytReaderSettingsItemBinding.inflate(layoutInflater).apply {
                root.setOnClickListener { openAbout(this@ActivityAbout) }
            },
            R.drawable.dr_icon_info,
            R.string.strTitleAboutUs
        )
        setup(
            binding,
            LytReaderSettingsItemBinding.inflate(layoutInflater).apply {
                root.setOnClickListener {
                    AppBridge.newOpener(it.context).browseLink(ApiConfig.CODECANYON_URL)
                }
            },
            R.drawable.cc_logo,
            R.string.codecanyon
        )
    }

    private fun setup(
        parent: ActivityAboutBinding,
        binding: LytReaderSettingsItemBinding,
        startIcon: Int,
        titleRes: Int,
        subtitle: String? = null,
        showArrow: Boolean = true
    ) {
        setupLauncherParams(binding.root)
        prepareTitle(binding, titleRes, subtitle)
        setupIcons(startIcon, binding.launcher, showArrow)
        parent.container.addView(binding.root)
    }

    private fun prepareTitle(
        binding: LytReaderSettingsItemBinding,
        titleRes: Int,
        subtitle: String?
    ) {
        val ssb = SpannableStringBuilder()

        ssb.append(
            SpannableString(str(titleRes)).apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        )

        if (!subtitle.isNullOrEmpty()) {
            ssb.append("\n").append(
                SpannableString(subtitle).apply {
                    setSpan(
                        AbsoluteSizeSpan(dimen(R.dimen.dmnCommonSize2)),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    setSpan(
                        ForegroundColorSpan(color(R.color.colorText3)),
                        0,
                        length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            )
        }

        binding.launcher.text = ssb
    }

    private fun setupIcons(startIconRes: Int, textView: com.quran.quranaudio.online.quran_module.widgets.IconedTextView, showArrow: Boolean) {
        var chevronRight = if (showArrow) drawable(R.drawable.dr_icon_chevron_right) else null
        if (chevronRight != null && isRTL()) {
            chevronRight = DrawableUtils.rotate(this, chevronRight, 180f)
        }
        textView.setDrawables(drawable(startIconRes), null, chevronRight, null)
    }

    private fun setupLauncherParams(launcherView: View) {
        launcherView.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
            updateMarginVertical(dp2px(5f))
            updateMarginHorizontal(dp2px(10f))
        }
    }
}
