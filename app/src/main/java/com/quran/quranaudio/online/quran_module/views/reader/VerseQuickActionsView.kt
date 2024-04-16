package com.quran.quranaudio.online.quran_module.views.reader

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.quran_module.components.bookmark.BookmarkModel
import com.quran.quranaudio.online.quran_module.components.reader.ChapterVersePair
import com.quran.quranaudio.online.databinding.LytReaderVerseQuickActionsBinding
import com.quran.quranaudio.online.quran_module.activities.ActivityReader
import com.quran.quranaudio.online.quran_module.adapters.ADPQuranPages
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse
import com.quran.quranaudio.online.quran_module.components.reader.QuranPageSectionModel
import com.quran.quranaudio.online.quran_module.interfaceUtils.BookmarkCallbacks
import com.quran.quranaudio.online.quran_module.utils.extensions.dp2px
import com.quran.quranaudio.online.quran_module.utils.extensions.updateMargins

class VerseQuickActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
    BookmarkCallbacks {
    private val binding = LytReaderVerseQuickActionsBinding.inflate(
        LayoutInflater.from(context),
        this,
        true
    )
    private val closeRunnable = Runnable { close() }

    private var lastSection: QuranPageSectionModel? = null
    private val reader = context as ActivityReader

    init {
        setBackgroundResource(R.drawable.bg_verse_quick_actions)
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams) {
        if (params is MarginLayoutParams) {
            params.updateMargins(context.dp2px(10F))
        }

        super.setLayoutParams(params)
    }

    fun show(section: QuranPageSectionModel, verse: Verse) {
        if (lastSection?.quickActionsOpenedVerseNo == verse.verseNo) return

        lastSection?.let {
            it.quickActionsOpenedVerseNo = -1
            (reader.mBinding.readerVerses.adapter as? ADPQuranPages)?.notifyItemChanged(
                it.parentIndexInAdapter
            )
        }

        removeCallbacks(closeRunnable)
        lastSection = section

        section.quickActionsOpenedVerseNo = verse.verseNo
        (reader.mBinding.readerVerses.adapter as? ADPQuranPages)?.notifyItemChanged(
            section.parentIndexInAdapter
        )

        visibility = VISIBLE

        setupWithVerse(reader, reader.mQuranMetaRef.get(), verse)
    }

    private fun setupWithVerse(reader: ActivityReader, quranMeta: QuranMeta, verse: Verse) {
        val chapterNo = verse.chapterNo
        val verseNo = verse.verseNo

        binding.btnVerseOptions.setOnClickListener {
            close()
            reader.mActionController.openVerseOptionDialog(verse, null)
        }

        binding.btnVerseRecitation.setOnClickListener {
            close()
            reader.mPlayer.reciteControl(ChapterVersePair(verse.chapterNo, verse.verseNo))
        }

        onBookmarkChanged(reader.isBookmarked(chapterNo, verseNo, verseNo))
        binding.btnBookmark.setOnClickListener {
            if (reader.isBookmarked(chapterNo, verseNo, verseNo)) {
                reader.onBookmarkView(chapterNo, verseNo, verseNo, this)
            } else {
                reader.addVerseToBookmark(chapterNo, verseNo, verseNo, this)
            }
        }

        val verseSerial: String
        val verseSerialDesc: String
        if (verse.includeChapterNameInSerial) {
            val name: String = quranMeta.getChapterName(context, chapterNo)
            verseSerial = context.getString(
                R.string.strLabelVerseSerialWithChapter,
                name,
                chapterNo,
                verseNo
            )
            verseSerialDesc = context.getString(R.string.strDescVerseNoWithChapter, name, verseNo)
        } else {
            verseSerial = context.getString(R.string.strLabelVerseSerial, chapterNo, verseNo)
            verseSerialDesc = context.getString(R.string.strDescVerseNo, verseNo)
        }

        binding.verseSerial.text = verseSerial
        binding.verseSerial.contentDescription = verseSerialDesc
    }

    private fun onBookmarkChanged(isBookmarked: Boolean) {
        binding.btnBookmark.setColorFilter(
            ContextCompat.getColor(
                context,
                if (isBookmarked) {
                    R.color.colorPrimary
                } else {
                    R.color.colorIcon2
                }
            )
        )
        binding.btnBookmark.setImageResource(
            if (isBookmarked) {
                R.drawable.dr_icon_bookmark_added
            } else {
                R.drawable.dr_icon_bookmark_outlined
            }
        )
    }

    override fun onBookmarkRemoved(model: BookmarkModel) {
        onBookmarkChanged(false)
    }

    override fun onBookmarkAdded(model: BookmarkModel) {
        onBookmarkChanged(true)
    }

    fun close() {
        visibility = GONE
        lastSection?.let {
            it.quickActionsOpenedVerseNo = -1
            (reader.mBinding.readerVerses.adapter as? ADPQuranPages)?.notifyItemChanged(
                it.parentIndexInAdapter
            )
            lastSection = null
        }
    }

    fun scheduleClose() {
        postDelayed(closeRunnable, 100)
    }
}
