package com.raiadnan.quranreader.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.raiadnan.quranreader.Constant
import com.raiadnan.quranreader.R
import com.raiadnan.quranreader.activities.CalendarActivity
import com.raiadnan.quranreader.activities.HomeActivity
import com.raiadnan.quranreader.activities.LiveActivity
import com.raiadnan.quranreader.compass.QiblaDirectionActivity
import com.raiadnan.quranreader.hadith.HadithActivity
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave
import com.raiadnan.quranreader.prayertimes.prayers.fragment.PrayerFragment
import com.raiadnan.quranreader.prayertimes.prayers.helper.*
import com.raiadnan.quranreader.prayertimes.utils.Utils
import com.raiadnan.quranreader.sixteenlines.SixteenLines_MainPage
import com.raiadnan.quranreader.ui.Main.View.CategoriesActivity
import com.raiadnan.quranreader.ui.Main.View.SurahActivity
import com.shaheendevelopers.ads.sdk.format.BannerAd
import com.shaheendevelopers.ads.sdk.format.InterstitialAd
import kotlinx.android.synthetic.main.fragment_today.*
import java.text.SimpleDateFormat
import java.util.*


class TodayFragment : BaseFragment(), View.OnClickListener {

    var bannerAd: BannerAd.Builder? = null
    var interstitialAd: InterstitialAd.Builder? = null

    var activity: HomeActivity? = null
    private var count: CountDownTimer? = null
    private var imgNotification: ImageView? = null
    private var nextPostition = 0
    var time: Long = 0
    private var tvCity: TextView? = null
    private var tvDate: TextView? = null
    private var tvDateMuslim: TextView? = null
    private var tvNext: TextView? = null
    var tvTimeDown: TextView? = null
    private var tvTimeNext: TextView? = null


    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        this.activity = getActivity() as HomeActivity?
        initView()
        setOnClick(view)
        imageGreet
        loadBannerAd();
        loadInterstitialAd();
    }



    /* access modifiers changed from: protected */
    override fun getLayoutId(): Int {
        return R.layout.fragment_today
    }

    private fun initView() {
        tvDateMuslim = view.findViewById<View>(R.id.tv_date_muslim) as TextView
        tvDate = view.findViewById<View>(R.id.tv_date) as TextView
        imgNotification = view.findViewById<View>(R.id.languages) as ImageView
        tvNext = view.findViewById<View>(R.id.tv_next) as TextView
        tvTimeNext = view.findViewById<View>(R.id.tv_time_next) as TextView
        tvTimeDown = view.findViewById<View>(R.id.tv_time_down) as TextView
        tvCity = view.findViewById<View>(R.id.tv_city) as TextView
    }

    private fun loadBannerAd() {
        bannerAd = BannerAd.Builder(context as Activity?)
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
        interstitialAd = InterstitialAd.Builder(context as Activity?)
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
    private fun showInterstitialAd() {
        interstitialAd!!.show()
    }




    private fun setOnClick(view: View) {
        val quran = view.findViewById<CardView>(R.id.read_quran)
        quran.setOnClickListener(this)
        val offlineQuran = view.findViewById<CardView>(R.id.offline_quran)
        offlineQuran.setOnClickListener(this)
        val hadithBooks = view.findViewById<CardView>(R.id.hadith_books)
        hadithBooks.setOnClickListener(this)
        val azkaar = view.findViewById<CardView>(R.id.azkar_list)
        azkaar.setOnClickListener(this)
        val qiblaDirection = view.findViewById<CardView>(R.id.qibla_direction)
        qiblaDirection.setOnClickListener(this)
        val prayerCalender = view.findViewById<ImageView>(R.id.islamic_Calender)
        prayerCalender.setOnClickListener(this)
        val liveMedina = view.findViewById<CardView>(R.id.medina_live)
        liveMedina.setOnClickListener(this)
        val liveMecca = view.findViewById<CardView>(R.id.mecca_live)
        liveMecca.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        var fragment: Fragment
        when (v.id) {
            R.id.read_quran -> {
                startActivity(Intent(getActivity(), SurahActivity::class.java))
            (getActivity() as HomeActivity?)?.showInterstitialAd() }

                R.id.offline_quran -> startActivity(Intent(getActivity(), SixteenLines_MainPage::class.java))

            R.id.hadith_books -> startActivity(Intent(getActivity(), HadithActivity::class.java))
            R.id.azkar_list -> startActivity(Intent(getActivity(), CategoriesActivity::class.java))
            R.id.qibla_direction -> startActivity(Intent(getActivity(), QiblaDirectionActivity::class.java))
            R.id.islamic_Calender -> { startActivity(Intent(getActivity(), CalendarActivity::class.java))
                showInterstitialAd()
                bannerAd!!.destroyAndDetachBanner() }

            R.id.medina_live -> {
                val intent = Intent(getActivity(), LiveActivity::class.java)
                intent.putExtra("live", "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8")
                startActivity(intent)
            }

            R.id.mecca_live -> {
                val intent = Intent(getActivity(), LiveActivity::class.java)
                intent.putExtra("live", "http://m.live.net.sa:1935/live/quran/playlist.m3u8")
                startActivity(intent)            }


        }
    }

    private fun loadFragment(fragment: Fragment) {
        val fragmentTransaction = parentFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.home_host_fragment, fragment)
        fragmentTransaction.addToBackStack(null).commit()
    }

    override fun onResume() {
        super.onResume()
        getTime()
    }

    override fun onPause() {
        super.onPause()
        val countDownTimer = count
        countDownTimer?.cancel()
    }

    @SuppressLint("SetTextI18n")
    fun getTime() {
        val countDownTimer = count
        countDownTimer?.cancel()
        var str = ""
        val hGDate = HGDate()
        hGDate.toGregorian()
        val convertNumberType =
            NumbersLocal.convertNumberType(this.activity, hGDate.year.toString() + "")
        val textView = tvDate
        textView!!.text = NumbersLocal.convertNumberType(
            this.activity,
            hGDate.day.toString() + ""
        ) + " " + (Dates.gregorianMonthName(
            this.activity,
            hGDate.month + -1
        ) + "") + " " + convertNumberType
        hGDate.toHigri()
        val convertNumberType2 =
            NumbersLocal.convertNumberType(this.activity, hGDate.day.toString().trim { it <= ' ' })
        val trim = Dates.islamicMonthName(this.activity, hGDate.month + -1).trim { it <= ' ' }
        val convertNumberType3 =
            NumbersLocal.convertNumberType(this.activity, hGDate.year.toString() + "")
        tvDateMuslim!!.text = "$convertNumberType2 $trim $convertNumberType3"
        hGDate.toGregorian()
        var date = getDate(
            hGDate,
            CalculatePrayerTime(this.activity).NamazTimings(
                hGDate,
                LocationSave.getLat(),
                LocationSave.getLon()
            )
        )
        if (System.currentTimeMillis() > date[date.size - 1]!!.time) {
            hGDate.nextDay()
            hGDate.toGregorian()
            date = getDate(
                hGDate,
                CalculatePrayerTime(this.activity).NamazTimings(
                    hGDate,
                    LocationSave.getLat(),
                    LocationSave.getLon()
                )
            )
            str = "( tomorrow )"
        }
        var i = 0
        while (true) {
            if (i >= date.size) {
                break
            } else if (date[i]!!.time > System.currentTimeMillis()) {
                nextPostition = i
                break
            } else {
                i++
            }
        }
        var str2 = ""
        when (nextPostition) {
            0 -> str2 = PrayerFragment.FAJR
            1 -> str2 = PrayerFragment.SUNRISE
            2 -> str2 = PrayerFragment.DHUHR
            3 -> str2 = PrayerFragment.ASR
            4 -> str2 = PrayerFragment.MAGHRIB
            5 -> str2 = PrayerFragment.ISHA
        }
        tvNext!!.text = "$str2 $str"
        tvTimeNext!!.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(
            date[nextPostition]
        )
        val countDownTimer2 = count
        countDownTimer2?.cancel()
        time = date[nextPostition]!!.time - System.currentTimeMillis()
        count = object : CountDownTimer(time, 1000) {
            override fun onFinish() {}
            override fun onTick(j: Long) {
                val todayFragment = this@TodayFragment
                todayFragment.time = todayFragment.time - 1000
                val unused = todayFragment.time
                if (time < 0) {
                    getTime()
                    cancel()
                    return
                }
                val `access$100` = tvTimeDown
                `access$100`!!.text = " " + Utils.milliSecondsToTimerDown(
                    time
                )
            }
        }
        count?.start()
        tvCity!!.text = " " + LocationSave.getCity()
        imgNotification!!.setImageResource(ToneHelper.get().getToneIconFromKey(str2))
    }

    private val imageGreet: Unit
        get() {
            val calendar = Calendar.getInstance()
            val timeOfDay = calendar[Calendar.HOUR_OF_DAY]
            if (timeOfDay >= 0 && timeOfDay < 6) {
                greeting_img!!.setImageResource(R.drawable.bg_header_fajr)
            } else if (timeOfDay >= 6 && timeOfDay < 12) {
                greeting_img!!.setImageResource(R.drawable.bg_header_dhuhr)
            } else if (timeOfDay >= 12 && timeOfDay < 16) {
                greeting_img!!.setImageResource(R.drawable.bg_header_asr)
            } else if (timeOfDay >= 16 && timeOfDay < 18) {
                greeting_img!!.setImageResource(R.drawable.bg_header_maghrib)
            } else if (timeOfDay >= 18 && timeOfDay < 24) {
                greeting_img!!.setImageResource(R.drawable.bg_header_isha)
            }
        }

    private fun getDate(hGDate: HGDate, arrayList: ArrayList<String>): Array<Date?> {
        val dateArr = arrayOfNulls<Date>(6)
        for (i in arrayList.indices) {
            val split = arrayList[i].split(":").toTypedArray()
            val parseInt = split[0].toInt()
            val parseInt2 = split[1].toInt()
            val instance = Calendar.getInstance()
            instance[hGDate.year, hGDate.month - 1, hGDate.day, parseInt, parseInt2] = 0
            dateArr[i] = instance.time
        }
        return dateArr
    }

    companion object {
        fun newInstance(): TodayFragment {
            val bundle = Bundle()
            val todayFragment = TodayFragment()
            todayFragment.arguments = bundle
            return todayFragment
        }
    }
}