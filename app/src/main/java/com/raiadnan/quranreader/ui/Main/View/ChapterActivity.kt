package com.raiadnan.quranreader.ui.Main.View

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.raiadnan.quranreader.Constant
import com.raiadnan.quranreader.Data.Repository.ChapterRepository
import com.raiadnan.quranreader.Data.database.QuranDatabase
import com.raiadnan.quranreader.Data.database.entities.Chapter
import com.raiadnan.quranreader.R
import com.raiadnan.quranreader.DataHolder.ApplicationData
import com.raiadnan.quranreader.ui.Base.ChapterViewModelFactory
import com.raiadnan.quranreader.ui.BaseActivity
import com.raiadnan.quranreader.ui.Main.Adapter.ChapterAdapter
import com.raiadnan.quranreader.ui.Main.ViewModel.ChapterViewModel
import com.shaheendevelopers.ads.sdk.format.BannerAd
import dev.kosrat.muslimdata.models.Language
import dev.kosrat.muslimdata.repository.MuslimRepository
import kotlinx.android.synthetic.main.activity_chapter.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class ChapterActivity : BaseActivity(), ChapterAdapter.ItemAction {

    var bannerAd: BannerAd.Builder? = null
    private lateinit var adapter: ChapterAdapter
    private var categorNum: Int? = null
    private var chapterList: List<Chapter>? = null
    private lateinit var viewModel: ChapterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        if (ApplicationData(this).theme)
            setTheme(R.style.AppThemeDark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chapter)

        val imgSurah = findViewById<View>(R.id.back) as ImageView
        imgSurah.setOnClickListener { this@ChapterActivity.finish() }

        categorNum = intent.getIntExtra("category", 1)
        adapter = ChapterAdapter { it1 -> goToItemActivity(it1) }
        setViewModel()
        setUI()
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

    private fun setViewModel() {
        val dao =
            QuranDatabase.getDatabase(application, lifecycleScope, application.resources).surahDao()
        viewModel = ViewModelProvider(
            this,
            ChapterViewModelFactory(application, ChapterRepository(dao))
        ).get(ChapterViewModel::class.java)
        categorNum?.let { viewModel.fetchChapters(it) }
    }

    private fun setUI() {
        rv_chapter.adapter = adapter
        adapter.setItemClickAction(this)
        categorNum?.let {
            if(Locale.getDefault().language.contentEquals("fr")){
              viewModel.chapters.observe(this, androidx.lifecycle.Observer {
                  Timber.d("list in Activity$it")
                  chapterList = it
                  adapter.setFrenchData(it)
              })

            }else{
                getChapters(it)
            }
             }
    }


    private fun goToItemActivity(chapterNum: Int) {
        val intent = Intent(applicationContext, ItemActivity::class.java)
        intent.putExtra("chapter", chapterNum)
        startActivity(intent)
    }

    private fun getChapters(categoryId: Int) {
        lifecycleScope.launch {
            val repository = MuslimRepository(this@ChapterActivity)
            val azkarChapters = repository.getAzkarChapters(Language.EN, categoryId)
            if (azkarChapters != null) {
                adapter.setData(azkarChapters)
            }
            /*Log.i("azkarChapters", "$azkarChapters")
            Log.i("azkarChapters", "${azkarChapters?.size}")*/
        }
    }

    override fun clickItemAction(chapterId: Int) {
        goToItemActivity(chapterId)
    }
}