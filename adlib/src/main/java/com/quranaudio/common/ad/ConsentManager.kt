package com.quranaudio.common.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

fun interface ConsentResultCallback {
    fun onConsentResult(canRequestAds: Boolean)
}

/** Coordinates Google UMP so no ad request is sent before consent is ready. */
object ConsentManager {
    private const val TAG = "ConsentManager"

    private lateinit var consentInformation: ConsentInformation
    private var appContext: Context? = null
    private var requestInFlight = false
    private var requestCompleted = false
    private val callbacks = mutableListOf<(Boolean) -> Unit>()

    /**
     * 只记录 applicationContext，不触碰 UMP SDK。
     *
     * 原实现在这里直接调 [UserMessagingPlatform.getConsentInformation]，该调用会触发
     * consent_sdk 的类加载与偏好读取，在低端机（Itel / Infinix / Tecno）上耗时可达数秒。
     * 而本方法是被 AdFactory.init() 从 Application.onCreate 同步调用的——进程被
     * 祈祷闹钟 / Widget 刷新 / FCM 在后台拉起时，会直接阻塞主线程造成后台 ANR
     * （Crashlytics 堆栈：main runnable → UserMessagingPlatform.a → ConsentManager
     * → AdFactory → App.onCreate）。
     *
     * 真正的 ConsentInformation 改为首次需要时才创建（见 [ensureConsentInformation]）。
     * 所有需要它的路径（gatherConsent / canRequestAds / 隐私选项）都只在前台发生，
     * 因此前台的行为和耗时与改动前完全一致，只是从 onCreate 挪到了 Splash 的
     * gatherConsent 调用点——仍在任何广告请求之前。
     */
    fun initialize(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    /**
     * 真正创建 ConsentInformation。仅在需要用到同意状态时调用。
     * 若尚未 [initialize] 过（拿不到 Context），返回 null，调用方按「不可请求广告」处理。
     */
    private fun ensureConsentInformation(context: Context? = null): ConsentInformation? {
        if (::consentInformation.isInitialized) return consentInformation
        val ctx = context?.applicationContext ?: appContext ?: return null
        consentInformation = UserMessagingPlatform.getConsentInformation(ctx)
        return consentInformation
    }

    fun canRequestAds(): Boolean =
        ensureConsentInformation()?.canRequestAds() ?: false

    /** Must be called once on every process launch with the foreground Activity. */
    @JvmStatic
    fun gatherConsent(activity: Activity, callback: (Boolean) -> Unit) {
        initialize(activity.applicationContext)

        val info = ensureConsentInformation(activity.applicationContext)
        if (info == null) {
            Log.w(TAG, "ConsentInformation unavailable; treating as cannot request ads")
            callback(false)
            return
        }

        if (requestCompleted) {
            callback(info.canRequestAds())
            return
        }

        callbacks += callback
        if (requestInFlight) return
        requestInFlight = true

        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error ${formError.errorCode}: ${formError.message}")
                    }
                    finishRequest()
                }
            },
            { requestError ->
                Log.w(TAG, "Consent update error ${requestError.errorCode}: ${requestError.message}")
                // A valid choice from a previous session may still allow requests.
                finishRequest()
            }
        )
    }

    private fun finishRequest() {
        requestInFlight = false
        requestCompleted = true
        val allowed = ensureConsentInformation()?.canRequestAds() ?: false
        Log.d(TAG, "Consent flow complete; canRequestAds=$allowed")
        val pending = callbacks.toList()
        callbacks.clear()
        pending.forEach { it(allowed) }
    }

    @JvmStatic
    fun isPrivacyOptionsRequired(): Boolean =
        ensureConsentInformation()?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    @JvmStatic
    fun showPrivacyOptions(activity: Activity, callback: (Boolean) -> Unit = {}) {
        initialize(activity.applicationContext)
        val info = ensureConsentInformation(activity.applicationContext)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.w(TAG, "Privacy options error ${error.errorCode}: ${error.message}")
            }
            callback(info?.canRequestAds() ?: false)
        }
    }
}
