@file:Suppress("DEPRECATION")

package com.raiadnan.quranreader.ui.Main.View

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.elconfidencial.bubbleshowcase.BubbleShowCaseBuilder
import com.elconfidencial.bubbleshowcase.BubbleShowCaseSequence
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.raiadnan.quranreader.Data.Model.Pojo.Verse
import com.raiadnan.quranreader.Data.Repository.AyahRepository
import com.raiadnan.quranreader.Data.database.QuranDatabase
import com.raiadnan.quranreader.Data.database.entities.Ayah
import com.raiadnan.quranreader.Data.database.entities.Surah
import com.raiadnan.quranreader.R
import com.raiadnan.quranreader.DataHolder.ApplicationData
import com.raiadnan.quranreader.ui.Base.AyahViewModelFactory
import com.raiadnan.quranreader.ui.Main.Adapter.AyahAdapter
import com.raiadnan.quranreader.ui.Main.ViewModel.AyahViewModel
import com.raiadnan.quranreader.ui.services.SurahDLService
import com.raiadnan.quranreader.Utils.SharePref
import io.ghyeok.stickyswitch.widget.StickySwitch
import kotlinx.android.synthetic.main.activity_ayah.*
import kotlinx.android.synthetic.main.activity_ayah.fab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.util.*


class AyahActivity : AppCompatActivity(), AyahAdapter.OnClickAyah, Player.Listener {
    private lateinit var viewModel: AyahViewModel
    private var surahList: List<Surah>? = null
    private var previousSate = true
    private lateinit var ayahAdapter: AyahAdapter
    private lateinit var simpleExoplayer: SimpleExoPlayer
    private var playbackPosition: Long = 0
    private var ayahNumber = 1
    private var baseNumber = 0
    private var surahName: String? = null
    private var surahNumber: Int? = null
    private lateinit var sharedPref:SharePref
    private var isFirstTime:Boolean ?= null
    private lateinit var switch: StickySwitch
    private var verseList: List<Verse> ?= null
    companion object{
        val STATE_SURAH = "state"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        if (ApplicationData(this).theme)
            setTheme(R.style.AppThemeDark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ayah)
        sharedPref = SharePref(this)
        isFirstTime = sharedPref.loadFirstTimeAyah()

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            with(savedInstanceState) {
                // Restore value of members from saved state
                surahNumber = getInt(STATE_SURAH)
                previousSate = this.getBoolean("LOST_CONNECTION")

            }
        } else {
            // Probably initialize members with default values for a new instance
            surahNumber = intent.getIntExtra("surah_number", 1)
        }


        setViewModel()
        setUI(surahNumber!!)
        if (isFirstTime == true){
            activateShowcase()
            sharedPref.setFirstTimeAyah(false)
        }


        //   setNetworkMonitor()
    }

    @SuppressLint("MissingSuperCall")
    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            surahNumber?.let { putInt(STATE_SURAH, it) }
            Timber.d("saved surah number to saveInstance")

        }

        super.onSaveInstanceState(outState)

    }


    /* override fun onRestoreInstanceState(savedInstanceState: Bundle) {
         super.onRestoreInstanceState(savedInstanceState)
         // Restore state members from saved instance
         savedInstanceState.run {
             surahNumber = getInt(STATE_SURAH)

         }

     }*/

    private fun activateShowcase() {
        BubbleShowCaseSequence()
            .addShowCase(
                BubbleShowCaseBuilder(this) //Activity instance
                    .title(getString(R.string.bubble_aya_title1)) //Any title for the bubble view
                    .targetView(play_all) //View to point out
                    .description(getString(R.string.bubble_ayah_des1))
                    .backgroundColorResourceId(R.color.colorPrimary)
                    .imageResourceId(R.drawable.ic_notify)
                    .textColorResourceId(R.color.colorWhite)
            ) //First BubbleShowCase to show
            .addShowCase(
                BubbleShowCaseBuilder(this) //Activity instance
                    .title(getString(R.string.bubble_ayah_title3)) //Any title for the bubble view
                    .targetView(fab) //View to point out
                    .description(getString(R.string.bubble_ayah_desc3))
                    .backgroundColorResourceId(R.color.colorPrimary)
                    .imageResourceId(R.drawable.ic_notify)
                    .textColorResourceId(R.color.colorWhite)
            )
            .show() //Display the ShowCaseSequence
    }

    private fun setViewModel() {
        val dao =
            QuranDatabase.getDatabase(application, lifecycleScope, application.resources).surahDao()
        viewModel = ViewModelProvider(
            this,
            AyahViewModelFactory(application, AyahRepository(dao))
        ).get(AyahViewModel::class.java)
    }



    private fun setUI(number_surah: Int) {
        ayahAdapter = AyahAdapter(viewModel)

        viewModel.getSurahList(number_surah)
        viewModel.getAllAyah(number_surah)
        viewModel.getSurahInfo(number_surah)


        viewModel.listSurah.observe(this, Observer {
            surahList = it
            Log.d("Test", it.size.toString())
            if (it.isNotEmpty()) {
                calculateBase()
            }

        })
        viewModel.ayah.observe(this, Observer {
            if (it.isNotEmpty()) {
                if(Locale.getDefault().language.contentEquals("fr")){
                    if (it[0].frenchTranslation == null || it[0].frenchTranslation.equals("empty")){
                        lifecycleScope.launch(Dispatchers.Main){
                            viewModel.getFrenchSurah(number_surah)
                            delay(2000)
                            viewModel.translation.observe(this@AyahActivity , Observer {
                                if (it.isNotEmpty()){
                                    ayahAdapter.setNetworkList(it)
                                }
                            })
                            ayahRecycler.adapter = ayahAdapter
                            ayahAdapter.setData(it)
                            ayahAdapter.setOnItemClick(this@AyahActivity)
                        }

                    }
                }

                ayahRecycler.adapter = ayahAdapter
                ayahAdapter.setData(it)
                ayahAdapter.setOnItemClick(this@AyahActivity)


            }

        })

        viewModel.surah.observe(this, Observer {
            if(it != null){
                surah_title.text = it.transliteration
                surahName = it.transliteration
                verse_number.text = it.totalVerses.toString() + " " + "Ayah"
            }

        })



        switch = findViewById(R.id.switch_control)
        switch.visibility = View.GONE
        switch.onSelectedChangeListener = object : StickySwitch.OnSelectedChangeListener {
            override fun onSelectedChange(direction: StickySwitch.Direction, text: String) {
                Log.d("TAG", "Now Selected : " + direction.name + ", Current Text : " + text);
                if (direction == StickySwitch.Direction.LEFT) {
                    simpleExoplayer.pause()
                } else {
                    simpleExoplayer.play()
                }
            }

        }

        txt_play_all.setOnClickListener(View.OnClickListener {
            sendDataToPlayer()
        })

        play_all.setOnClickListener(View.OnClickListener {
            sendDataToPlayer()
        })

        fab.setOnClickListener(View.OnClickListener {
            if (checkIfFileExist()){
                Toast.makeText(this,getString(R.string.dl_available),Toast.LENGTH_LONG).show()
            } else{
                downloadDialog()
            }

        })


    }

    private fun checkIfFileExist():Boolean{
        val fileName = "$surahNumber-$surahName.mp3"
        val file = File(this.getExternalFilesDir(null).toString()+"/surahs/"+fileName)
        //Log.d("Test",this.getExternalFilesDir(null).toString())
        return file.exists()
    }

    private fun downloadDialog() {
        MaterialDialog(this).show {
            title(text = getString(R.string.actionbar_name))
            message(text = getString(R.string.surah_dl))
            positiveButton(text = getString(R.string.yes)) { dialog ->
                dialog.cancel()
                initializeService()
            }
            negativeButton(text = getString(R.string.cancel)) { dialog ->
                dialog.cancel()

            }
            icon(R.drawable.ic_notify)
        }
    }

    private fun initializeService() {
        val number = formatSurahNumber()
        val surahUrl =
            "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/mishary" +
                    "-rashid-alafasy/mishary-rashid-alafasy-$number-muslimcentral.com.mp3"
        val intent = Intent(this, SurahDLService::class.java)
        intent.putExtra("link", surahUrl)
        intent.putExtra("number", surahNumber)
        intent.putExtra("surah", surahName)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
            Toast.makeText(this, getString(R.string.dl_started), Toast.LENGTH_LONG).show()
        } else {
            startService(intent)
            Toast.makeText(this, getString(R.string.dl_started), Toast.LENGTH_LONG).show()
        }
    }

    private fun formatSurahNumber(): String {
        var formatNumber: String? = null
        formatNumber = when {
            surahNumber!! in 1..9 -> {
                "00$surahNumber"
            }
            surahNumber!! in 10..99 -> {
                "0$surahNumber"
            }
            else -> {
                surahNumber.toString()
            }
        }
        return formatNumber
    }

    private fun sendDataToPlayer() {
        val intent = Intent(this, MusicActivity::class.java)
        intent.putExtra("surah_name", surahName)
        intent.putExtra("surah_number", surahNumber)
        startActivity(intent)

    }

    private fun calculateBase() {
        for (item in surahList!!) {
            baseNumber += item.totalVerses
        }
    }


    override fun onClickAyah(ayah: Ayah) {

        val verseNumber = baseNumber + ayah.verse_number
        Timber.d("ayaNum:$verseNumber")
        switch.visibility = View.VISIBLE
        switch.setDirection(StickySwitch.Direction.RIGHT)

        if (simpleExoplayer.isPlaying) {
            simpleExoplayer.stop()
            initializePlayer(verseNumber)
        } else {
            initializePlayer(verseNumber)
        }
    }

    override fun onStart() {
        super.onStart()
        simpleExoplayer = SimpleExoPlayer.Builder(this).build()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    private fun initializePlayer(ayaNum: Int) {

        val link = "http://cdn.islamic.network/quran/audio/128/ar.alafasy/$ayaNum.mp3"


        preparePlayer(link, "default")
        exoplayerView.player = simpleExoplayer
        //exoplayerView.visibility = View.VISIBLE
        exoplayerView.controllerShowTimeoutMs = 0;
        exoplayerView.controllerHideOnTouch = false;
        simpleExoplayer.playWhenReady = true
        simpleExoplayer.addListener(this)


    }


    private fun buildMediaItem(uri: Uri, type: String): MediaItem {
        return MediaItem.fromUri(uri)
    }

    private fun preparePlayer(ayahUrl: String, type: String) {
        val uri = Uri.parse(ayahUrl)
        Timber.d("link:$ayahUrl")
        /* val evictor = LeastRecentlyUsedCacheEvictor((100 * 1024 * 1024).toLong())
         val databaseProvider: DatabaseProvider = ExoDatabaseProvider(this)

         val simpleCache = SimpleCache(File(this.cacheDir, "media"), evictor, databaseProvider)


         val mediaSource = ProgressiveMediaSource.Factory(
             simpleCache?.let {
                 CacheDataSource.Factory().setCache(it)
             }
         )
             .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))*/
        val mediaItem = buildMediaItem(uri, type)
        //simpleExoplayer.setMediaSource(mediaSource)
        simpleExoplayer.setMediaItem(mediaItem)
        simpleExoplayer.prepare()

    }

    private fun releasePlayer() {
        playbackPosition = simpleExoplayer.currentPosition
        simpleExoplayer.release()
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        // handle error
    }

}