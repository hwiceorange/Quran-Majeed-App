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
        logSubscription("page_open", result = "shown")
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
            logSubscription(
                "trial_toggle",
                plan = "monthly",
                offer = if (isChecked) "free" else "base",
                result = if (isChecked) "on" else "off"
            )
            updateSubscriptionInfo()
            updateButtonText()
            updateNoPaymentVisibility()
            updateCheckoutAvailability()
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
        updateCheckoutAvailability()
        logSubscription("plan_select", plan = "yearly", offer = "base", result = "selected")
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
        updateCheckoutAvailability()
        logSubscription(
            "plan_select",
            plan = "monthly",
            offer = if (isFreeTrialEnabled) "free" else "base",
            result = "selected"
        )
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
        
        if (loading) {
            binding.btnSubscribe.isEnabled = false
            binding.btnSubscribe.setText(R.string.subscription_loading)
            binding.btnSubscribe.alpha = 0.6f
        } else {
            updateButtonText()
            updateCheckoutAvailability()
        }
    }

    /**
     * 基础方案缺失时失败关闭：不允许拿 free/off 或其他促销 Token 代替普通购买。
     * 这同时保证标准页价格和 Google Play 购物车来自同一个基础方案。
     */
    private fun updateCheckoutAvailability() {
        if (isLoading) return
        val product = if (isYearlySelected) yearlyProduct else monthlyProduct
        val offerAvailable = if (isFreeTrialEnabled && !isYearlySelected) {
            product?.let(::eligibleTrialOffer) != null
        } else {
            product?.let(::basePlanOffer) != null
        }
        binding.btnSubscribe.isEnabled = offerAvailable
        binding.btnSubscribe.alpha = if (offerAvailable) 1.0f else 0.6f
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

            val yearlyBaseAvailable = yearlyProduct?.let(::basePlanOffer) != null
            val monthlyBaseAvailable = monthlyProduct?.let(::basePlanOffer) != null
            binding.cardYearlyPlan.alpha = if (yearlyBaseAvailable) 0.85f else 0.45f
            binding.cardYearlyPlan.isEnabled = yearlyBaseAvailable
            binding.radioYearly.isEnabled = yearlyBaseAvailable
            binding.cardMonthlyPlan.alpha = if (monthlyBaseAvailable) 0.85f else 0.45f
            binding.cardMonthlyPlan.isEnabled = monthlyBaseAvailable
            binding.radioMonthly.isEnabled = monthlyBaseAvailable

            setLoadingState(false)

            if (products.isEmpty() || (!yearlyBaseAvailable && !monthlyBaseAvailable)) {
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
            logSubscription(
                "purchase_result",
                plan = if (isYearlySelected) "yearly" else "monthly",
                offer = if (isFreeTrialEnabled) "free" else "base",
                result = "success"
            )

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
            logSubscription(
                "purchase_result",
                plan = if (isYearlySelected) "yearly" else "monthly",
                offer = if (isFreeTrialEnabled) "free" else "base",
                result = if (errorCode == 1) "cancel" else "error_$errorCode"
            )
            
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
        val price = basePlanRecurringPhase(product)?.formattedPrice
        
        if (price != null) {
            // 更新月度套餐价格显示 - 使用占位符格式化字符串
            updateDisplayedPlanPrices()
            android.util.Log.d("SubscriptionActivity", "💰 Monthly price updated: $price")
        }
    }

    private fun updateYearlyPlanUI(product: ProductDetails) {
        val price = basePlanRecurringPhase(product)?.formattedPrice
        
        if (price != null) {
            // 更新年度套餐价格显示 - 使用占位符格式化字符串
            updateDisplayedPlanPrices()
            android.util.Log.d("SubscriptionActivity", "💰 Yearly price updated: $price")
        }
    }

    /** Always show the exact Play-localized charge for the currently selected terms. */
    private fun updateDisplayedPlanPrices() {
        val monthlyPhase = monthlyProduct?.let(::basePlanRecurringPhase)
        monthlyPhase?.formattedPrice?.let { price ->
            // 左侧：试用时提示试用条款；非试用时只留「随时取消」，价格交给右侧单价列（避免重复）。
            binding.tvMonthlySubText.text =
                if (isFreeTrialEnabled) getString(R.string.subscription_monthly_sub_text, price)
                else getString(R.string.subscription_cancel_anytime)
            // 右侧：月订阅本身即每月真实价。
            binding.tvMonthlyPermonth.text = price
        }

        val yearlyPhase = yearlyProduct?.let(::basePlanRecurringPhase)
        yearlyPhase?.formattedPrice?.let { price ->
            // 实际一次收取的完整年费是主视觉；月均换算只作为次要比较信息。
            binding.tvYearlySubText.visibility = View.VISIBLE
            binding.tvYearlySubText.text = getString(R.string.subscription_amount_per_year, price)
            perUnitPrice(yearlyPhase.priceCurrencyCode, yearlyPhase.priceAmountMicros, 12)?.let {
                binding.tvYearlyPermonth.text = it
            }
        }
    }

    /** 仅用于弱化的辅助月均价；实际扣款金额始终取上方完整年费。 */
    private fun perUnitPrice(currencyCode: String, totalMicros: Long, divisor: Int): String? =
        try {
            val amount = totalMicros.toDouble() / 1_000_000.0 / divisor
            java.text.NumberFormat.getCurrencyInstance().apply {
                currency = java.util.Currency.getInstance(currencyCode)
            }.format(amount)
        } catch (_: Exception) {
            null
        }

    /**
     * 标准订阅页只读取基础方案（offerId == null）的无限续订价格。
     * 该基础方案的 offerToken 同时用于普通购买，保证页面醒目价格与 Play 购物车恒等。
     */
    private fun basePlanOffer(product: ProductDetails) =
        product.subscriptionOfferDetails?.firstOrNull { offer ->
            offer.offerId.isNullOrEmpty() && offer.pricingPhases.pricingPhaseList.none {
                it.priceAmountMicros == 0L
            }
        }

    private fun basePlanRecurringPhase(product: ProductDetails): ProductDetails.PricingPhase? =
        basePlanOffer(product)?.pricingPhases?.pricingPhaseList?.firstOrNull {
            it.priceAmountMicros > 0L && it.recurrenceMode == 1
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
            
            val basePlanOffer = basePlanOffer(selectedProduct)
            
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
        logSubscription(
            "checkout_start",
            plan = if (isYearlySelected) "yearly" else "monthly",
            offer = if (isFreeTrialEnabled) "free" else "base",
            result = "launch"
        )
        setLoadingState(true)
        
        billingManager.launchPurchaseFlow(
            activity = this,
            productDetails = selectedProduct,
            offerToken = offerToken
        )
    }

    private fun eligibleTrialOffer(product: ProductDetails) =
        // 免费试用必须精确绑定 Play Console 中配置的 "free" offer。
        // 不按“任意零价阶段”兜底，避免未来新增其他促销后误提交错误的 offerToken。
        // Play 会把 P1W/P7D 等实际试用周期放在 pricing phase 中；许可证测试账号
        // 的结算页统一把免费试用压缩为约 3 分钟，这不改变正式用户的 7 天配置。
        product.subscriptionOfferDetails?.firstOrNull { offer ->
            offer.offerId == BillingManager.FREE_TRIAL_OFFER_ID &&
                offer.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
        }

    /**
     * 处理关闭按钮和返回键
     */
    private fun handleClose() {
        android.util.Log.d("SubscriptionActivity", "🔙 User closed subscription page")
        logSubscription(
            "page_close",
            plan = if (isYearlySelected) "yearly" else "monthly",
            offer = if (isFreeTrialEnabled) "free" else "base",
            result = "user_close"
        )

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
        logSubscription("discount_offer", plan.key, "off", "shown")
        startActivity(
            Intent(this, DiscountActivity::class.java)
                .putExtra(DiscountActivity.EXTRA_PLAN, plan.key)
        )
    }

    private fun logSubscription(
        stage: String,
        plan: String = "none",
        offer: String = "none",
        result: String = "none"
    ) {
        try {
            com.quran.quranaudio.online.analytics.RetentionFunnel.subscription(
                this, stage, subscriptionSource, plan, offer, result
            )
        } catch (_: Throwable) {
        }
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
