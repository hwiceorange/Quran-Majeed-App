package com.quran.quranaudio.online.quran_module.frags.readerindex

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.WindowUtils
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.adapters.quranIndex.ADPFavChaptersList
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPFavouriteChapters
import com.quran.quranaudio.online.quran_module.widgets.PageAlert

class FragReaderIndexFavChapters : BaseFragReaderIndex() {
    private var pageAlert: PageAlert? = null

    override fun initList(list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2, ctx: Context) {
        super.initList(list, ctx)

        list.post {
            list.layoutManager = GridLayoutManager(
                ctx,
                if (WindowUtils.isLandscapeMode(ctx)) 2 else 1,
                RecyclerView.VERTICAL,
                false
            )
        }

        val adapter = ADPFavChaptersList(this)
        list.adapter = adapter
        list.visibility = View.GONE

        updateAdapter(ctx, list, adapter, SPFavouriteChapters.getFavouriteChapters(ctx))

        favChaptersModel.favChapters.observe(viewLifecycleOwner) {
            updateAdapter(ctx, list, adapter, it)
        }
    }

    private fun updateAdapter(ctx: Context, list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2, adapter: ADPFavChaptersList, items: List<Int>) {
        if (items.isEmpty()) {
            noItems(ctx, list)
        } else {
            pageAlert?.remove()
            adapter.chapterNos = items
            list.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }

    private fun noItems(ctx: Context, list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2) {
        pageAlert = PageAlert(ctx).apply {
            setMessage(R.string.noItems, R.string.msgNoFavouriteChapters)
            show(list.parent as ViewGroup)
            list.visibility = View.GONE
        }
    }
}