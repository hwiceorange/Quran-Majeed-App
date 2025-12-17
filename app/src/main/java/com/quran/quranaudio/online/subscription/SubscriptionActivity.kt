package com.quran.quranaudio.online.subscription

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
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
        
        return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            updateResourcesLocale(context, locale)
        } else {
            updateResourcesLocaleLegacy(context, locale)
        }
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
     * 🌐 更新资源配置 (旧版本 Android)
     */
    @Suppress("DEPRECATION")
    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
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
    }
    
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
            isFreeTrialEnabled = isChecked
            
            // 开启试用时自动切换到月套餐，关闭时切换回年套餐
            if (isChecked) {
                selectMonthlyPlan()
            } else {
                selectYearlyPlan()
            }
            
            updateSubscriptionInfo()
            updateButtonText()
            updateNoPaymentVisibility()
        }

        // 订阅按钮
        binding.btnSubscribe.setOnClickListener {
            handleSubscription()
        }
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

    private fun selectMonthlyPlan() {
        isYearlySelected = false
        binding.radioYearly.isChecked = false
        binding.radioMonthly.isChecked = true
        
        // 选择月套餐时开启试用开关
        binding.switchFreeTrial.isChecked = true
        isFreeTrialEnabled = true
        
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
            
            setLoadingState(false)
            
            if (products.isEmpty()) {
                android.util.Log.e("SubscriptionActivity", "❌ No products found!")
                showDetailedError(
                    getString(R.string.subscription_error_no_products_title),
                    getString(
                        R.string.subscription_error_no_products_message,
                        BillingManager.MONTHLY_PLAN_ID,
                        BillingManager.YEARLY_PLAN_ID
                    )
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
            Toast.makeText(
                this@SubscriptionActivity,
                getString(R.string.subscription_message_success),
                Toast.LENGTH_LONG
            ).show()
            
            // 订阅成功后导航到主页
            navigateToMainActivity()
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
            val subText = getString(R.string.subscription_monthly_sub_text, price)
            binding.tvMonthlySubText.text = subText
            android.util.Log.d("SubscriptionActivity", "💰 Monthly price updated: $subText")
        }
    }

    private fun updateYearlyPlanUI(product: ProductDetails) {
        // 获取价格信息
        val offer = product.subscriptionOfferDetails?.firstOrNull()
        val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
        
        if (price != null) {
            // 更新年度套餐价格显示 - 使用占位符格式化字符串
            val subText = getString(R.string.subscription_yearly_sub_text, price)
            binding.tvYearlySubText.text = subText
            android.util.Log.d("SubscriptionActivity", "💰 Yearly price updated: $subText")
        }
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
            // 🔑 月订阅的免费试用：通过特定的 offer ID 来查找
            android.util.Log.d("SubscriptionActivity", "🔍 Looking for trial offer with ID '${BillingManager.FREE_TRIAL_OFFER_ID}'...")
            
            val trialOffer = selectedProduct.subscriptionOfferDetails?.firstOrNull { offer ->
                // 查找特定的免费试用优惠 ID
                val hasTrialOfferId = offer.offerId == BillingManager.FREE_TRIAL_OFFER_ID
                
                android.util.Log.d("SubscriptionActivity", "  Checking offer: basePlanId=${offer.basePlanId}, offerId=${offer.offerId}")
                
                hasTrialOfferId
            }
            
            if (trialOffer != null) {
                android.util.Log.d("SubscriptionActivity", "✅ Found trial offer: basePlanId=${trialOffer.basePlanId}, offerId=${trialOffer.offerId}")
                
                // 打印pricing phases信息
                trialOffer.pricingPhases.pricingPhaseList.forEachIndexed { index, phase ->
                    android.util.Log.d("SubscriptionActivity", "  Phase $index: ${phase.formattedPrice}, billingPeriod=${phase.billingPeriod}")
                }
                
                trialOffer.offerToken
            } else {
                android.util.Log.w("SubscriptionActivity", "⚠️ No trial offer with ID '${BillingManager.FREE_TRIAL_OFFER_ID}' found")
                
                // 打印所有可用的offers用于调试
                selectedProduct.subscriptionOfferDetails?.forEachIndexed { index, offer ->
                    android.util.Log.d("SubscriptionActivity", "  Available offer $index: basePlanId=${offer.basePlanId}, offerId=${offer.offerId}")
                }
                
                // 回退到基础方案（没有offerId的offer）
                val basePlanOffer = selectedProduct.subscriptionOfferDetails?.firstOrNull { offer ->
                    offer.offerId.isNullOrEmpty()
                }
                
                if (basePlanOffer != null) {
                    android.util.Log.d("SubscriptionActivity", "📦 Using base plan offer: basePlanId=${basePlanOffer.basePlanId}")
                    basePlanOffer.offerToken
                } else {
                    android.util.Log.w("SubscriptionActivity", "⚠️ No base plan offer found, using first available")
                    selectedProduct.subscriptionOfferDetails?.firstOrNull()?.offerToken
                }
            }
        } else {
            // 年订阅或未启用免费试用：选择基础方案（没有offerId的offer）
            android.util.Log.d("SubscriptionActivity", "📦 Looking for base plan offer (no trial)...")
            
            val basePlanOffer = selectedProduct.subscriptionOfferDetails?.firstOrNull { offer ->
                offer.offerId.isNullOrEmpty()
            }
            
            if (basePlanOffer != null) {
                android.util.Log.d("SubscriptionActivity", "✅ Found base plan: basePlanId=${basePlanOffer.basePlanId}")
                basePlanOffer.offerToken
            } else {
                android.util.Log.w("SubscriptionActivity", "⚠️ No base plan found, using first available")
                selectedProduct.subscriptionOfferDetails?.firstOrNull()?.offerToken
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

    /**
     * 处理关闭按钮和返回键
     */
    private fun handleClose() {
        android.util.Log.d("SubscriptionActivity", "🔙 User closed subscription page")
        
        // 检查是否来自引导流程
        val fromOnboarding = intent.getBooleanExtra("from_onboarding", false)
        
        if (fromOnboarding) {
            // 来自引导流程，导航到主页
            android.util.Log.d("SubscriptionActivity", "📱 From onboarding, navigating to MainActivity")
            navigateToMainActivity()
        } else {
            // 普通页面打开，直接关闭
            android.util.Log.d("SubscriptionActivity", "❌ Normal close, finishing activity")
            finish()
        }
    }
    
    /**
     * 导航到主页
     * 🎯 在导航前尝试展示插屏广告（仅未付费用户）
     */
    private fun navigateToMainActivity() {
        android.util.Log.d("SubscriptionActivity", "🏠 Navigating to MainActivity")
        
        // 🎯 Check if this is from onboarding flow (only show ad in onboarding flow)
        val fromOnboarding = intent.getBooleanExtra("from_onboarding", false)
        
        if (fromOnboarding) {
            android.util.Log.d("SubscriptionActivity", "🎯 From onboarding - attempting to show interstitial ad")
            
            // Try to show interstitial ad with callback (handles subscription check internally)
            val adShown = com.quranaudio.common.ad.InterstitialAdManager.getInstance().showAdIfAvailable(this) {
                // This callback is invoked when user dismisses the ad or ad fails to show
                android.util.Log.d("SubscriptionActivity", "✅ Ad closed by user, proceeding to MainActivity")
                proceedToMainActivity()
            }
            
            if (!adShown) {
                // No ad available or user subscribed - navigate immediately
                android.util.Log.d("SubscriptionActivity", "⚠️ No ad shown (subscribed or unavailable), navigating immediately")
                proceedToMainActivity()
            } else {
                android.util.Log.d("SubscriptionActivity", "✅ Interstitial ad shown, waiting for user to close it")
            }
        } else {
            // Not from onboarding - navigate directly without ad
            android.util.Log.d("SubscriptionActivity", "📱 Not from onboarding, navigating directly")
            proceedToMainActivity()
        }
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
    }
}
