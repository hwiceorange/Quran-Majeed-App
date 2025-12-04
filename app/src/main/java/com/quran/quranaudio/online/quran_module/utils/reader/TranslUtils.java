package com.quran.quranaudio.online.quran_module.utils.reader;

import android.content.Context;
import android.util.Pair;
import androidx.annotation.Nullable;

import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo;
import com.quran.quranaudio.online.quran_module.components.transls.TranslModel;
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo;
import com.quran.quranaudio.online.quran_module.components.transls.TranslModel;
import com.quran.quranaudio.online.quran_module.utils.Log;
import com.quran.quranaudio.online.quran_module.utils.Logger;
import com.quran.quranaudio.online.quran_module.utils.app.AppUtils;
import com.quran.quranaudio.online.quran_module.utils.univ.FileUtils;
import com.quran.quranaudio.online.quran_module.utils.univ.MessageUtils;
import com.quran.quranaudio.quiz.utils.AppConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import kotlin.io.FilesKt;
import kotlin.text.Charsets;

public class TranslUtils {
    public static final String DIR_NAME = FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR,
        "translations");
    public static final String DIR_NAME_4_AVAILABLE_DOWNLOADS =
        FileUtils.createPath(AppUtils.BASE_APP_DOWNLOADED_SAVED_DATA_DIR, "available_translation_downloads");
    public static final String TRANSL_INFO_FILE_NAME = "manifest.json";
    public static final String KEY_TRANSLATIONS = "key.translations";
    public static final String KEY_NEW_TRANSLATIONS = "key.translations_new";

    /**
     * Format -> translation_101_en_en_sahih-international.json
     */
    public static final String TRANSL_FILE_NAME_FORMAT = "translation_%d_%s_%s.json";
    public static final String TRANSL_AVAILABLE_DOWNLOADS_FILE_NAME = "available_downloads.json";

    public static final String TRANSL_SLUG_EN_SAHIH_INTERNATIONAL = "en_101_sahih-international";
    public static final String TRANSL_SLUG_EN_THE_CLEAR_QURAN = "en_102_the-clear-quran";
    public static final String TRANSL_SLUG_UR_JUNAGARHI = "in_junagarhi";

    public static final String TRANSL_SLUG_IN = "in_quran-complex";

    // Malay translation slug (Abdullah Basmeih - ID: 39)
    public static final String TRANSL_SLUG_MS_ABDULLAH = "ms_39_abdullah";
    
    // Turkish translation slug (Diyanet İşleri - ID: 77)
    public static final String TRANSL_SLUG_TR_DIYANET = "tr_77_diyanet";
    
    // Bengali translation slug (Taisirul Quran - ID: 161)
    public static final String TRANSL_SLUG_BN_TAISIRUL = "bn_161_taisirul-quran";


    /**
     * This translation slug must be updated if it updates on the server.
     */
    private static final String TRANSL_TRANSLITERATION_SLUG_PART = "transliteration";

    public static final String TRANSL_SLUG_DEFAULT = TRANSL_SLUG_UR_JUNAGARHI;

    public static final int TRANSL_MAX_SELECTION_LIMIT = 6;

    /**
     * 🌐 获取默认译本：根据用户系统语言自动匹配对应的译本
     * 
     * 匹配规则：
     * - 印尼语 (id/in) → Kompleks Al Quran Raja Fahd
     * - 英语 (en) → Sahih International
     * - 乌尔都语 (ur) → مولانا محمد جوناگڑهی
     * - 其他语言 → Sahih International (英语作为默认)
     * 
     * @return 默认译本的 slug 集合
     * @deprecated 使用 defaultTranslationSlugs(Context) 以确保语言与应用设置同步
     */
    @Deprecated
    public static Set<String> defaultTranslationSlugs() {
        Set<String> defTranslations = new HashSet<>();
        
        // 获取系统语言（仅作为回退）
        String systemLanguage = getSystemLanguage();
        
        // 根据系统语言自动选择对应的译本
        switch (systemLanguage) {
            case "id":  // 印尼语（应用统一使用 "id"）
                defTranslations.add(TRANSL_SLUG_IN);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: Indonesian (Kompleks Al Quran)");
                break;
                
            case "en":  // 英语
                defTranslations.add(TRANSL_SLUG_EN_SAHIH_INTERNATIONAL);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: English (Sahih International)");
                break;
                
            case "ur":  // 乌尔都语
                defTranslations.add(TRANSL_SLUG_UR_JUNAGARHI);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: Urdu (Junagarhi)");
                break;
                
            default:    // 其他语言：默认使用英语译本
                defTranslations.add(TRANSL_SLUG_EN_SAHIH_INTERNATIONAL);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: English (Sahih International) - Default for unsupported language: " + systemLanguage);
                break;
        }
        
        return defTranslations;
    }
    
    /**
     * 🌐 获取默认译本（推荐）：根据用户在应用内设置的语言自动匹配对应的译本
     * 
     * 优先级：
     * 1. 应用内设置的语言（SPAppConfigs.getLocale）
     * 2. 系统语言（作为回退）
     * 
     * 匹配规则：
     * - 印尼语 (id/in) → Kompleks Al Quran Raja Fahd
     * - 英语 (en) → Sahih International
     * - 乌尔都语 (ur) → مولانا محمد جوناگڑهی
     * - 阿拉伯语 (ar) → (未来可添加阿拉伯语Tafsir)
     * - 其他语言 → Sahih International (英语作为默认)
     * 
     * @param context Android Context，用于读取应用语言设置
     * @return 默认译本的 slug 集合
     */
    public static Set<String> defaultTranslationSlugs(Context context) {
        Set<String> defTranslations = new HashSet<>();
        
        // 🔧 优先从应用设置获取语言，确保与用户选择的应用语言同步
        String appLanguage = getAppLanguage(context);
        
        android.util.Log.d("TranslUtils", "🌐 App language: " + appLanguage + " (from SPAppConfigs)");
        
        // 根据应用语言自动选择对应的译本
        switch (appLanguage) {
            case "id":  // 印尼语（应用统一使用 "id"）
                defTranslations.add(TRANSL_SLUG_IN);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: Indonesian (Kompleks Al Quran)");
                break;
                
            case "en":  // 英语
                defTranslations.add(TRANSL_SLUG_EN_SAHIH_INTERNATIONAL);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: English (Sahih International)");
                break;
                
            case "ur":  // 乌尔都语
                defTranslations.add(TRANSL_SLUG_UR_JUNAGARHI);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: Urdu (Junagarhi)");
                break;
                
            case "ar":  // 阿拉伯语
                // 阿拉伯语通常不需要翻译，但可以默认英语作为辅助
                defTranslations.add(TRANSL_SLUG_EN_SAHIH_INTERNATIONAL);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: English (for Arabic speakers)");
                break;
                
            case "ms":  // 马来语
                defTranslations.add(TRANSL_SLUG_MS_ABDULLAH);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: Malay (Abdullah Basmeih)");
                break;
                
            case "tr":  // 土耳其语
                defTranslations.add(TRANSL_SLUG_TR_DIYANET);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: Turkish (Diyanet İşleri)");
                break;
                
            case "bn":  // 孟加拉语
                defTranslations.add(TRANSL_SLUG_BN_TAISIRUL);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: Bengali (Taisirul Quran)");
                break;
                
            default:    // 其他语言：默认使用英语译本
                defTranslations.add(TRANSL_SLUG_EN_SAHIH_INTERNATIONAL);
                android.util.Log.d("TranslUtils", "🌐 Auto-selected translation: English (Sahih International) - Default for unsupported language: " + appLanguage);
                break;
        }
        
        return defTranslations;
    }
    
    /**
     * 🌐 获取应用设置的语言代码
     * 
     * 优先级：
     * 1. 用户在应用内设置的语言（SPAppConfigs）
     * 2. 系统语言（作为回退）
     * 
     * @param context Android Context
     * @return 语言代码 (如: "en", "id", "ur", "ar", 等)
     */
    private static String getAppLanguage(Context context) {
        try {
            if (context != null) {
                // 从 SharedPreferences 读取用户设置的语言
                String savedLanguage = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(context);
                
                if (savedLanguage != null && !savedLanguage.isEmpty()) {
                    android.util.Log.d("TranslUtils", "📱 Using app language from settings: " + savedLanguage);
                    return savedLanguage;
                }
            }
        } catch (Exception e) {
            android.util.Log.w("TranslUtils", "Failed to get app language from settings: " + e.getMessage());
        }
        
        // 回退到系统语言
        String systemLanguage = getSystemLanguage();
        android.util.Log.d("TranslUtils", "🌍 Falling back to system language: " + systemLanguage);
        return systemLanguage;
    }
    
    /**
     * 🌐 获取系统语言代码
     * 
     * @return 系统语言代码 (如: "en", "id", "ur", "zh", "de", 等)
     */
    private static String getSystemLanguage() {
        try {
            // 方法1：使用 Java Locale (最简单可靠)
            String language = java.util.Locale.getDefault().getLanguage();
            
            // 处理 Android 旧标准的印尼语代码
            if ("in".equals(language)) {
                return "id";  // 转换为 ISO 639-1 标准
            }
            
            android.util.Log.d("TranslUtils", "📱 Detected system language: " + language);
            return language;
        } catch (Exception e) {
            android.util.Log.w("TranslUtils", "Failed to get system language: " + e.getMessage());
        }
        
        // 默认返回英语
        return "en";
    }

    public static List<QuranTranslBookInfo> preBuiltTranslBooksInfo() {
        List<QuranTranslBookInfo> translItems = new ArrayList<>();

        String[] enTranslations = {TRANSL_SLUG_EN_SAHIH_INTERNATIONAL, TRANSL_SLUG_EN_THE_CLEAR_QURAN};
        String[] urTranslations = {TRANSL_SLUG_UR_JUNAGARHI};
        String[] inTranslations = {TRANSL_SLUG_IN};

        for (String slug : enTranslations) {
            translItems.add(createPrebuiltTranslBookInfo(slug, "en", "English"));
        }

        for (String slug : urTranslations) {
            translItems.add(createPrebuiltTranslBookInfo(slug, "ur", "Urdu"));
        }

        for (String slug : inTranslations) {
            translItems.add(createPrebuiltTranslBookInfo(slug, "id", "Bahasa Indonesia"));  // 统一使用 "id"
        }

        return translItems;
    }

    private static QuranTranslBookInfo createPrebuiltTranslBookInfo(String slug, String langCode, String langName) {
        QuranTranslBookInfo bookInfo = new QuranTranslBookInfo(slug);
        bookInfo.setLangCode(langCode);
        bookInfo.setLangName(getPrebuiltTranslLangName(slug));
        bookInfo.setBookName(getPrebuiltTranslBookName(slug));
        bookInfo.setAuthorName(getPrebuiltTranslAuthorName(slug));
        bookInfo.setDisplayName(getPrebuiltTranslDisplayName(slug));
        bookInfo.setLangName(langName);
        return bookInfo;
    }

    public static String getPrebuiltTranslLangName(String slug) {
        switch (slug) {
            case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL:
            case TRANSL_SLUG_EN_THE_CLEAR_QURAN:
                return "English";

            case TRANSL_SLUG_UR_JUNAGARHI:
                return "Urdu";

            case TRANSL_SLUG_IN:
                return "Bahasa Indonesia";

            default:
                return "";
        }
    }

    public static String getPrebuiltTranslBookName(String slug) {
        switch (slug) {
            case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL:
                return "Sahih International";

            case TRANSL_SLUG_EN_THE_CLEAR_QURAN:
                return "The Clear Quran";

            case TRANSL_SLUG_UR_JUNAGARHI:
                return "مولانا محمد جوناگڑھی";

            case TRANSL_SLUG_IN:
                return "Kompleks Al Quran";

            default:
                return "";
        }
    }

    public static String getPrebuiltTranslAuthorName(String slug) {
        switch (slug) {
            case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL:
                return "Saheeh International";

            case TRANSL_SLUG_EN_THE_CLEAR_QURAN:
                return "Dr. Mustafa Khattab";

            case TRANSL_SLUG_UR_JUNAGARHI:
                return "مولانا محمد جوناگڑھی";

            case TRANSL_SLUG_IN:
                return "Kompleks Al Quran Raja Fahd";

            default:
                return "";
        }
    }

    public static String getPrebuiltTranslDisplayName(String slug) {
        switch (slug) {
            case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL:
                return "Sahih International";

            case TRANSL_SLUG_EN_THE_CLEAR_QURAN:
                return "The Clear Quran";

            case TRANSL_SLUG_UR_JUNAGARHI:
                return "مولانا محمد جوناگڑھی";

            case TRANSL_SLUG_IN:
                return "Kompleks Al Quran";

            default:
                return "";
        }
    }

    public static String getPrebuiltTranslInfoPath(String slug) {
        switch (slug) {
            case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL:
                return "prebuilt_translations/en_saheeh_v1_1_1/manifest.json";
            case TRANSL_SLUG_UR_JUNAGARHI:
                return "prebuilt_translations/ur_junagarhi/manifest.json";
            case TRANSL_SLUG_EN_THE_CLEAR_QURAN:
                return "prebuilt_translations/en_the_clear_quran/manifest.json";
            case TRANSL_SLUG_IN:
                return "prebuilt_translations/in/manifest.json";
        }
        return null;
    }

    public static String getPrebuiltTranslPath(String slug) {
        switch (slug) {
            case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL:
                return "prebuilt_translations/en_saheeh_v1_1_1/en_saheeh_v1_1_1.json";
            case TRANSL_SLUG_UR_JUNAGARHI:
                return "prebuilt_translations/ur_junagarhi/ur_junagarhi.json";
            case TRANSL_SLUG_EN_THE_CLEAR_QURAN:
                return "prebuilt_translations/en_the_clear_quran/en_the_clear_quran.json";
            case TRANSL_SLUG_IN:
                return "prebuilt_translations/in/in_quran-complex.json";
        }
        return null;
    }

    public static boolean isPrebuilt(String slug) {
        switch (slug) {
            case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL:
                /*case TRANSL_SLUG_EN_HILALI_KHAN:*/
            case TRANSL_SLUG_UR_JUNAGARHI:
            case TRANSL_SLUG_EN_THE_CLEAR_QURAN:
            case TRANSL_SLUG_IN:
                return true;
        }
        return false;
    }

    public static boolean isUrdu(String slug) {
        return Objects.equals(slug.split("_")[0], "ur");
    }

    public static boolean isTransliteration(String slug) {
        return slug.contains(TRANSL_TRANSLITERATION_SLUG_PART);
    }

    private static String prepareTranslDirPathForSpecificLangNSlug(String langCode, String translSlug) {
        return FileUtils.createPath(langCode, translSlug);
    }

    public static String prepareTranslInfoPathForSpecificLangNSlug(String langCode, String translSlug) {
        String path2TranslDir = prepareTranslDirPathForSpecificLangNSlug(langCode, translSlug);
        return FileUtils.createPath(path2TranslDir, TRANSL_INFO_FILE_NAME);
    }

    public static String prepareTranslPathForSpecificLangNSlug(int translId, String langCode, String translSlug) {
        String path2TranslDir = prepareTranslDirPathForSpecificLangNSlug(langCode, translSlug);
        String filename = String.format(TRANSL_FILE_NAME_FORMAT, translId, langCode, translSlug);
        return FileUtils.createPath(path2TranslDir, filename);
    }

    @Nullable
    public static List<Pair<QuranTranslBookInfo, File>> getTranslInfosAndFilesForMigration(FileUtils fileUtils, File translDir) throws Exception {
        File[] dirsOfLangCodes = translDir.listFiles();
        if (dirsOfLangCodes == null || dirsOfLangCodes.length == 0) {
            Log.d("Nothing was found, deleting root translation directory: " + translDir.getName());
            FilesKt.deleteRecursively(translDir);
            return null;
        }

        List<Pair<QuranTranslBookInfo, File>> translInfosAndFiles = new ArrayList<>();
        for (File langCodeDir : dirsOfLangCodes) {
            File[] translFiles = langCodeDir.listFiles();
            if (translFiles == null || translFiles.length == 0) {
                Log.d("Deleting language directory with its contents: " + langCodeDir.getName());
                FilesKt.deleteRecursively(langCodeDir);
                continue;
            }

            for (File singleTranslDir : translFiles) {
                if (!singleTranslDir.isDirectory()) {
                    FilesKt.deleteRecursively(singleTranslDir);
                    continue;
                }

                try {
                    File infoJSONFile = new File(singleTranslDir, TRANSL_INFO_FILE_NAME);
                    Pair<QuranTranslBookInfo, File> pair = readTranslInfoFromJSONFile(fileUtils, infoJSONFile);
                    if (pair == null) {
                        Logger.print(
                            "Deleting translation directory with its manifest and data files: " + singleTranslDir.getName());
                        FilesKt.deleteRecursively(singleTranslDir);
                        continue;
                    }

                    translInfosAndFiles.add(pair);
                } catch (Exception e) {
                    Logger.print(
                        "Error occurred, deleting translation directory with its manifest and data files dire: " + singleTranslDir.getName());
                    FilesKt.deleteRecursively(singleTranslDir);
                    e.printStackTrace();
                }
            }
        }
        return translInfosAndFiles;
    }

    // For migrating file base translations to database.
    private static Pair<QuranTranslBookInfo, File> readTranslInfoFromJSONFile(FileUtils fileUtils, File infoJSONFile) throws JSONException, IOException {
        if (!infoJSONFile.isFile()) {
            return null;
        }

        String json = FilesKt.readText(infoJSONFile, Charsets.UTF_8);
        JSONObject jsonObject = new JSONObject(json);

        String slug = jsonObject.optString("slug", "");

        if (isPrebuilt(slug)) return null;

        int id = jsonObject.optInt("id");
        String langCode = jsonObject.optString("lang-code", "");

        File translFile = fileUtils.getSingleTranslationFile(id, langCode, slug);
        if (!translFile.exists() || translFile.length() == 0) {
            Log.d("Translation file does not exist or is empty.");
            return null;
        }

        QuranTranslBookInfo bookInfo = new QuranTranslBookInfo(slug);
        bookInfo.setLangCode(langCode);
        bookInfo.setBookName(jsonObject.optString("book", ""));
        bookInfo.setAuthorName(jsonObject.optString("author", ""));
        bookInfo.setLangName(jsonObject.optString("lang-name", ""));
        bookInfo.setDisplayName(jsonObject.optString("display-name", ""));
        bookInfo.setLastUpdated(1636309799000L /* 2021-11-07 23:59 */);
        return new Pair<>(bookInfo, translFile);
    }

    public static boolean resolveSelectionChange(Context ctx, Set<String> slugSet, TranslModel model, boolean isSelected, boolean saveToSP) {
        String slug = model.getBookInfo().getSlug();
        if (isSelected) {
            if (slugSet.size() >= TRANSL_MAX_SELECTION_LIMIT) {
                String msg = ctx.getString(R.string.strMsgTranslSelectionLimit, TRANSL_MAX_SELECTION_LIMIT);
                String btn = ctx.getString(R.string.strLabelGotIt);
                MessageUtils.popMessage(ctx, ctx.getString(R.string.strTitleInfo), msg, btn, null);
                return false;
            }
            slugSet.add(slug);
        } else {
            slugSet.remove(slug);
        }

        if (saveToSP) {
            SPReader.setSavedTranslations(ctx, slugSet);
        }

        return true;
    }
}
