package com.bible.tools.utils

import android.content.Context
import android.content.res.AssetManager
import com.blankj.utilcode.util.EncodeUtils
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

object FileUtil {
    const val TAG = "FileUtil"
    private const val BIBLE_ZIP_PW = "a2p2MjAyMw=="

    fun unZipBible(context: Context, fileName: String) {
        try {
            val startUnzipTime = System.currentTimeMillis()
            val password = String(EncodeUtils.base64Decode(BIBLE_ZIP_PW))
            val manager: AssetManager = context.getAssets()
            val OUTPUT_FOLDER: String = context.filesDir.toString()
            // create output directory if not exists
            val fileNameSplit = fileName.split("/").last().split(".").first()
            val folder = File("$OUTPUT_FOLDER/$fileNameSplit")
            if (folder.exists()) {
                // zip 解压完整文件数量是 68 个
                val isAllFile = folder.listFiles()?.filter { it.isFile }?.size == 68
                if (isAllFile) {
                    // 解压过不解压了
                    return
                }
            } else {
                val zipFile = File("$OUTPUT_FOLDER/$fileName")
                copyAssetsToDst(manager, fileName, zipFile)
                val zip = ZipFile("$OUTPUT_FOLDER/$fileName")
                if (zip.isEncrypted) {
                    if (password.isNotEmpty()) {
                        zip.setPassword(password.toCharArray())
                    }
                }
                zip.extractAll(OUTPUT_FOLDER)
                zipFile.delete()
                if (folder.exists()) {
                    // zip 解压完整文件数量是 68 个
                    val size = folder.listFiles()?.filter { it.isFile }?.size?:0
                    if (size != 68) {
                       // FirebaseCrashlytics.getInstance().recordException(NullPointerException("un zip fail, count = $size"))
                    }
                } else {
                  //  FirebaseCrashlytics.getInstance().recordException(NullPointerException("un zip fail, this file is empty"))
                }

            }
//            ReportHelper.uploadData(
//                Report.Builder()
//                    .refer((System.currentTimeMillis() - startUnzipTime).toString())
//                    .actionParam("bible")
//                    .action("unzip_time")
//                    .build()
//            )
        } catch (e: Exception) {
//            FirebaseCrashlytics.getInstance().recordException(e)
//            ReportHelper.uploadData(
//                Report.Builder()
//                    .actionParam("bible")
//                    .action("unzip_fail")
//                    .build()
//            )
        }
    }

    fun copyFile(context: Context, fileName: String) {
        val manager: AssetManager = context.getAssets()
        val OUTPUT_FOLDER: String = context.filesDir.toString()
        copyAssetsToDst(manager, fileName, File("$OUTPUT_FOLDER/$fileName"))
    }

    fun unzip2(context: Context, fileName: String, password: String) {
        val OUTPUT_FOLDER: String = context.filesDir.toString()
        val zip = ZipFile("$OUTPUT_FOLDER/$fileName")
        if (zip.isEncrypted) {
            if (password.isNotEmpty()) {
                zip.setPassword(password.toCharArray())
            }
        }
        zip.extractAll(OUTPUT_FOLDER)
    }

    fun copyAssetsToDst(assetManager: AssetManager, srcPath: String, outFile: File): Boolean {
        var input: InputStream? = null
        var fos: FileOutputStream? = null
        return try {
            input = assetManager.open(srcPath)
            fos = FileOutputStream(outFile)
            val buffer = ByteArray(1024)
            var byteCount: Int
            while (input.read(buffer).also { byteCount = it } != -1) {
                fos.write(buffer, 0, byteCount)
            }
            fos.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            try {
                input?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}