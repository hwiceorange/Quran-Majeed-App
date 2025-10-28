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

    fun getQuestionStr(): String {
        val isIndonesian = AppConfig.isIDLan()
        val planFileName = if (isIndonesian) {
            "quiz_all_id"
        }else {
            "quiz_all_en"
        }
        val readPath = "${saveRootPath}${File.separator}quiz${File.separator}$planFileName"
        
        android.util.Log.d("QuestionTools", "🔍 getQuestionStr:")
        android.util.Log.d("QuestionTools", "  - 是否印尼语: $isIndonesian")
        android.util.Log.d("QuestionTools", "  - 文件名: $planFileName")
        android.util.Log.d("QuestionTools", "  - 完整路径: $readPath")
        android.util.Log.d("QuestionTools", "  - 文件是否存在: ${FileUtils.isFileExists(readPath)}")
        
        if (FileUtils.isFileExists(readPath)) {
            val content = FileIOUtils.readFile2String(readPath)
            android.util.Log.d("QuestionTools", "  ✅ 成功读取题目文件，内容长度: ${content.length}")
            return content
        }
        
        android.util.Log.e("QuestionTools", "  ❌ 题目文件不存在！")
        return ""
    }

    private val saveRootPath by lazy {
        PathUtils.getInternalAppFilesPath()
    }

    /**
     * quiz 解压缩到 file 目录
     * 存放地址:data/data/package/file/quiz
     */
    fun unZipBibleQuiz() {
        try {
            val verifyFilePath = "${saveRootPath}${File.separator}quiz${File.separator}quiz_all_en"
            val fileExists = FileUtils.isFileExists(verifyFilePath)
            if (fileExists) {
                //如果已经存在说明已经解压，不再进行解压操作
                return
            }
            val assetsInput = Utils.getApp().assets.open("quiz")
            val filePath = "${saveRootPath}${File.separator}quiz.zip"
            val saveFile = File(filePath)
            val copySuccess = FileIOUtils.writeFileFromIS(saveFile, assetsInput)
            if (copySuccess) {
               // val password = String(EncodeUtils.base64Decode(BIBLE_QUIZ_ZIP_PW))
                val zipFile = ZipFile(filePath, null)
                zipFile.extractAll(saveRootPath)
                if (FileUtils.isFileExists(filePath)) {
                    FileUtils.delete(filePath)
                }
            }
        } catch (e: Exception) {
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