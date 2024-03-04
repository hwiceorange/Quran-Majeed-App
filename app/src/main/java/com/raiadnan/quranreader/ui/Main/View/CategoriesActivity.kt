package com.raiadnan.quranreader.ui.Main.View

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.raiadnan.quranreader.Constant
import com.raiadnan.quranreader.R
import com.raiadnan.quranreader.DataHolder.ApplicationData
import com.raiadnan.quranreader.ui.BaseActivity
import com.shaheendevelopers.ads.sdk.format.BannerAd
import kotlinx.android.synthetic.main.activity_categories.*

class CategoriesActivity : BaseActivity() {

    var bannerAd: BannerAd.Builder? = null


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (ApplicationData(this).theme)
            setTheme(R.style.AppThemeDark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)
        val imgSurah = findViewById<View>(R.id.back) as ImageView
        imgSurah.setOnClickListener { this@CategoriesActivity.finish() }
        setClickListeners()
        loadBannerAd()
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

    private fun setClickListeners() {
        cardMorning.setOnClickListener(View.OnClickListener {
            goToChapterActivity(1)
        })
        cardHome.setOnClickListener(View.OnClickListener {
            goToChapterActivity(2)
        })
        cardFood.setOnClickListener(View.OnClickListener {
            goToChapterActivity(3)
        })
        cardJoy.setOnClickListener(View.OnClickListener {
            goToChapterActivity(4)
        })
        cardTravel.setOnClickListener(View.OnClickListener {
            goToChapterActivity(5)
        })
        cardPrayer.setOnClickListener(View.OnClickListener {
            goToChapterActivity(6)
        })
        cardPraising.setOnClickListener(View.OnClickListener {
            goToChapterActivity(7)
        })
        cardHajj.setOnClickListener(View.OnClickListener {
            goToChapterActivity(8)
        })
        cardEtiquette.setOnClickListener(View.OnClickListener {
            goToChapterActivity(9)
        })
        cardNature.setOnClickListener(View.OnClickListener {
            goToChapterActivity(10)
        })
        cardSickness.setOnClickListener(View.OnClickListener {
            goToChapterActivity(11)
        })

    }

    private fun goToChapterActivity(categoryNum:Int){
        val intent = Intent(this,ChapterActivity::class.java)
        intent.putExtra("category",categoryNum)
        startActivity(intent)
    }
}