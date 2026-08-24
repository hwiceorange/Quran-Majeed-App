package com.quran.quranaudio.online.subscription

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.ads.AdPolicy
import com.quranaudio.common.ad.SubscriptionChecker

/**
 * 去广告买断弹窗（商品 removeads）。
 *
 * 展示前置条件（[shouldShow]）：
 *   - 订阅用户不展示（他们已经无广告，再弹是骚扰且显得贪婪）；
 *   - 已买断去广告的用户不展示；
 *   - 距上次展示需满 [AdPolicy.AD_FREE_PROMO_MIN_INTERVAL_HOURS]；
 *   - 用户手动关闭累计达 [AdPolicy.AD_FREE_PROMO_MAX_DISMISS] 次后不再自动弹
 *     （设置页的固定入口始终保留，想买的人永远找得到）。
 *
 * 阿语适配交给布局：全部使用 Start/End 而非 Left/Right，
 * 关闭按钮在 RTL 下自动移到左上角。
 */
class AdFreeDialog(private val activity: Activity) : Dialog(activity) {

    companion object {
        private const val PREFS = "ad_free_promo"
        private const val K_LAST_SHOWN = "last_shown_at"
        private const val K_DISMISS_COUNT = "dismiss_count"

        /** 是否可以自动弹出。设置页的固定入口不走这个判断。 */
        @JvmStatic
        fun shouldShow(context: Context): Boolean {
            // 订阅用户 / 已买断用户：不弹
            if (SubscriptionChecker.shouldHideAds(context)) return false

            val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (sp.getInt(K_DISMISS_COUNT, 0) >= AdPolicy.AD_FREE_PROMO_MAX_DISMISS) return false

            val last = sp.getLong(K_LAST_SHOWN, 0L)
            val gap = AdPolicy.AD_FREE_PROMO_MIN_INTERVAL_HOURS * 60 * 60 * 1000L
            return System.currentTimeMillis() - last >= gap
        }

        /** 满足条件才弹；否则静默跳过。供插屏关闭等时机调用。 */
        @JvmStatic
        fun showIfEligible(activity: Activity) {
            if (activity.isFinishing || activity.isDestroyed) return
            if (!shouldShow(activity)) return
            try {
                AdFreeDialog(activity).show()
            } catch (t: Throwable) {
                android.util.Log.w("AdFreeDialog", "show failed", t)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_ad_free)
        window?.apply {
            // 透明窗口背景 + 布局自带的左右 padding，才能让 MaterialCardView 真正「浮起」
            // （上一版是满宽贴边，看起来像卡在半空的 bottom sheet）。
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            // 加深背后遮罩，让卡片从页面里跳出来，与参考图的观感一致
            setDimAmount(0.6f)
        }
        setCanceledOnTouchOutside(true)

        val prefs = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong(K_LAST_SHOWN, System.currentTimeMillis()).apply()

        val cta = findViewById<TextView>(R.id.adFreeCta)
        // 有缓存价格就显示在按钮上——标价能显著提升一次性买断的转化，
        // 尤其是当价格远低于用户对「订阅」的心理预期时。
        val price = AdFreeBilling.cachedPrice(context)
        if (price.isNotEmpty()) {
            cta.text = context.getString(R.string.ad_free_cta_priced, price)
        }

        cta.setOnClickListener { startPurchase() }

        findViewById<TextView>(R.id.adFreeRestore).setOnClickListener {
            AdFreeBilling.restore(context) { owned ->
                activity.runOnUiThread {
                    if (owned) {
                        toast(R.string.ad_free_owned)
                        dismiss()
                    } else {
                        toast(R.string.ad_free_restore_none)
                    }
                }
            }
        }

        findViewById<android.widget.ImageView>(R.id.adFreeClose).setOnClickListener {
            // 记一次主动关闭：连续被拒 N 次后不再自动弹，避免把用户推到差评
            prefs.edit()
                .putInt(K_DISMISS_COUNT, prefs.getInt(K_DISMISS_COUNT, 0) + 1)
                .apply()
            dismiss()
        }
    }

    private fun startPurchase() {
        AdFreeBilling.purchase(activity, object : AdFreeBilling.Callback {
            override fun onSuccess() {
                toast(R.string.ad_free_owned)
                dismiss()
            }

            override fun onFailure(code: Int, message: String, userCancelled: Boolean) {
                // 用户主动取消不是错误，不要弹提示去责备他
                if (!userCancelled) toast(R.string.ad_free_failed)
            }
        })
    }

    private fun toast(resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }
}
