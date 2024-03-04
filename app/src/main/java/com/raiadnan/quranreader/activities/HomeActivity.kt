@file:Suppress("DEPRECATION")
@file:JvmName("DemoUtils")
package com.raiadnan.quranreader.activities

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Window
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.kayvannj.permission_utils.PermissionUtil.PermissionRequestObject
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.raiadnan.quranreader.*
import com.raiadnan.quranreader.Allah.names99.fragments.Name99Fragment
import com.raiadnan.quranreader.DataHolder.ApplicationData
import com.raiadnan.quranreader.DataHolder.Variable
import com.raiadnan.quranreader.ManageNetworkState.ConnectivityReciever
import com.raiadnan.quranreader.helper.LocaleHelper
import com.raiadnan.quranreader.fragments.SettingFragment
import com.raiadnan.quranreader.fragments.TodayFragment
import com.raiadnan.quranreader.prayertimes.prayers.fragment.PrayerFragment
import com.raiadnan.quranreader.tasbih.fragments.TasbihFragment
import com.raiadnan.quranreader.ui.Base.SurahViewModelFactory
import com.raiadnan.quranreader.ui.BaseActivity
import com.raiadnan.quranreader.ui.Main.ViewModel.SurahViewModel
import com.shaheendevelopers.ads.sdk.format.BannerAd
import com.shaheendevelopers.ads.sdk.format.InterstitialAd
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.d_rate_design.*
import java.util.*


@Suppress("DEPRECATION")
class HomeActivity : BaseActivity(), ConnectivityReciever.ConnectivityReceiverListener  {

    @JvmField
    var mRequestObject: PermissionRequestObject? = null

    lateinit var drawer: DrawerLayout
    lateinit var applicationData: ApplicationData
    private lateinit var viewModel: SurahViewModel
    private var isPermissionGranted = false
    var bannerAd: BannerAd.Builder? = null
    var interstitialAd: InterstitialAd.Builder? = null

    private final var TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        applicationData = ApplicationData(this)
        if (applicationData.theme)
            setTheme(R.style.AppThemeDark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        isStoragePermissionGranted()
        //loadBannerAd()
        loadInterstitialAd();

        val todayFragment = TodayFragment()
        val  prayerFragment = PrayerFragment()
        val name99Fragment = Name99Fragment()

        replaceFragment(TodayFragment())

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_nav)
        bottomNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        setViewModel()
    }

    private fun loadBannerAd() {
        bannerAd = BannerAd.Builder(this)
            .setAdStatus(Constant.AD_STATUS)
            .setAdNetwork(Constant.AD_NETWORK)
            .setBackupAdNetwork(Constant.BACKUP_AD_NETWORK)
            .setAdMobBannerId(Constant.ADMOB_BANNER_ID)
            .setGoogleAdManagerBannerId(Constant.GOOGLE_AD_MANAGER_BANNER_ID)
            .setFanBannerId(Constant.FAN_BANNER_ID)
            .setUnityBannerId(Constant.UNITY_BANNER_ID)
            .setAppLovinBannerId(Constant.APPLOVIN_BANNER_ID)
            .setAppLovinBannerZoneId(Constant.APPLOVIN_BANNER_ZONE_ID)
            .setDarkTheme(false)
            .build()
    }

    private fun loadInterstitialAd() {
        interstitialAd = InterstitialAd.Builder(this)
            .setAdStatus(Constant.AD_STATUS)
            .setAdNetwork(Constant.AD_NETWORK)
            .setBackupAdNetwork(Constant.BACKUP_AD_NETWORK)
            .setAdMobInterstitialId(Constant.ADMOB_INTERSTITIAL_ID)
            .setGoogleAdManagerInterstitialId(Constant.GOOGLE_AD_MANAGER_INTERSTITIAL_ID)
            .setFanInterstitialId(Constant.FAN_INTERSTITIAL_ID)
            .setUnityInterstitialId(Constant.UNITY_INTERSTITIAL_ID)
            .setAppLovinInterstitialId(Constant.APPLOVIN_INTERSTITIAL_ID)
            .setAppLovinInterstitialZoneId(Constant.APPLOVIN_INTERSTITIAL_ZONE_ID)
            .setInterval(1)
            .build()
    }

    fun showInterstitialAd() {
        interstitialAd!!.show()
    }


    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val fragment: Fragment? = null
        when (item.itemId) {
            R.id.nav_home -> {
                val mFragment = TodayFragment.newInstance()
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_namaz -> {
                val mFragment = PrayerFragment.newInstance()
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }

            R.id.nav_name_99 -> {
               // startActivity(Intent(this, AllahNameActivity::class.java))
              val mFragment = Name99Fragment.newInstance()
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }

            R.id.nav_tasbih -> {
              val mFragment = TasbihFragment.newInstance()
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_settings -> {
                val mFragment = SettingFragment.newInstance()
                replaceFragment(mFragment)
                return@OnNavigationItemSelectedListener true
            }
        }
        true
    }

private fun replaceFragment(fragment: Fragment) {
    if (fragment!=null){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.home_host_fragment,fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
    //fetch Surah
    private fun setViewModel() {
        viewModel = ViewModelProvider(
            this,
            SurahViewModelFactory(application)
        ).get(SurahViewModel::class.java)

        viewModel.getSurahs()
        viewModel.surahList.observe(this, androidx.lifecycle.Observer {

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            if (data!!.getStringExtra("THEME") == "CHANGE")
                reCreate()
            else if (data.getStringExtra("LAYOUT") == "CHANGE") {
                val rec = Intent(this, HomeActivity::class.java)
                Variable.recreate = true
                intent.putExtra("SESSION", true)
                val loop = intent.getStringExtra("0")!!.toInt()
                rec.putExtra("0", loop.toString())
                for (x in 1..loop) rec.putExtra("$x", intent.getStringExtra("$x"))
                startActivity(rec)
                finish()
            }
        }
    }

    private fun reCreate() {
        Handler().postDelayed({
            val rec = Intent(this, HomeActivity::class.java)
            rec.putExtra("SESSION", true)
            Variable.recreate = true
            startActivity(rec)
            Objects.requireNonNull(overridePendingTransition(
                R.anim.fade_in,
                R.anim.fade_out
            ))
            finish()
        },200)
    }


    //HeaderEnd

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                isPermissionGranted = true
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            // Log.v(FragmentActivity.TAG, "Permission is granted")
            isPermissionGranted = true
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionRequestObject = mRequestObject
        permissionRequestObject?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

        }
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {}

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }


    override fun onBackPressed() {

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.custom_dialog_ad_exit)
        val yesBtn = dialog.findViewById(R.id.btn_exitdialog_yes) as Button
        val noBtn = dialog.findViewById(R.id.btn_exitdialog_no) as Button

        yesBtn.setOnClickListener {
            System.exit(0)
        }
        noBtn.setOnClickListener { dialog.dismiss() }
        dialog.show()

    }
}