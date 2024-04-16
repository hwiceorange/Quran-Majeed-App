package com.bible.tools.reader;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;

import com.bible.tools.base.BaseApp;
import com.bible.tools.extension.SPTools;
import com.bible.tools.reader.Book;
import com.bible.tools.utils.AppConfig;
import com.bible.tools.utils.AppConfigInfo;


public class S {

    /**
     * values applied from settings
     */
    public static class applied {
        /**
         * in dp
         */
        public static float fontSize2dp;

//        public static Typeface fontFace;
        public static float lineSpacingMult;
        public static int fontBold;

        public static int fontColor;
        public static int fontRedColor;
        /**
         * 背景色
         */
        public static int backgroundColor;
        public static int verseNumberColor;

        /**
         * 0.f to 1.f
         */
        public static float backgroundBrightness;

        // semua di bawah dalam px
        public static int indentParagraphRest;
    }

    //# 22nya harus siap di siapinKitab
    public static Version activeVersion;
    public static String activeVersionId = MVersionInternal.getVersionInternalId();//add default
    public static int activeChapter;
    public static Book mActiveBook;
    public static float verseHighlightAlpha;

    public static void calculateAppliedValuesBasedOnPreferences() {
        //# configure font size
        {
          //  applied.fontSize2dp = SPTools.INSTANCE.getFloat(Prefkey.ukuranHuruf2.name(), getBibleFontDefaultSize());
        }

        //# configure fonts
        {
//            applied.fontFace = FontManager.typeface(FontManager.getFontName());
            applied.lineSpacingMult = 1.3f;
            applied.fontBold = SPTools.INSTANCE.getBoolean(Prefkey.boldHuruf.name(), false) ? Typeface.BOLD : Typeface.NORMAL;
        }

        verseHighlightAlpha = 1f;


        applied.fontColor = Color.parseColor("#333333");
        applied.verseNumberColor = Color.parseColor("#99333333");
        applied.backgroundColor = Color.parseColor("#FFFFFF");
        applied.fontRedColor = Color.parseColor("#BC232B");
        //# configure text color, red text color, bg color, and verse color
        {
            // calculation of backgroundColor brightness. Used somewhere else.
            int c = applied.backgroundColor;
            applied.backgroundBrightness = (0.30f * Color.red(c) + 0.59f * Color.green(c) + 0.11f * Color.blue(c)) * 0.003921568627f;
        }

        Resources res = BaseApp.Companion.getInstance().getResources();

        float scaleBasedOnFontSize = applied.fontSize2dp / 17.f;
      //  applied.indentParagraphRest = (int) (scaleBasedOnFontSize * res.getDimensionPixelOffset(R.dimen.indentParagraphRest) + 0.5f);
    }


    /**
     * Get the internal version model. This does not return a singleton. The ordering is the latest taken from preferences.
     */
    public static MVersionInternal getMVersionInternal() {
        final AppConfigInfo ac = AppConfig.get();
        final MVersionInternal res = new MVersionInternal();
        res.locale = ac.getInternalLocale();
        res.shortName = ac.getInternalShortName();
        res.longName = ac.getInternalLongName();
        res.description = null;
        res.ordering = SPTools.INSTANCE.getInt(Prefkey.internal_version_ordering.name(), MVersionInternal.DEFAULT_ORDERING);
        return res;
    }

}
