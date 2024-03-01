package com.quran.quranaudio.online.quran_module.adapters.extended

import android.content.Context
import android.view.View
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.utils.extensions.color
import com.quran.quranaudio.online.quran_module.utils.extensions.dp2px
import com.quran.quranaudio.online.quran_module.utils.extensions.updatePaddingVertical
import com.quran.quranaudio.online.quran_module.widgets.list.base.BaseListAdapter
import com.quran.quranaudio.online.quran_module.widgets.list.base.BaseListItem
import com.quran.quranaudio.online.quran_module.widgets.list.base.BaseListItemView

class PeaceBottomSheetMenuAdapter(context: Context) : BaseListAdapter(context) {
    private val mMessageColor = context.color(R.color.colorText2)

    override fun onCreateItemView(item: BaseListItem, position: Int): View {
        val view = super.onCreateItemView(item, position) as BaseListItemView

        if (item.message.isNullOrEmpty()) {
            view.containerView.updatePaddingVertical(context.dp2px(3f))
        } else {
            view.messageView?.setTextColor(mMessageColor)
        }

        return view
    }
}
