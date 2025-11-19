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
            if (fileExists) {
                android.util.Log.d("QuestionTools", "✅ Quiz files already extracted, skipping")
                return
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