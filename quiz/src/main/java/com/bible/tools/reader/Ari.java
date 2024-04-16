package com.bible.tools.reader;


import android.util.Log;

import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alkitab resource identifier -- 2012-01-16: not really.
 *
 * @author yuku
 * <p>
 * ari is a 32bit integer.
 * LSB is bit 0, MSB is bit 31.
 * <p>
 * bit 31..24 is not used, always 0x00
 * bit 23..16 is book number, 0 to 255. 0 is Genesis, 65 is Revelation, 66 and above is defined elsewhere
 * bit 15..8 is chapter number, starts from 1. 0 is undefined or refers to the whole book
 * bit 7..0 is verse number, starts from 1. 0 is undefined or refers to the whole chapter
 */
public class Ari {
    public static int encode(int bookId, int chapter_1, int verse_1) {
        return (bookId & 0xff) << 16 | (chapter_1 & 0xff) << 8 | (verse_1 & 0xff);
    }

    public static int encode(int bookId, int chapterAndVerse) {
        return (bookId & 0xff) << 16 | (chapterAndVerse & 0xffff);
    }

    public static int encodeWithBc(int ari_bc, int verse_1) {
        return (ari_bc & 0x00ffff00) | (verse_1 & 0xff);
    }

    /**
     * 0..255
     * bookId starts from 0 (Gen = 0)
     */
    public static int toBook(int ari) {
        return (ari & 0x00ff0000) >> 16;
    }

    /**
     * 1..255
     * 1-based chapter
     */
    public static int toChapter(int ari) {
        return (ari & 0x0000ff00) >> 8;
    }

    /**
     * 1..255
     * 1-based verse
     */
    public static int toVerse(int ari) {
        return (ari & 0x000000ff);
    }

    /**
     * bookId-chapter_1 only, with verse_1 set to 0
     */
    public static int toBookChapter(int ari) {
        return (ari & 0x00ffff00);
    }

    /**
     * Similar to Integer.parseInt() but supports 0x and won't throw any exception when failed
     */
    public static int parseInt(String s, int def) {
        if (s == null || s.length() == 0) return def;

        // need to trim?
        if (s.charAt(0) == ' ' || s.charAt(s.length() - 1) == ' ') {
            s = s.trim();
        }

        // 0x?
        if (s.startsWith("0x")) {
            try {
                return Integer.parseInt(s.substring(2), 16);
            } catch (NumberFormatException e) {
                return def;
            }
        }

        // normal decimal
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static int toAri(String s) {
        try {
            String[] des = s.split(":");
            int bookId = Integer.parseInt(des[0]);
            int chapterId = Integer.parseInt(des[1]);
            int verseId = Integer.parseInt(des[2].split("-")[0]);
            return encode(bookId, chapterId, verseId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getBookId(String s) {
        try {
            Book[] books = ReadManager.getInstance().getAllKJVBook();
            for (Book book : books) {
                if (s.toLowerCase().contains(book.shortName.toLowerCase())) {
                    return book.bookId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static int getAriByRef(String ref) {
        try {
//            eg:
//            String ref = "1 Peter 11:12";
//            String ref = "Peter 11:12";
//            可能会存在没有:的情况，会导致解析失败。
            if (!ref.contains(":")) {
                ref = ref + ":1";
            }
            String[] split = ref.split(":");
            String vId = split[1].trim();
            String[] s1 = split[0].split("\\s+");
            String cId = s1[s1.length - 1].trim();
            String bookName = ref.substring(0, split[0].indexOf(cId)).trim();

            //TODO 因为从数据库中查询到的有些BookName会多带一个特殊的空格(Proverbs 10:12)，导致名称匹配失败，所以这里暂时这样处理一下
            if (s1.length == 2) {
                bookName = s1[0];
            } else if (s1.length == 3) {
                bookName = s1[0] + " " + s1[1];
            }

            Book[] books = ReadManager.getInstance().getAllKJVBook();
            for (Book book : books) {
                String shortName = book.shortName.toLowerCase();
                String bName = bookName.toLowerCase();
                if (shortName.toLowerCase().contains(bName)) {
                    String ariStr = book.bookId + ":" + cId + ":" + vId;
                    return Ari.toAri(ariStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            e.printStackTrace();
        }
        return 0;
    }

    public static int getAriByRefNew(String ref) {
        try {
//            String ref = "1 Peter 11:12";
//            1 Corinthians 1:25
//            John 1:1-2
            String[] split = ref.split(":");
            String vId = split[1].trim();
            if (vId.contains("-")) {
                vId = vId.split("-")[0];
            }
            String[] s1 = split[0].split(" ");
            String cId = s1[s1.length - 1].trim();
            String bookName = ref.substring(0, split[0].length() - cId.length()).trim();
            Book[] books = ReadManager.getInstance().getAllKJVBook();
            for (Book book : books) {
                if (book.shortName.toLowerCase().contains(bookName.toLowerCase())) {
                    String ariStr = book.bookId + ":" + cId + ":" + vId;
                    return Ari.toAri(ariStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static String[] getRefs(String ref) {

        try {
            if (ref.contains(",")) {
                //John 10:7,9-10
                //Psalms 136:1,26
                //John 1:1-2,14
                Pattern pattern = Pattern.compile("([\\d\\s]*[A-Z|a-z]+)\\s(\\d+):([\\d-]+)[,\\s]*([\\d-]+)");
                Matcher matcher = pattern.matcher(ref);
                if (matcher.find() && matcher.groupCount() == 4) {
                    String bookName = matcher.group(1).trim();
                    String cId = matcher.group(2).trim();
                    String verse1 = matcher.group(3).trim();
                    String verse2 = matcher.group(4).trim();
                    System.out.println(bookName + " " + cId + ":" + verse1);
                    System.out.println(bookName + " " + cId + ":" + verse2);
                    return new String[]{bookName + " " + cId + ":" + verse1, bookName + " " + cId + ":" + verse2};
                } else {
                    return new String[]{ref};
                }
            } else if (ref.contains(";")) {
                //Luke 2:1;Luke 5:4-5(vod 20190718)
                return ref.split(";");
            } else {
                return new String[]{ref};
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{ref};
        }
    }

    public static CharSequence getVerseByRefNew(String ref) {
        String[] refs = Ari.getRefs(ref);
        if (refs.length == 0) {
            return "no found";
        } else  {
            return getVerseByRef(refs[0]);
        }
    }

    public static CharSequence getVerseByRef(String ref) {
        int ari = getAriByRefNew(ref);
        String verse = getVerseByAri(ari);
        VerseRenderer.FormattedTextResult formatter = new VerseRenderer.FormattedTextResult();
        VerseRenderer.render(null, ari, verse, "10", null, false, formatter);
        return formatter.result;
    }

    public static String getVerseByAri(int ari) {
        int bookId = Ari.toBook(ari);
        Book book = ReadManager.getInstance().getAllKJVBook()[bookId];
        SingleChapterVerses singleChapterVerses = S.activeVersion.loadChapterText(book, Ari.toChapter(ari));
        if (singleChapterVerses == null) {
            return "no found";
        }
        int verse = Ari.toVerse(ari);
        if (verse - 1 >= 0) {
            verse -= 1;
        }
        return singleChapterVerses.getVerse(verse);
    }

    /**
     * 根据verse 应用获取verse索引和结束索引
     *
     * @param ref verse 引用 例如："John 10:9-10"
     * @return "John 10:9-10" -> [9,10] ; "John 10:9" -> [9,9]
     */
    @Nullable
    public static int[] getRoundVerseIndex(String ref) {
        try {
            if (ref.contains(":")) {
                String[] split = ref.split(":");
                if (split.length > 1) {
                    if (split[1].contains("-")) {
                        String[] result = split[1].split("-");
                        if (result.length > 1) {
                            int startIndex = Integer.parseInt(result[0]);
                            int endIndex = Integer.parseInt(result[1]);
                            if (endIndex < startIndex) {
                                new IllegalArgumentException("ref:" + ref).printStackTrace();
                                return null;
                            }
                            return new int[]{startIndex, endIndex};
                        }
                    } else {
                        int index = Integer.parseInt(split[1]);
                        return new int[]{index, index};
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String ariToRef(int ari) {
        int bookId = toBook(ari);
        int chapterId = toChapter(ari);
        int verseId = toVerse(ari);
        Book[] books = ReadManager.getInstance().getAllKJVBook();
        Book book = books[bookId];
        return book.shortName + " " + chapterId + ":" + verseId;
    }

    public static boolean isAvailableByAri(int ari) {
        int bookId = toBook(ari);
        int chapterId = toChapter(ari);
        int verseId = toVerse(ari);
        Book[] books = ReadManager.getInstance().getAllKJVBook();
        Book book = books[bookId];
        return chapterId < book.chapter_count && verseId < book.verse_counts[chapterId - 1];
    }

    /**
     * 根据 ref 获取 ari 数组
     *
     * @param ref verse 引用 例如："genesis 1:1-3"
     * @return "genesis 1:1-3" -> [257,258,259]
     */
    public static int[] getRoundAri(String ref) {
        if (!ref.contains(":")) {
            Log.d("ARI", "getRoundAri: ref no contains :");
            return new int[]{};
        }
        if (ref.contains("-")) {
            String[] result = ref.split("-");
            if (result.length < 2 || !isNumeric(result[1])) {
                Log.d("ARI", "getRoundAri: end verse is error");
                return new int[]{};
            }
            int startAri = getAriByRef(result[0]);
            if (startAri == 0) {
                Log.d("ARI", "getRoundAri: ref-format is error");
                return new int[]{};
            }
            int bookId = toBook(startAri);
            Book[] books = ReadManager.getInstance().getAllKJVBook();
            Book findBook = books[bookId];
            int chapterId = toChapter(startAri);
            int verseCount = findBook.verse_counts[chapterId - 1];
            int searchEndVerse = Integer.parseInt(result[1]);
            int verseIndex = Math.min(verseCount, searchEndVerse);

            int startVerseIndex = toVerse(startAri);
            int offsetCount = verseIndex - startVerseIndex + 1;
            int[] resultAris = new int[offsetCount];
            for (int i = 0; i < offsetCount; i++) {
                resultAris[i] = encode(bookId, chapterId, startVerseIndex + i);
            }
            return resultAris;
        }
        int ari = getAriByRef(ref);
        if (ari == 0 || !isAvailableByAri(ari)) {
            return new int[]{};
        }
        return new int[]{ari};
    }

    public static boolean isNumeric(String strNum) {
        Pattern pattern = Pattern.compile("-?\\d+(\\.\\d+)?");
        if (strNum == null) {
            return false;
        }
        return pattern.matcher(strNum).matches();
    }
}
