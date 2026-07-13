package com.quran.quranaudio.online.quran_module.views.reader

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.api.models.VerseWords
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs
import com.quran.quranaudio.online.quran_module.utils.wbw.WordByWordManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 逐词翻译底部弹窗：点某节的"逐词"图标打开，展示该节每个词的阿拉伯文 + 词义 + 转写。
 *
 * 完全增量、只读、按需拉取(缓存)——不改经文渲染、不进数据库。免费学习工具(引流)，
 * 逐词发音/词根/语法等作会员留待后续。
 */
object WordByWordSheet {

    @JvmStatic
    fun show(activity: Activity, chapterNo: Int, verseNo: Int) {
        if (activity.isFinishing) return
        val ctx = activity

        val dp = ctx.resources.displayMetrics.density
        fun dp(v: Int) = (v * dp).toInt()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
        }
        // 标题
        container.addView(TextView(ctx).apply {
            text = ctx.getString(R.string.wbw_title) + "  ·  $chapterNo:$verseNo"
            setTextColor(0xFF4E8545.toInt())
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
        })
        val loading = TextView(ctx).apply {
            text = ctx.getString(R.string.loading)
            setTextColor(0xFF8A94A6.toInt())
            textSize = 13f
            setPadding(0, dp(16), 0, 0)
        }
        container.addView(loading)

        val scroll = ScrollView(ctx).apply {
            addView(container)
            isFillViewport = false
        }

        val sheet = BottomSheetDialog(ctx)
        sheet.setContentView(scroll)

        // 用带 SupervisorJob 的作用域，保证 dismiss 时能真正取消协程(避免关闭后继续操作已分离视图)
        val scope = CoroutineScope(Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
        sheet.setOnDismissListener {
            scope.cancel()
        }

        val lang = WordByWordManager.mapLang(SPAppConfigs.getLocale(ctx))
        scope.launch {
            val words: VerseWords? = WordByWordManager.getVerseWords(ctx, chapterNo, verseNo, lang)
            if (activity.isFinishing || activity.isDestroyed || !isActive) return@launch
            container.removeView(loading)
            if (words == null || words.words.isEmpty()) {
                container.addView(TextView(ctx).apply {
                    text = ctx.getString(R.string.wbw_unavailable)
                    setTextColor(0xFF8A94A6.toInt())
                    textSize = 13f
                    setPadding(0, dp(16), 0, 0)
                })
                return@launch
            }
            words.words.forEach { w ->
                container.addView(buildWordRow(ctx, ::dp, w.arabic, w.transliteration, w.translation))
            }
        }
        sheet.show()
    }

    private fun buildWordRow(
        ctx: Activity, dp: (Int) -> Int,
        arabic: String, translit: String, translation: String
    ): View {
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, dp(12))
        }
        // 阿拉伯词(大、RTL)
        row.addView(TextView(ctx).apply {
            text = arabic
            textSize = 22f
            setTextColor(0xFF2D3A53.toInt())
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.END
        })
        // 转写(斜体灰)
        if (translit.isNotEmpty()) {
            row.addView(TextView(ctx).apply {
                text = translit
                textSize = 13f
                setTextColor(0xFF8A94A6.toInt())
                setTypeface(typeface, Typeface.ITALIC)
                setPadding(0, dp(2), 0, 0)
            })
        }
        // 词义
        row.addView(TextView(ctx).apply {
            text = translation
            textSize = 15f
            setTextColor(0xFF4E8545.toInt())
            setPadding(0, dp(2), 0, 0)
        })
        // 分隔线
        row.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply { topMargin = dp(12) }
            setBackgroundColor(0x14000000)
        })
        return row
    }
}
