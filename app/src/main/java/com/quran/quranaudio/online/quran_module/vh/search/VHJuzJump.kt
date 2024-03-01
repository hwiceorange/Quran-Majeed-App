package com.quran.quranaudio.online.quran_module.vh.search

import com.quran.quranaudio.online.databinding.LytReaderJuzSpinnerItemBinding
import com.quran.quranaudio.online.quran_module.components.search.JuzJumpModel
import com.quran.quranaudio.online.quran_module.components.search.SearchResultModelBase
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory.startJuz

class VHJuzJump(private val mBinding: LytReaderJuzSpinnerItemBinding, applyMargins: Boolean) : VHSearchResultBase(
    mBinding.root
) {
    init {
        setupJumperView(mBinding.root, applyMargins)
    }

    override fun bind(model: SearchResultModelBase, pos: Int) {
        (model as JuzJumpModel).apply {
            mBinding.juzSerial.text = model.juzSerial
            mBinding.root.setOnClickListener { startJuz(it.context, model.juzNo) }
        }
    }
}
