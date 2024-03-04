package com.raiadnan.quranreader.ui.Main.View

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.raiadnan.quranreader.R
import com.raiadnan.quranreader.DataHolder.ApplicationData
import com.raiadnan.quranreader.ui.Base.AzkharViewModelFactory
import com.raiadnan.quranreader.ui.Main.Adapter.ItemAdapter
import com.raiadnan.quranreader.ui.Main.ViewModel.AzkharViewModel
import dev.kosrat.muslimdata.models.Language
import dev.kosrat.muslimdata.repository.MuslimRepository
import kotlinx.android.synthetic.main.activity_item.*
import kotlinx.coroutines.launch

class ItemActivity : AppCompatActivity() {
    private lateinit var adapter:ItemAdapter
    private var chapterId:Int ?= null
    private lateinit var viewModel: AzkharViewModel

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (ApplicationData(this).theme)
            setTheme(R.style.AppThemeDark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_item)
        val imgSurah = findViewById<View>(R.id.back) as ImageView
        imgSurah.setOnClickListener { this@ItemActivity.finish() }
        setViewModel()
        chapterId = intent.getIntExtra("chapter",1)
        adapter = ItemAdapter(viewModel,this)
        setUI()

    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(this,AzkharViewModelFactory(application))
            .get(AzkharViewModel::class.java)
    }

    private fun setUI() {
        rv_azkhar.adapter = adapter
        chapterId?.let { getItem(it) }
    }


    private fun getItem(chapterNum:Int) {
        lifecycleScope.launch {
            val repository = MuslimRepository(this@ItemActivity)
            val azkarItems = repository.getAzkarItems(chapterNum, Language.EN)
            if (azkarItems != null){
                adapter.setData(azkarItems)
            }
            /*Log.i("azkarItems", "$azkarItems")
            Log.i("azkarItems", "${azkarItems?.size}")*/
        }
    }
}