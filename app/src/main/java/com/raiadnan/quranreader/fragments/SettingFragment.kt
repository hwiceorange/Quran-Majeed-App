package com.raiadnan.quranreader.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.raiadnan.quranreader.R
import com.raiadnan.quranreader.prayertimes.App
import com.raiadnan.quranreader.prayertimes.locations.fragment.LocationSettingFragment
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave
import com.raiadnan.quranreader.prayertimes.prayers.fragment.PrayerTimeSettingFragment
import com.raiadnan.quranreader.ui.Config.Author_mail


class SettingFragment : BaseFragment() {
    private var settingCallback: App.SimpleCallback? = null
    override fun getLayoutId(): Int {
        return R.layout.fragment_setting
    }

    fun setSettingCallback(simpleCallback: App.SimpleCallback?) {
        settingCallback = simpleCallback
    }

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val newInstance = PrayerTimeSettingFragment.newInstance()
        newInstance.setCallback(settingCallback)

        view.findViewById<View>(R.id.share_app).setOnClickListener { share() }

        view.findViewById<View>(R.id.rate_us).setOnClickListener { rateApp() }

        view.findViewById<View>(R.id.policy).setOnClickListener {
            val browserIntent =
                Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)))
            startActivity(browserIntent)
        }

        view.findViewById<View>(R.id.contact_us).setOnClickListener {
            startActivity(
                Intent.createChooser(
                    Intent(
                        Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto",
                            Author_mail, null
                        )
                    ), "Send email...")
            )
        }


        view.findViewById<View>(R.id.bt_prayer_setting).setOnClickListener {
            val fragment: Fragment = PrayerTimeSettingFragment()
            val fragmentManager = fragmentManager
            fragmentManager!!.beginTransaction().replace(R.id.home_host_fragment, fragment).commit()
        }
        val textView = view.findViewById<View>(R.id.tv_city) as TextView
        textView.text = LocationSave.getCity()
        val newInstance2 = LocationSettingFragment.newInstance()
        newInstance2.setCallback {
            textView.text = LocationSave.getCity()
            settingCallback!!.callback(Integer.valueOf(LOCATION_CHANGE))
        }
        view.findViewById<View>(R.id.bt_location_setting).setOnClickListener {
            val fragment: Fragment = LocationSettingFragment()
            val fragmentManager = fragmentManager
            fragmentManager!!.beginTransaction().replace(R.id.home_host_fragment, fragment).commit()
            //     BaseFragment.addFragment(SettingFragment.this.activity, LocationSettingFragment.newInstance());
        }
    }
    private fun share() {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Writer")
            var shareMessage = "\nLet me recommend you this application\n\n"
            shareMessage = ("${shareMessage}https://play.google.com/store/apps/details?id="
                    + com.raiadnan.quranreader.BuildConfig.APPLICATION_ID).trimIndent()
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            startActivity(Intent.createChooser(shareIntent, "choose one"))
        } catch (e: java.lang.Exception) {
            //e.toString();
        }
    }

    fun rateApp() {
        val uri = Uri.parse("market://details?id=" + requireContext().getPackageName())
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + requireContext().getPackageName())
                )
            )
        }
    }

    companion object {
        @JvmField
        var LOCATION_CHANGE = 1
        @JvmField
        var PRAYER_CHANGE = 0
        fun newInstance(): SettingFragment {
            val bundle = Bundle()
            val settingFragment = SettingFragment()
            settingFragment.arguments = bundle
            return settingFragment
        }
    }
}