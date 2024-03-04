package com.raiadnan.quranreader.ui.Main.View

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.raiadnan.quranreader.R
import com.raiadnan.quranreader.ui.Base.SurahViewModelFactory
import com.raiadnan.quranreader.Utils.Common
import com.raiadnan.quranreader.Utils.SharePref
import com.raiadnan.quranreader.activities.HomeActivity
import com.raiadnan.quranreader.ui.Main.ViewModel.SurahViewModel
import timber.log.Timber

class LoadingActivity : AppCompatActivity() {
    companion object {
        val FILTER = "com.raiadnan.quranreader.DBLOAD"
    }

    private lateinit var viewModel: SurahViewModel
    private var sharedPref:SharePref ?= null
    private var user:com.raiadnan.quranreader.Data.Model.User ?= null
    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            goToHomeActivity()
            Timber.d("Test Receiver")
        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPref = SharePref(this)
        val filter = IntentFilter(FILTER)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,filter)
        setContentView(R.layout.activity_loading)
        setViewModel()
    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(
            this,
            SurahViewModelFactory(application)
        ).get(SurahViewModel::class.java)

        viewModel.getSurahs()
        viewModel.surahList.observe(this, androidx.lifecycle.Observer {

        })

    }

    override fun onStart() {
        super.onStart()

        //registerReceiver(receiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }



    fun goToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.putExtra(Common.USER,user)
        startActivity(intent)
    }
}