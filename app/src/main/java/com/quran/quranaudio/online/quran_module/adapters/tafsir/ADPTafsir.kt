package com.quran.quranaudio.online.quran_module.adapters.tafsir

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirInfoModel
import com.quran.quranaudio.online.quran_module.utils.extensions.dp2px
import com.quran.quranaudio.online.quran_module.utils.extensions.updatePaddings
import com.quran.quranaudio.online.quran_module.widgets.compound.PeaceCompoundButton
import com.quran.quranaudio.online.quran_module.widgets.radio.PeaceRadioButton

class ADPTafsir(private val tafsirs: List<TafsirInfoModel>, private val selectCallback: (Int) -> Unit) :
    RecyclerView.Adapter<ADPTafsir.VHTafsirItem>() {

    inner class VHTafsirItem(private val checkBox: PeaceRadioButton) : RecyclerView.ViewHolder(checkBox) {
        fun bind(info: TafsirInfoModel) {
            val ctx = checkBox.context
            val isFree = com.quran.quranaudio.online.quran_module.utils.tafsir
                .TafsirLanguageMapper.isFreeTafsir(info.key)

            // 步骤1：副标题加"免费/会员"彩色角标，让用户选之前就看清哪部免费、哪部付费
            checkBox.setTexts(info.name, buildSubtitle(ctx, info.author, isFree))
            checkBox.isChecked = info.isChecked

            // 步骤2：付费门放在"选中之前"否决 —— 未订阅选高级 tafsir 直接拦截，
            // 根本不写入默认(修复"选中即保存、退出付费墙后默认卡在读不了的那部"陷阱)
            checkBox.beforeCheckChangeListener = { _, willBeChecked ->
                if (willBeChecked && !isFree &&
                    !com.quran.quranaudio.online.subscription.SubscriptionHelper.isUserSubscribed(ctx)) {
                    com.quran.quranaudio.online.subscription.SubscriptionHelper
                        .launchSubscriptionPage(ctx, "tafsir_list_premium")
                    false // 否决勾选：不选中、不保存
                } else {
                    true
                }
            }

            checkBox.onCheckChangedListener = { _, checked ->
                if (checked) {
                    com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.setSavedTafsirKey(ctx, info.key)
                    selectCallback(bindingAdapterPosition)
                }
            }
        }

        /** 副标题 = 作者 + 彩色角标(免费=绿 / 会员=金) */
        private fun buildSubtitle(ctx: Context, author: String, isFree: Boolean): CharSequence {
            val badge = "  " + ctx.getString(
                if (isFree) R.string.tafsir_badge_free else R.string.tafsir_badge_premium
            )
            val ss = android.text.SpannableString(author + badge)
            val color = if (isFree) 0xFF4CAF50.toInt() else 0xFFE9B44C.toInt()
            ss.setSpan(android.text.style.ForegroundColorSpan(color),
                author.length, ss.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            ss.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                author.length, ss.length, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            return ss
        }
    }

    override fun getItemCount(): Int {
        return tafsirs.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VHTafsirItem {
        return VHTafsirItem(makeCheckBox(parent.context))
    }

    private fun makeCheckBox(context: Context): PeaceRadioButton {
        return PeaceRadioButton(context).apply {
            setCompoundDirection(PeaceCompoundButton.COMPOUND_TEXT_LEFT)
            setTextAppearance(R.style.TextAppearanceCommonTitle)
            setSpaceBetween(context.dp2px(15F))

            updatePaddings(horizontalPadding = context.dp2px(15F), verticalPadding = context.dp2px(10F))

            setBackgroundResource(R.drawable.dr_bg_action)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
        }
    }

    override fun onBindViewHolder(holder: VHTafsirItem, position: Int) {
        holder.bind(tafsirs[position])
    }
}