package com.quran.quranaudio.online.quran_module.frags.readerindex

import android.content.Context
import androidx.recyclerview.widget.LinearLayoutManager

class FragReaderIndexJuz : BaseFragReaderIndex() {
    override fun initList(list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2, ctx: Context) {
        super.initList(list, ctx)
        activity?.runOnUiThread {
            val layoutManager = LinearLayoutManager(ctx)
            list.layoutManager = layoutManager
        }
        resetAdapter(list, ctx, false)
    }

    override fun resetAdapter(list: com.quran.quranaudio.online.quran_module.views.helper.RecyclerView2, ctx: Context, reverse: Boolean) {
        super.resetAdapter(list, ctx, reverse)
        val adapter =
            com.quran.quranaudio.online.quran_module.adapters.quranIndex.ADPJuzList(
                this,
                ctx,
                reverse
            )
        activity?.runOnUiThread { list.adapter = adapter }
    }

    companion object {
        @JvmStatic
        fun newInstance(): FragReaderIndexJuz {
            return FragReaderIndexJuz()
        }
    }
}