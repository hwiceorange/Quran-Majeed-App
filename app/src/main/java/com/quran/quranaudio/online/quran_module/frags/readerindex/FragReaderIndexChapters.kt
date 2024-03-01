package com.quran.quranaudio.online.quran_module.frags.readerindex

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peacedesign.android.utils.WindowUtils

class FragReaderIndexChapters : BaseFragReaderIndex() {
    override fun initList(list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2, ctx: Context) {
        super.initList(list, ctx)
        activity?.runOnUiThread {
            val spanCount = if (WindowUtils.isLandscapeMode(ctx)) 2 else 1
            val layoutManager = GridLayoutManager(ctx, spanCount, RecyclerView.VERTICAL, false)
            list.layoutManager = layoutManager
        }
        resetAdapter(list, ctx, false)
    }

    override fun resetAdapter(list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2, ctx: Context, reverse: Boolean) {
        super.resetAdapter(list, ctx, reverse)
        val adapter =
            com.quran.quranaudio.online.quran_module.adapters.quranIndex.ADPChaptersList(
                this,
                ctx,
                reverse
            )
        activity?.runOnUiThread {
            list.adapter = adapter
            favChaptersModel.favChapters.observe(viewLifecycleOwner) {
                adapter.onFavChaptersChanged()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(): FragReaderIndexChapters {
            return FragReaderIndexChapters()
        }
    }
}