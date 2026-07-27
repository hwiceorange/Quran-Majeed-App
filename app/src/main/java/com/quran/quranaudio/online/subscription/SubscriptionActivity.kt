package com.quran.quranaudio.online.subscription

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.ActivitySubscriptionBinding
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 订阅功能页面
 * 提供年度和月度订阅套餐选择
 */
class SubscriptionActivity : AppCompatActivity(), BillingManager.BillingListener {

    private lateinit var binding: ActivitySubscriptionBinding
    private lateinit var billingManager: BillingManager
    
    private var isYearlySelected = true
    private var isFreeTrialEnabled = true
    
    private var monthlyProduct: ProductDetails? = null
    private var yearlyProduct: ProductDetails? = null
    private var isLoading = false
    private var trialOfferAvailable = false

    /**
     * 🌐 应用语言配置
     * 在 Activity 创建之前应用保存的语言设置
     */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(updateBaseContextLocale(base))
    }

    /**
     * 🌐 更新 Context 的语言配置
     */
    private fun updateBaseContextLocale(context: Context): Context {
        val language = SPAppConfigs.getLocale(context)
        
        android.util.Log.d("SubscriptionActivity", "🌐 Applying language: $language")

        // 只检查空值
        if (language.isNullOrEmpty()) {
            android.util.Log.d("SubscriptionActivity", "⚠️ Language is null or empty, using default")
            return context
        }

        // 🔄 资源目录映射：应用使用 "id"，但 Android 资源使用 "in"
        val resourceLanguage = if (language == "id") "in" else language
        android.util.Log.d("SubscriptionActivity", "📍 Language mapping: app='$language' → resource='$resourceLanguage'")
        
        val locale = Locale(resourceLanguage)
        Locale.setDefault(locale)
        
        // minSdk 26：直接用现代 API(旧的 legacy 方法在 API 24+ 永不执行，已移除)
        return updateResourcesLocale(context, locale)
    }

    /**
     * 🌐 更新资源配置 (Android N+)
     */
    @TargetApi(Build.VERSION_CODES.N_MR1)
    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    /**
     * 🌐 应用配置覆盖 (兼容性处理)
     */
    override fun applyOverrideConfiguration(overrideConfiguration: Configuration?) {
        if (overrideConfiguration != null && Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            val language = SPAppConfigs.getLocale(this)
            if (!language.isNullOrEmpty()) {
                val resourceLanguage = if (language == "id") "in" else language
                val locale = Locale(resourceLanguage)
                overrideConfiguration.setLocale(locale)
            }
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackPressHandler()
        setupBillingManager()
        setupViews()
        setupListeners()

        // 📊 订阅来源打点：记录本次订阅页从哪里打开(定位哪个触发点带来付费)
        subscriptionSource = intent.getStringExtra(EXTRA_SOURCE) ?: "unknown"
        try {
            val params = HashMap<String, Any>()
            params["source"] = subscriptionSource
            com.quran.quranaudio.online.analytics.AnalyticsManager
                .getInstance(this).logEvent("subscription_page_open", params)
        } catch (_: Exception) {
        }
    }

    private var subscriptionSource: String = "unknown"
    
    /**
     * 设置返回键处理
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleClose()
            }
        })
    }

    private fun setupBillingManager() {
        billingManager = BillingManager(this, lifecycleScope)
        billingManager.setBillingListener(this)
        billingManager.initialize()
    }

    private fun setupViews() {
        // 默认选中年度套餐
        binding.radioYearly.isChecked = true
        binding.radioMonthly.isChecked = false
        
        // 免费试用默认关闭
        binding.switchFreeTrial.isChecked = false
        isFreeTrialEnabled = false
        
        updateSubscriptionInfo()
        updateButtonText()
        updateNoPaymentVisibility()
        
        // 显示加载状态
        setLoadingState(true)
    }

    private fun setupListeners() {
        // 关闭按钮
        binding.btnClose.setOnClickListener {
            handleClose()
        }

        // 年度套餐选择
        binding.cardYearlyPlan.setOnClickListener {
            selectYearlyPlan()
        }
        
        binding.radioYearly.setOnClickListener {
            selectYearlyPlan()
        }

        // 月度套餐选择
        binding.cardMonthlyPlan.setOnClickListener {
            selectMonthlyPlan()
        }
        
        binding.radioMonthly.setOnClickListener {
            selectMonthlyPlan()
        }

        // 免费试用开关
        binding.switchFreeTrial.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !trialOfferAvailable) {
                binding.switchFreeTrial.isChecked = false
                Toast.makeText(this, R.string.subscription_trial_unavailable, Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            isFreeTrialEnabled = isChecked
            if (isChecked && isYearlySelected) selectMonthlyPlan(keepTrialSelection = true)
            updateSubscriptionInfo()
            updateButtonText()
            updateNoPaymentVisibility()
        }

        // The whole row is a touch target. This restores the familiar behaviour from the
        // previous paywall and is considerably easier to use than targeting the switch alone.
        binding.freeTrialCard.setOnClickListener {
            // Always toggle; the listener above is the single place that decides eligibility.
            binding.switchFreeTrial.toggle()
        }

        // 订阅按钮
        binding.btnSubscribe.setOnClickListener {
            handleSubscription()
        }

        setupLegalLinks()
    }

    private fun selectYearlyPlan() {
        isYearlySelected = true
        binding.radioYearly.isChecked = true
        binding.radioMonthly.isChecked = false
        
        // 选择年套餐时关闭试用开关
        binding.switchFreeTrial.isChecked = false
        isFreeTrialEnabled = false
        
        updateSubscriptionInfo()
        updateButtonText()
        updateNoPaymentVisibility()
    }

    private fun selectMonthlyPlan(keepTrialSelection: Boolean = false) {
        isYearlySelected = false
        binding.radioYearly.isChecked = false
        binding.radioMonthly.isChecked = true
        
        // Restore the established flow: an eligible monthly plan includes the advertised trial.
        // If the account is not eligible, the same card remains available at its normal price.
        if (!keepTrialSelection) {
            isFreeTrialEnabled = trialOfferAvailable
            binding.switchFreeTrial.isChecked = trialOfferAvailable
        }
        
        updateSubscriptionInfo()
        updateButtonText()
        updateNoPaymentVisibility()
    }

    private fun updateSubscriptionInfo() {
        val infoRes = when {
            isFreeTrialEnabled && !isYearlySelected -> R.string.subscription_info_trial_monthly
            isFreeTrialEnabled && isYearlySelected -> R.string.subscription_info_trial_yearly
            !isFreeTrialEnabled && isYearlySelected -> R.string.subscription_info_autorenew_yearly
            else -> R.string.subscription_info_autorenew_monthly
        }
        binding.tvSubscriptionInfo.setText(infoRes)
        updateDisplayedPlanPrices()
    }

    private fun updateButtonText() {
        binding.btnSubscribe.setText(
            if (isFreeTrialEnabled) R.string.subscription_cta_trial else R.string.subscription_cta_premium
        )
    }

    private fun updateNoPaymentVisibility() {
        if (isFreeTrialEnabled) {
            binding.noPaymentContainer.visibility = View.VISIBLE
        } else {
            binding.noPaymentContainer.visibility = View.GONE
        }
    }

    private fun setLoadingState(loading: Boolean) {
        isLoading = loading
        binding.btnSubscribe.isEnabled = !loading
        
        if (loading) {
            binding.btnSubscribe.setText(R.string.subscription_loading)
            binding.btnSubscribe.alpha = 0.6f
        } else {
            binding.btnSubscribe.alpha = 1.0f
            updateButtonText()
        }
    }

    // ==================== BillingManager 回调 ====================

    override fun onBillingSetupFinished(success: Boolean) {
        lifecycleScope.launch {
            if (success) {
                android.util.Log.d("SubscriptionActivity", "✅ Billing setup successful, querying products...")
                billingManager.querySubscriptionProducts()
            } else {
                android.util.Log.e("SubscriptionActivity", "❌ Billing setup failed")
                setLoadingState(false)
                showDetailedError(
                    getString(R.string.subscription_error_billing_title),
                    getString(R.string.subscription_error_billing_message)
                )
            }
        }
    }

    override fun onProductsLoaded(products: List<ProductDetails>) {
        lifecycleScope.launch {
            android.util.Log.d("SubscriptionActivity", "📦 Loaded ${products.size} products")
            
            products.forEach { product ->
                when (product.productId) {
                    BillingManager.MONTHLY_PLAN_ID -> {
                        monthlyProduct = product
                        updateMonthlyPlanUI(product)
                        android.util.Log.d("SubscriptionActivity", "✅ Monthly product loaded")
                    }
                    BillingManager.YEARLY_PLAN_ID -> {
                        yearlyProduct = product
                        updateYearlyPlanUI(product)
                        android.util.Log.d("SubscriptionActivity", "✅ Yearly product loaded")
                    }
                }
            }
            updateTrialAvailability()
            updateAnnualSavings()

            setLoadingState(false)

            if (products.isEmpty()) {
                android.util.Log.e("SubscriptionActivity", "❌ No products found!")
                // 加载失败：付费按钮保持禁用，避免用户点到没有价格的坏按钮；弹友好重试对话框。
                binding.btnSubscribe.isEnabled = false
                binding.btnSubscribe.alpha = 0.6f
                showDetailedError(
                    getString(R.string.subscription_error_no_products_title),
                    getString(R.string.subscription_error_no_products_message)
                )
            } else {
                android.util.Log.d("SubscriptionActivity", "✅ Products loaded successfully")
                if (monthlyProduct == null) {
                    android.util.Log.w("SubscriptionActivity", "⚠️ Monthly product not found")
                }
                if (yearlyProduct == null) {
                    android.util.Log.w("SubscriptionActivity", "⚠️ Yearly product not found")
                }
            }
        }
    }

    override fun onPurchaseSuccess(purchase: Purchase) {
        lifecycleScope.launch {
            android.util.Log.d("SubscriptionActivity", "🎉 Purchase successful!")

            // 📊 记录付费来源 + 产品，定位哪个触发点真正带来付费转化
            try {
                val params = HashMap<String, Any>()
                params["source"] = subscriptionSource
                params["product_id"] = purchase.products.firstOrNull() ?: "unknown"
                com.quran.quranaudio.online.analytics.AnalyticsManager
                    .getInstance(this@SubscriptionActivity)
                    .logEvent("subscription_purchased", params)
            } catch (_: Exception) {
            }

            Toast.makeText(
                this@SubscriptionActivity,
                getString(R.string.subscription_message_success),
                Toast.LENGTH_LONG
            ).show()
            
            // Never interrupt a successful purchase with an advertisement.
            proceedToMainActivity()
        }
    }

    override fun onPurchaseFailure(errorCode: Int, errorMessage: String) {
        lifecycleScope.launch {
            android.util.Log.e("SubscriptionActivity", "❌ Purchase failed: $errorMessage")
            
            val message = when (errorCode) {
                1 -> getString(R.string.subscription_message_canceled)  // USER_CANCELED
                7 -> getString(R.string.subscription_message_already_owned)  // ITEM_ALREADY_OWNED
                else -> getString(R.string.subscription_message_failed, errorMessage)
            }
            
            Toast.makeText(
                this@SubscriptionActivity,
                message,
                Toast.LENGTH_SHORT
            ).show()
            
            setLoadingState(false)
        }
    }

    override fun onSubscriptionStatusChanged(isSubscribed: Boolean, productId: String?) {
        lifecycleScope.launch {
            android.util.Log.d("SubscriptionActivity", "📊 Subscription status: $isSubscribed, product: $productId")
            
            if (isSubscribed) {
                // 用户已订阅，可以显示特殊UI或直接关闭页面
                Toast.makeText(
                    this@SubscriptionActivity,
                    getString(R.string.subscription_message_already_subscribed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ==================== UI 更新 ====================

    private fun updateMonthlyPlanUI(product: ProductDetails) {
        // 获取价格信息 - 获取实际付费价格（跳过免费试用期）
        val offer = product.subscriptionOfferDetails?.firstOrNull()
        val pricingPhases = offer?.pricingPhases?.pricingPhaseList
        
        // 查找实际付费阶段的价格（通常是第二个phase，第一个是免费试用）
        val paidPhase = pricingPhases?.find { phase ->
            phase.priceAmountMicros > 0
        } ?: pricingPhases?.firstOrNull()
        
        val price = paidPhase?.formattedPrice
        
        if (price != null) {
            // 更新月度套餐价格显示 - 使用占位符格式化字符串
            updateDisplayedPlanPrices()
            android.util.Log.d("SubscriptionActivity", "💰 Monthly price updated: $price")
        }
    }

    private fun updateYearlyPlanUI(product: ProductDetails) {
        val price = paidRecurringPhase(product)?.formattedPrice
        
        if (price != null) {
            // 更新年度套餐价格显示 - 使用占位符格式化字符串
            updateDisplayedPlanPrices()
            android.util.Log.d("SubscriptionActivity", "💰 Yearly price updated: $price")
        }
    }

    /** Always show the exact Play-localized charge for the currently selected terms. */
    private fun updateDisplayedPlanPrices() {
        val monthlyPhase = monthlyProduct?.let(::paidRecurringPhase)
        monthlyPhase?.formattedPrice?.let { price ->
            // 左侧：试用时提示试用条款；非试用时只留「随时取消」，价格交给右侧单价列（避免重复）。
            binding.tvMonthlySubText.text =
                if (isFreeTrialEnabled) getString(R.string.subscription_monthly_sub_text, price)
                else getString(R.string.subscription_cancel_anytime)
            // 右侧：月订阅本身即每月真实价。
            binding.tvMonthlyPermonth.text = price
        }

        val yearlyPhase = yearlyProduct?.let(::paidRecurringPhase)
        yearlyPhase?.formattedPrice?.let { price ->
            // 左侧：真实年费全额（加大醒目）；「随时取消」在下方独立成行（合规：真实计费金额始终完整展示）。
            binding.tvYearlySubText.text = getString(R.string.subscription_amount_per_year, price)
        }
        // 右侧：年费拆算到「每月」，与月订阅同单位直接对比（对比锚点，非计费金额）。
        yearlyPhase?.let { phase ->
            perUnitPrice(phase.priceCurrencyCode, phase.priceAmountMicros, 12)?.let {
                binding.tvYearlyPermonth.text = it
            }
        }
    }

    /**
     * 把整期价格按 Play 返回的币种/金额换算成「每单位」价（如年费÷12=每月），
     * 用系统货币格式化以匹配 Play 的本地化符号与小数位。仅用于对比展示，不用于计费。
     */
    private fun perUnitPrice(currencyCode: String, totalMicros: Long, divisor: Int): String? =
        try {
            val amount = totalMicros.toDouble() / 1_000_000.0 / divisor
            val formatter = java.text.NumberFormat.getCurrencyInstance()
            formatter.currency = java.util.Currency.getInstance(currencyCode)
            formatter.format(amount)
        } catch (e: Exception) {
            null
        }

    /**
     * 订阅页永远展示「原价」：取无限续订相（真实续订价）。合规关键——绝不能把折扣 offer 的
     * 首月/首年折后价泄漏到订阅页醒目位置（那正是被 Play 拒审的原因）。若无无限相则兜底取
     * 价格最高的付费相（折扣价一定更低，取最高即原价）。
     */
    private fun paidRecurringPhase(product: ProductDetails): ProductDetails.PricingPhase? {
        val paid = product.subscriptionOfferDetails
            ?.flatMap { it.pricingPhases.pricingPhaseList }
            ?.filter { it.priceAmountMicros > 0 }
            ?: return null
        return paid.firstOrNull { it.recurrenceMode == 1 }
            ?: paid.maxByOrNull { it.priceAmountMicros }
    }

    private fun updateTrialAvailability() {
        // ProductDetails only contains offers this Play account is eligible for. Do not bind
        // eligibility to a console offerId: migrated/base-plan offers may have a different ID,
        // and Play commonly represents seven days as P1W rather than P7D.
        trialOfferAvailable = monthlyProduct?.let(::eligibleTrialOffer) != null
        // Dump exactly what Play returned so trial eligibility can be told apart from a
        // detection bug: an ineligible account legitimately receives no free-price phase.
        android.util.Log.d("SubscriptionActivity", "🎁 trialOfferAvailable=$trialOfferAvailable")
        listOfNotNull(monthlyProduct, yearlyProduct).forEach { product ->
            android.util.Log.d("SubscriptionActivity", "🎁 product=${product.productId}")
            product.subscriptionOfferDetails?.forEachIndexed { i, offer ->
                android.util.Log.d("SubscriptionActivity", "  offer[$i] basePlanId=${offer.basePlanId} offerId=${offer.offerId} tags=${offer.offerTags}")
                offer.pricingPhases.pricingPhaseList.forEachIndexed { p, phase ->
                    android.util.Log.d("SubscriptionActivity", "    phase[$p] price=${phase.formattedPrice} micros=${phase.priceAmountMicros} period=${phase.billingPeriod} recurrence=${phase.recurrenceMode} cycles=${phase.billingCycleCount}")
                }
            }
        }
        // The trial is a headline selling point, so the row stays on screen for everyone. What
        // changes is the promise: Play only returns offers this account can actually claim, so an
        // ineligible account is told the trial is unavailable rather than shown an offer that
        // would silently bill at full price on purchase.
        binding.freeTrialCard.visibility = View.VISIBLE
        // Stay enabled even when ineligible — a disabled switch swallows the touch and reads as a
        // broken paywall. The guard in the listener explains the situation instead.
        binding.switchFreeTrial.isEnabled = true
        binding.freeTrialCard.isClickable = true
        binding.freeTrialCard.alpha = if (trialOfferAvailable) 0.85f else 0.62f
        binding.tvFreeTrialLabel.setText(
            if (trialOfferAvailable) R.string.subscription_enable_trial
            else R.string.subscription_trial_unavailable_label
        )
        if (!trialOfferAvailable) {
            isFreeTrialEnabled = false
            binding.switchFreeTrial.isChecked = false
            binding.tvMonthlyMainText.setText(R.string.subscription_monthly_plan_label)
            updateSubscriptionInfo()
            updateButtonText()
            updateNoPaymentVisibility()
        } else {
            binding.tvMonthlyMainText.setText(R.string.subscription_free_trial_badge)
        }
    }

    private fun updateAnnualSavings() {
        // 年卡主标题与月卡平级用「Annual plan」，避免左上过挤；省钱感由右侧每月单价对比
        // （HK$6.50/月 vs HK$23.00/月）传达，无需重复文字。
        binding.tvYearlyMainText.setText(R.string.subscription_annual_plan_label)
    }

    private fun setupLegalLinks() {
        val privacy = getString(R.string.privacy_policy_title)
        val terms = getString(R.string.google_play_terms_title)
        val text = "$privacy  •  $terms"
        val spannable = SpannableString(text)
        fun link(label: String, url: String) {
            val start = text.indexOf(label)
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

    private fun handleSubscription() {
        if (isLoading) {
            return
        }

        val selectedProduct = if (isYearlySelected) yearlyProduct else monthlyProduct
        val planName = if (isYearlySelected) {
            getString(R.string.subscription_plan_yearly)
        } else {
            getString(R.string.subscription_plan_monthly)
        }
        
        if (selectedProduct == null) {
            android.util.Log.e("SubscriptionActivity", "❌ Selected product is null: $planName")
            val productId = if (isYearlySelected) BillingManager.YEARLY_PLAN_ID else BillingManager.MONTHLY_PLAN_ID
            showDetailedError(
                getString(R.string.subscription_error_plan_title),
                getString(R.string.subscription_error_plan_message, planName, productId)
            )
            return
        }

        // 获取 offer token
        val offerToken = if (isFreeTrialEnabled && !isYearlySelected) {
            val trialOffer = eligibleTrialOffer(selectedProduct)
            if (trialOffer != null) {
                android.util.Log.d("SubscriptionActivity", "✅ Found trial offer: basePlanId=${trialOffer.basePlanId}, offerId=${trialOffer.offerId}")
                
                // 打印pricing phases信息
                trialOffer.pricingPhases.pricingPhaseList.forEachIndexed { index, phase ->
                    android.util.Log.d("SubscriptionActivity", "  Phase $index: ${phase.formattedPrice}, billingPeriod=${phase.billingPeriod}")
                }
                
                trialOffer.offerToken
            } else {
                android.util.Log.w("SubscriptionActivity", "⚠️ Play returned no eligible seven-day trial offer")
                
                // 打印所有可用的offers用于调试
                selectedProduct.subscriptionOfferDetails?.forEachIndexed { index, offer ->
                    android.util.Log.d("SubscriptionActivity", "  Available offer $index: basePlanId=${offer.basePlanId}, offerId=${offer.offerId}")
                }
                
                // Never replace a promised free trial with a paid base plan.
                Toast.makeText(this, R.string.subscription_trial_unavailable, Toast.LENGTH_LONG).show()
                null
            }
        } else {
            // 年订阅或未启用免费试用：选择基础方案（没有offerId的offer）
            android.util.Log.d("SubscriptionActivity", "📦 Looking for base plan offer (no trial)...")
            
            val basePlanOffer = selectedProduct.subscriptionOfferDetails?.firstOrNull { offer ->
                offer.offerId.isNullOrEmpty() && offer.pricingPhases.pricingPhaseList.none {
                    it.priceAmountMicros == 0L
                }
            } ?: selectedProduct.subscriptionOfferDetails?.firstOrNull { offer ->
                offer.pricingPhases.pricingPhaseList.none { it.priceAmountMicros == 0L }
            }
            
            if (basePlanOffer != null) {
                android.util.Log.d("SubscriptionActivity", "✅ Found base plan: basePlanId=${basePlanOffer.basePlanId}")
                basePlanOffer.offerToken
            } else {
                android.util.Log.w("SubscriptionActivity", "⚠️ No non-trial offer is eligible")
                null
            }
        }

        if (offerToken == null) {
            android.util.Log.e("SubscriptionActivity", "❌ No offer available for product: ${selectedProduct.productId}")
            
            // 打印所有可用的 offers 用于调试
            selectedProduct.subscriptionOfferDetails?.forEachIndexed { index, offer ->
                android.util.Log.d("SubscriptionActivity", "  Offer $index: ${offer.pricingPhases.pricingPhaseList.size} pricing phases")
                offer.pricingPhases.pricingPhaseList.forEachIndexed { phaseIndex, phase ->
                    android.util.Log.d("SubscriptionActivity", "    Phase $phaseIndex: ${phase.formattedPrice} (${phase.priceAmountMicros} micros)")
                }
            }
            
            Toast.makeText(
                this,
                getString(R.string.subscription_message_no_offer),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        android.util.Log.d("SubscriptionActivity", "🚀 Starting purchase flow...")
        setLoadingState(true)
        
        billingManager.launchPurchaseFlow(
            activity = this,
            productDetails = selectedProduct,
            offerToken = offerToken
        )
    }

    private fun eligibleTrialOffer(product: ProductDetails) =
        // Play only returns offers this account is eligible for, so any free phase it hands back
        // is a genuinely claimable trial. Match the console offer ID first (this is the mapping
        // production 1.9.35 shipped), then fall back to any zero-price phase regardless of how
        // Play spells the period — P1W, P7D and localized base-plan variants are all valid.
        product.subscriptionOfferDetails?.firstOrNull { offer ->
            offer.offerId == BillingManager.FREE_TRIAL_OFFER_ID &&
                offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        } ?: product.subscriptionOfferDetails?.firstOrNull { offer ->
            offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        }

    /**
     * 处理关闭按钮和返回键
     */
    private fun handleClose() {
        android.util.Log.d("SubscriptionActivity", "🔙 User closed subscription page")

        // 检查是否来自引导流程
        val fromOnboarding = intent.getBooleanExtra("from_onboarding", false)

        // 先把付费页底下的目标屏（引导流程需先落到主页）安排好，再决定是否叠加折扣挽回页，
        // 这样用户关闭折扣页后回到的是 App 而不是付费页。
        if (fromOnboarding) {
            android.util.Log.d("SubscriptionActivity", "📱 From onboarding, navigating to MainActivity")
            navigateToMainActivity()
        }

        // 💡 折扣挽回：用户主动关闭付费页时，若从未挽回过且 Play 确有折扣 offer，
        // 叠加一次 5 折挽回页（一生只弹一次，功修时段避让）。
        maybeInterceptWithDiscount()

        if (!fromOnboarding) {
            finish()
        }
    }

    /**
     * 关闭付费页时尝试叠加折扣挽回页。
     * 折扣 offer 由 Play 实际下发决定：拿不到就不拦截，绝不显示虚假折扣。
     */
    private fun maybeInterceptWithDiscount() {
        // 按用户关闭时所选方案区分：取消月订阅→月5折挽回页；取消年订阅→年5折挽回页。
        val plan = if (isYearlySelected) DiscountManager.Plan.YEARLY else DiscountManager.Plan.MONTHLY
        val product = if (isYearlySelected) yearlyProduct else monthlyProduct
        val discountOffer = DiscountManager.findDiscountOffer(product, plan)
        val isSubscribed = SubscriptionHelper.isUserSubscribed(this)
        if (!DiscountManager.shouldInterceptClose(this, plan, discountOffer != null, isSubscribed)) {
            return
        }
        // 启动该方案 1 小时窗口（只写一次），随后拉起对应挽回页。
        if (!DiscountManager.startWindow(this, plan)) return
        // 缓存真实折扣百分比供主页角标显示（不硬编码）。
        DiscountManager.computePercent(discountOffer!!)?.let {
            DiscountManager.cacheDiscountPercent(this, plan, it)
        }
        android.util.Log.d("SubscriptionActivity", "💡 Intercepting close with ${plan.key} discount recovery page")
        startActivity(
            Intent(this, DiscountActivity::class.java)
                .putExtra(DiscountActivity.EXTRA_PLAN, plan.key)
        )
    }
    
    /**
     * 导航到主页
     * 🎯 在导航前尝试展示插屏广告（仅未付费用户）
     */
    private fun navigateToMainActivity() {
        android.util.Log.d("SubscriptionActivity", "🏠 Navigating to MainActivity")
        // Closing the onboarding paywall is part of the first-run journey, not an ad break.
        proceedToMainActivity()
    }
    
    /**
     * 执行导航到主页的实际操作
     */
    private fun proceedToMainActivity() {
        android.util.Log.d("SubscriptionActivity", "🚀 Proceeding to MainActivity")
        
        val intent = android.content.Intent(this, com.quran.quranaudio.online.prayertimes.ui.MainActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
    }

    /**
     * 显示详细错误信息（开发调试用）
     */
    private fun showDetailedError(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.strLabelOkay)) { dialog, _ -> dialog.dismiss() }
            .setNegativeButton(getString(R.string.strLabelRetry)) { _, _ ->
                setLoadingState(true)
                billingManager.querySubscriptionProducts()
            }
            .setCancelable(true)
            .show()
    }

    companion object {
        const val FREE_TRIAL_DAYS = 7
        const val EXTRA_SOURCE = "subscription_source"
    }
}
