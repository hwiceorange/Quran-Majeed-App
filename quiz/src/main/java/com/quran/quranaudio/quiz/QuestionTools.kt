package com.quranaudio.quiz.quiz

import com.quran.quranaudio.quiz.utils.AppConfig
import com.blankj.utilcode.util.EncodeUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.Utils
import net.lingala.zip4j.ZipFile
import java.io.File
import java.lang.Exception

object QuestionTools {
    // kjv2023quiz
    private const val BIBLE_QUIZ_ZIP_PW = "a2p2MjAyM3F1aXo="

    /**
     * 题库内容版本。每次更新 assets/quiz.zip 里的题目内容都要 +1，
     * 触发老装机强制覆盖解压（否则 unZipBibleQuiz 见到旧文件存在就跳过，新题库上不了线）。
     * v2 = 精编分层题库（易→难，意义/主题/故事/词汇/填空，覆盖多章）。
     * v3 = 第1关开场题重排(礼拜/常念，温暖易答) + 去冗余(Ummul-Kitab/Maliki填空)。
     * v4 = densify：常读短章补深度题(63→90, 30关)，6章≥5题以支撑情境化。
     * v5 = 续densify(90→108,36关,25章) + 页面改进配套。
     * v6 = 印尼语(id)多语言适配：108题精编库译为印尼语。
     * v7 = 阿拉伯语(ar)多语言：108题译为阿语，27填空题经文逐字取自App自带乌斯玛尼源。
     * v8 = 乌尔都语(ur)多语言：108题，填空经文取自源，界面提示乌尔都语。
     */
    private const val CONTENT_VERSION = 8
    private const val SP_KEY_CONTENT_VERSION = "quiz_content_version"

    /**
     * 根据应用语言获取题目内容
     * - 英语 (en): quiz_all_en.txt
     * - 印尼语 (id/in): quiz_all_id.txt
     * - 阿拉伯语 (ar): quiz_all_ar.txt
     * - 其他语言: 默认使用英语
     */
    fun getQuestionStr(): String {
        // 🔧 每次调用时重新读取语言设置，确保语言切换后能及时更新
        AppConfig.setLanguage()
        
        val appLanguage = AppConfig.lan
        val planFileName = getQuizFileNameByLanguage(appLanguage)
        val readPath = "${saveRootPath}${File.separator}quiz${File.separator}$planFileName"
        
        android.util.Log.d("QuestionTools", "═════════════════════════════════════")
        android.util.Log.d("QuestionTools", "🔍 getQuestionStr() 调用")
        android.util.Log.d("QuestionTools", "  - 应用语言: $appLanguage")
        android.util.Log.d("QuestionTools", "  - 题目文件: $planFileName")
        android.util.Log.d("QuestionTools", "  - 完整路径: $readPath")
        android.util.Log.d("QuestionTools", "  - 文件是否存在: ${FileUtils.isFileExists(readPath)}")
        
        if (FileUtils.isFileExists(readPath)) {
            val content = FileIOUtils.readFile2String(readPath)
            android.util.Log.d("QuestionTools", "  ✅ 成功读取题目文件，内容长度: ${content.length}")
            android.util.Log.d("QuestionTools", "  - 内容前100字符: ${content.take(100)}")
            android.util.Log.d("QuestionTools", "═════════════════════════════════════")
            return content
        }
        
        android.util.Log.e("QuestionTools", "  ❌ 题目文件不存在！")
        android.util.Log.d("QuestionTools", "═════════════════════════════════════")
        return ""
    }
    
    /**
     * 根据语言代码获取题目文件名
     * @param languageCode 语言代码 (en, id, ar, etc.)
     * @return 题目文件名 (包含扩展名.txt)
     */
    private fun getQuizFileNameByLanguage(languageCode: String): String {
        return when (languageCode) {
            "id", "in" -> "quiz_all_id.txt"  // 印尼语
            "ar" -> "quiz_all_ar.txt"         // 阿拉伯语
            "ur" -> "quiz_all_ur.txt"         // 乌尔都语
            else -> "quiz_all_en.txt"          // 英语（默认）
        }
    }

    private val saveRootPath by lazy {
        PathUtils.getInternalAppFilesPath()
    }

    /**
     * quiz 解压缩到 file 目录
     * 存放地址:data/data/package/file/quiz
     */
    fun unZipBibleQuiz() {
        android.util.Log.d("QuestionTools", "📦 Starting unZipBibleQuiz...")
        try {
            val verifyFilePath = "${saveRootPath}${File.separator}quiz${File.separator}quiz_all_en.txt"
            android.util.Log.d("QuestionTools", "🔍 Checking if quiz already extracted: $verifyFilePath")

            val fileExists = FileUtils.isFileExists(verifyFilePath)
            val extractedVersion = com.quran.quranaudio.quiz.extension.SPTools.getInt(SP_KEY_CONTENT_VERSION, 1)
            if (fileExists && extractedVersion >= CONTENT_VERSION) {
                android.util.Log.d("QuestionTools", "✅ Quiz files already extracted (v$extractedVersion), skipping")
                return
            }
            if (fileExists) {
                // 内容版本升级：清掉旧题库目录，确保新题库覆盖生效
                android.util.Log.d("QuestionTools", "♻️ Content version $extractedVersion -> $CONTENT_VERSION, re-extracting")
                FileUtils.delete("${saveRootPath}${File.separator}quiz")
            }

            android.util.Log.d("QuestionTools", "📂 Quiz not found, starting extraction...")
            android.util.Log.d("QuestionTools", "   Save root path: $saveRootPath")
            
            // List available assets for debugging
            try {
                val assets = Utils.getApp().assets.list("")
                android.util.Log.d("QuestionTools", "📋 Available assets: ${assets?.joinToString(", ")}")
            } catch (e: Exception) {
                android.util.Log.w("QuestionTools", "Could not list assets: ${e.message}")
            }
            
            android.util.Log.d("QuestionTools", "📥 Opening quiz.zip from assets...")
            val assetsInput = Utils.getApp().assets.open("quiz.zip")
            android.util.Log.d("QuestionTools", "✅ quiz.zip opened successfully")
            
            val filePath = "${saveRootPath}${File.separator}quiz.zip"
            android.util.Log.d("QuestionTools", "💾 Copying to: $filePath")
            val saveFile = File(filePath)
            val copySuccess = FileIOUtils.writeFileFromIS(saveFile, assetsInput)
            
            if (copySuccess) {
                android.util.Log.d("QuestionTools", "✅ quiz.zip copied successfully")
                android.util.Log.d("QuestionTools", "🗜️ Extracting zip file...")
                val zipFile = ZipFile(filePath, null)
                zipFile.extractAll(saveRootPath)
                android.util.Log.d("QuestionTools", "✅ Extraction completed")
                // 记录已解压的内容版本，供下次启动判断是否需要覆盖更新
                com.quran.quranaudio.quiz.extension.SPTools.put(SP_KEY_CONTENT_VERSION, CONTENT_VERSION)
                
                // Verify extraction
                val extractedFiles = File("${saveRootPath}${File.separator}quiz").listFiles()
                android.util.Log.d("QuestionTools", "📁 Extracted files: ${extractedFiles?.map { it.name }?.joinToString(", ")}")
                
                if (FileUtils.isFileExists(filePath)) {
                    FileUtils.delete(filePath)
                    android.util.Log.d("QuestionTools", "🗑️ Cleaned up temporary zip file")
                }
                
                android.util.Log.d("QuestionTools", "🎉 unZipBibleQuiz completed successfully")
            } else {
                android.util.Log.e("QuestionTools", "❌ Failed to copy quiz.zip from assets")
            }
        } catch (e: Exception) {
            android.util.Log.e("QuestionTools", "❌ Exception in unZipBibleQuiz: ${e.message}", e)
            e.printStackTrace()
//            FirebaseCrashlytics.getInstance().recordException(e)
//            ReportHelper.uploadData(
//                Report.Builder()
//                    .actionParam("quiz")
//                    .action("unzip_fail")
//                    .build()
//            )
        }
    }

}