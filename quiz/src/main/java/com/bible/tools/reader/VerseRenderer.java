package com.bible.tools.reader;

import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.LeadingMarginSpan;
import android.text.style.MetricAffectingSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class VerseRenderer {
    public static final String TAG = VerseRenderer.class.getSimpleName();

    public static class VerseNumberSpan extends MetricAffectingSpan {
        private final boolean applyColor;

        public VerseNumberSpan(boolean applyColor) {
            this.applyColor = applyColor;
        }

        @Override
        public void updateMeasureState(TextPaint tp) {
            tp.baselineShift += (int) (tp.ascent() * 0.3f + 0.5f);
            tp.setTextSize(tp.getTextSize() * 0.7f);
        }

        @Override
        public void updateDrawState(TextPaint tp) {
            tp.baselineShift += (int) (tp.ascent() * 0.3f + 0.5f);
            tp.setTextSize(tp.getTextSize() * 0.7f);
            if (applyColor) {
                tp.setColor(S.applied.verseNumberColor);
            }
        }
    }

    public static class FormattedTextResult {
        public CharSequence result;
    }

    /**
     * Creates a leading margin span based on version:
     * - API 7 or 11 and above: LeadingMarginSpan.Standard
     * - API 8..10: LeadingMarginSpanFixed, which is based on LeadingMarginSpan.LeadingMarginSpan2
     */
    static Object createLeadingMarginSpan(int rest) {
        return new LeadingMarginSpan.Standard(0, rest);
    }

    private final static ThreadLocal<char[]> buf_char_ = new ThreadLocal<char[]>() {
        @Override
        protected char[] initialValue() {
            return new char[1024];
        }
    };


    /**
     * @param lText                TextView for verse text, but can be null if rendering is for non-display
     * @param ftr                  optional container for result that contains the verse text with span formattings, without the verse numbers
     * @return how many characters was used before the real start of verse text. This will be > 0 if the verse number is embedded inside lText.
     */
    public static int render(
            @Nullable final TextView lText,
            final int ari,
            @NonNull final String text,
            final String verseNumberText,
            @Nullable final Highlights.Info highlightInfo,
            final boolean checked,
            @Nullable final FormattedTextResult ftr) {
        // @@ = start a verse containing paragraphs or formatting
        // @6 = start of red text [formatting]
        // @5 = end of red text   [formatting]
        // @9 = start of italic [formatting]
        // @7 = end of italic   [formatting]

        final int text_len = text.length();

        // Determine if this verse text is a simple verse or formatted verse.
        // Formatted verses start with "@@".
        // Second character must be '@' too, if not it's wrong, we will fallback to simple render.
        if (text_len < 2 || text.charAt(0) != '@' || text.charAt(1) != '@') {
            //不是格式化 verse
            if (ftr != null) {
                ftr.result = text;
            }
            return simpleRender(lText, text, verseNumberText, highlightInfo, checked);
        }

        //具有特殊格式
        // optimization, to prevent repeated calls to charAt()
        char[] text_c = buf_char_.get();
        if (text_c.length < text_len) {
            text_c = new char[text_len];
            buf_char_.set(text_c);
        }
        text.getChars(0, text_len, text_c, 0);

        /*
         * position of start red marker
         */
        int startRed = -1;
        /*
         * position of start italic marker
         */
        int startItalic = -1;

        final SpannableStringBuilder sb = new SpannableStringBuilder();

        // this has two uses
        // - to check whether a verse number has been written
        // - to check whether we need to put a new line when encountering a new para
        final int startPosAfterVerseNumber;

        int pos = 2; // we start after "@@"

//        sb.append(verseNumberText);
//        sb.setSpan(new VerseNumberSpan(!checked), 0, sb.length(), 0);
//        sb.append("  ");
        startPosAfterVerseNumber = sb.length();

        while (true) {
            if (pos >= text_len) {
                break;
            }

            int nextAt = text.indexOf('@', pos);

            if (nextAt == -1) { // no more, just append till the end of everything and exit
                sb.append(text, pos, text_len);
                break;
            }

            // insert all text until the nextAt
            if (nextAt != pos) /* extra check for optimization (prevent call to sb.append()) */ {
                sb.append(text, pos, nextAt);
                pos = nextAt;
            }

            pos++;
            // just in case
            if (pos >= text_len) {
                break;
            }

            char marker = text_c[pos];
            switch (marker) {
                case '6':
                    startRed = sb.length();
                    break;
                case '5':
                    if (startRed != -1) {
//                        if (!checked) {
//                            sb.setSpan(new ForegroundColorSpan(S.applied.fontRedColor), startRed, sb.length(), 0);
//                        }
                        startRed = -1;
                    }
                    break;
                case '9':
                    startItalic = sb.length();
                    break;
                case '7':
                    if (startItalic != -1) {
//                        sb.setSpan(new StyleSpan(Typeface.ITALIC), startItalic, sb.length(), 0);
                        startItalic = -1;
                    }
                    break;
            }

            pos++;
        }

        // apply unapplied
        applyParaStyle(sb, verseNumberText);

        if (highlightInfo != null) {
            if (lText instanceof VerseTextView) {
                ((VerseTextView)lText).setPaintFilterColor(highlightInfo.colorRgb);
            }
        }

        if (lText != null) {
            lText.setText(sb);
        }

        if (ftr != null) {
            if (startPosAfterVerseNumber == 0) {
                ftr.result = sb;
            } else {
                ftr.result = sb.subSequence(startPosAfterVerseNumber, sb.length());
            }
        }

        return startPosAfterVerseNumber;
    }


    static void applyParaStyle(SpannableStringBuilder sb, String verseNumberText) {
        int len = sb.length();

        if (len == 0) {
            return;
        }
        sb.setSpan(createLeadingMarginSpan(S.applied.indentParagraphRest), 0, len, 0);
    }

    /**
     * @return how many characters was used before the real start of verse text. This will be > 0 if the verse number is embedded inside lText.
     */
    public static int simpleRender(@Nullable TextView lText, String text, String verseNumberText, @Nullable final Highlights.Info highlightInfo, boolean checked) {
        final SpannableStringBuilder sb = new SpannableStringBuilder();

        // verse number
//        sb.append(verseNumberText).append("  ");
//        sb.setSpan(new VerseNumberSpan(!checked), 0, verseNumberText.length(), 0);
        final int startPosAfterVerseNumber = sb.length();

        // verse text
        sb.append(text);
        sb.setSpan(createLeadingMarginSpan(S.applied.indentParagraphRest), 0, sb.length(), 0);

        if (lText instanceof VerseTextView) {
            VerseTextView verseTextView = (VerseTextView) lText;
            if (highlightInfo != null) {
                verseTextView.setPaintFilterColor(highlightInfo.colorRgb);
            } else {
                verseTextView.setPaintFilterColor(Color.TRANSPARENT);
            }
        }

        if (lText != null) {
            lText.setText(sb);
        }

        return startPosAfterVerseNumber;
    }


}
