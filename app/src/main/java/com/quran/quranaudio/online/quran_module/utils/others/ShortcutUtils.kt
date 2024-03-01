
package com.quran.quranaudio.online.quran_module.utils.others

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.activities.ActivityReader
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory

object ShortcutUtils {
    @JvmStatic
    fun pushVOTDShortcut(ctx: Context, chapterNo: Int, verseNo: Int) {
        val intent = ReaderFactory.prepareVerseRangeIntentForShortcut(chapterNo, verseNo, verseNo).apply {
            setClass(ctx, ActivityReader::class.java)
            action = Intent.ACTION_VIEW
        }

        val builder = ShortcutInfoCompat.Builder(ctx, ctx.getString(R.string.strNotifChannelIdVOTD))
            .setShortLabel(ctx.getString(R.string.strTitleVOTD))
            .setLongLabel(ctx.getString(R.string.strTitleVOTD))
            .setIntent(intent)

        try {
            builder.setIcon(IconCompat.createWithResource(ctx, R.drawable.dr_ic_shortcut_votd))
            ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun pushLastVersesShortcut(
        ctx: Context,
        quranMeta: QuranMeta,
        readType: Int,
        readerStyle: Int,
        juzNo: Int,
        chapterNo: Int,
        fromVerse: Int,
        toVerse: Int
    ) {
        val id = ctx.getString(R.string.strShortcutIdLastVerses)
        val intent = ReaderFactory.prepareLastVersesIntentForShortcut(
            quranMeta,
            juzNo,
            chapterNo,
            fromVerse,
            toVerse,
            readType,
            readerStyle
        )

        if (intent == null) {
            val ids: MutableList<String> = ArrayList()
            ids.add(id)
            ShortcutManagerCompat.removeDynamicShortcuts(ctx, ids)
            return
        }

        intent.setClass(ctx, ActivityReader::class.java)
        intent.action = Intent.ACTION_VIEW

        val builder = ShortcutInfoCompat.Builder(ctx, id)
            .setShortLabel(ctx.getString(R.string.strLabelContinueReading))
            .setLongLabel(ctx.getString(R.string.strLabelContinueReading))
            .setIntent(intent)
        try {
            builder.setIcon(IconCompat.createWithResource(ctx, R.drawable.logo))
            ShortcutManagerCompat.pushDynamicShortcut(ctx, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
