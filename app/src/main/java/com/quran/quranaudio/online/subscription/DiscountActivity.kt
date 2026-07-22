package com.quran.quranaudio.online.subscription

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.ActivityDiscountBinding
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 折扣挽回页（按方案：月订阅首月5折 / 年订阅首年5折）。
 *
 * 合规要点：
 *  - 折扣百分比、划线原价、折后价全部取自 Play 返回的折扣 offer，无任何硬编码价格。
 *  - offer 拿不到（Play 未下发或账号无资格）→ 直接关闭本页，绝不展示虚假折扣。
 *  - 倒计时读 [DiscountManager] 的持久化剩余时间，关掉重进不重置；归零即失效并结束。
 *  - 续订条款购买前明示；关闭按钮首帧即可见可点。
 */
class DiscountActivity : AppCompatActivity(), BillingManager.BillingListener {

    companion object {
        /** Intent extra：方案 key（"monthly" / "yearly"）。缺省按年订阅。 */
        const val EXTRA_PLAN = "discount_plan"
    }

    private lateinit var binding: ActivityDiscountBinding
    private lateinit var billingManager: BillingManager

    private lateinit var plan: DiscountManager.Plan
    private var product: ProductDetails? = null
    private var discountOffer: ProductDetails.SubscriptionOfferDetails? = null
    private var countdownTimer: CountDownTimer? = null
    private var contentReady = false

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(updateBaseContextLocale(base))
    }

    // 与 SubscriptionActivity 一致的成熟语言处理：attachBaseContext + applyOverrideConfiguration。
    // 仅在 attachBaseContext 里 createConfigurationContext 会导致 AppCompat subDecor 为 null 而崩溃。
    private fun updateBaseContextLocale(context: Context): Context {
        val language = SPAppConfigs.getLocale(context)
        if (language.isNullOrEmpty()) return context
        val resourceLanguage = if (language == "id") "in" else language
        val locale = Locale(resourceLanguage)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            val language = SPAppConfigs.getLocale(this)
            if (!language.isNullOrEmpty()) {
                val resourceLanguage = if (language == "id") "in" else language
                overrideConfiguration.setLocale(Locale(resourceLanguage))
            }
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 方案：默认年订阅（兼容旧入口）。
        plan = DiscountManager.Plan.of(intent.getStringExtra(EXTRA_PLAN)) ?: DiscountManager.Plan.YEARLY

        // 窗口已过期/已消费的兜底：即便被误拉起也立即退出，不展示过期折扣。
        if (!DiscountManager.isActive(this, plan)) {
            finish()
            return
        }

        // 方案相关文案（首月/首年）。
        binding.tvDiscountCaption.setText(
            if (plan == DiscountManager.Plan.MONTHLY) R.string.discount_first_month_caption
            else R.string.discount_first_year_caption
        )
        binding.tvPricePeriod.setText(
            if (plan == DiscountManager.Plan.MONTHLY) R.string.discount_price_period_month
            else R.string.discount_price_period
        )

        binding.btnClose.setOnClickListener { finish() }
        setupLegalLinks()
        startCountdown()

        // 折扣数字与价格在拿到真实 offer 前一律隐藏，杜绝占位「50% OFF」闪现。
        binding.tvDiscountPercent.visibility = View.INVISIBLE
        binding.priceRow.visibility = View.INVISIBLE
        binding.btnSubscribe.visibility = View.INVISIBLE
        binding.tvDisclosure.visibility = View.INVISIBLE

        billingManager = BillingManager(this, lifecycleScope)
        billingManager.setBillingListener(this)
        billingManager.initialize()

        logEvent("discount_page_open")
    }

    // ---- Billing 回调 ----

    override fun onBillingSetupFinished(success: Boolean) {
        if (success) {
            billingManager.querySubscriptionProducts()
        } else {
            android.util.Log.e("DiscountActivity", "❌ Billing setup failed; closing.")
            runOnUiThread { finish() }
        }
    }

    override fun onProductsLoaded(products: List<ProductDetails>) = runOnUiThread {
        product = products.firstOrNull { it.productId == plan.productId }
        discountOffer = DiscountManager.findDiscountOffer(product, plan)
        if (discountOffer == null) {
            // Play 没有下发折扣 offer —— 绝不显示假折扣，直接退出。
            android.util.Log.w("DiscountActivity", "⚠️ No discount offer returned by Play; closing.")
            Toast.makeText(this, R.string.discount_unavailable, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            bindPrices(discountOffer!!)
        }
    }

    override fun onPurchaseSuccess(purchase: Purchase) = runOnUiThread {
        logEvent("discount_purchased")
        // 购买成功即消费掉挽回机会（防止再次弹出）。
        markConsumed()
        Toast.makeText(this, R.string.subscription_message_success, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onPurchaseFailure(errorCode: Int, errorMessage: String) {
        android.util.Log.w("DiscountActivity", "Purchase failed: $errorCode $errorMessage")
    }

    override fun onSubscriptionStatusChanged(isSubscribed: Boolean, productId: String?) = runOnUiThread {
        if (isSubscribed) finish()
    }

    // ---- 价格绑定：全部来自真实 offer ----

    private fun bindPrices(offer: ProductDetails.SubscriptionOfferDetails) {
        // 折扣阶段与续订阶段的判定统一收敛在 DiscountManager，保证与主页角标一致。
        val introPhase = DiscountManager.introPhase(offer)
        val renewalPhase = DiscountManager.renewalPhase(offer)
        val percent = DiscountManager.computePercent(offer)

        if (introPhase == null || renewalPhase == null || percent == null) {
            android.util.Log.w("DiscountActivity", "⚠️ Offer lacks intro/renewal phase; closing.")
            finish()
            return
        }

        val discountedPrice = introPhase.formattedPrice
        val originalPrice = renewalPhase.formattedPrice

        binding.tvDiscountPercent.text = getString(R.string.discount_percent_big, percent)
        binding.btnSubscribe.text = getString(R.string.discount_cta_percent, percent)
        binding.tvDiscountedPrice.text = discountedPrice
        binding.tvOriginalPrice.text = originalPrice
        binding.tvOriginalPrice.paintFlags =
            binding.tvOriginalPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        // 续订条款：首月/首年折扣价，之后按原价自动续订，购买前明示。
        binding.tvDisclosure.text = getString(
            if (plan == DiscountManager.Plan.MONTHLY) R.string.discount_disclosure_month
            else R.string.discount_disclosure,
            discountedPrice, originalPrice
        )

        binding.btnSubscribe.setOnClickListener { launchPurchase() }
        contentReady = true

        // 真实价格到位，显示折扣数字/价格/CTA/续订披露。
        binding.tvDiscountPercent.visibility = View.VISIBLE
        binding.priceRow.visibility = View.VISIBLE
        binding.btnSubscribe.visibility = View.VISIBLE
        binding.tvDisclosure.visibility = View.VISIBLE
    }

    private fun launchPurchase() {
        val p = product
        val offer = discountOffer
        if (!contentReady || p == null || offer == null) {
            Toast.makeText(this, R.string.discount_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        // 用折扣 offer 的 token 发起付费 —— 结算即折后价，与页面醒目价一致。
        billingManager.launchPurchaseFlow(
            activity = this,
            productDetails = p,
            offerToken = offer.offerToken
        )
    }

    // ---- 倒计时 ----

    private fun startCountdown() {
        val remaining = DiscountManager.remainingMillis(this, plan)
        if (remaining <= 0) {
            finish()
            return
        }
        renderCountdown(remaining)
        countdownTimer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(millisUntilFinished: Long) = renderCountdown(millisUntilFinished)
            override fun onFinish() {
                // 窗口结束：恢复原价视角，关闭本页。
                markConsumed()
                Toast.makeText(this@DiscountActivity, R.string.discount_expired, Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
    }

    private fun renderCountdown(millis: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        binding.tvCountdown.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    private fun markConsumed() {
        DiscountManager.markConsumed(this, plan)
    }

    // ---- 法务链接 ----

    private fun setupLegalLinks() {
        val privacy = getString(R.string.privacy_policy_title)
        val terms = getString(R.string.google_play_terms_title)
        val text = "$privacy  •  $terms"
        val spannable = SpannableString(text)
        fun link(label: String, url: String) {
            val start = text.indexOf(label)
            if (start < 0) return
            spannable.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }, start, start + label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        link(privacy, getString(R.string.privacy_policy_url))
        link(terms, getString(R.string.google_play_terms_url))
        binding.tvLegalLinks.text = spannable
        binding.tvLegalLinks.movementMethod = LinkMovementMethod.getInstance()
        binding.tvLegalLinks.highlightColor = android.graphics.Color.TRANSPARENT
    }

    private fun logEvent(name: String) {
        try {
            com.quran.quranaudio.online.analytics.AnalyticsManager
                .getInstance(this).logEvent(name, HashMap())
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        countdownTimer?.cancel()
        super.onDestroy()
    }
}
