package com.raiadnan.quranreader.ui.Main.View

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.raiadnan.quranreader.Constant
import com.raiadnan.quranreader.Data.database.entities.Surah
import com.raiadnan.quranreader.DataHolder.ApplicationData
import com.raiadnan.quranreader.R
import com.raiadnan.quranreader.ui.Base.SurahViewModelFactory
import com.raiadnan.quranreader.ui.BaseActivity
import com.raiadnan.quranreader.ui.Main.Adapter.SurahAdapter
import com.raiadnan.quranreader.ui.Main.ViewModel.SurahViewModel
import com.shaheendevelopers.ads.sdk.format.BannerAd
import kotlinx.android.synthetic.main.activity_surah.*


class SurahActivity : BaseActivity(), SurahAdapter.OnClickSurah {

    var bannerAd: BannerAd.Builder? = null
    private lateinit var viewModel: SurahViewModel
    private var previousSate = true
    private lateinit var surahAdapter: SurahAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ApplicationData(this).theme)
            setTheme(R.style.AppThemeDark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surah)

        val imgSurah = findViewById<View>(R.id.back) as ImageView
        imgSurah.setOnClickListener { this@SurahActivity.finish() }

        loadBannerAd();
        setViewModel()
        hideSoftKeyboard()
        setUI()
        setSearch()
        //setTransparentStatusBar()
      //  setNetworkMonitor()

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

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    private fun setSearch() {
        searchEdit.doOnTextChanged { text, _, _, _ ->
            val search = text.toString()
            surahAdapter.filter.filter(search)
        }
    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(
            this,
            SurahViewModelFactory(application)
        ).get(SurahViewModel::class.java)
    }

    private fun hideSoftKeyboard() {
        this.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    private fun setUI() {
        surahAdapter = SurahAdapter()
        setRecycler()
    }

    private fun setRecycler() {
        viewModel.getSurahs()
        viewModel.surahList.observe(this, Observer {
            if (it.isNotEmpty()) {

                surahAdapter.setData(it)
                surahAdapter.setItemClick(this@SurahActivity)
                surahRecycler.adapter = surahAdapter
                surahAdapter.list = it as MutableList<Surah>
                surahAdapter.notifyDataSetChanged()
            }

        })


    }



    override fun onClickSurah(surah: Surah) {
        var intent = Intent(this, AyahActivity::class.java)
        intent.putExtra("surah_number", surah.id)
        startActivity(intent)
    }
}