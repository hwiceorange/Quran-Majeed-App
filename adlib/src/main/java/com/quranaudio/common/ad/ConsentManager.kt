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
    private var requestInFlight = false
    private var requestCompleted = false
    private val callbacks = mutableListOf<(Boolean) -> Unit>()

    fun initialize(context: Context) {
        if (!::consentInformation.isInitialized) {
            consentInformation = UserMessagingPlatform.getConsentInformation(context)
        }
    }

    fun canRequestAds(): Boolean =
        ::consentInformation.isInitialized && consentInformation.canRequestAds()

    /** Must be called once on every process launch with the foreground Activity. */
    @JvmStatic
    fun gatherConsent(activity: Activity, callback: (Boolean) -> Unit) {
        initialize(activity.applicationContext)

        if (requestCompleted) {
            callback(consentInformation.canRequestAds())
            return
        }

        callbacks += callback
        if (requestInFlight) return
        requestInFlight = true

        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
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
        val allowed = consentInformation.canRequestAds()
        Log.d(TAG, "Consent flow complete; canRequestAds=$allowed")
        val pending = callbacks.toList()
        callbacks.clear()
        pending.forEach { it(allowed) }
    }

    @JvmStatic
    fun isPrivacyOptionsRequired(): Boolean =
        ::consentInformation.isInitialized &&
            consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    @JvmStatic
    fun showPrivacyOptions(activity: Activity, callback: (Boolean) -> Unit = {}) {
        initialize(activity.applicationContext)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.w(TAG, "Privacy options error ${error.errorCode}: ${error.message}")
            }
            callback(consentInformation.canRequestAds())
        }
    }
}
